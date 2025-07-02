package com.ketch.android.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import android.webkit.WebView
import androidx.webkit.WebResourceErrorCompat
import androidx.webkit.WebViewClientCompat
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.ketch.android.Ketch
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

@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
class KetchWebView(context: Context, shouldRetry: Boolean = false) : WebView(context) {

    var listener: WebViewListener? = null
    private val localContentWebViewClient = LocalContentWebViewClient(shouldRetry)

    init {
        webViewClient = localContentWebViewClient
        settings.javaScriptEnabled = true
        setBackgroundColor(context.getColor(android.R.color.transparent))

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
        localContentWebViewClient.cancelCoroutines()
        stopLoading()
        clearHistory()
        clearCache(true)
        (parent as ViewGroup).removeView(this)
        destroy()
    }

    class LocalContentWebViewClient(private var shouldRetry: Boolean = false) : WebViewClientCompat() {

        // Flag indicating if the webview has finished loading
        // We use atomic boolean here because we are using it within a coroutine
        private var isLoaded = AtomicBoolean(false)

        // Reload delay, increases exponentially in onPageStarted
        private var reloadDelay = INITIAL_RELOAD_DELAY

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val intent = Intent(Intent.ACTION_VIEW, request.url)
            view.context.startActivity(intent)
            return true
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
                ketchWebView.listener?.onClose(HideExperienceStatus.None)
                ketchWebView.kill()
            }

            return true
        }

        @SuppressLint("RequiresFeature")
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
        bottomPadding: Int?,
        topPadding: Int?,
        cssStyle: String?
    ) {
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
            identities = identities.map { identity ->
                "${identity.key}: \"${identity.value}\""
            }.joinToString(separator = ",\n", prefix = "\n", postfix = "\n"),
            region = region,
            environment = environment,
            forceShow = forceShow?.getUrlParameter(),
            preferencesTabs = preferencesTabs.takeIf { it.isNotEmpty() }?.map { it.getUrlParameter() }
                ?.joinToString(","),
            preferencesTab = preferencesTab?.getUrlParameter(),
            bottomPadding = bottomPaddingPx,
            topPadding = topPaddingPx,
            cssStyleOverride = cssStyle
        )

        loadDataWithBaseURL("http://localhost", indexHtml, "text/html", "UTF-8", null)
    }

    private class PreferenceCenterJavascriptInterface(private val ketchWebView: KetchWebView) {
        @JavascriptInterface
        fun hideExperience(status: String?) {
            // Determine the hideExperience event status
            val parsedStatus = parseHideExperienceStatus(status)
            Log.d(TAG, "hideExperience: $status = ${parsedStatus.name}")
            runOnMainThread {
                ketchWebView.listener?.onClose(parsedStatus)
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
            Log.d(TAG, "willShowExperience: $type = ${parsedType.name}")
            runOnMainThread {
                if (parsedType === WillShowExperienceType.ConsentExperience) {
                    ketchWebView.listener?.showConsent()
                } else {
                    ketchWebView.listener?.showPreferences()
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
        }

        @JavascriptInterface
        fun showPreferenceExperience(showPreferenceExperience: String?) {
            Log.d(TAG, "showPreferenceExperience: $showPreferenceExperience")
        }

        @JavascriptInterface
        fun onConfigLoaded(configJson: String?) {
            Log.d(TAG, "onConfigLoaded: $configJson")

            try {
                val config = GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
                    .setPrettyPrinting()
                    .create()
                    .fromJson(configJson, KetchConfig::class.java)
                Log.d(TAG, "config: $config")
                runOnMainThread {
                    ketchWebView.listener?.onConfigUpdated(config)
                }
            } catch (ex: JsonParseException) {
                Log.e(TAG, ex.message, ex)
            }
        }

        @JavascriptInterface
        fun tapOutside(dialogSize: String?) {
            Log.d(TAG, "tapOutside: $dialogSize")
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

        private fun parseIabTcfGpp(json: String): Map<String, String>? {
            val gson = GsonBuilder()
                .create()

            val map = gson.fromJson(json, Array<Any>::class.java)
                .filter {
                    it is Map<*, *>
                }
                .map {
                    it as? Map<String, String>
                }.firstOrNull()

            return map
        }

        private fun runOnMainThread(action: () -> Unit) {
            Handler(Looper.getMainLooper()).post {
                action.invoke()
            }
        }
    }

    interface WebViewListener {
        fun showConsent()
        fun showPreferences()
        fun onUSPrivacyUpdated(values: Map<String, Any?>)
        fun onTCFUpdated(values: Map<String, Any?>)
        fun onGPPUpdated(values: Map<String, Any?>)
        fun onConfigUpdated(config: KetchConfig?)
        fun onEnvironmentUpdated(environment: String?)
        fun onRegionInfoUpdated(regionInfo: String?)
        fun onJurisdictionUpdated(jurisdiction: String?)
        fun onIdentitiesUpdated(identities: String?)
        fun onConsentUpdated(consent: Consent)
        fun onError(errMsg: String?)
        fun changeDialog(display: ContentDisplay)
        fun onClose(status: HideExperienceStatus)
        fun onWillShowExperience(experienceType: WillShowExperienceType)
        fun onHasShownExperience()
        fun onTapOutside()
    }

    internal enum class ExperienceType {
        CONSENT,
        PREFERENCES;

        fun getUrlParameter(): String = when (this) {
            CONSENT -> "cd"
            PREFERENCES -> "preferences"
        }
    }

    companion object {
        private val TAG: String = KetchWebView::class.java.simpleName
    }
}