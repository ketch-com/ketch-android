package com.ketch.android

import android.content.Context
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.ViewGroup
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
import java.util.concurrent.atomic.AtomicBoolean

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
    
    // WebView instance and state flags
    private var currentWebView: KetchWebView? = null
    private var isActive = false
    
    // Debounce mechanism to prevent rapid clicks
    private var lastClickTime = 0L
    private val CLICK_DEBOUNCE_TIME = 1000L // 1 second between allowed clicks

    /**
     * Retrieve a String value from the preferences.
     *
     * @param key The name of the preference to retrieve.
     * @return Returns the preference value if it exists
     */
    fun getSavedString(key: String) = getPreferences().getSavedValue(key)

    /**
     * Retrieve IABTCF_TCString value from the preferences.
     * @return Returns the preference value if it exists
     */
    fun getTCFTCString() = getPreferences().getSavedValue(KetchSharedPreferences.IAB_TCF_TC_STRING)

    /**
     * Retrieve IABUSPrivacy_String value from the preferences.
     * @return Returns the preference value if it exists
     */
    fun getUSPrivacyString() = getPreferences().getSavedValue(KetchSharedPreferences.IAB_US_PRIVACY_STRING)

    /**
     * Retrieve IABGPP_HDR_GppString value from the preferences.
     * @return Returns the preference value if it exists
     */
    fun getGPPHDRGppString() = getPreferences().getSavedValue(KetchSharedPreferences.IAB_GPP_HDR_GPP_STRING)

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
        // Debounce rapid clicks
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastClickTime < CLICK_DEBOUNCE_TIME) {
            Log.d(TAG, "Ignoring rapid click, debouncing for ${CLICK_DEBOUNCE_TIME}ms")
            return false
        }
        lastClickTime = currentTime
        
        // Force a complete reset of all state to ensure we start clean
        isActive = false
        
        // Check for any lingering WebViews and destroy them
        synchronized(instanceLock) {
            cleanupWebView()
            currentWebView = null
        }
        
        // Force cleanup any existing fragments before creating a new one - more aggressive cleanup
        fragmentManager.get()?.let { fm ->
            if (!fm.isDestroyed) {
                // First try our normal cleanup
                removeAllKetchDialogFragments(fm)
                
                // Then force an explicit check for any fragments that might remain
                try {
                    val remainingFragments = fm.fragments.filterIsInstance<KetchDialogFragment>()
                    if (remainingFragments.isNotEmpty()) {
                        Log.w(TAG, "Found ${remainingFragments.size} remaining fragments before creating dialog, forcing removal")
                        
                        // Emergency direct cleanup, fragment by fragment
                        remainingFragments.forEach { fragment ->
                            try {
                                // Reset touch listeners
                                fragment.webView?.setOnTouchListener(null)
                                
                                // Clear WebView
                                fragment.webView?.destroy()
                                fragment.webView = null
                                
                                // Force removal with individual transaction
                                val emergencyTransaction = fm.beginTransaction()
                                emergencyTransaction.remove(fragment)
                                emergencyTransaction.commitNowAllowingStateLoss()
                                fm.executePendingTransactions()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in emergency fragment cleanup: ${e.message}", e)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking for remaining fragments: ${e.message}", e)
                }
                
                // Force global reset
                KetchDialogFragment.resetShowingState()
            }
        }
        
        // Wait a small amount of time to ensure cleanup completes
        try {
            Thread.sleep(50)
        } catch (e: InterruptedException) {
            // Ignore
        }
        
        // Now create the WebView
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
        // Debounce rapid clicks
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastClickTime < CLICK_DEBOUNCE_TIME) {
            Log.d(TAG, "Ignoring rapid click, debouncing for ${CLICK_DEBOUNCE_TIME}ms")
            return false
        }
        lastClickTime = currentTime
        
        // Force cleanup any existing fragments before creating a new one
        fragmentManager.get()?.let { fm ->
            if (!fm.isDestroyed) {
                KetchDialogFragment.forceCleanupAllInstances(fm)
            }
        }
        
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
        cleanupDialogFragment { status ->
            this@Ketch.listener?.onDismiss(status ?: HideExperienceStatus.None)
        }
    }

    /**
     * Set identities
     * @param identities: Map<String, String>
     */
    fun setIdentities(identities: Map<String, String>) { this.identities = identities }

    /**
     * Set the language
     * @param language: a language name (EN, FR, etc.)
     */
    fun setLanguage(language: String?) { this.language = language }

    /**
     * Set the jurisdiction
     * @param jurisdiction: the jurisdiction value
     */
    fun setJurisdiction(jurisdiction: String?) { this.jurisdiction = jurisdiction }

    /**
     * Set Region
     * @param region: the region name
     */
    fun setRegion(region: String?) { this.region = region }

    init {
        getPreferences()

        fragmentManager.get()?.let { fm ->
            try {
                val initialExistingDialog = fm.findFragmentByTag(KetchDialogFragment.TAG)
                if (initialExistingDialog != null) {
                    cleanupDialogFragment(forceRemove = true) { _ ->
                        this@Ketch.listener?.onDismiss(HideExperienceStatus.None)
                    }
                } else {
                    // No existing dialog to clean up
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up existing dialogs: ${e.message}", e)
            }
        }
        
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
        synchronized(instanceLock) {
            if (isActive) {
                Log.w(TAG, "WebView creation attempted while another is active")
                return null
            }
            
            val existingFragment = findDialogFragment()
            if (existingFragment != null) {
                Log.d(TAG, "Found existing dialog fragment, cleaning up before creating new WebView")
                cleanupDialogFragment(forceRemove = true)
                // Return null to prevent creating a new WebView while cleanup is in progress
                return null
            }
            
            isActive = true
            
            try {
                // Clean up any existing WebView first
                cleanupWebView()
                
                val ctx = context.get() ?: run {
                    Log.e(TAG, "Context is null, cannot create WebView")
                    isActive = false
                    return null
                }
                
                val webView = KetchWebView(ctx)
                if (shouldRetry) {
                    Log.d(TAG, "WebView created with retry enabled")
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

                        if (findDialogFragment() != null) {
                            Log.d(TAG, "Not showing as dialog already exists")
                            if (!isActive) {
                                isActive = true
                            }
                            return
                        }

                        val dialog = KetchDialogFragment.newInstance()?.apply {
                            isCancelable = !getDisposableContentInteractions()
                        }

                        fragmentManager.get()?.let { manager ->
                            try {
                                dialog?.show(manager, webView)
                                this@Ketch.listener?.onShow()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error showing dialog: ${e.message}", e)
                                isActive = false
                            }
                        } ?: run {
                            isActive = false
                        }
                        
                        showConsent = false
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
                        this.config = config
                        this@Ketch.listener?.onConfigUpdated(config)

                        if (showConsent) {
                            showConsentPopup()
                        }
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
                                isCancelable = getDisposableContentInteractions()
                            }
                        }
                    }

                    override fun onClose(status: HideExperienceStatus) {
                        cleanupDialogFragment { _ ->
                            this@Ketch.listener?.onDismiss(status)
                            isActive = false
                        }
                    }

                    override fun onWillShowExperience(experienceType: WillShowExperienceType) {
                        this@Ketch.listener?.onWillShowExperience(experienceType)
                    }
                    
                    /**
                     * @deprecated This method is deprecated and will be removed in a future release
                     */
                    @Deprecated("This method is deprecated and will be removed in a future release")
                    override fun onTapOutside() {
                        // Dismiss dialog fragment
                        findDialogFragment()?.let {
                            (it as? KetchDialogFragment)?.dismissAllowingStateLoss()
                            
                            // Execute onDismiss event listener
                            this@Ketch.listener?.onDismiss(HideExperienceStatus.None)
                        }
                    }

                    private fun showConsentPopup() {
                        if (!isActivityActive()) {
                            Log.d(TAG, "Not showing as activity is not active")
                            isActive = false
                            return
                        }

                        if (findDialogFragment() != null) {
                            Log.d(TAG, "Not showing as dialog already exists")
                            if (!isActive) {
                                isActive = true
                            }
                            return
                        }

                        val dialog = KetchDialogFragment.newInstance()?.apply {
                            isCancelable = !getDisposableContentInteractions()
                        }
                        
                        fragmentManager.get()?.let { manager ->
                            try {
                                dialog?.show(manager, webView)
                                this@Ketch.listener?.onShow()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error showing dialog: ${e.message}", e)
                                isActive = false
                            }
                        } ?: run {
                            isActive = false
                        }
                        
                        showConsent = false
                    }
                    
                    private fun getDisposableContentInteractions(): Boolean =
                        config?.theme?.modal?.container?.backdrop?.disableContentInteractions ?: false
                }
                return webView
            } catch (e: Exception) {
                Log.e(TAG, "Error creating WebView: ${e.message}", e)
                isActive = false
                return null
            }
        }
    }
    
    /**
     * Helper method to show a dialog with the WebView
     */
    private fun showDialogWithWebView(
        webView: KetchWebView
    ) {
        // Since we can't access the config property from the WebViewListener,
        // we'll use a default value for disableContentInteractions
        val disableContentInteractions = false
        
        val dialog = KetchDialogFragment.newInstance()?.apply {
            isCancelable = !disableContentInteractions
        }
        
        fragmentManager.get()?.let { manager ->
            try {
                dialog?.show(manager, webView)
                this@Ketch.listener?.onShow()
            } catch (e: Exception) {
                Log.e(TAG, "Error showing dialog: ${e.message}", e)
                isActive = false
            }
        } ?: run {
            isActive = false
        }
    }

    private fun findDialogFragment() = fragmentManager.get()?.findFragmentByTag(KetchDialogFragment.TAG)

    private fun isActivityActive() = (context.get() as? LifecycleOwner)?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) ?: false

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
        private val instanceLock = Any()
        
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
     * Centralized helper to clean up WebView resources
     */
    private fun cleanupWebView() = synchronized(instanceLock) {
        runCatching {
            Log.d(TAG, "Cleaning up WebView")
            currentWebView?.let { webView ->
                try {
                    // CRITICAL: Reset touch listener first to ensure it doesn't block touches
                    webView.setOnTouchListener(null)
                    
                    // Destroy the WebView
                    webView.destroy()
                    
                    // Clear the reference
                    currentWebView = null
                    
                    Log.d(TAG, "WebView cleanup completed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during WebView cleanup: ${e.message}", e)
                    // Even if there's an error, ensure we reset the touch listener
                    try {
                        webView.setOnTouchListener(null)
                    } catch (e2: Exception) {
                        Log.e(TAG, "Error resetting touch listener: ${e2.message}", e2)
                    }
                }
            }
        }.onFailure {
            Log.e(TAG, "Exception during WebView cleanup: ${it.message}", it)
            // Even if there's a failure, try to reset the touch listener
            currentWebView?.setOnTouchListener(null)
            currentWebView = null
        }
    }
    
    /**
     * Centralized helper to clean up dialog fragments
     * 
     * @param forceRemove Whether to forcefully remove the fragment from the FragmentManager
     * @param onComplete Optional callback to execute after cleanup
     */
    private fun cleanupDialogFragment(forceRemove: Boolean = false, onComplete: ((HideExperienceStatus?) -> Unit) = {}) {
        synchronized(instanceLock) {
            try {
                Log.d(TAG, "cleanupDialogFragment: Beginning cleanup, forceRemove=$forceRemove")
                
                // First, reset the WebView touch listener to ensure it doesn't block touches
                currentWebView?.setOnTouchListener(null)
                
                fragmentManager.get()?.let { fm ->
                    if (!fm.isDestroyed) {
                        // ALWAYS use force remove to ensure complete cleanup after our fixes
                        // This helps prevent multiple lingering fragments
                        removeAllKetchDialogFragments(fm)
                        
                        // Clean up WebView after fragments are removed
                        cleanupWebView()
                        
                        // Reset active state with a small delay to ensure fragment dismissal completes
                        Handler(android.os.Looper.getMainLooper()).postDelayed({
                            isActive = false
                            
                            // Force reset the showing state in KetchDialogFragment
                            KetchDialogFragment.resetShowingState()
                            
                            // Double-check for any remaining fragments and force remove them
                            try {
                                removeAllKetchDialogFragments(fm)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in final fragment cleanup: ${e.message}", e)
                            }
                            
                            onComplete.invoke(null)
                        }, 500) // Increased delay to ensure fragments are fully dismissed
                    } else {
                        // FragmentManager is destroyed, just reset state
                        cleanupWebView()
                        isActive = false
                        KetchDialogFragment.resetShowingState()
                        onComplete.invoke(null)
                    }
                } ?: run {
                    // No FragmentManager, just reset state
                    cleanupWebView()
                    isActive = false
                    KetchDialogFragment.resetShowingState()
                    onComplete.invoke(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during fragment cleanup: ${e.message}", e)
                cleanupWebView()
                isActive = false
                KetchDialogFragment.resetShowingState()
                onComplete.invoke(null)
            }
        }
    }
    
    /**
     * Helper method to forcefully remove all KetchDialogFragment instances
     * This is more thorough than using dismiss() which might leave fragments in memory
     */
    private fun removeAllKetchDialogFragments(fm: FragmentManager) {
        try {
            val fragments = fm.fragments.filterIsInstance<KetchDialogFragment>()
            if (fragments.isNotEmpty()) {
                Log.d(TAG, "Force removing ${fragments.size} KetchDialogFragment instances with transaction")
                
                // First reset touch listeners, detach from parents, and destroy WebViews
                fragments.forEach { fragment ->
                    try {
                        // Reset dialog window parameters first
                        fragment.dialog?.window?.let { window ->
                            try {
                                // Clear any flags that might interfere with touch events
                                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                                
                                // Reset window parameters to default
                                val attrs = window.attributes
                                attrs.flags = attrs.flags or android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                                window.attributes = attrs
                                
                                // Reset any touch interceptors
                                val decorView = window.decorView
                                decorView.setOnTouchListener(null)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error resetting window parameters: ${e.message}", e)
                            }
                        }
                        
                        // Get the WebView from the fragment
                        fragment.webView?.let { wv ->
                            // Reset touch listener first
                            wv.setOnTouchListener(null)
                            
                            // Disable hardware acceleration
                            try {
                                wv.setLayerType(android.view.View.LAYER_TYPE_NONE, null)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error disabling WebView hardware acceleration: ${e.message}", e)
                            }
                            
                            // Detach WebView from any parent views
                            (wv.parent as? ViewGroup)?.removeView(wv)
                            
                            // Clear all content
                            wv.loadUrl("about:blank")
                            
                            // Destroy WebView
                            wv.destroy()
                        }
                        
                        // Clear the fragment's WebView reference
                        fragment.webView = null
                    } catch (e: Exception) {
                        Log.e(TAG, "Error cleaning up fragment WebView: ${e.message}", e)
                    }
                }
                
                // Then remove the fragments with a transaction and execute it immediately
                val transaction = fm.beginTransaction()
                fragments.forEach { fragment ->
                    try {
                        // First try dismissing the dialog to properly remove the window
                        try {
                            fragment.dismissAllowingStateLoss()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error dismissing fragment: ${e.message}", e)
                        }
                        
                        // Then remove the fragment from the manager
                        transaction.remove(fragment)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error removing fragment in transaction: ${e.message}", e)
                    }
                }
                transaction.commitNowAllowingStateLoss()
                
                // Make sure the transaction is processed immediately
                try {
                    fm.executePendingTransactions()
                } catch (e: Exception) {
                    Log.e(TAG, "Error executing pending transactions: ${e.message}", e)
                }
                
                // Reset the showing state
                KetchDialogFragment.resetShowingState()
                
                // Make sure we clean up again after a delay
                Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        val remainingFragments = fm.fragments.filterIsInstance<KetchDialogFragment>()
                        if (remainingFragments.isNotEmpty()) {
                            Log.w(TAG, "Still found ${remainingFragments.size} fragments, removing again with IMMEDIATE execution")
                            
                            // More aggressive cleanup for remaining fragments
                            remainingFragments.forEach { fragment ->
                                try {
                                    // Reset window parameters first
                                    fragment.dialog?.window?.let { window ->
                                        try {
                                            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                                            val decorView = window.decorView
                                            decorView.setOnTouchListener(null)
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Error in delayed window parameter reset: ${e.message}", e)
                                        }
                                    }
                                    
                                    // Clear any remaining WebView
                                    fragment.webView?.let { wv ->
                                        wv.setOnTouchListener(null)
                                        wv.setLayerType(android.view.View.LAYER_TYPE_NONE, null)
                                        (wv.parent as? ViewGroup)?.removeView(wv)
                                        wv.loadUrl("about:blank")
                                        wv.destroy()
                                    }
                                    fragment.webView = null
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error in delayed WebView cleanup: ${e.message}", e)
                                }
                            }
                            
                            val finalTransaction = fm.beginTransaction()
                            remainingFragments.forEach { fragment ->
                                finalTransaction.remove(fragment)
                            }
                            finalTransaction.commitNowAllowingStateLoss()
                            
                            // Force immediate execution
                            fm.executePendingTransactions()
                        }
                        
                        // Clear current WebView reference to be safe
                        currentWebView?.let { wv ->
                            wv.setOnTouchListener(null)
                            wv.stopLoading()
                        }
                        cleanupWebView()
                        
                        // Force another check after a longer delay
                        Handler(android.os.Looper.getMainLooper()).postDelayed({
                            val finalFragments = fm.fragments.filterIsInstance<KetchDialogFragment>()
                            if (finalFragments.isNotEmpty()) {
                                Log.e(TAG, "CRITICAL: Still found ${finalFragments.size} fragments after multiple cleanup attempts")
                                
                                // Last-ditch attempt with detached transactions
                                try {
                                    val lastTransaction = fm.beginTransaction()
                                    finalFragments.forEach { fragment ->
                                        // Detach first, then remove
                                        lastTransaction.detach(fragment)
                                        lastTransaction.remove(fragment)
                                    }
                                    lastTransaction.commitNowAllowingStateLoss()
                                    fm.executePendingTransactions()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error in final fragment cleanup attempt: ${e.message}", e)
                                }
                                
                                // Force showing state reset
                                KetchDialogFragment.resetShowingState()
                            }
                            
                            // Suggest garbage collection
                            System.gc()
                            
                        }, 500) // Final check after 500ms
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in delayed fragment cleanup: ${e.message}", e)
                    }
                }, 300)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing KetchDialogFragments: ${e.message}", e)
        }
    }
}
