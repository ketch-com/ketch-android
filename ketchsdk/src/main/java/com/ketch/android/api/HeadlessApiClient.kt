package com.ketch.android.api

import com.google.gson.Gson
import com.ketch.android.data.Consent
import com.ketch.android.data.ConsentConfig
import com.ketch.android.data.ConsentUpdate
import com.ketch.android.data.FullConfigurationRequest
import com.ketch.android.data.HeadlessConfiguration
import com.ketch.android.data.configPathSegment
import com.ketch.android.data.configQueryParams
import com.ketch.android.data.HeadlessException
import com.ketch.android.data.InvokeRightRequest
import com.ketch.android.data.LocationResponse
import com.ketch.android.data.PreferenceQRRequest
import com.ketch.android.data.SubscriptionsRequest
import com.ketch.android.data.VendorConsents
import com.ketch.android.data.SubscriptionsResponse
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
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Native HTTP client mirroring ketch-tag [KetchWebAPI] (web/v3).
 */
class HeadlessApiClient(
    dataCenter: KetchDataCenter = KetchDataCenter.US,
    baseUrl: String? = null,
    private val okHttpClient: OkHttpClient = defaultOkHttpClient,
    private val gson: Gson = Gson(),
) {
    private val baseUrl = baseUrl ?: dataCenter.baseUrl
    private val jsonMediaType = "application/json".toMediaType()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TIMEOUT_SECONDS = 30L

        /**
         * Shared client across [HeadlessApiClient] instances so per-call helpers (e.g. [com.ketch.android.KetchSdk]
         * static methods) reuse connections/threads instead of allocating a fresh pool each call, and so
         * requests fail fast on bad networks instead of hanging indefinitely.
         */
        private val defaultOkHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build()
        }
    }

    internal fun getLocation(callback: (Result<LocationResponse>) -> Unit) {
        launchAsync(callback) { getLocation() }
    }

    internal suspend fun getLocation(): LocationResponse = withContext(Dispatchers.IO) {
        get("/ip", LocationResponse::class.java)
    }

    fun getBootstrapConfiguration(
        organization: String,
        property: String,
        callback: (Result<HeadlessConfiguration>) -> Unit,
    ) {
        launchAsync(callback) { getBootstrapConfiguration(organization, property) }
    }

    suspend fun getBootstrapConfiguration(
        organization: String,
        property: String,
    ): HeadlessConfiguration = withContext(Dispatchers.IO) {
        get("/config/$organization/$property/boot.json", HeadlessConfiguration::class.java)
    }

    fun getFullConfiguration(
        request: FullConfigurationRequest,
        callback: (Result<HeadlessConfiguration>) -> Unit,
    ) {
        launchAsync(callback) { getFullConfiguration(request) }
    }

    suspend fun getFullConfiguration(request: FullConfigurationRequest): HeadlessConfiguration =
        withContext(Dispatchers.IO) {
            var path = "/config/${request.organizationCode}/${request.propertyCode}"
            val fullSegment = request.configPathSegment()
            fullSegment?.let { (env, jurisdiction, language) ->
                path += "/$env/$jurisdiction/$language"
            }
            path += "/config.json"
            val query = request.configQueryParams()
            // Belt-and-suspenders: the `language` query param is what the server actually reads.
            val headers = if (fullSegment == null) {
                mapOf("Accept-Language" to Locale.getDefault().toLanguageTag())
            } else {
                emptyMap()
            }
            get(path, HeadlessConfiguration::class.java, query, headers)
        }

    fun getConsent(
        config: ConsentConfig,
        callback: (Result<Consent>) -> Unit,
    ) {
        launchAsync(callback) { getConsent(config) }
    }

    suspend fun getConsent(config: ConsentConfig): Consent = withContext(Dispatchers.IO) {
        val path = "/consent/${config.organizationCode}/get"
        postConsent(path, ConsentConfigPayload.from(config))
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

    fun getPreferenceQRUrl(request: PreferenceQRRequest): String {
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

    private fun <T> get(
        path: String,
        type: Class<T>,
        query: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
    ): T {
        val url = buildUrl(path, query)
        val builder = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .get()
        headers.forEach { (key, value) -> builder.header(key, value) }
        return execute(builder.build(), type)
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

    private fun postConsent(path: String, body: ConsentConfigPayload): Consent {
        val request = buildConsentPostRequest(path, body)
        return executeConsentFetch(request)
    }

    private fun postSetConsent(path: String, body: SetConsentPayload, fallback: ConsentUpdate): Consent {
        val request = buildConsentPostRequest(path, body)
        return executeConsentSet(request, fallback)
    }

    private fun buildConsentPostRequest(path: String, body: Any): Request {
        val json = gson.toJson(body)
        return Request.Builder()
            .url(buildUrl(path))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .post(json.toRequestBody(jsonMediaType))
            .build()
    }

    private fun executeConsentFetch(request: Request): Consent {
        try {
            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw HeadlessException("HTTP ${response.code} for ${request.url}")
                }
                if (body.isBlank() || body == "null") {
                    return emptyConsent()
                }
                val decoded = decodeConsent(body)
                if (decoded != null && hasUsableConsentFields(decoded)) {
                    return decoded
                }
                return emptyConsent()
            }
        } catch (error: HeadlessException) {
            throw error
        } catch (error: IOException) {
            throw HeadlessException("Network error for ${request.url}", error)
        }
    }

    private fun executeConsentSet(request: Request, fallback: ConsentUpdate): Consent {
        try {
            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw HeadlessException("HTTP ${response.code} for ${request.url}")
                }
                val decoded = decodeConsent(body)
                if (decoded != null && hasUsableConsentFields(decoded)) {
                    return decoded
                }
                return consentFromUpdate(fallback)
            }
        } catch (error: HeadlessException) {
            throw error
        } catch (error: IOException) {
            throw HeadlessException("Network error for ${request.url}", error)
        }
    }

    private fun decodeConsent(body: String): Consent? =
        try {
            gson.fromJson(body, Consent::class.java)
        } catch (_: Exception) {
            null
        }

    @Suppress("DEPRECATION")
    private fun hasUsableConsentFields(consent: Consent): Boolean {
        if (!consent.purposes.isNullOrEmpty()) return true
        if (!consent.vendors.isNullOrEmpty()) return true
        if (!consent.protocols.isNullOrEmpty()) return true
        if (consent.vendorConsents != null) return true
        return false
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

    @Suppress("DEPRECATION")
    private fun emptyConsent(): Consent =
        Consent(purposes = emptyMap(), vendors = null, protocols = null, vendorConsents = null)

    // Fallback used when setConsent's HTTP response is 2xx but the body has no usable purposes/
    // protocols (e.g. empty body). We build a Consent from the values in the request itself, so the
    // caller sees the purposes they asked for as "on". This is a guess: we never confirmed the server
    // actually stored them, since the response didn't tell us anything.
    @Suppress("DEPRECATION")
    private fun consentFromUpdate(update: ConsentUpdate): Consent {
        val purposes = update.purposes.mapValues { (_, basis) ->
            basis.allowed.equals("true", ignoreCase = true)
        }
        return Consent(
            purposes = purposes,
            vendors = update.vendors,
            protocols = null,
            vendorConsents = update.vendorConsents,
        )
    }

    private fun <T> launchAsync(
        callback: (Result<T>) -> Unit,
        block: suspend () -> T,
    ) {
        // Network work runs on the IO dispatcher (scope + block's own withContext); the callback is
        // delivered on Main to match the rest of the SDK's threading contract (see KetchWebView).
        scope.launch {
            val result = try {
                Result.success(block())
            } catch (error: HeadlessException) {
                Result.failure(error)
            } catch (error: Exception) {
                Result.failure(HeadlessException(error.message ?: "Headless API error", error))
            }
            withContext(Dispatchers.Main) { callback(result) }
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
                identities = config.identities ?: emptyMap(),
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
        val vendorConsents: VendorConsents?,
    ) {
        companion object {
            @Suppress("DEPRECATION")
            fun from(update: ConsentUpdate) = SetConsentPayload(
                organizationCode = update.organizationCode,
                propertyCode = update.propertyCode,
                environmentCode = update.environmentCode,
                identities = update.identities ?: emptyMap(),
                jurisdictionCode = update.jurisdictionCode,
                migrationOption = update.migrationOption,
                purposes = update.purposes,
                vendors = update.vendors,
                vendorConsents = update.vendorConsents,
            )
        }
    }
}
