package com.ketch.android.integration.tests

import android.app.Application
import android.util.Log
import com.ketch.android.Ketch
import com.ketch.android.KetchSdk
import com.ketch.android.data.Consent
import com.ketch.android.data.HideExperienceStatus
import com.ketch.android.data.KetchConfig
import com.ketch.android.data.WillShowExperienceType

class IntegrationTestApp : Application() {

    lateinit var ketch: Ketch
        private set

    var testEventListener: TestEventListener? = null

    override fun onCreate() {
        super.onCreate()
        ketch = KetchSdk.create(
            context = this,
            organization = ORG_CODE,
            property = PROPERTY,
            environment = ENVIRONMENT,
            listener = ketchListener,
            ketchUrl = null,
            logLevel = Ketch.LogLevel.DEBUG,
        )
        ketch.setIdentities(mapOf("aaid" to "test-123"))
    }

    private val ketchListener = object : Ketch.Listener {
        override fun onShow() {
            Log.d(TAG, "Dialog shown")
            testEventListener?.onShow()
        }

        override fun onDismiss(status: HideExperienceStatus) {
            Log.d(TAG, "Dialog dismissed with status: $status")
            testEventListener?.onDismiss(status)
        }

        override fun onConfigUpdated(config: KetchConfig?) {
            Log.d(TAG, "Config updated: $config")
            testEventListener?.onConfigUpdated()
        }

        override fun onEnvironmentUpdated(environment: String?) {
            Log.d(TAG, "Environment updated: $environment")
            testEventListener?.onEnvironmentUpdated(environment ?: "")
        }

        override fun onRegionInfoUpdated(regionInfo: String?) {
            Log.d(TAG, "Region info updated: $regionInfo")
        }

        override fun onJurisdictionUpdated(jurisdiction: String?) {
            Log.d(TAG, "Jurisdiction updated: $jurisdiction")
        }

        override fun onIdentitiesUpdated(identities: String?) {
            Log.d(TAG, "Identities updated: $identities")
        }

        override fun onConsentUpdated(consent: Consent) {
            Log.d(TAG, "Consent updated: $consent")
            testEventListener?.onConsentUpdated(consent.purposes.toString())
            testEventListener?.onConsentUpdated(consent)
        }

        override fun onError(errMsg: String?) {
            Log.e(TAG, "Error: $errMsg")
            testEventListener?.onError(errMsg ?: "Unknown error")
        }

        override fun onUSPrivacyUpdated(values: Map<String, Any?>) {
            Log.d(TAG, "US Privacy updated: $values")
        }

        override fun onTCFUpdated(values: Map<String, Any?>) {
            Log.d(TAG, "TCF updated: $values")
        }

        override fun onGPPUpdated(values: Map<String, Any?>) {
            Log.d(TAG, "GPP updated: $values")
        }

        override fun onWillShowExperience(type: WillShowExperienceType) {
            Log.d(TAG, "Will show experience: $type")
            testEventListener?.onWillShowExperience(type)
        }

        override fun onHasShownExperience() {
            Log.d(TAG, "Has shown experience")
            testEventListener?.onHasShownExperience()
        }
    }

    fun setTestMode(listener: TestEventListener) {
        testEventListener = listener
    }

    fun clearTestMode() {
        testEventListener = null
    }

    fun validateWebViewContent(expectedInnerElementId: String, callback: (Boolean) -> Unit) {
        try {
            val ketchClass = ketch::class.java
            val activeWebViewField = ketchClass.getDeclaredField("activeWebView")
            activeWebViewField.isAccessible = true
            val activeWebView = activeWebViewField.get(ketch) as? android.webkit.WebView

            if (activeWebView != null) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    val containerJavascript = "document.getElementById('lanyard_root') !== null"
                    activeWebView.evaluateJavascript(containerJavascript) { containerResult ->
                        if (containerResult == "true") {
                            val innerJavascript =
                                "document.getElementById('$expectedInnerElementId') !== null"
                            activeWebView.evaluateJavascript(innerJavascript) { innerResult ->
                                val innerElementExists = innerResult == "true"
                                Log.d(
                                    TAG,
                                    "Container 'lanyard_root' exists: true, Inner element " +
                                        "'$expectedInnerElementId' exists: $innerElementExists"
                                )
                                callback(innerElementExists)
                            }
                        } else {
                            Log.d(TAG, "Container 'lanyard_root' not found")
                            callback(false)
                        }
                    }
                }, 2000)
            } else {
                Log.d(TAG, "No active webview found")
                callback(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating webview content: ${e.message}")
            callback(false)
        }
    }

    fun checkForConsentBanner(callback: (Boolean) -> Unit) {
        validateWebViewContent("ketch-consent-banner", callback)
    }

    fun checkForPreferencesCenter(callback: (Boolean) -> Unit) {
        validateWebViewContent("ketch-preferences", callback)
    }

    fun isShowingExperience(): Boolean = try {
        val field = ketch.javaClass.getDeclaredField("isShowingExperience")
        field.isAccessible = true
        field.getBoolean(ketch)
    } catch (e: Exception) {
        Log.e(TAG, "Error reading isShowingExperience: ${e.message}")
        false
    }

    fun hasActiveWebView(): Boolean = try {
        val field = ketch.javaClass.getDeclaredField("activeWebView")
        field.isAccessible = true
        field.get(ketch) != null
    } catch (e: Exception) {
        Log.e(TAG, "Error reading activeWebView: ${e.message}")
        false
    }

    fun getActiveWebViewIdentityHash(): Int? = try {
        val field = ketch.javaClass.getDeclaredField("activeWebView")
        field.isAccessible = true
        System.identityHashCode(field.get(ketch))
    } catch (e: Exception) {
        Log.e(TAG, "Error reading activeWebView identity: ${e.message}")
        null
    }

    fun getPageLoadCount(): Int = try {
        val ketchClass = ketch.javaClass
        val activeWebViewField = ketchClass.getDeclaredField("activeWebView")
        activeWebViewField.isAccessible = true
        val webView = activeWebViewField.get(ketch) as? com.ketch.android.ui.KetchWebView
        webView?.pageLoadCount ?: 0
    } catch (e: Exception) {
        Log.e(TAG, "Error reading pageLoadCount: ${e.message}")
        0
    }

    fun getLoadedSignature(): String? = try {
        val field = ketch.javaClass.getDeclaredField("loadedSignature")
        field.isAccessible = true
        field.get(ketch) as? String
    } catch (e: Exception) {
        Log.e(TAG, "Error reading loadedSignature: ${e.message}")
        null
    }

    fun hasTapOutsideBridgeMethod(): Boolean = try {
        val ketchClass = ketch.javaClass
        val activeWebViewField = ketchClass.getDeclaredField("activeWebView")
        activeWebViewField.isAccessible = true
        val webView = activeWebViewField.get(ketch) ?: return false
        webView.javaClass.methods.any { it.name == "tapOutside" }
    } catch (e: Exception) {
        false
    }

    fun updateIdentitiesWithUniqueValue() {
        val uniqueId = java.util.UUID.randomUUID().toString()
        ketch.setIdentities(mapOf("aaid" to uniqueId))
        Log.d(TAG, "Updated identities with unique ID: $uniqueId")
    }

    fun clickButtonById(buttonId: String, callback: (Boolean) -> Unit) {
        try {
            val ketchClass = ketch::class.java
            val activeWebViewField = ketchClass.getDeclaredField("activeWebView")
            activeWebViewField.isAccessible = true
            val activeWebView = activeWebViewField.get(ketch) as? android.webkit.WebView

            if (activeWebView != null) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    val javascript = """
                        (function() {
                            var button = document.getElementById('$buttonId');
                            if (button) {
                                button.click();
                                return true;
                            }
                            return false;
                        })();
                    """
                    activeWebView.evaluateJavascript(javascript) { result ->
                        val buttonClicked = result == "true"
                        Log.d(TAG, "Button with ID '$buttonId' click result: $buttonClicked")
                        callback(buttonClicked)
                    }
                }, 1000)
            } else {
                Log.d(TAG, "No active webview found for button click")
                callback(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clicking button by ID: ${e.message}")
            callback(false)
        }
    }

    interface TestEventListener {
        fun onConsentUpdated(consent: String) {}
        fun onConsentUpdated(consent: Consent) {}
        fun onEnvironmentUpdated(environment: String) {}
        fun onConfigUpdated() {}
        fun onShow() {}
        fun onError(error: String) {}
        fun onWillShowExperience(type: WillShowExperienceType) {}
        fun onHasShownExperience() {}
        fun onWebViewContentValidated(elementId: String, exists: Boolean) {}
        fun onDismiss(status: HideExperienceStatus) {}
    }

    companion object {
        private const val TAG = "KetchIntegrationTests"
        const val ORG_CODE = "ketch_samples"
        const val PROPERTY = "android"
        const val ENVIRONMENT = "production"
    }
}
