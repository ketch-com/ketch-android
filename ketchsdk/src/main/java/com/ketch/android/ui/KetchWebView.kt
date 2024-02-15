package com.ketch.android.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.ketch.android.Ketch
import com.ketch.android.data.Consent
import com.ketch.android.data.ContentDisplay
import com.ketch.android.data.KetchConfig


@SuppressLint("SetJavaScriptEnabled")
class KetchWebView(context: Context) : WebView(context) {
    private lateinit var orgCode: String
    private lateinit var property: String
    private var environment: String? = null
    private var ketchUrl: String? = null
    private lateinit var logLevel: Ketch.LogLevel
    private var identities: Map<String, String> = emptyMap()
    private var forceShow: ExperienceType? = null
    private var preferencesTabs: List<Ketch.PreferencesTab> = emptyList()
    private var preferencesTab: Ketch.PreferencesTab? = null
    private var language: String = ENGLISH
    private var jurisdiction: String? = null
    private var region: String? = null

    var listener: KetchListener? = null
        set(value) {
            field = value
            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(context))
                .build()

            value?.let {
                webViewClient = LocalContentWebViewClient(assetLoader, it)
            }
        }

    init {
        settings.javaScriptEnabled = true
        setBackgroundColor(context.getColor(android.R.color.transparent))

        setWebContentsDebuggingEnabled(true)

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

    private class LocalContentWebViewClient(
        private val assetLoader: WebViewAssetLoader,
        private val listener: KetchListener
    ) : WebViewClientCompat() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val intent = Intent(Intent.ACTION_VIEW, request.url)
            view.context.startActivity(intent)
            return true
        }

        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest
        ): WebResourceResponse? {
            return assetLoader.shouldInterceptRequest(request.url)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            listener.onLoad()
        }
    }

    fun load(
        orgCode: String,
        property: String,
        environment: String?,
        identities: Map<String, String> = emptyMap(),
        ketchUrl: String?,
        logLevel: Ketch.LogLevel
    ) {
        this.orgCode = orgCode
        this.property = property
        this.environment = environment
        this.identities = identities
        this.ketchUrl = ketchUrl
        this.logLevel = logLevel
        load()
    }

    internal fun forceShow(forceShow: ExperienceType?) {
        this.forceShow = forceShow
        this.preferencesTabs = emptyList()
        this.preferencesTab = null
        load()
    }

    internal fun showPreferencesTab(tabs: List<Ketch.PreferencesTab>, tab: Ketch.PreferencesTab) {
        this.forceShow = ExperienceType.PREFERENCES
        this.preferencesTabs = tabs
        this.preferencesTab = tab
        load()
    }

    fun setLanguage(language: String) {
        this.forceShow = null
        this.preferencesTab = null
        this.preferencesTabs = emptyList()
        this.language = language
    }

    fun setJurisdiction(jurisdiction: String?) {
        this.forceShow = null
        this.preferencesTab = null
        this.preferencesTabs = emptyList()
        this.jurisdiction = jurisdiction
    }

    fun setRegion(region: String?) {
        this.forceShow = null
        this.preferencesTab = null
        this.preferencesTabs = emptyList()
        this.region = region
    }

    private fun load() {
        //pass in the property code and  to be used with the Ketch Smart Tag
        var url =
            "https://appassets.androidplatform.net/assets/index.html?ketch_lang=$language&orgCode=$orgCode&propertyName=$property&ketch_log=${logLevel.name}"

        ketchUrl?.let {
            url += "&ketch_mobilesdk_url=${it}"
        }

        jurisdiction?.let {
            url += "&ketch_jurisdiction=$it"
        }

        identities.forEach { identity ->
            url += "&${identity.key}=${identity.value}"
        }

        region?.let {
            url += "&ketch_region=$it"
        }

        environment?.let {
            url += "&ketch_env=$environment"
        }

        forceShow?.let {
            url += "&ketch_show=${it.getUrlParameter()}"
            if (preferencesTabs.isNotEmpty()) {
                url += "&ketch_preferences_tabs=${
                    preferencesTabs.map { it.getUrlParameter() }.joinToString(",")
                }"
            }
            preferencesTab?.let {
                url += "&ketch_preferences_tab=${it.getUrlParameter()}"
            }
        }

        Log.d(TAG, "load: $url")

        clearCache(true)

        loadUrl(url)
    }

    private class PreferenceCenterJavascriptInterface(private val ketchWebView: KetchWebView) {
        @JavascriptInterface
        fun hideExperience(status: String?) {
            Log.d(TAG, "hideExperience: $status")
            if (status?.equals(CLOSE, ignoreCase = true) == true
                || status?.equals(SET_CONSENT, ignoreCase = true) == true
            ) {
                runOnMainThread {
                    ketchWebView.listener?.onClose()
                }
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
        fun willShowExperience(willShowExperience: String?) {
            Log.d(TAG, "willShowExperience: $willShowExperience")
            runOnMainThread {
                if (willShowExperience == "experiences.consent") {
                    ketchWebView.listener?.showConsent()
                } else {
                    ketchWebView.listener?.showPreferences()
                }
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
        fun hasChangedExperience(experience: String?) {
            // experiencedisplays.modal
            // experiencedisplays.banner
            // experiencedisplays.preference
            Log.d(TAG, "hasChangedExperience: $experience")
            if (experience == "experiencedisplays.preference") {
                runOnMainThread {
                    ketchWebView.listener?.showPreferences()
                }
            } else {
                val dialogType = when (experience) {
                    "experiencedisplays.modal" -> ContentDisplay.Modal
                    "experiencedisplays.banner" -> ContentDisplay.Banner
                    else -> null
                }

                dialogType?.let { display ->
                    runOnMainThread {
                        ketchWebView.listener?.changeDialog(display)
                    }
                }
            }
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
        fun usprivacy_updated(usPrivacyString: String?) {
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
        fun tcf_updated(tcfString: String?) {
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
        fun gpp_updated(gppString: String?) {
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

    interface KetchListener {
        fun onLoad()
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
        fun onClose()
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
        private const val CLOSE = "close"
        private const val SET_CONSENT = "setConsent"
        private const val ENGLISH = "en"
    }
}