package com.ketch.android

import android.content.Context
import android.os.Handler
import android.util.Log
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.ketch.android.data.Consent
import com.ketch.android.data.ContentDisplay
import com.ketch.android.data.HideExperienceStatus
import com.ketch.android.data.KetchConfig
import com.ketch.android.data.WillShowExperienceType
import com.ketch.android.ui.KetchDialogFragment
import com.ketch.android.ui.KetchWebView
import java.lang.ref.WeakReference

/**
 * Main Ketch SDK class
 **/
class Ketch private constructor(
    private val context: WeakReference<Context>,
    private val fragmentManager: WeakReference<FragmentManager>,
    private val orgCode: String,
    private val property: String,
    private val environment: String?,
    private val listener: Listener?,
    private val ketchUrl: String?,
    private val logLevel: LogLevel
) {
    private var identities: Map<String, String> = emptyMap()
    private var language: String? = null
    private var jurisdiction: String? = null
    private var region: String? = null
    
    // Add a WebView instance and dialog state flag
    private var currentWebView: KetchWebView? = null
    private var isActive = false

    /**
     * Retrieve a String value from the preferences.
     *
     * @param key The name of the preference to retrieve.
     *
     * @return Returns the preference value if it exists
     */
    fun getSavedString(key: String) = getPreferences().getSavedValue(key)

    /**
     * Retrieve IABTCF_TCString value from the preferences.
     *
     * @return Returns the preference value if it exists
     */
    fun getTCFTCString() = getPreferences().getSavedValue(KetchSharedPreferences.IAB_TCF_TC_STRING)

    /**
     * Retrieve IABUSPrivacy_String value from the preferences.
     *
     * @return Returns the preference value if it exists
     */
    fun getUSPrivacyString() =
        getPreferences().getSavedValue(KetchSharedPreferences.IAB_US_PRIVACY_STRING)

    /**
     * Retrieve IABGPP_HDR_GppString value from the preferences.
     *
     * @return Returns the preference value if it exists
     */
    fun getGPPHDRGppString() =
        getPreferences().getSavedValue(KetchSharedPreferences.IAB_GPP_HDR_GPP_STRING)

    /**
     * Loads a web page and shows a popup if necessary
     *
     * @param bottomPadding: Pixels of padding to add to the bottom of the experience (if shown)
     */
    fun load(
        shouldRetry: Boolean = false,
        synchronousPreferences: Boolean = false,
        bottomPadding: Int = 0,
    ): Boolean {
        val webView = createWebView(shouldRetry, synchronousPreferences)
        return if (webView != null) {
            webView.load(
                orgCode,
                property,
                language,
                jurisdiction,
                region,
                environment,
                identities,
                null,
                emptyList(),
                null,
                ketchUrl,
                logLevel,
                bottomPadding,
            )
            true
        } else {
            false
        }
    }

    /**
     * Display the consent, adding the fragment dialog to the given FragmentManager.
     *
     * @param bottomPadding: Pixels of padding to add to the bottom of the experience
     */
    fun showConsent(
        shouldRetry: Boolean = false,
        synchronousPreferences: Boolean = false,
        bottomPadding: Int = 0,
    ): Boolean {
        val webView = createWebView(shouldRetry, synchronousPreferences)
        return if (webView != null) {
            webView.load(
                orgCode,
                property,
                language,
                jurisdiction,
                region,
                environment,
                identities,
                KetchWebView.ExperienceType.CONSENT,
                emptyList(),
                null,
                ketchUrl,
                logLevel,
                bottomPadding
            )
            true
        } else {
            false
        }
    }

    /**
     * Display the preferences, adding the fragment dialog to the given FragmentManager.
     *
     * @param bottomPadding: Pixels of padding to add to the bottom of the experience
     */
    fun showPreferences(
        shouldRetry: Boolean = false,
        synchronousPreferences: Boolean = false,
        bottomPadding: Int = 0,
    ): Boolean {
        val webView = createWebView(shouldRetry, synchronousPreferences)
        return if (webView != null) {
            webView.load(
                orgCode,
                property,
                language,
                jurisdiction,
                region,
                environment,
                identities,
                KetchWebView.ExperienceType.PREFERENCES,
                emptyList(),
                null,
                ketchUrl,
                logLevel,
                bottomPadding
            )
            true
        } else {
            false
        }
    }

    /**
     * Display the preferences tab, adding the fragment dialog to the given FragmentManager.
     *
     * @param tabs: list of preferences tab
     * @param tab: the current tab
     * @param bottomPadding: Pixels of padding to add to the bottom of the experience
     */
    fun showPreferencesTab(
        tabs: List<PreferencesTab>,
        tab: PreferencesTab,
        shouldRetry: Boolean = false,
        synchronousPreferences: Boolean = false,
        bottomPadding: Int = 0,
    ): Boolean {
        val webView = createWebView(shouldRetry, synchronousPreferences)
        return if (webView != null) {
            webView.load(
                orgCode,
                property,
                language,
                jurisdiction,
                region,
                environment,
                identities,
                KetchWebView.ExperienceType.PREFERENCES,
                tabs,
                tab,
                ketchUrl,
                logLevel,
                bottomPadding
            )
            true
        } else {
            false
        }
    }

    /**
     * Dismiss the dialog
     */
    fun dismissDialog() {
        findDialogFragment()?.let {
            (it as? KetchDialogFragment)?.dismissAllowingStateLoss()
            Handler(android.os.Looper.getMainLooper()).postDelayed({
                // Ensure WebView is properly cleaned up
                currentWebView?.let { webView ->
                    Log.d(TAG, "Cleaning up WebView after dialog dismissal")
                    webView.setOnTouchListener { _, _ -> true } // Disable touch events during cleanup
                    webView.clearCache(true)
                    webView.clearHistory()
                    webView.destroy()
                    currentWebView = null
                }
                
                this@Ketch.listener?.onDismiss(HideExperienceStatus.None)
                isActive = false
            }, 100)
        } ?: run {
            // Even if no dialog is found, clean up any lingering WebView resources
            currentWebView?.let { webView ->
                Log.d(TAG, "Cleaning up orphaned WebView")
                webView.destroy()
                currentWebView = null
            }
            this@Ketch.listener?.onDismiss(HideExperienceStatus.None)
            isActive = false
        }
    }

    /**
     * Force cleanup of any existing dialog fragments - use this if dialogs appear to be stuck
     */
    fun forceCleanupDialogs() {
        // First, set isActive to false to allow new WebView creation
        isActive = false
        
        // Thoroughly clean up WebView
        if (currentWebView != null) {
            try {
                Log.d(TAG, "Force cleaning up WebView")
                currentWebView?.setOnTouchListener { _, _ -> true } // Disable touch interactions
                // Clear out all content and event handlers
                currentWebView?.evaluateJavascript(
                    """
                    (function() {
                        // Remove all event listeners from document and window
                        var oldElement = document.documentElement;
                        var newElement = oldElement.cloneNode(true);
                        oldElement.parentNode.replaceChild(newElement, oldElement);
                        
                        // Empty the body to remove any content
                        document.body.innerHTML = '';
                        
                        // Disable all pointer events
                        document.body.style.pointerEvents = 'none';
                        
                        // Remove any overlay elements that might block input
                        var overlays = document.querySelectorAll('.ketch-backdrop, .ketch-modal, .ketch-banner');
                        for (var i = 0; i < overlays.length; i++) {
                            if (overlays[i].parentNode) {
                                overlays[i].parentNode.removeChild(overlays[i]);
                            }
                        }
                    })();
                    """,
                    null
                )
                
                // Force WebView to reset its internal state
                currentWebView?.clearCache(true)
                currentWebView?.clearHistory()
                currentWebView?.clearFocus() // Clear focus to prevent lingering keyboard or focus issues
                currentWebView?.reload() // Force refresh the WebView to clear any stuck state
                
                // Schedule delayed destruction to ensure JS has executed
                Handler(android.os.Looper.getMainLooper()).postDelayed({
                    currentWebView?.destroy()
                    currentWebView = null
                    
                    // Suggest garbage collection
                    System.gc()
                }, 100)
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up WebView: ${e.message}", e)
                // Ensure WebView is nulled out even if error occurs
                currentWebView = null
            }
        }
        
        findDialogFragment()?.let { fragment ->
            try {
                if (fragment is KetchDialogFragment) {
                    fragment.dismissAllowingStateLoss()
                }
                
                // Ensure it's really removed if still hanging around
                Handler(android.os.Looper.getMainLooper()).postDelayed({
                    fragmentManager.get()?.let { fm ->
                        try {
                            // Check any fragment with our tag and remove it
                            fm.findFragmentByTag(KetchDialogFragment.TAG)?.let { foundFragment ->
                                if (foundFragment.isAdded) {
                                    Log.d(TAG, "Fragment still exists - force removing it")
                                    fm.beginTransaction()
                                        .remove(foundFragment)
                                        .commitNowAllowingStateLoss()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error force removing fragment: ${e.message}", e)
                        }
                    }
                }, 200)
            } catch (e: Exception) {
                Log.e(TAG, "Error in force cleanup: ${e.message}", e)
            }
        }
    }

    /**
     * Provides a complete reset of the SDK state.
     * Call this method if the UI becomes unresponsive to restore normal operation.
     */
    fun resetState() {
        Log.d(TAG, "Performing full SDK state reset")
        
        // First, aggressively clean up dialogs
        try {
            forceCleanupDialogs()
        } catch (e: Exception) {
            Log.e(TAG, "Error during force cleanup: ${e.message}", e)
        }
        
        // Make sure isActive flag is reset
        isActive = false
        
        // Clean up WebView
        try {
            if (currentWebView != null) {
                currentWebView?.destroy()
                currentWebView = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing WebView: ${e.message}", e)
            currentWebView = null
        }
        
        // Make one more attempt to cleanup fragments
        try {
            val fm = fragmentManager.get()
            if (fm != null) {
                val tag = KetchDialogFragment.TAG
                val fragment = fm.findFragmentByTag(tag)
                if (fragment != null) {
                    try {
                        fm.beginTransaction()
                            .remove(fragment)
                            .commitNowAllowingStateLoss()
                        Log.d(TAG, "Removed lingering fragment during reset")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error removing fragment: ${e.message}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during fragment cleanup: ${e.message}", e)
        }
        
        // Try to reclaim memory
        try {
            System.gc()
        } catch (e: Exception) {
            Log.e(TAG, "Error during GC: ${e.message}", e)
        }
        
        // Schedule status completion message
        try {
            Handler(android.os.Looper.getMainLooper()).postDelayed(
                { Log.d(TAG, "State reset completed") },
                100
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling completion message: ${e.message}", e)
        }
    }

    /**
     * Set identities
     *
     * @param identities: Map<String, String>
     */
    fun setIdentities(identities: Map<String, String>) {
        this.identities = identities
    }

    /**
     * Set the language
     *
     * @param language: a language name (EN, FR, etc.)
     */
    fun setLanguage(language: String?) {
        this.language = language
    }

    /**
     * Set the jurisdiction
     *
     * @param jurisdiction: the jurisdiction value
     */
    fun setJurisdiction(jurisdiction: String?) {
        this.jurisdiction = jurisdiction
    }

    /**
     * Set Region
     *
     * @param region: the region name
     */
    fun setRegion(region: String?) {
        this.region = region
    }

    init {
        getPreferences()

        // Aggressively clean up any existing dialogs that might be leftover
        fragmentManager.get()?.let { fm ->
            try {
                val existingDialog = fm.findFragmentByTag(KetchDialogFragment.TAG)
                if (existingDialog != null) {
                    Log.d(TAG, "Found existing dialog during initialization - removing it")
                    
                    if (existingDialog is KetchDialogFragment) {
                        existingDialog.dismissAllowingStateLoss()
                    } else {
                        // Non-KetchDialogFragment, still attempt to dismiss
                        try {
                            (existingDialog as? DialogFragment)?.dismissAllowingStateLoss()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error dismissing non-KetchDialogFragment: ${e.message}")
                        }
                    }
                    
                    // Force remove it from the fragment manager
                    fm.beginTransaction()
                        .remove(existingDialog)
                        .commitNowAllowingStateLoss()
                    
                    this@Ketch.listener?.onDismiss(HideExperienceStatus.None)
                } else {
                    // No existing dialog found, nothing to clean up
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up existing dialogs: ${e.message}", e)
            }
        }
        
        // Ensure the isActive flag is reset to a clean state
        isActive = false
    }

    // Get the singleton KetchSharedPreferences object
    private fun getPreferences(): KetchSharedPreferences {
        context.get()?.let {
            // Initialize will create KetchSharedPreferences if it doesn't already exist
            KetchSharedPreferences.initialize(it)
        }
        return KetchSharedPreferences
    }

    private fun createWebView(shouldRetry: Boolean = false, synchronousPreferences: Boolean = false): KetchWebView? {
        if (isActive) {
            Log.d(TAG, "WebView creation blocked - dialog operation in progress")
            return null
        }
        
        val existingDialog = findDialogFragment()
        if (existingDialog != null) {
            Log.d(TAG, "WebView creation blocked - dialog already exists")
            
            // Extra safety: if we detected a dialog but isActive is false,
            // force dismiss the dialog as it may be stuck
            if (!isActive) {
                Log.d(TAG, "Detected orphaned dialog - force cleaning up")
                (existingDialog as? KetchDialogFragment)?.dismissAllowingStateLoss()
                Handler(android.os.Looper.getMainLooper()).postDelayed({
                    forceCleanupDialogs()
                }, 100)
            }
            
            return null
        }
        
        isActive = true
        
        try {
            // Always create a new WebView instance for better reliability
            // This prevents potential issues with reuse of WebView instances
            if (currentWebView != null) {
                Log.d(TAG, "Cleaning up previous WebView instance")
                currentWebView?.destroy()
                currentWebView = null
                
                // Short delay to ensure cleanup is complete
                try {
                    Thread.sleep(50)
                } catch (e: InterruptedException) {
                    Log.e(TAG, "Sleep interrupted during WebView cleanup", e)
                }
            }
            
            val webView = context.get()?.let { KetchWebView(it, shouldRetry) } ?: run {
                isActive = false
                return null
            }
            
            currentWebView = webView

            if (logLevel === LogLevel.DEBUG) {
                webView.setDebugMode()
            }

            webView.listener = object : KetchWebView.WebViewListener {
            
                private var config: KetchConfig? = null
                private var showConsent: Boolean = false
                
                override fun showConsent() {
                    if (config == null) {
                        showConsent = true
                        return
                    }
                    showConsentPopup()
                }

                override fun showPreferences() {
                    if (!isActivityActive()) {
                        Log.d(TAG, "Not showing as activity is not active")
                        isActive = false
                        return
                    }

                    val existingDialog = findDialogFragment()
                    if (existingDialog != null) {
                        Log.d(TAG, "Not showing as dialog already exists")
                        
                        // Extra safety: ensure the dialog state is consistent
                        if (!isActive) {
                            Log.d(TAG, "Dialog exists but isActive=false - fixing state")
                            isActive = true
                        }
                        
                        return
                    }

                    val dialog = KetchDialogFragment.newInstance()

                    fragmentManager.get()?.let {
                        // Make sure any existing dialogs with the same tag are removed first
                        val existingFragment = it.findFragmentByTag(KetchDialogFragment.TAG)
                        if (existingFragment != null) {
                            try {
                                it.beginTransaction().remove(existingFragment).commitNow()
                                Log.d(TAG, "Removed existing fragment before showing new one")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error removing existing fragment: ${e.message}", e)
                            }
                        }
                        
                        dialog.show(it, webView)
                        this@Ketch.listener?.onShow()
                    } ?: run {
                        isActive = false
                    }
                }

                override fun onUSPrivacyUpdated(values: Map<String, Any?>) {
                    getPreferences().saveValues(values, "USPrivacy", synchronousPreferences)
                    this@Ketch.listener?.onUSPrivacyUpdated(values)
                }

                override fun onTCFUpdated(values: Map<String, Any?>) {
                    getPreferences().saveValues(values, "TCF", synchronousPreferences)
                    this@Ketch.listener?.onTCFUpdated(values)
                }

                override fun onGPPUpdated(values: Map<String, Any?>) {
                    getPreferences().saveValues(values, "GPP", synchronousPreferences)
                    this@Ketch.listener?.onGPPUpdated(values)
                }

                override fun onConfigUpdated(config: KetchConfig?) {
                    // Set internal config field
                    this.config = config

                    // Call config update listener
                    this@Ketch.listener?.onConfigUpdated(config)

                    if (!showConsent) {
                        return
                    }
                    showConsentPopup()
                }

                override fun onEnvironmentUpdated(environment: String?) {
                    this@Ketch.listener?.onEnvironmentUpdated(environment)
                }

                override fun onRegionInfoUpdated(regionInfo: String?) {
                    this@Ketch.listener?.onRegionInfoUpdated(regionInfo)
                }

                override fun onJurisdictionUpdated(jurisdiction: String?) {
                    this@Ketch.listener?.onJurisdictionUpdated(jurisdiction)
                }

                override fun onIdentitiesUpdated(identities: String?) {
                    this@Ketch.listener?.onIdentitiesUpdated(identities)
                }

                override fun onConsentUpdated(consent: Consent) {
                    this@Ketch.listener?.onConsentUpdated(consent)
                }

                override fun onError(errMsg: String?) {
                    this@Ketch.listener?.onError(errMsg)
                }

                override fun changeDialog(display: ContentDisplay) {
                    findDialogFragment()?.let {
                        (it as? KetchDialogFragment)?.apply {
                            isCancelable = getDisposableContentInteractions(display)
                        }
                    }
                }

                override fun onClose(status: HideExperienceStatus) {
                    Log.d(TAG, "onClose called with status: ${status.name}")
                    
                    try {
                        findDialogFragment()?.let { fragment ->
                            if (fragment.isAdded && !fragment.isRemoving) {
                                (fragment as? KetchDialogFragment)?.dismissAllowingStateLoss()
                                Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    this@Ketch.listener?.onDismiss(status)
                                    isActive = false
                                }, 100)
                                return@onClose
                            }
                        }
                        
                        this@Ketch.listener?.onDismiss(status)
                        isActive = false
                    } catch (e: Exception) {
                        Log.e(TAG, "Error dismissing dialog: ${e.message}", e)
                        this@Ketch.listener?.onDismiss(status)
                        isActive = false
                    }
                }

                override fun onWillShowExperience(experienceType: WillShowExperienceType) {
                    // Execute onWillShowExperience listener
                    this@Ketch.listener?.onWillShowExperience(experienceType)
                }

                private fun showConsentPopup() {
                    if (!isActivityActive()) {
                        Log.d(TAG, "Not showing as activity is not active")
                        isActive = false
                        return
                    }

                    val existingDialog = findDialogFragment()
                    if (existingDialog != null) {
                        Log.d(TAG, "Not showing as dialog already exists")
                        
                        // Extra safety: ensure the dialog state is consistent
                        if (!isActive) {
                            Log.d(TAG, "Dialog exists but isActive=false - fixing state")
                            isActive = true
                        }
                        
                        return
                    }

                    val dialog = KetchDialogFragment.newInstance().apply {
                        val disableContentInteractions = getDisposableContentInteractions(
                            config?.experiences?.consent?.display ?: ContentDisplay.Banner
                        )
                        isCancelable = !disableContentInteractions
                    }
                    
                    fragmentManager.get()?.let {
                        // Make sure any existing dialogs with the same tag are removed first
                        val existingFragment = it.findFragmentByTag(KetchDialogFragment.TAG)
                        if (existingFragment != null) {
                            try {
                                it.beginTransaction().remove(existingFragment).commitNow()
                                Log.d(TAG, "Removed existing fragment before showing new one")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error removing existing fragment: ${e.message}", e)
                            }
                        }
                        
                        dialog.show(it, webView)
                        this@Ketch.listener?.onShow()
                    } ?: run {
                        isActive = false
                    }
                    
                    showConsent = false
                }

                private fun getDisposableContentInteractions(display: ContentDisplay): Boolean {
                    return config?.let {
                        when (display) {
                            ContentDisplay.Modal -> {
                                it.theme?.modal?.container?.backdrop?.disableContentInteractions == true
                            }
                            ContentDisplay.Banner -> {
                                it.theme?.modal?.container?.backdrop?.disableContentInteractions == true
                            }
                            else -> {
                                false
                            }
                        }
                    } ?: false
                }
            }
            return webView
        } catch (e: Exception) {
            Log.e(TAG, "Error creating WebView: ${e.message}", e)
            isActive = false
            return null
        }
    }

    private fun findDialogFragment(): androidx.fragment.app.Fragment? {
        return fragmentManager.get()?.findFragmentByTag(KetchDialogFragment.TAG)
    }

    private fun isActivityActive(): Boolean {
        return (context.get() as? LifecycleOwner)?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) ?: false
    }

    enum class PreferencesTab {
        OVERVIEW,
        RIGHTS,
        CONSENTS,
        SUBSCRIPTIONS;

        fun getUrlParameter(): String = when (this) {
            OVERVIEW -> "overviewTab"
            RIGHTS -> "rightsTab"
            CONSENTS -> "consentsTab"
            SUBSCRIPTIONS -> "subscriptionsTab"
        }
    }

    enum class LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR
    }

    interface Listener {
        /**
         * Called when a dialog is displayed
         */
        fun onShow()

        /**
         * Called when a dialog is dismissed
         */
        fun onDismiss(status: HideExperienceStatus)

        /**
         * Called when the config is updated
         */
        fun onConfigUpdated(config: KetchConfig?)

        /**
         * Called when the environment is updated.
         */
        fun onEnvironmentUpdated(environment: String?)

        /**
         * Called when the region is updated.
         */
        fun onRegionInfoUpdated(regionInfo: String?)

        /**
         * Called when the jurisdiction is updated.
         */
        fun onJurisdictionUpdated(jurisdiction: String?)

        /**
         * Called when the identities is updated.
         */
        fun onIdentitiesUpdated(identities: String?)

        /**
         * Called when the consent is updated.
         */
        fun onConsentUpdated(consent: Consent)

        /**
         * Called on error.
         */
        fun onError(errMsg: String?)

        /**
         * Called when USPrivacy is updated.
         */
        fun onUSPrivacyUpdated(values: Map<String, Any?>)

        /**
         * Called when TCF is updated.
         */
        fun onTCFUpdated(values: Map<String, Any?>)

        /**
         * Called when GPP is updated.
         */
        fun onGPPUpdated(values: Map<String, Any?>)

        /**
         * Called when an experience will show.
         */
        fun onWillShowExperience(type: WillShowExperienceType)
    }

    companion object {
        val TAG = Ketch::class.java.simpleName
        fun create(
            context: Context,
            fragmentManager: FragmentManager,
            orgCode: String,
            property: String,
            environment: String?,
            listener: Listener?,
            ketchUrl: String?,
            logLevel: LogLevel,
        ) = Ketch(
            WeakReference(context),
            WeakReference(fragmentManager),
            orgCode,
            property,
            environment,
            listener,
            ketchUrl,
            logLevel
        )
    }

    /**
     * Clean up resources when the host app is being destroyed or paused for a long time
     * This should be called from onDestroy or when the app knows it won't use the SDK for a while
     */
    fun cleanup() {
        dismissDialog()
        
        Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                Log.d(TAG, "Cleaning up WebView resources")
                currentWebView?.destroy()
                currentWebView = null
                Runtime.getRuntime().gc()
            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup: ${e.message}", e)
            }
            isActive = false
        }, 200)
    }
}
