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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class KetchWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    var listener: WebViewListener? = null
    private val localContentWebViewClient = LocalContentWebViewClient()
    private var isPageLoaded = false
    private var currentUrl: String? = null

    init {
        webViewClient = localContentWebViewClient
        setupWebView()

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

    private fun setupWebView() {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setGeolocationEnabled(false)
            mediaPlaybackRequiresUserGesture = false
        }
    }

    override fun loadUrl(url: String) {
        currentUrl = url
        isPageLoaded = false
        super.loadUrl(url)
    }

    fun setDebugMode() {
        setWebContentsDebuggingEnabled(true)
    }

    // Properly clean up WebView resources to prevent memory leaks and renderer crashes
    override fun destroy() {
        try {
            Log.d(TAG, "Beginning WebView cleanup")
            
            // Prevent further page loads
            stopLoading()
            
            // Add a blank/empty handler for JS errors during cleanup
            evaluateJavascript("window.onerror = function(message, url, line, column, error) { return true; };", null)
            
            // Remove JavaScript interface first to prevent any further callbacks
            removeJavascriptInterface("androidListener")
            
            // Set listener to null to prevent callbacks during cleanup
            listener = null
            
            // Cancel all coroutines next
            localContentWebViewClient.cancelCoroutines()
            
            // Disable JavaScript to prevent further execution
            settings.javaScriptEnabled = false
            
            // Stop any ongoing loads or processing
            onPause()
            
            // Small pause to allow WebView internals to stabilize
            try {
                Thread.sleep(50)
            } catch (e: InterruptedException) {
                Log.e(TAG, "Sleep interrupted during WebView cleanup", e)
            }
            
            // Clear WebView state
            clearHistory()
            clearCache(true)
            clearFormData()
            clearSslPreferences()
            
            // Remove all views
            removeAllViews()
            
            // Set a low global layout limit to reduce memory pressure
            // This is a common practice for WebView cleanup to ensure the view doesn't
            // maintain large layout allocations while waiting for destruction
            setLayoutParams(
                ViewGroup.LayoutParams(1, 1)
            )
            
            // Finally call the parent WebView's destroy method
            super.destroy()
            
            // Suggest garbage collection to reclaim memory
            Runtime.getRuntime().gc()
            
            Log.d(TAG, "WebView cleanup completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during WebView cleanup: ${e.message}", e)
        }
    }

    class LocalContentWebViewClient : WebViewClientCompat() {

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val intent = Intent(Intent.ACTION_VIEW, request.url)
            view.context.startActivity(intent)
            return true
        }

        override fun onLoadResource(view: WebView?, url: String?) {
            super.onLoadResource(view, url)
            Log.d(TAG, "onLoadResource: $url")
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

            if (url == currentUrl) {
                isPageLoaded = false
            }
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)

            if (url == currentUrl && !isPageLoaded) {
                isPageLoaded = true
                onPageLoadedListener?.onPageLoaded()
            }
            Log.d(TAG, "onPageFinished: $url")
        }

        // Cancel all coroutines
        fun cancelCoroutines() {
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
        bottomPadding: Int?
    ) {
        clearCache(true)

        // Convert padding value to string
        var bottomPaddingPx = "0px"
        if (bottomPadding != null) {
            bottomPaddingPx = bottomPadding.toString() + "px"
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
            preferencesTabs = preferencesTabs.takeIf { it.isNotEmpty() }?.map { it.getUrlParameter() }?.joinToString(","),
            preferencesTab = preferencesTab?.getUrlParameter(),
            bottomPadding = bottomPaddingPx
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
        fun onPageLoaded()
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