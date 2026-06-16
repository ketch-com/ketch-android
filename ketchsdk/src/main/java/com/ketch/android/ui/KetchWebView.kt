package com.ketch.android.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.WebResourceErrorCompat
import androidx.webkit.WebViewClientCompat
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.ketch.android.Ketch
import com.ketch.android.NativeStorage
import com.ketch.android.NativeStoragePutPayload
import com.ketch.android.data.Consent
import com.ketch.android.data.ContentDisplay
import com.ketch.android.data.HideExperienceStatus
import com.ketch.android.data.KetchConfig
import com.ketch.android.data.WillShowExperienceType
import com.ketch.android.data.getIndexHtml
import com.ketch.android.data.parseHideExperienceStatus
import com.ketch.android.data.parseWillShowExperienceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean


const val INITIAL_RELOAD_DELAY = 4000L

@Suppress("SetJavaScriptEnabled", "ViewConstructor")
class KetchWebView(context: Context, shouldRetry: Boolean = false) : WebView(context) {

    var listener: WebViewListener? = null
    private val localContentWebViewClient = LocalContentWebViewClient(shouldRetry)
    private var webResourceUrlOverrides: Map<String, String> = emptyMap()

    fun setWebResourceUrlOverrides(overrides: Map<String, String>) {
        webResourceUrlOverrides = overrides
        localContentWebViewClient.webResourceUrlOverrides = overrides
    }

    init {
        NativeStorage.initialize(context)
        setBackgroundColor(Color.TRANSPARENT)
        webViewClient = localContentWebViewClient

        settings.apply {
            javaScriptEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
        }

        // Prevent from randomly appearing scrollbars while content is loading
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false

        // Explicitly set to false to address android webview security concern
        setWebContentsDebuggingEnabled(false)

        addJavascriptInterface(
            PreferenceCenterJavascriptInterface(this),
            "androidListener"
        )

        //receive console messages from the WebView
        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                Log.d(TAG, consoleMessage.message())
                return true
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                Log.d(TAG, "progress: $newProgress")
            }
        }
    }

    fun setDebugMode() {
        setWebContentsDebuggingEnabled(true)
    }

    // Cancel any coroutines in KetchWebView and fully tear down webview to prevent memory leaks
    fun kill() {
        (parent as? ViewGroup)?.removeView(this)
        localContentWebViewClient.cancelCoroutines()
        stopLoading()
        clearHistory()
        destroy()
    }

    class LocalContentWebViewClient(private var shouldRetry: Boolean = false) : WebViewClientCompat() {

        var webResourceUrlOverrides: Map<String, String> = emptyMap()

        // Flag indicating if the webview has finished loading
        // We use atomic boolean here because we are using it within a coroutine
        private var isLoaded = AtomicBoolean(false)

        // Reload delay, increases exponentially in onPageStarted
        private var reloadDelay = INITIAL_RELOAD_DELAY

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val intent = Intent(Intent.ACTION_VIEW, request.url)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            view.context.startActivity(intent)
            return true
        }

        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest,
        ): WebResourceResponse? {
            WebResourceOverrideHandler.intercept(webResourceUrlOverrides, request)?.let { return it }
            return super.shouldInterceptRequest(view, request)
        }

        override fun onLoadResource(view: WebView?, url: String?) {
            super.onLoadResource(view, url)
            Log.d(TAG, "onLoadResource: $url")
        }

        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail): Boolean {
            if (detail.didCrash()) {
                // Renderer crashed. Handle it (e.g., log, show error, restart WebView)
                Log.e(TAG, "WebView renderer crashed: " + detail.rendererPriorityAtExit())
            } else {
                // Renderer was killed by the system (often due to OOM)
                Log.w(TAG, "WebView renderer killed by system: " + detail.rendererPriorityAtExit())
            }

            (view as? KetchWebView)?.let { ketchWebView ->
                Log.w(TAG, "onDismiss source=rendererCrash status=None")
                ketchWebView.listener?.onClose(HideExperienceStatus.None, "rendererCrash")
                ketchWebView.kill()
            }

            return true
        }

        @Suppress("RequiresFeature")
        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceErrorCompat
        ) {
            super.onReceivedError(view, request, error)
            Log.e(
                TAG,
                "onReceivedError: request: ${request.url}, error: ${error.errorCode} ${error.description}"
            )
        }

        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceResponse
        ) {
            super.onReceivedHttpError(view, request, errorResponse)
            Log.e(TAG, "onReceivedHttpError: requestL ${request.url}, ${errorResponse.statusCode}")
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            Log.d(TAG, "onPageStarted: $url")

            // Reset loaded flag
            isLoaded.set(false)

            // Launch retry if flag set
            if (shouldRetry) {
                scope.launch(Dispatchers.Main) {
                    delay(reloadDelay)

                    // If not yet loaded stop current webview, reload, and increase future delay
                    if (!isLoaded.get()) {
                        Log.d(TAG, "Reloading webview after $reloadDelay ms")
                        view?.stopLoading()
                        view?.reload()
                        reloadDelay *= 2 // Exponentially increase reload delay
                    }
                }
            }
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)

            // Set loaded flag
            isLoaded.set(true)

            // Only reset reload delay when second onPageFinished callback has fired
            if (url === "data:text/html;charset=utf-8;base64,") {
                reloadDelay = INITIAL_RELOAD_DELAY
            }
            Log.d(TAG, "onPageFinished: $url")
        }

        // Cancel all coroutines
        fun cancelCoroutines() {
            scope.cancel()
            Log.d(TAG, "webViewClient coroutines cancelled")
        }
    }

    internal fun load(
        orgCode: String,
        property: String,
        language: String?,
        jurisdiction: String?,
        region: String?,
        environment: String?,
        identities: Map<String, String>,
        forceShow: ExperienceType?,
        preferencesTabs: List<Ketch.PreferencesTab>,
        preferencesTab: Ketch.PreferencesTab?,
        ketchUrl: String?,
        logLevel: Ketch.LogLevel,
        age: Int?,
        ageLower: Int?,
        ageUpper: Int?,
        bottomPadding: Int?,
        topPadding: Int?,
        cssStyle: String?,
        webResourceUrlOverrides: Map<String, String> = emptyMap(),
    ) {
        setWebResourceUrlOverrides(webResourceUrlOverrides)
        clearCache(true)

        // Convert padding values to string
        var bottomPaddingPx = "0px"
        if (bottomPadding != null) {
            bottomPaddingPx = bottomPadding.toString() + "px"
        }
        var topPaddingPx = "0px"
        if (topPadding != null) {
            topPaddingPx = topPadding.toString() + "px"
        }

        val indexHtml = getIndexHtml(
            orgCode = orgCode,
            propertyName = property,
            logLevel = logLevel.name,
            ketchMobileSdkUrl = ketchUrl ?: "https://global.ketchcdn.com/web/v3",
            language = language,
            jurisdiction = jurisdiction,
            identities = identities,
            region = region,
            environment = environment,
            forceShow = forceShow?.getUrlParameter(),
            preferencesTabs = preferencesTabs.takeIf { it.isNotEmpty() }?.joinToString(",") { it.getUrlParameter() },
            preferencesTab = preferencesTab?.getUrlParameter(),
            age = age,
            ageLower = ageLower,
            ageUpper = ageUpper,
            bottomPadding = bottomPaddingPx,
            topPadding = topPaddingPx,
            cssStyleOverride = cssStyle,
            webResourceUrlOverrides = webResourceUrlOverrides,
        )

        loadDataWithBaseURL("http://localhost", indexHtml, "text/html", "UTF-8", null)
    }

    @Suppress("unused")
    private class PreferenceCenterJavascriptInterface(private val ketchWebView: KetchWebView) {
        @JavascriptInterface
        fun hideExperience(status: String?) {
            // Determine the hideExperience event status
            val parsedStatus = parseHideExperienceStatus(status)
            if (parsedStatus === HideExperienceStatus.None && !status.isNullOrBlank()) {
                Log.w(TAG, "onDismiss source=hideExperience parseFallback rawStatus=$status")
            } else {
                Log.d(TAG, "onDismiss source=hideExperience status=${parsedStatus.name} rawStatus=$status")
            }
            runOnMainThread {
                ketchWebView.listener?.onClose(parsedStatus, "hideExperience")
            }
        }

        @JavascriptInterface
        fun environment(environment: String?) {
            Log.d(TAG, "environment: $environment")
            runOnMainThread {
                ketchWebView.listener?.onEnvironmentUpdated(environment)
            }
        }

        @JavascriptInterface
        fun regionInfo(regionInfo: String?) {
            Log.d(TAG, "regionInfo: $regionInfo")
            runOnMainThread {
                ketchWebView.listener?.onRegionInfoUpdated(regionInfo)
            }
        }

        @JavascriptInterface
        fun jurisdiction(jurisdiction: String?) {
            Log.d(TAG, "jurisdiction: $jurisdiction")
            runOnMainThread {
                ketchWebView.listener?.onJurisdictionUpdated(jurisdiction)
            }
        }

        @JavascriptInterface
        fun identities(identities: String?) {
            Log.d(TAG, "identities: $identities")
            runOnMainThread {
                ketchWebView.listener?.onIdentitiesUpdated(identities)
            }
        }

        @JavascriptInterface
        fun consent(consentJson: String?) {
            Log.d(TAG, "consent: $consentJson")
            // {"purposes":{"essential_services":true,"tcf.purpose_1":true,"analytics":false,"behavioral_advertising":false,"email_marketing":false,"data_broking":false,"somepurpose_key":false},"vendors":[]}
            try {
                val consent = Gson().fromJson(consentJson, Consent::class.java)
                Log.d(TAG, "consent: $consent")
                runOnMainThread {
                    ketchWebView.listener?.onConsentUpdated(consent)
                }
            } catch (ex: JsonParseException) {
                Log.e(TAG, ex.message, ex)
            }
        }

        @JavascriptInterface
        fun willShowExperience(type: String?) {
            val parsedType = parseWillShowExperienceType(type)
            Log.d(TAG, "willShowExperience: raw=$type parsed=${parsedType.name}")
            runOnMainThread {
                if (parsedType === WillShowExperienceType.ConsentExperience ||
                    type?.contains("consent", ignoreCase = true) == true
                ) {
                    ketchWebView.listener?.showConsent()
                } else if (parsedType === WillShowExperienceType.PreferenceExperience ||
                    type?.contains("preference", ignoreCase = true) == true
                ) {
                    ketchWebView.listener?.showPreferences()
                } else {
                    Log.w(TAG, "willShowExperience: unknown type, defaulting to consent")
                    ketchWebView.listener?.showConsent()
                }
                ketchWebView.listener?.onWillShowExperience(parsedType)
            }
        }

        @JavascriptInterface
        fun hasShownExperience() {
            Log.d(TAG, "hasShownExperience")
            runOnMainThread {
                ketchWebView.listener?.onHasShownExperience()
            }
        }

        @JavascriptInterface
        fun showConsentExperience(showConsentExperience: String?) {
            Log.d(TAG, "showConsentExperience: $showConsentExperience")
            runOnMainThread {
                ketchWebView.listener?.showConsent()
            }
        }

        @JavascriptInterface
        fun showPreferenceExperience(showPreferenceExperience: String?) {
            Log.d(TAG, "showPreferenceExperience: $showPreferenceExperience")
            runOnMainThread {
                ketchWebView.listener?.showPreferences()
            }
        }

        @JavascriptInterface
        fun showExperience(payload: String?) {
            Log.d(TAG, "showExperience: $payload")
            runOnMainThread {
                if (payload?.contains("preference", ignoreCase = true) == true) {
                    ketchWebView.listener?.showPreferences()
                } else {
                    ketchWebView.listener?.showConsent()
                }
            }
        }

        @JavascriptInterface
        fun renderExperience(payload: String?) {
            Log.d(TAG, "renderExperience: $payload")
            runOnMainThread {
                if (payload?.contains("preference", ignoreCase = true) == true) {
                    ketchWebView.listener?.showPreferences()
                } else {
                    ketchWebView.listener?.showConsent()
                }
            }
        }

        @JavascriptInterface
        fun onConfigLoaded(configJson: String?) {
            val configSummary = summarizeConfigJson(configJson)
            val purposesSummary = summarizePurposesJson(configJson)
            Log.d(TAG, "onConfigLoaded summary: $configSummary")
            Log.d(TAG, "onConfigLoaded purposes: $purposesSummary")

            try {
                val config = GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
                    .setPrettyPrinting()
                    .create()
                    .fromJson(configJson, KetchConfig::class.java)
                Log.d(TAG, "config parsed: experiences=${config?.experiences?.consent?.display}")
                runOnMainThread {
                    ketchWebView.listener?.onConfigDebugInfo(configSummary, purposesSummary)
                    ketchWebView.listener?.onConfigUpdated(config)
                }
            } catch (ex: JsonParseException) {
                Log.e(TAG, ex.message, ex)
                runOnMainThread {
                    ketchWebView.listener?.onConfigDebugInfo(configSummary, purposesSummary)
                    ketchWebView.listener?.onConfigUpdated(null)
                }
            }
        }

        @JavascriptInterface
        fun tapOutside(dialogSize: String?) {
            Log.d(TAG, "onDismiss source=tapOutside dialogSize=$dialogSize")
            runOnMainThread {
                ketchWebView.listener?.onTapOutside()
            }
        }

        @JavascriptInterface
        fun geoip(ip: String?) {
        }

        @JavascriptInterface
        fun error(errMsg: String?) {
            Log.d(TAG, "error: $errMsg")
            runOnMainThread {
                ketchWebView.listener?.onError(errMsg)
            }
        }

        @JavascriptInterface
        fun usprivacy_updated_data(usPrivacyString: String?) {
            Log.d(TAG, "onUSPrivacyUpdate: $usPrivacyString")
            usPrivacyString?.let {
                parseIabTcfGpp(it)?.let { values ->
                    runOnMainThread {
                        ketchWebView.listener?.onUSPrivacyUpdated(values)
                    }
                }
            }
        }

        @JavascriptInterface
        fun tcf_updated_data(tcfString: String?) {
            Log.d(TAG, "onTCFUpdate: tcfString: $tcfString")
            tcfString?.let {
                parseIabTcfGpp(it)?.let { values ->
                    runOnMainThread {
                        ketchWebView.listener?.onTCFUpdated(values)
                    }
                }
            }
        }

        @JavascriptInterface
        fun gpp_updated_data(gppString: String?) {
            Log.d(TAG, "onGPPUpdate: gppString: $gppString")
            gppString?.let {
                parseIabTcfGpp(it)?.let { values ->
                    runOnMainThread {
                        ketchWebView.listener?.onGPPUpdated(values)
                    }
                }
            }
        }

        @JavascriptInterface
        fun nativeStoragePut(payloadJson: String?) {
            if (payloadJson.isNullOrBlank()) {
                Log.e(TAG, "nativeStoragePut: empty payload")
                return
            }
            try {
                val payload = Gson().fromJson(payloadJson, NativeStoragePutPayload::class.java)
                if (payload.key.isBlank()) {
                    Log.e(TAG, "nativeStoragePut: missing key")
                    return
                }
                NativeStorage.write(payload.key, payload.value)
                Log.d(TAG, "nativeStoragePut: ${payload.key}=${payload.value}")
                runOnMainThread {
                    ketchWebView.listener?.onNativeStoragePut(payload.key, payload.value)
                }
            } catch (ex: JsonParseException) {
                Log.e(TAG, "nativeStoragePut parse error: ${ex.message}", ex)
            }
        }

        private fun parseIabTcfGpp(json: String): Map<String, String>? {
            val gson = GsonBuilder()
                .create()

            @Suppress("unchecked_cast")
            return gson.fromJson(json, Array<Any>::class.java)
                .firstOrNull { it is Map<*, *> } as? Map<String, String>
        }

        private fun runOnMainThread(action: () -> Unit) {
            Handler(Looper.getMainLooper()).post {
                action.invoke()
            }
        }
    }

    internal fun requestShowConsent(forceImmediate: Boolean = false) {
        listener?.requestShowConsent(forceImmediate)
    }

    internal fun requestShowPreferences(forceImmediate: Boolean = false) {
        listener?.requestShowPreferences(forceImmediate)
    }

    interface WebViewListener {
        fun showConsent()
        fun showPreferences()
        fun requestShowConsent(forceImmediate: Boolean = false) {
            showConsent()
        }
        fun requestShowPreferences(forceImmediate: Boolean = false) {
            showPreferences()
        }
        fun onUSPrivacyUpdated(values: Map<String, Any?>)
        fun onTCFUpdated(values: Map<String, Any?>)
        fun onGPPUpdated(values: Map<String, Any?>)
        fun onConfigUpdated(config: KetchConfig?)
        fun onConfigDebugInfo(configSummary: String, purposesSummary: String) {}
        fun onEnvironmentUpdated(environment: String?)
        fun onRegionInfoUpdated(regionInfo: String?)
        fun onJurisdictionUpdated(jurisdiction: String?)
        fun onIdentitiesUpdated(identities: String?)
        fun onConsentUpdated(consent: Consent)
        fun onError(errMsg: String?)
        fun changeDialog(display: ContentDisplay)
        fun onClose(status: HideExperienceStatus, source: String = "hideExperience")
        fun onWillShowExperience(experienceType: WillShowExperienceType)
        fun onHasShownExperience()
        fun onTapOutside()
        fun onNativeStoragePut(key: String, value: String) {}
    }

    internal fun requestWebDismiss(callback: ((Boolean) -> Unit)? = null) {
        evaluateJavascript(TRIGGER_OUTSIDE_TAP_DISMISS_JS) { result ->
            callback?.invoke(result.equals("true", ignoreCase = true))
        }
    }

    internal enum class ExperienceType {
        CONSENT,
        PREFERENCES;

        fun getUrlParameter(): String = when (this) {
            CONSENT -> "consent"
            PREFERENCES -> "preferences"
        }
    }

    companion object {
        private val TAG: String = KetchWebView::class.java.simpleName

        private const val TRIGGER_OUTSIDE_TAP_DISMISS_JS =
            "(function(){if(typeof triggerOutsideTapDismiss==='function'){return triggerOutsideTapDismiss();}return false;})()"
    }
}

private fun summarizeConfigJson(configJson: String?): String {
    if (configJson.isNullOrBlank()) return "config: empty"
    return try {
        val root = JsonParser.parseString(configJson)
        if (!root.isJsonObject) return "config: not an object"
        summarizeConfigElement(root.asJsonObject)
    } catch (e: Exception) {
        "config parse error: ${e.message}"
    }
}

private fun summarizePurposesJson(configJson: String?): String {
    if (configJson.isNullOrBlank()) return "purposes: empty"
    return try {
        val root = JsonParser.parseString(configJson)
        if (!root.isJsonObject) return "purposes: n/a"
        summarizePurposesElement(root.asJsonObject)
    } catch (e: Exception) {
        "purposes parse error: ${e.message}"
    }
}

private fun summarizeConfigElement(root: JsonObject): String {
    val env = root.get("environment")?.asJsonObject?.get("code")?.asString
    val jurisdiction = root.get("jurisdiction")?.asJsonObject?.get("code")?.asString
        ?: root.get("policyScope")?.asJsonObject?.get("code")?.asString
    val language = root.get("language")?.asString
    val experiences = summarizeExperiences(root.get("experiences"))
    return buildString {
        append("env=").append(env ?: "?")
        append(" jurisdiction=").append(jurisdiction ?: "?")
        append(" lang=").append(language ?: "?")
        append(" ").append(experiences)
    }
}

private fun summarizePurposesElement(root: JsonObject): String {
    val canonical = root.getAsJsonObject("canonicalPurposes")
    if (canonical != null && canonical.size() > 0) {
        val codes = canonical.entrySet().map { it.key }.sorted()
        return "canonicalPurposes(${codes.size}): ${codes.joinToString(", ")}"
    }
    val purposes = root.getAsJsonArray("purposes")
    if (purposes != null && purposes.size() > 0) {
        val codes = purposes.mapNotNull { element ->
            element.asJsonObject.get("code")?.asString
        }.sorted()
        return "purposes(${codes.size}): ${codes.joinToString(", ")}"
    }
    return "purposes: none in config.json"
}

private fun summarizeExperiences(experiences: JsonElement?): String {
    if (experiences == null || experiences.isJsonNull) return "experiences=none"
    if (!experiences.isJsonObject) return "experiences=unexpected"
    val obj = experiences.asJsonObject
    val keys = obj.keySet().sorted()
    val autoInitiated = obj.getAsJsonObject("autoInitiated")
    val layout = autoInitiated?.getAsJsonObject("layout")
    val banner = layout?.has("banner") == true
    val modal = layout?.has("modal") == true
    val pref = layout?.has("preference") == true
    return buildString {
        append("experiences keys=[${keys.joinToString(",")}]")
        if (layout != null) {
            append(" layout banner=$banner modal=$modal pref=$pref")
        }
    }
}

private fun JsonArray.mapNotNull(transform: (JsonElement) -> String?): List<String> {
    val result = mutableListOf<String>()
    for (element in this) {
        transform(element)?.let { result.add(it) }
    }
    return result
}