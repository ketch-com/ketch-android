package com.ketch.android.repository

import android.content.Context
import com.google.gson.Gson
import com.ketch.android.api.*
import com.ketch.android.api.adapter.ConfigurationDataAdapter
import com.ketch.android.api.model.*
import com.ketch.android.cache.CacheProvider
import com.ketch.android.model.Consent
import com.ketch.android.model.UserDataV2
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import mobile.MobileOuterClass

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
    private val client: MobileClient = MobileClient(context!!)
) {

    private val gson = Gson()

    fun getConfigurationProto(
        environment: String,
        countryCode: String,
        languageCode: String,
        regionCode: String = "",
        IP: String = ""
    ): Flow<Result<RequestError, ConfigurationV2>> {
        return flow {
            val blockingStub = client.blockingStub
            val request = MobileOuterClass.GetConfigurationRequest.newBuilder()
                .setOrganizationCode(organizationCode)
                .setApplicationCode(applicationCode)
                .setApplicationEnvironmentCode(environment)
                .setLanguageCode(languageCode)
                .setCountryCode(countryCode)
                .setRegionCode(regionCode)
                .setIP("")
                .build()

            val response = blockingStub.getConfiguration(request)
            emit(response)
        }
            .map { ConfigurationDataAdapter().toModel(it) }
            .map<ConfigurationV2, Result<RequestError, ConfigurationV2>> {
                Result.succeed(it)
            }
            .handleErrors()
            .wrapWithCache(
                ConfigurationV2::class.java,
                ConfigurationV2.version,
                organizationCode,
                applicationCode,
                environment,
                languageCode
            )
            .fallbackWithCache(
                ConfigurationV2::class.java,
                ConfigurationV2.version,
                organizationCode,
                applicationCode,
                environment,
                languageCode
            )
            .flowOn(Dispatchers.IO)
    }

    /**
     * Sends a request for updating consent status
     * Uses organizationCode to form a full URL
     * @param configuration full configuration
     * @param identities map of identityCodes and identityValues. Keys and values shouldn't be null
     * @param consents map of consent names and information if this particular legalBasisCode should be allowed or not
     * @param migrationOption rule that represents how updating should be performed
     * @return Flow of Result.Success if successful and with an error if request or its handling failed
     */
    fun getConsentStatusProto(
        configuration: ConfigurationV2,
        identities: Iterable<IdentityV2>,
        purposes: Iterable<PurposeV2>
    ): Flow<Result<RequestError, GetConsentStatusResponseV2>> =
        flow {
            val blockingStub = client.blockingStub
            val request = MobileOuterClass.GetConsentRequest.newBuilder()
                .addAllPurposes(
                    purposes.map {
                        MobileOuterClass.Purpose.newBuilder()
                            .setPurpose(it.purpose)
                            .setLegalBasis(it.legalBasis)
                            .build()
                    })
                .addAllIdentities(identities.map {
                    MobileOuterClass.Identity.newBuilder()
                        .setIdentitySpace(it.space)
                        .setIdentityValue(it.value)
                        .build()
                })
                .setOrganizationId(configuration.organization!!.code)
                .setContext(
                    MobileOuterClass.Context.newBuilder()
                        .setApplication(configuration.applicationInfo!!.code)
                        .setCollectedFrom("phone")
                        .setEnvironment(configuration.environment!!.code)
                        .build()
                )

                .build()

            val response = blockingStub.getConsent(request)

            emit(
                response
            )
        }
            .map<MobileOuterClass.GetConsentResponse, Result<RequestError, GetConsentStatusResponseV2>> {
                Result.succeed(
                    GetConsentStatusResponseV2(it.consentsList.map { consent ->
                        Consent(
                            consent.purpose,
                            consent.legalBasis,
                            consent.allowed
                        )
                    })
                )
            }
            .handleErrors()
            .wrapWithCache(
                GetConsentStatusResponseV2::class.java,
                GetConsentStatusResponseV2.version,
                organizationCode,
                applicationCode,
                configuration.environment?.code,
                identities,
                purposes
            )
            .fallbackWithCache(
                GetConsentStatusResponseV2::class.java,
                GetConsentStatusResponseV2.version,
                organizationCode,
                applicationCode,
                configuration.environment?.code,
                identities,
                purposes
            )
            .flowOn(Dispatchers.IO)


    /**
     * Sends a request for updating consent status
     * Uses organizationCode to form a full URL
     * @param configuration full configuration
     * @param identities map of identityCodes and identityValues. Keys and values shouldn't be null
     * @param purposes map of consent names and information if this particular legalBasisCode should be allowed or not
     * @return Flow of Result.Success if successful and with an error if request or its handling failed
     */
    fun updateConsentStatusProto(
        configuration: ConfigurationV2,
        identities: Iterable<IdentityV2>,
        purposes: Iterable<PurposeV2>
    ): Flow<Result<RequestError, Long>> =
        flow {
            val blockingStub = client.blockingStub
            val request = MobileOuterClass.SetConsentRequest.newBuilder()
                .setPolicyScope(configuration.policyScope!!.code)
                .addAllConsents(
                    purposes.map {
                        MobileOuterClass.Consent.newBuilder()
                            .setPurpose(it.purpose)
                            .setLegalBasis(it.legalBasis)
                            .setAllowed(it.allowed).build()
                    })
                .addAllIdentities(identities.map {
                    MobileOuterClass.Identity.newBuilder()
                        .setIdentitySpace(it.space)
                        .setIdentityValue(it.value)
                        .build()
                })
                .setOrganization(
                    MobileOuterClass.Organization.newBuilder()
                        .setId(configuration.organization!!.code)
                        .setName(configuration.organization.name)
                        .build()
                )
                .setCollectedTime(System.currentTimeMillis())
                .setContext(
                    MobileOuterClass.Context.newBuilder()
                        .setApplication(configuration.applicationInfo!!.code)
                        .setCollectedFrom("phone")
                        .setEnvironment(configuration.environment!!.code)
                        .build()
                )

                .build()

            val response = blockingStub.setConsent(request)

            emit(
                response
            )
        }
            .map<MobileOuterClass.SetConsentResponse, Result<RequestError, Long>> {
                Result.succeed(
                    it.receivedTime
                )
            }
            .handleErrors()
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
    fun invokeRightsProto(
        configuration: ConfigurationV2,
        identities: Iterable<IdentityV2>,
        userData: UserDataV2,
        rights: List<String>
    ): Flow<Result<RequestError, Unit>> =
        flow {
            val blockingStub = client.blockingStub
            val request = MobileOuterClass.InvokeRightRequest.newBuilder()
                .setPolicyScope(configuration.policyScope!!.code)
                .setRight(rights.first())
                .setDataSubject(
                    MobileOuterClass.DataSubject.newBuilder()
                        .setFirst(userData.first)
                        .setLast(userData.last)
                        .setCountry(userData.country)
                        .setRegion(userData.region)
                        .setEmail(userData.email)
                )
                .addAllIdentities(identities.map {
                    MobileOuterClass.Identity.newBuilder()
                        .setIdentitySpace(it.space)
                        .setIdentityValue(it.value)
                        .build()
                })
                .setOrganization(
                    MobileOuterClass.Organization.newBuilder()
                        .setId(configuration.organization!!.code)
                        .setName(configuration.organization.name)
                        .build()
                )
                .setSubmittedTime(System.currentTimeMillis())
                .setContext(
                    MobileOuterClass.Context.newBuilder()
                        .setApplication(configuration.applicationInfo!!.code)
                        .setCollectedFrom("phone")
                        .setEnvironment(configuration.environment!!.code)
                        .build()
                )
                .build()

            val response = blockingStub.invokeRight(request)

            emit(response)
        }
            .map<MobileOuterClass.InvokeRightResponse, Result<RequestError, Unit>> {
                Result.succeed(
                    Unit
                )
            }
            .handleErrors()
            .flowOn(Dispatchers.IO)


    private fun <T : Any> Flow<Result<RequestError, T>>.handleErrors(): Flow<Result<RequestError, T>> =
    catch { e ->
        when (e) {
            is StatusRuntimeException -> emit(
                Result.fail(
                    StatusError(
                        e.status.code,
                        e.status.description
                    )
                )
            )
            else -> emit(Result.fail(OtherErrorV2(e)))
        }
    }

    /**
     * Tries to store {@link Cacheable} object from the upstream flow if result is successful
     * and cacheProvider was set during repository configuration
     * Uses set of params to generate a unique name for a request to cache its result
     * @param type class of the object that needs to be cached
     * @param params set of objects that represent each request
     */
    private fun <T : Cacheable> Flow<Result<RequestError, T>>.wrapWithCache(
        type: Class<*>,
        version: Int,
        vararg params: Any?
    ): Flow<Result<RequestError, T>> =
        onEach { result ->
            if (result is Result.Success) {
                val data = result.value
                cacheProvider?.store(
                    "${type.simpleName}_${version}_${cacheProvider.generateSalt(*params)}",
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
    private fun <T : Any> Flow<Result<RequestError, T>>.fallbackWithCache(
        type: Class<*>,
        version: Int,
        vararg params: Any?
    ): Flow<Result<RequestError, T>> =
        transform { result ->
            if (result is Result.Error) {
                cacheProvider?.obtain("${type.simpleName}_${version}_${cacheProvider.generateSalt(*params)}")
                    ?.let {
                        emit(Result.succeed(gson.fromJson(it, type) as T))
                    } ?: emit(result)
            } else
                emit(result)
        }

    /**
     * Builder class to config KetchRepository. Repository should be created only with this class
     */
    class Builder() {
        private var organizationCode: String? = null
        private var applicationCode: String? = null
        private var cacheProvider: CacheProvider? = null
        private var context: Context? = null

        fun organizationCode(code: String): Builder = this.apply { organizationCode = code }

        fun applicationCode(code: String): Builder = this.apply { applicationCode = code }

        fun cacheProvider(provider: CacheProvider): Builder =
            this.apply { cacheProvider = provider }

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
