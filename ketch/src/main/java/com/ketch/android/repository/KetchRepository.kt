package com.ketch.android.repository

import android.content.Context
import com.google.gson.Gson
import com.ketch.android.api.ApiUtils
import com.ketch.android.api.KetchApiClient
import com.ketch.android.api.MigrationOption
import com.ketch.android.api.OtherError
import com.ketch.android.api.RequestError
import com.ketch.android.api.Result
import com.ketch.android.api.flatMapSuccess
import com.ketch.android.api.mapWithResult
import com.ketch.android.api.model.ApiConsentStatus
import com.ketch.android.api.model.BootstrapConfiguration
import com.ketch.android.api.model.Cacheable
import com.ketch.android.api.model.Configuration
import com.ketch.android.model.ConsentStatus
import com.ketch.android.api.model.Environment
import com.ketch.android.api.model.GetConsentStatusBody
import com.ketch.android.api.model.GetConsentStatusResponse
import com.ketch.android.api.model.InvokeRightsBody
import com.ketch.android.api.model.UpdateConsentStatusBody
import com.ketch.android.api.toDomain
import com.ketch.android.cache.CacheProvider
import com.ketch.android.location.LocationUtils
import com.ketch.android.model.UserData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform

/**
 * Main class for accessing ketch resources
 * Has private constructor as it's creation should be done only with Builder
 * @param organizationCode
 * @param applicationCode
 * @param context — Android context repository should work within
 * @param cacheProvider — component that provides caching functionality. If not set, no caching will be performed
 */
class KetchRepository internal constructor(
    private val organizationCode: String,
    private val applicationCode: String,
    private val context: Context?,
    private val cacheProvider: CacheProvider?,
    private val client: KetchApiClient = KetchApiClient()
) {
    /**
     * Collection of base service URLs received as a part of bootstrap json and needed to form other
     * ketch endpoints URLs
     */
    private var serviceUrls: Map<String, String>? = null

    private val gson = Gson()

    /**
     * Retrieves bootstrap configuration needed for full config
     * Uses organizationCode and applicationCode to form a full URL
     * Result will be cached for each unique pair of organizationCode and applicationCode
     * In case of response fail previously cached data will be allegedly returned as Result.Success
     * @return Flow with Result with {@link BootstrapConfiguration} if successful and with an error if request or its handling failed
     */
    fun getBootstrapConfiguration(): Flow<Result<RequestError, BootstrapConfiguration>> {
        return flow {
            emit(
                client.getApiService().getBootstrapConfiguration(
                    organizationCode,
                    applicationCode
                )
            )
        }
            .mapWithResult { it }
            .catch { e ->
                emit(Result.fail(OtherError(e)))
            }
            .wrapWithCache(BootstrapConfiguration::class.java, organizationCode, applicationCode)
            .fallbackWithCache(BootstrapConfiguration::class.java, organizationCode, applicationCode)
            .onEach { result ->
                if (result is Result.Success) {
                    // save received service URLs to use them for further requests
                    serviceUrls = result.value.services
                }
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * Retrieves full configuration data
     * Should be used if location latitude and longitude are provided
     * Caching is performed in {@see getConfigurationFlow} method
     * Tries to determine locationCode based on provided coordinates using Android components. If fails, fallbacks to using of ketch resources
     * @param configuration bootstrap configuration
     * @param environment environment value that should match one of environment patterns
     * @param languageCode current locale code (e.g. en_US)
     * @param latitude
     * @param longitude
     * @return Flow of Result with {@link Configuration} if successful and with an error if request or its handling failed
     */
    fun getFullConfiguration(
        configuration: BootstrapConfiguration,
        environment: String,
        languageCode: String,
        latitude: Double,
        longitude: Double
    ): Flow<Result<RequestError, Configuration>> =
        flow {
            emit(
                context?.let {
                    LocationUtils.getLocationCode(latitude, longitude, it)
                }
            )
        }
            .catch {
                emit(null)
            }
            .flatMapLatest { locationCode ->
                // if locationCode is successfully determined, use it to form a final getFullConfiguration request
                // otherwise use ketch ASTROLABE endpoint to get location code first
                locationCode?.let {
                    getConfigurationFlow(
                        configuration = configuration,
                        scope = configuration.policyScope?.getRecommendedScope(locationCode.getGeolocationCode()),
                        environment = environment,
                        languageCode = languageCode
                    )
                } ?: getFullConfiguration(
                    configuration = configuration,
                    environment = environment,
                    languageCode = languageCode)
            }
            .flowOn(Dispatchers.IO)

    /**
     * Retrieves full configuration data
     * Should be used if location latitude and longitude are absent or Android component failed to get location code
     * Caching is performed in {@see getConfigurationFlow} method
     * Tries to determine locationCode using ketch ASTROLABE resource
     * @param configuration bootstrap configuration
     * @param environment environment value that should match one of environment patterns
     * @param languageCode current locale code (e.g. en_US)
     * @return Flow of Result with {@link Configuration} if successful and with an error if request or its handling failed
     */
    fun getFullConfiguration(
        configuration: BootstrapConfiguration,
        environment: String,
        languageCode: String
    ): Flow<Result<RequestError, Configuration>> =
        flow {
            emit(
                client.getApiService(serviceUrls?.get(ServiceNames.ASTROLABE.value)).getGeolocation()
            )
        }
            .mapWithResult { it.getGeolocationCode().orEmpty() }
            .catch { e ->
                emit(Result.fail(OtherError(e)))
            }
            .flatMapLatest { locationResult ->
                val locationCode = when (locationResult) {
                    is Result.Success -> locationResult.value
                    is Result.Error -> null
                }
                val scope = configuration.policyScope?.getRecommendedScope(locationCode)
                getConfigurationFlow(
                    configuration, scope, environment, languageCode
                )
            }
            .catch { e ->
                emit(Result.fail(OtherError(e)))
            }
            .flowOn(Dispatchers.IO)

    /**
     * Retrieves full configuration data
     * Uses organizationCode, applicationCode, environment, policyScopeCode and languageCode to form a full URL
     * Result will be cached for each unique set of  of organizationCode, applicationCode, environment, scope and languageCode
     * In case of response fail previously cached data will be allegedly returned as Result.Success
     * @return Flow of Result with {@link BootstrapConfiguration} if successful and with an error if request or its handling failed
     */
    private fun getConfigurationFlow(
        configuration: BootstrapConfiguration,
        scope: String?,
        environment: String,
        languageCode: String
    ): Flow<Result<RequestError, Configuration>> {
        val chosenEnvironment: Environment? = configuration.environments?.firstOrNull {
            it.matchPattern(environment)
        } ?: configuration.getDefaultEnvironment()
        return flow {
            emit(
                client.getApiService(serviceUrls?.get(ServiceNames.SUPERCARGO.value))
                    .getFullConfiguration(
                        organizationCode = organizationCode,
                        applicationCode = applicationCode,
                        environmentCode = chosenEnvironment?.code,
                        environmentHash = chosenEnvironment?.hash,
                        policyScopeCode = scope,
                        languageCode = languageCode
                    )
            )
        }
            .mapWithResult { it }
            .catch { e ->
                emit(Result.fail(OtherError(e)))
            }
            .wrapWithCache(
                Configuration::class.java,
                organizationCode,
                applicationCode,
                environment,
                scope,
                languageCode
            )
            .fallbackWithCache(
                Configuration::class.java,
                organizationCode,
                applicationCode,
                environment,
                scope,
                languageCode
            )
            .flowOn(Dispatchers.IO)
    }

    /**
     * Retrieves currently set consent status
     * Uses organizationCode to form a full URL
     * Result will be cached for each unique set of  of organizationCode, applicationCode, environment, identities and purposes
     * @param configuration full configuration
     * @param identities map of identityCodes and identityValues. Keys and values shouldn't be null
     * @param purposes map of activity names and names of legalBasisCode of this activity. Keys and values shouldn't be null
     * @return Flow of Result with Map<String, ConsentStatus>> if successful and with an error if request or its handling failed
     */
    fun getConsentStatus(
        configuration: Configuration,
        identities: Map<String, String>,
        purposes: Map<String, String>
    ): Flow<Result<RequestError, Map<String, ConsentStatus>>> =
        flow {
            val body = GetConsentStatusBody(
                applicationCode = applicationCode,
                applicationEnvironmentCode = configuration.environment?.code,
                identities = ApiUtils.constructIdentities(organizationCode, identities),
                purposes = GetConsentStatusBody.constructPurposes(
                    purposes
                )
            )
            emit(
                client.getApiService(serviceUrls?.get(ServiceNames.WHEELHOUSE.value)).getConsentStatus(
                    organizationCode = organizationCode,
                    getConsentStatusBody = body
                )
            )
        }
            .mapWithResult { it }
            .catch { e ->
                emit(Result.fail(OtherError(e)))
            }
            .wrapWithCache(
                GetConsentStatusResponse::class.java,
                organizationCode,
                applicationCode,
                configuration.environment?.code,
                identities,
                purposes
            )
            .fallbackWithCache(
                GetConsentStatusResponse::class.java,
                organizationCode,
                applicationCode,
                configuration.environment?.code,
                identities,
                purposes
            )
            .flatMapSuccess { getConsentStatusResponse ->
                // as far as server response doesn't contain all needed information client awaits,
                // successful result should be merged with input parameter purposes to form a proper value to return
                flow {
                    val activities: Map<String, ApiConsentStatus>? =
                        getConsentStatusResponse.purposes
                    activities?.let {
                        emit(
                            Result.succeed(
                                activities.map { activity ->
                                    activity.key to activity.value.copy(
                                        legalBasisCode = purposes[activity.key]
                                    ).toDomain()
                                }
                                    .filter {
                                        it.second.legalBasisCode != null
                                    }
                                    .toMap()
                            )
                        )
                    } ?: emit(Result.succeed(emptyMap()))
                }
            }
            .catch { e ->
                emit(Result.fail(OtherError(e)))
            }
            .flowOn(Dispatchers.IO)

    /**
     * Sends a request for updating consent status
     * Uses organizationCode to form a full URL
     * @param configuration full configuration
     * @param identities map of identityCodes and identityValues. Keys and values shouldn't be null
     * @param consents map of consent names and information if this particular legalBasisCode should be allowed or not
     * @param migrationOption rule that represents how updating should be performed
     * @return Flow of Result.Success if successful and with an error if request or its handling failed
     */
    fun updateConsentStatus(
        configuration: Configuration,
        identities: Map<String, String>,
        consents: Map<String, ConsentStatus>,
        migrationOption: MigrationOption
    ): Flow<Result<RequestError, Unit>> =
        flow {
            val body = UpdateConsentStatusBody(
                applicationCode = applicationCode,
                applicationEnvironmentCode = configuration.environment?.code,
                identities = ApiUtils.constructIdentities(
                    organizationCode,
                    identities
                ),
                policyScopeCode = configuration.policyScope?.code,
                purposes = UpdateConsentStatusBody.constructPurposes(
                    consents
                ),
                migrationOption = migrationOption.value
            )
            emit(
                client.getApiService(serviceUrls?.get(ServiceNames.WHEELHOUSE.value)).updateConsentStatus(
                    organizationCode = organizationCode,
                    updateConsentStatusBody = body
                )
            )
        }
            .mapWithResult { it }
            .catch { e ->
                emit(Result.fail(OtherError(e)))
            }
            .flowOn(Dispatchers.IO)

    /**
     * Sends a request for invoking rights
     * Uses organizationCode to form a full URL
     * @param configuration full configuration
     * @param identities map of identityCodes and identityValues. Keys and values shouldn't be null
     * @param userData consists user information like email
     * @param rights list of strings of rights. Rights shouldn't bu null
     * @return Flow of Result.Success if successful and with an error if request or its handling failed
     */
    fun invokeRights(
        configuration: Configuration,
        identities: Map<String, String>,
        userData: UserData,
        rights: List<String>
    ): Flow<Result<RequestError, Unit>> =
        flow {
            val body = InvokeRightsBody(
                applicationCode = applicationCode,
                applicationEnvironmentCode = configuration.environment?.code,
                identities = ApiUtils.constructIdentities(
                    organizationCode,
                    identities
                ),
                policyScopeCode = configuration.policyScope?.code,
                rightsEmail = userData.email,
                rightCodes = rights
            )
            emit(
                client.getApiService(serviceUrls?.get(ServiceNames.GANGPLANK.value)).invokeRights(
                    organizationCode = organizationCode,
                    invokeRightsBody = body
                )
            )
        }
            .mapWithResult { it }
            .catch { e ->
                emit(Result.fail(OtherError(e)))
            }
            .flowOn(Dispatchers.IO)

    /**
     * Tries to store {@link Cacheable} object from the upstream flow if result is successful
     * and cacheProvider was set during repository configuration
     * Uses set of params to generate a unique name for a request to cache its result
     * @param type class of the object that needs to be cached
     * @param params set of objects that represent each request
     */
    private fun <T : Cacheable> Flow<Result<RequestError, T>>.wrapWithCache(type: Class<*>, vararg params: Any?): Flow<Result<RequestError, T>> =
        onEach { result ->
            if (result is Result.Success) {
                val data = result.value
                cacheProvider?.store(
                    "${type.simpleName}_${cacheProvider.generateSalt(*params)}",
                    gson.toJson(data.cacheableCopy(System.currentTimeMillis()))
                )
            }
        }

    /**
     * Tries to retrieve previoгsly cached object if result is not successful
     * and cacheProvider was set during repository configuration
     * Otherwise initial error will be passed to downstream flow
     * Uses set of params to generate a unique name for a request to cache its result
     * @param type class of the object that needs to be cached
     * @param params set of objects that represent each request
     */
    private fun <T : Any> Flow<Result<RequestError, T>>.fallbackWithCache(type: Class<*>, vararg params: Any?): Flow<Result<RequestError, T>> =
        transform { result ->
            if (result is Result.Error) {
                println("hash ${type.simpleName}_${cacheProvider?.generateSalt(*params)}")
                cacheProvider?.obtain("${type.simpleName}_${cacheProvider.generateSalt(*params)}")?.let {
                    println(gson.fromJson(it, type) as T)
                    emit(Result.succeed(gson.fromJson(it, type) as T))
                } ?: emit(result)
            } else
                emit(result)
        }

    private enum class ServiceNames(val value: String) {
        ASTROLABE("astrolabe"),
        GANGPLANK("gangplank"),
        SUPERCARGO("supercargo"),
        WHEELHOUSE("wheelhouse"),
    }

    /**
     * Builder class to config ketchRepository. Repository should be created only with this class
     */
    class Builder() {
        private var organizationCode: String? = null
        private var applicationCode: String? = null
        private var cacheProvider: CacheProvider? = null
        private var context: Context? = null

        fun organizationCode(code: String): Builder = this.apply { organizationCode = code }

        fun applicationCode(code: String): Builder = this.apply { applicationCode = code }

        fun cacheProvider(provider: CacheProvider): Builder = this.apply { cacheProvider = provider }

        fun context(ctx: Context): Builder = this.apply { context = ctx }

        fun build(): KetchRepository =
            if (organizationCode == null || applicationCode == null) {
                throw IllegalStateException("Organization and application codes must not be null")
            } else {
                KetchRepository(
                    organizationCode = organizationCode!!,
                    applicationCode = applicationCode!!,
                    context = context,
                    cacheProvider = cacheProvider
                )
            }
    }
}
