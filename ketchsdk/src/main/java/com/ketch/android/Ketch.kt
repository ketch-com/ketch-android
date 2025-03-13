package com.ketch.android

import android.content.Context
import android.util.Log
import androidx.fragment.app.Fragment
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
    
    // Flag to prevent multiple overlapping experiences
    @Volatile
    private var isShowingExperience = false
    
    // Reference to the active fragment to do cleanup
    private var activeDialogFragment: WeakReference<KetchDialogFragment>? = null
    
    // Lock object for synchronization
    private val lock = Any()

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
        if (isShowingExperience) {
            Log.d(TAG, "Not loading as an experience is already being shown")
            return false
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
        if (isShowingExperience) {
            Log.d(TAG, "Not showing consent as an experience is already being shown")
            return false
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
        if (isShowingExperience) {
            Log.d(TAG, "Not showing preferences as an experience is already being shown")
            return false
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
        if (isShowingExperience) {
            Log.d(TAG, "Not showing preferences tab as an experience is already being shown")
            return false
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
        synchronized(lock) {
            val fragment = findDialogFragment()
            if (fragment != null) {
                try {
                    (fragment as? KetchDialogFragment)?.dismissAllowingStateLoss()
                } catch (e: Exception) {
                    Log.e(TAG, "Error dismissing dialog: ${e.message}")
                } finally {
                    isShowingExperience = false
                    activeDialogFragment = null
                    this@Ketch.listener?.onDismiss(HideExperienceStatus.None)
                }
            }
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

        // Ensure any existing dialog fragments are properly cleaned up
        synchronized(lock) {
            val existingFragment = fragmentManager.get()?.findFragmentByTag(KetchDialogFragment.TAG)
            if (existingFragment != null) {
                try {
                    (existingFragment as? KetchDialogFragment)?.dismissAllowingStateLoss()
                    this@Ketch.listener?.onDismiss(HideExperienceStatus.None)
                } catch (e: Exception) {
                    Log.e(TAG, "Error dismissing existing dialog in init: ${e.message}")
                }
            }
        }
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
        synchronized(lock) {
            if (isShowingExperience || findDialogFragment() != null) {
                Log.d(TAG, "Not creating WebView as experience is already being shown")
                return null
            }

            val webView = context.get()?.let { KetchWebView(it, shouldRetry) } ?: return null

            // Enable debug mode
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
                    synchronized(lock) {
                        if (isShowingExperience || findDialogFragment() != null) {
                            Log.d(TAG, "Not showing as dialog already exists")
                            return
                        }
                        
                        // Set flag to indicate we're showing an experience
                        isShowingExperience = true
                        
                        try {
                            val dialog = KetchDialogFragment.newInstance()
                            fragmentManager.get()?.let { fm ->
                                if (!fm.isDestroyed) {
                                    dialog.show(fm, webView) {
                                        // Reset flag when dialog is dismissed
                                        isShowingExperience = false
                                    }
                                    this@Ketch.listener?.onShow()
                                } else {
                                    isShowingExperience = false
                                    Log.e(TAG, "FragmentManager is destroyed, cannot show dialog")
                                    this@Ketch.listener?.onError("FragmentManager is destroyed, cannot show dialog")
                                }
                            } ?: run {
                                isShowingExperience = false
                                Log.e(TAG, "FragmentManager is null, cannot show dialog")
                                this@Ketch.listener?.onError("FragmentManager is null, cannot show dialog")
                            }
                        } catch (e: Exception) {
                            isShowingExperience = false
                            Log.e(TAG, "Error showing dialog: ${e.message}")
                            this@Ketch.listener?.onError("Error showing dialog: ${e.message}")
                        }
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
                    this.config = config

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
                    // Dismiss dialog fragment safely
                    synchronized(lock) {
                        val fragment = findDialogFragment()
                        if (fragment != null) {
                            try {
                                (fragment as? KetchDialogFragment)?.dismissAllowingStateLoss()
                                this@Ketch.listener?.onDismiss(status)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error dismissing dialog: ${e.message}")
                                // Ensure state is reset even if dismissal fails
                                isShowingExperience = false
                                this@Ketch.listener?.onDismiss(status)
                            }
                        } else {
                            // Even if fragment isn't found, reset state
                            isShowingExperience = false
                            this@Ketch.listener?.onDismiss(status)
                        }
                    }
                }

                override fun onWillShowExperience(experienceType: WillShowExperienceType) {
                    this@Ketch.listener?.onWillShowExperience(experienceType)
                }

                override fun onTapOutside() {
                    // Dismiss dialog fragment safely
                    synchronized(lock) {
                        val fragment = findDialogFragment()
                        if (fragment != null) {
                            try {
                                (fragment as? KetchDialogFragment)?.dismissAllowingStateLoss()
                                this@Ketch.listener?.onDismiss(HideExperienceStatus.None)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error dismissing dialog on tap outside: ${e.message}")
                                // Ensure state is reset even if dismissal fails
                                isShowingExperience = false
                                this@Ketch.listener?.onDismiss(HideExperienceStatus.None)
                            }
                        }
                    }
                }

                private fun showConsentPopup() {
                    synchronized(lock) {
                        if (isShowingExperience || findDialogFragment() != null) {
                            Log.d(TAG, "Not showing as already showing an experience")
                            return
                        }
                        
                        isShowingExperience = true
                        
                        try {
                            val dialog = KetchDialogFragment.newInstance().apply {
                                val disableContentInteractions = getDisposableContentInteractions(
                                    config?.experiences?.consent?.display ?: ContentDisplay.Banner
                                )
                                isCancelable = !disableContentInteractions
                            }
                            
                            fragmentManager.get()?.let { fm ->
                                if (!fm.isDestroyed) {
                                    dialog.show(fm, webView) {
                                        // Reset state on dismissal
                                        isShowingExperience = false
                                    }
                                    this@Ketch.listener?.onShow()
                                } else {
                                    isShowingExperience = false
                                    Log.e(TAG, "FragmentManager is destroyed, cannot show dialog")
                                    this@Ketch.listener?.onError("FragmentManager is destroyed, cannot show dialog")
                                }
                            } ?: run {
                                isShowingExperience = false
                                Log.e(TAG, "FragmentManager is null, cannot show dialog")
                                this@Ketch.listener?.onError("FragmentManager is null, cannot show dialog")
                            }
                        } catch (e: Exception) {
                            isShowingExperience = false
                            Log.e(TAG, "Error showing dialog: ${e.message}")
                            this@Ketch.listener?.onError("Error showing dialog: ${e.message}")
                        }
                        
                        showConsent = false
                    }
                }

                private fun getDisposableContentInteractions(display: ContentDisplay): Boolean =
                    config?.let {
                        if (display == ContentDisplay.Modal) {
                            it.theme?.modal?.container?.backdrop?.disableContentInteractions == true
                        } else if (display == ContentDisplay.Banner) {
                            it.theme?.modal?.container?.backdrop?.disableContentInteractions == true
                        } else false
                    } ?: false
            }
            return webView
        }
    }

    private fun findDialogFragment(): Fragment? {
        // First check our active reference, which is faster than searching
        val activeFragment = activeDialogFragment?.get()
        if (activeFragment != null && activeFragment.isAdded && !activeFragment.isDetached) {
            return activeFragment
        }
        
        // Fall back to searching by tag
        return fragmentManager.get()?.findFragmentByTag(KetchDialogFragment.TAG)
    }

    private fun isActivityActive(): Boolean {
        return (context.get() as? LifecycleOwner)?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED)
            ?: false
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
}
