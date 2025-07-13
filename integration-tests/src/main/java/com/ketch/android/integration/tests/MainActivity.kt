package com.ketch.android.integration.tests

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ketch.android.Ketch
import com.ketch.android.KetchSdk
import com.ketch.android.data.Consent
import com.ketch.android.data.HideExperienceStatus
import com.ketch.android.data.KetchConfig
import com.ketch.android.data.WillShowExperienceType
import com.ketch.android.integration.tests.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var ketch: Ketch
    
    // Test mode support for integration tests
    private var testEventListener: TestEventListener? = null

    companion object {
        private const val TAG = "KetchIntegrationTests"
        // Test configuration - these should be replaced with actual test values
        private const val ORG_CODE = "ketch_samples"
        private const val PROPERTY = "android"
        private const val ENVIRONMENT = "development"
    }

    private val ketchListener = object : Ketch.Listener {
        override fun onShow() {
            Log.d(TAG, "Dialog shown")
            runOnUiThread {
                binding.statusText.text = "Dialog shown"
                testEventListener?.onShow()
            }
        }

        override fun onDismiss(status: HideExperienceStatus) {
            Log.d(TAG, "Dialog dismissed with status: $status")
            runOnUiThread {
                binding.statusText.text = "Dialog dismissed: $status"
                testEventListener?.onDismiss(status)
            }
        }

        override fun onConfigUpdated(config: KetchConfig?) {
            Log.d(TAG, "Config updated: $config")
            runOnUiThread {
                binding.statusText.text = "Config updated"
                testEventListener?.onConfigUpdated()
            }
        }

        override fun onEnvironmentUpdated(environment: String?) {
            Log.d(TAG, "Environment updated: $environment")
            runOnUiThread {
                binding.environmentText.text = "Environment: $environment"
                testEventListener?.onEnvironmentUpdated(environment ?: "")
            }
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
            runOnUiThread {
                binding.consentText.text = "Consent: ${consent.purposes}"
                testEventListener?.onConsentUpdated(consent.purposes.toString())
                testEventListener?.onConsentUpdated(consent)
            }
        }

        override fun onError(errMsg: String?) {
            Log.e(TAG, "Error: $errMsg")
            runOnUiThread {
                binding.statusText.text = "Error: $errMsg"
                testEventListener?.onError(errMsg ?: "Unknown error")
            }
        }

        override fun onUSPrivacyUpdated(values: Map<String, Any?>) {
            Log.d(TAG, "US Privacy updated: $values")
            runOnUiThread {
                binding.usPrivacyText.text = "US Privacy: ${values["IABUSPrivacy_String"]}"
            }
        }

        override fun onTCFUpdated(values: Map<String, Any?>) {
            Log.d(TAG, "TCF updated: $values")
            runOnUiThread {
                binding.tcfText.text = "TCF: ${values["IABTCF_TCString"]}"
            }
        }

        override fun onGPPUpdated(values: Map<String, Any?>) {
            Log.d(TAG, "GPP updated: $values")
            runOnUiThread {
                binding.gppText.text = "GPP: ${values["IABGPP_HDR_GppString"]}"
            }
        }

        override fun onWillShowExperience(type: WillShowExperienceType) {
            Log.d(TAG, "Will show experience: $type")
            runOnUiThread {
                binding.statusText.text = "Will show experience: $type"
                testEventListener?.onWillShowExperience(type)
            }
        }

        override fun onHasShownExperience() {
            Log.d(TAG, "Has shown experience")
            runOnUiThread {
                binding.statusText.text = "Experience has been shown"
                testEventListener?.onHasShownExperience()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeTextViews()
        initializeKetch()
        setupClickListeners()
    }

    private fun initializeTextViews() {
        // Set initial values for all text views
        binding.environmentText.text = "Environment: Not set"
        binding.consentText.text = "Consent: Not set"
        binding.usPrivacyText.text = "US Privacy: Not set"
        binding.tcfText.text = "TCF: Not set"
        binding.gppText.text = "GPP: Not set"
    }

    private fun initializeKetch() {
        ketch = KetchSdk.create(
            this,
            supportFragmentManager,
            ORG_CODE,
            PROPERTY,
            ENVIRONMENT,
            ketchListener,
            null, // Use default ketch URL
            Ketch.LogLevel.DEBUG
        )

        // Set test identities
        ketch.setIdentities(
            mapOf(
                "aaid" to "test-123",
            )
        )

        binding.statusText.text = "Ketch initialized"
    }

    private fun setupClickListeners() {
        binding.loadButton.setOnClickListener {
            ketch.load()
            binding.statusText.text = "Load called"
        }

        binding.showConsentButton.setOnClickListener {
            ketch.showConsent()
            binding.statusText.text = "Show consent called"
        }

        binding.showPreferencesButton.setOnClickListener {
            ketch.showPreferences()
            binding.statusText.text = "Show preferences called"
        }



        binding.setLanguageButton.setOnClickListener {
            ketch.setLanguage("EN")
            binding.statusText.text = "Language set to EN"
        }

        binding.setJurisdictionButton.setOnClickListener {
            ketch.setJurisdiction("US")
            binding.statusText.text = "Jurisdiction set to US"
        }

        binding.setRegionButton.setOnClickListener {
            ketch.setRegion("California")
            binding.statusText.text = "Region set to California"
        }
    }
    
    // Test mode support - allows integration tests to listen for SDK events
    fun setTestMode(listener: TestEventListener) {
        this.testEventListener = listener
    }
    
    fun clearTestMode() {
        this.testEventListener = null
    }
    
    // Method to validate webview content for testing
    fun validateWebViewContent(expectedInnerElementId: String, callback: (Boolean) -> Unit) {
        // Use reflection to access the active webview from the Ketch SDK
        try {
            val ketchClass = ketch::class.java
            val activeWebViewField = ketchClass.getDeclaredField("activeWebView")
            activeWebViewField.isAccessible = true
            val activeWebView = activeWebViewField.get(ketch) as? android.webkit.WebView
            
            if (activeWebView != null) {
                // Add a small delay to allow JavaScript to fully load and create DOM elements
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    // First check for the container
                    val containerJavascript = "document.getElementById('lanyard_root') !== null"
                    activeWebView.evaluateJavascript(containerJavascript) { containerResult ->
                        if (containerResult == "true") {
                            // Then check for the specific inner element
                            val innerJavascript = "document.getElementById('$expectedInnerElementId') !== null"
                            activeWebView.evaluateJavascript(innerJavascript) { innerResult ->
                                val innerElementExists = innerResult == "true"
                                Log.d(TAG, "Container 'lanyard_root' exists: true, Inner element '$expectedInnerElementId' exists: $innerElementExists")
                                callback(innerElementExists)
                            }
                        } else {
                            Log.d(TAG, "Container 'lanyard_root' not found")
                            callback(false)
                        }
                    }
                }, 2000) // Wait 2 seconds for JS to load
            } else {
                Log.d(TAG, "No active webview found")
                callback(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating webview content: ${e.message}")
            callback(false)
                }
    }
    
    // Helper function to check for consent banner
    fun checkForConsentBanner(callback: (Boolean) -> Unit) {
        validateWebViewContent("ketch-consent-banner", callback)
    }
    
    // Helper function to check for preferences center
    fun checkForPreferencesCenter(callback: (Boolean) -> Unit) {
        validateWebViewContent("ketch-preferences", callback)
    }
    
    // Helper function to update identities to unique value
    fun updateIdentitiesWithUniqueValue() {
        val uniqueId = java.util.UUID.randomUUID().toString()
        ketch.setIdentities(mapOf("aaid" to uniqueId))
        Log.d(TAG, "Updated identities with unique ID: $uniqueId")
    }
    
    // Helper function to find and click button by ID in webview
    fun clickButtonById(buttonId: String, callback: (Boolean) -> Unit) {
        // Use reflection to access the active webview from the Ketch SDK
        try {
            val ketchClass = ketch::class.java
            val activeWebViewField = ketchClass.getDeclaredField("activeWebView")
            activeWebViewField.isAccessible = true
            val activeWebView = activeWebViewField.get(ketch) as? android.webkit.WebView
            
            if (activeWebView != null) {
                // Add a small delay to ensure DOM is fully loaded
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    // JavaScript to find and click button by ID
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
                }, 1000) // Wait 1 second for DOM to be ready
            } else {
                Log.d(TAG, "No active webview found for button click")
                callback(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clicking button by ID: ${e.message}")
            callback(false)
        }
    }
    
    // Interface for test event listening
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
} 