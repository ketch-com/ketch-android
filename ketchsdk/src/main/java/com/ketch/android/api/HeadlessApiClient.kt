package com.ketch.android.api

import com.google.gson.Gson
import com.ketch.android.data.Consent
import com.ketch.android.data.ConsentConfig
import com.ketch.android.data.ConsentUpdate
import com.ketch.android.data.FullConfigurationRequest
import com.ketch.android.data.GetProfileRequest
import com.ketch.android.data.GetProfileResponse
import com.ketch.android.data.HeadlessConfiguration
import com.ketch.android.data.HeadlessException
import com.ketch.android.data.InvokeRightRequest
import com.ketch.android.data.LocationResponse
import com.ketch.android.data.PreferenceQRRequest
import com.ketch.android.data.PutProfileRequest
import com.ketch.android.data.SubscriptionConfiguration
import com.ketch.android.data.SubscriptionConfigurationRequest
import com.ketch.android.data.SubscriptionsRequest
import com.ketch.android.data.SubscriptionsResponse
import com.ketch.android.data.WebReportRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Native HTTP client mirroring ketch-tag [KetchWebAPI] (web/v3).
 */
class HeadlessApiClient(
    dataCenter: KetchDataCenter = KetchDataCenter.US,
    private val okHttpClient: OkHttpClient = OkHttpClient(),
    private val gson: Gson = Gson(),
) {
    private val baseUrl = dataCenter.baseUrl
    private val jsonMediaType = "application/json".toMediaType()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun fetchLocation(callback: (Result<LocationResponse>) -> Unit) {
        launchAsync(callback) { fetchLocation() }
    }

    suspend fun fetchLocation(): LocationResponse = withContext(Dispatchers.IO) {
        get("/ip", LocationResponse::class.java)
    }

    fun fetchBootstrapConfiguration(
        organization: String,
        property: String,
        callback: (Result<HeadlessConfiguration>) -> Unit,
    ) {
        launchAsync(callback) { fetchBootstrapConfiguration(organization, property) }
    }

    suspend fun fetchBootstrapConfiguration(
        organization: String,
        property: String,
    ): HeadlessConfiguration = withContext(Dispatchers.IO) {
        get("/config/$organization/$property/boot.json", HeadlessConfiguration::class.java)
    }

    fun fetchFullConfiguration(
        request: FullConfigurationRequest,
        callback: (Result<HeadlessConfiguration>) -> Unit,
    ) {
        launchAsync(callback) { fetchFullConfiguration(request) }
    }

    suspend fun fetchFullConfiguration(request: FullConfigurationRequest): HeadlessConfiguration =
        withContext(Dispatchers.IO) {
            var path = "/config/${request.organizationCode}/${request.propertyCode}"
            if (request.environmentCode != null &&
                request.jurisdictionCode != null &&
                request.languageCode != null
            ) {
                path += "/${request.environmentCode}/${request.jurisdictionCode}/${request.languageCode}"
            }
            path += "/config.json"
            val query = request.hash?.let { mapOf("hash" to it) } ?: emptyMap()
            get(path, HeadlessConfiguration::class.java, query)
        }

    fun fetchConsent(
        config: ConsentConfig,
        callback: (Result<Consent>) -> Unit,
    ) {
        launchAsync(callback) { fetchConsent(config) }
    }

    suspend fun fetchConsent(config: ConsentConfig): Consent = withContext(Dispatchers.IO) {
        val path = "/consent/${config.organizationCode}/get"
        postConsent(path, ConsentConfigPayload.from(config), config)
    }

    fun fetchProtocols(
        config: ConsentConfig,
        callback: (Result<Consent>) -> Unit,
    ) {
        launchAsync(callback) { fetchProtocols(config) }
    }

    suspend fun fetchProtocols(config: ConsentConfig): Consent = withContext(Dispatchers.IO) {
        val response = fetchConsent(config)
        if (response.protocols.isNullOrEmpty()) {
            Consent(purposes = null, vendors = null, protocols = null)
        } else {
            response
        }
    }

    fun setConsent(
        update: ConsentUpdate,
        callback: (Result<Consent>) -> Unit,
    ) {
        launchAsync(callback) { setConsent(update) }
    }

    suspend fun setConsent(update: ConsentUpdate): Consent = withContext(Dispatchers.IO) {
        val path = "/consent/${update.organizationCode}/update"
        postSetConsent(path, SetConsentPayload.from(update), update)
    }

    fun invokeRight(
        request: InvokeRightRequest,
        callback: (Result<Unit>) -> Unit,
    ) {
        launchAsync(callback) { invokeRight(request) }
    }

    suspend fun invokeRight(request: InvokeRightRequest): Unit = withContext(Dispatchers.IO) {
        postVoid("/rights/${request.organizationCode}/invoke", request)
    }

    fun getProfile(
        request: GetProfileRequest,
        callback: (Result<GetProfileResponse>) -> Unit,
    ) {
        launchAsync(callback) { getProfile(request) }
    }

    suspend fun getProfile(request: GetProfileRequest): GetProfileResponse = withContext(Dispatchers.IO) {
        post("/profile/${request.organizationCode}/get", request, GetProfileResponse::class.java)
    }

    fun putProfile(
        request: PutProfileRequest,
        callback: (Result<Unit>) -> Unit,
    ) {
        launchAsync(callback) { putProfile(request) }
    }

    suspend fun putProfile(request: PutProfileRequest): Unit = withContext(Dispatchers.IO) {
        postVoid("/profile/${request.organizationCode}/put", request)
    }

    fun getSubscriptions(
        request: SubscriptionsRequest,
        callback: (Result<SubscriptionsResponse>) -> Unit,
    ) {
        launchAsync(callback) { getSubscriptions(request) }
    }

    suspend fun getSubscriptions(request: SubscriptionsRequest): SubscriptionsResponse =
        withContext(Dispatchers.IO) {
            post("/subscriptions/${request.organizationCode}/get", request, SubscriptionsResponse::class.java)
        }

    fun setSubscriptions(
        request: SubscriptionsRequest,
        callback: (Result<Unit>) -> Unit,
    ) {
        launchAsync(callback) { setSubscriptions(request) }
    }

    suspend fun setSubscriptions(request: SubscriptionsRequest): Unit = withContext(Dispatchers.IO) {
        postVoid("/subscriptions/${request.organizationCode}/update", request)
    }

    fun fetchSubscriptionsConfiguration(
        request: SubscriptionConfigurationRequest,
        callback: (Result<SubscriptionConfiguration>) -> Unit,
    ) {
        launchAsync(callback) { fetchSubscriptionsConfiguration(request) }
    }

    suspend fun fetchSubscriptionsConfiguration(
        request: SubscriptionConfigurationRequest,
    ): SubscriptionConfiguration = withContext(Dispatchers.IO) {
        val path = "/config/${request.organizationCode}/${request.propertyCode}/${request.languageCode}/${request.experienceCode}/subscriptions.json"
        get(path, SubscriptionConfiguration::class.java)
    }

    fun preferenceQRUrl(request: PreferenceQRRequest): String {
        val query = linkedMapOf<String, String>()
        request.environmentCode?.let { query["env"] = it }
        request.imageSize?.let { query["size"] = it.toString() }
        request.path?.let { query["path"] = it }
        request.backgroundColor?.let { query["bgcolor"] = it }
        request.foregroundColor?.let { query["fgcolor"] = it }
        query.putAll(request.parameters)
        return buildUrl(
            "/qr/${request.organizationCode}/${request.propertyCode}/preferences.png",
            query,
        )
    }

    fun webReport(
        channel: String,
        request: WebReportRequest,
        callback: (Result<Unit>) -> Unit,
    ) {
        launchAsync(callback) { webReport(channel, request) }
    }

    suspend fun webReport(channel: String, request: WebReportRequest): Unit =
        withContext(Dispatchers.IO) {
            postVoid("/report/$channel", request)
        }

    /** Builds an absolute CDN URL for unit tests and debugging. */
    fun buildUrl(path: String, query: Map<String, String> = emptyMap()): String {
        val normalized = if (path.startsWith("/")) path else "/$path"
        val url = baseUrl.trimEnd('/') + normalized
        if (query.isEmpty()) {
            return url
        }
        val httpUrl = url.toHttpUrl().newBuilder()
        query.forEach { (key, value) -> httpUrl.addQueryParameter(key, value) }
        return httpUrl.build().toString()
    }

    private fun <T> get(path: String, type: Class<T>, query: Map<String, String> = emptyMap()): T {
        val url = buildUrl(path, query)
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .get()
            .build()
        return execute(request, type)
    }

    private fun <T> execute(request: Request, type: Class<T>): T {
        try {
            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw HeadlessException("HTTP ${response.code} for ${request.url}")
                }
                if (body.isBlank() || body == "null") {
                    throw HeadlessException("Empty response for ${request.url}")
                }
                return gson.fromJson(body, type)
                    ?: throw HeadlessException("Failed to decode response for ${request.url}")
            }
        } catch (error: HeadlessException) {
            throw error
        } catch (error: IOException) {
            throw HeadlessException("Network error for ${request.url}", error)
        }
    }

    private fun postConsent(path: String, body: ConsentConfigPayload, config: ConsentConfig): Consent {
        return try {
            val response = post(path, body, Consent::class.java)
            if (response.purposes != null || response.protocols != null) {
                response
            } else {
                emptyConsent(config)
            }
        } catch (_: HeadlessException) {
            emptyConsent(config)
        } catch (_: IOException) {
            emptyConsent(config)
        }
    }

    private fun postSetConsent(path: String, body: SetConsentPayload, fallback: ConsentUpdate): Consent {
        return try {
            val response = post(path, body, Consent::class.java)
            if (!response.purposes.isNullOrEmpty()) {
                response
            } else {
                consentFromUpdate(fallback)
            }
        } catch (_: HeadlessException) {
            consentFromUpdate(fallback)
        } catch (_: IOException) {
            consentFromUpdate(fallback)
        }
    }

    private fun <T> post(path: String, body: Any, type: Class<T>): T {
        val json = gson.toJson(body)
        val request = Request.Builder()
            .url(buildUrl(path))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .post(json.toRequestBody(jsonMediaType))
            .build()
        return execute(request, type)
    }

    private fun postVoid(path: String, body: Any) {
        val json = gson.toJson(body)
        val request = Request.Builder()
            .url(buildUrl(path))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .post(json.toRequestBody(jsonMediaType))
            .build()
        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw HeadlessException("HTTP ${response.code} for ${request.url}")
                }
            }
        } catch (error: HeadlessException) {
            throw error
        } catch (error: IOException) {
            throw HeadlessException("Network error for ${request.url}", error)
        }
    }

    private fun emptyConsent(config: ConsentConfig): Consent =
        Consent(purposes = emptyMap(), vendors = null, protocols = null)

    private fun consentFromUpdate(update: ConsentUpdate): Consent {
        val purposes = update.purposes.mapValues { (_, basis) ->
            basis.allowed.equals("true", ignoreCase = true)
        }
        return Consent(purposes = purposes, vendors = update.vendors, protocols = null)
    }

    private fun <T> launchAsync(
        callback: (Result<T>) -> Unit,
        block: suspend () -> T,
    ) {
        scope.launch {
            try {
                callback(Result.success(block()))
            } catch (error: HeadlessException) {
                callback(Result.failure(error))
            } catch (error: Exception) {
                callback(Result.failure(HeadlessException(error.message ?: "Headless API error", error)))
            }
        }
    }

    private data class ConsentConfigPayload(
        val organizationCode: String,
        val propertyCode: String,
        val environmentCode: String,
        val jurisdictionCode: String,
        val identities: Map<String, String>,
        val purposes: Map<String, ConsentConfig.PurposeLegalBasis>,
    ) {
        companion object {
            fun from(config: ConsentConfig) = ConsentConfigPayload(
                organizationCode = config.organizationCode,
                propertyCode = config.propertyCode,
                environmentCode = config.environmentCode,
                jurisdictionCode = config.jurisdictionCode,
                identities = config.identities,
                purposes = config.purposes,
            )
        }
    }

    private data class SetConsentPayload(
        val organizationCode: String,
        val propertyCode: String,
        val environmentCode: String,
        val identities: Map<String, String>,
        val jurisdictionCode: String,
        val migrationOption: ConsentUpdate.MigrationOption,
        val purposes: Map<String, ConsentUpdate.PurposeAllowedLegalBasis>,
        val vendors: List<String>?,
    ) {
        companion object {
            fun from(update: ConsentUpdate) = SetConsentPayload(
                organizationCode = update.organizationCode,
                propertyCode = update.propertyCode,
                environmentCode = update.environmentCode,
                identities = update.identities,
                jurisdictionCode = update.jurisdictionCode,
                migrationOption = update.migrationOption,
                purposes = update.purposes,
                vendors = update.vendors,
            )
        }
    }
}
