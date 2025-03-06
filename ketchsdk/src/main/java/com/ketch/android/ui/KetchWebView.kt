package com.ketch.android.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
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
    internal var isPageLoaded = false
    internal var currentUrl: String? = null

    init {
        webViewClient = localContentWebViewClient
        setupWebView()
        
        // Ensure the WebView background is transparent
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        
        // Configure hardware acceleration properly
        setLayerType(LAYER_TYPE_HARDWARE, null)
        
        // Ensure that the WebView renders properly
        setWillNotDraw(false)

        // Add JavaScript interface for communication with WebView
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
            Log.d(TAG, "Beginning WebView destroy")
            
            // CRITICAL: Reset touch listener FIRST to ensure touches pass through
            // This must be the first operation to guarantee it happens even if other steps fail
            setOnTouchListener(null)
            
            // Disable hardware acceleration which can cause blocking issues
            try {
                setLayerType(LAYER_TYPE_NONE, null)
            } catch (e: Exception) {
                Log.e(TAG, "Error disabling hardware acceleration: ${e.message}")
            }
            
            // Prevent further page loads
            stopLoading()
            
            // Add a blank/empty handler for JS errors during cleanup
            try {
                evaluateJavascript("window.onerror = function(message, url, line, column, error) { return true; };", null)
            } catch (e: Exception) {
                Log.e(TAG, "Error setting JS error handler: ${e.message}")
            }
            
            // Remove JavaScript interface first to prevent any further callbacks
            try {
                removeJavascriptInterface("androidListener")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing JS interface: ${e.message}")
            }
            
            // Set listener to null to prevent callbacks during cleanup
            listener = null
            
            // Cancel all coroutines next
            try {
                localContentWebViewClient.cancelCoroutines()
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling coroutines: ${e.message}")
            }
            
            // Disable JavaScript to prevent further execution
            try {
                settings.javaScriptEnabled = false
            } catch (e: Exception) {
                Log.e(TAG, "Error disabling JavaScript: ${e.message}")
            }
            
            // Stop any ongoing loads or processing
            try {
                onPause()
            } catch (e: Exception) {
                Log.e(TAG, "Error pausing WebView: ${e.message}")
            }
            
            // Clear WebView state
            try {
                clearHistory()
                clearCache(true)
                clearFormData()
                clearSslPreferences()
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing WebView state: ${e.message}")
            }
            
            // Remove all views
            try {
                removeAllViews()
            } catch (e: Exception) {
                Log.e(TAG, "Error removing views: ${e.message}")
            }
            
            // Set a low global layout limit to reduce memory pressure
            try {
                setLayoutParams(
                    ViewGroup.LayoutParams(1, 1)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error setting layout params: ${e.message}")
            }
            
            // Finally call the parent WebView's destroy method
            try {
                super.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error in super.destroy(): ${e.message}")
            }
            
            Log.d(TAG, "WebView destroy completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during WebView destroy: ${e.message}", e)
        } finally {
            // CRITICAL: Reset touch listener AGAIN in finally block to ensure it happens
            // This is our last line of defense to prevent touch blocking
            try {
                setOnTouchListener(null)
            } catch (e: Exception) {
                Log.e(TAG, "Final attempt to reset touch listener failed: ${e.message}", e)
            }
        }
    }

    class LocalContentWebViewClient : WebViewClientCompat() {
        private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        private val isRetrying = AtomicBoolean(false)

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

            if (view is KetchWebView && url == view.currentUrl) {
                view.isPageLoaded = false
            }
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)

            if (view is KetchWebView && url == view.currentUrl && !view.isPageLoaded) {
                view.isPageLoaded = true
            }
            Log.d(TAG, "onPageFinished: $url")
        }

        // Cancel all coroutines
        fun cancelCoroutines() {
            coroutineScope.cancel()
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
        /**
         * @deprecated This method is deprecated and will be removed in a future release
         */
        @Deprecated("This method is deprecated and will be removed in a future release")
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