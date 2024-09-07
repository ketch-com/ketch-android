package com.ketch.android

import android.content.Context
import android.util.Log
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.ketch.android.data.Consent
import com.ketch.android.data.ContentDisplay
import com.ketch.android.data.HideExperienceStatus
import com.ketch.android.data.KetchConfig
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

    /**
     * Retrieve a String value from the preferences.
     *
     * @param key The name of the preference to retrieve.
     *
     * @return Returns the preference value if it exists
     */
    fun getSavedString(key: String) = getPreferences()?.getSavedValue(key)

    /**
     * Retrieve IABTCF_TCString value from the preferences.
     *
     * @return Returns the preference value if it exists
     */
    fun getTCFTCString() = getPreferences()?.getSavedValue(KetchSharedPreferences.IAB_TCF_TC_STRING)

    /**
     * Retrieve IABUSPrivacy_String value from the preferences.
     *
     * @return Returns the preference value if it exists
     */
    fun getUSPrivacyString() =
        getPreferences()?.getSavedValue(KetchSharedPreferences.IAB_US_PRIVACY_STRING)

    /**
     * Retrieve IABGPP_HDR_GppString value from the preferences.
     *
     * @return Returns the preference value if it exists
     */
    fun getGPPHDRGppString() =
        getPreferences()?.getSavedValue(KetchSharedPreferences.IAB_GPP_HDR_GPP_STRING)

    /**
     * Loads a web page and shows a popup if necessary
     */
    fun load(shouldRetry: Boolean = false) {
        createWebView(shouldRetry)?.load(
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
            logLevel
        )
    }

    /**
     * Display the consent, adding the fragment dialog to the given FragmentManager.
     */
    fun showConsent(shouldRetry: Boolean = false) {
        createWebView(shouldRetry)?.load(
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
            logLevel
        )
    }

    /**
     * Display the preferences, adding the fragment dialog to the given FragmentManager.
     */
    fun showPreferences(shouldRetry: Boolean = false) {
        createWebView(shouldRetry)?.load(
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
            logLevel
        )
    }

    /**
     * Display the preferences tab, adding the fragment dialog to the given FragmentManager.
     *
     * @param tabs: list of preferences tab
     * @param tab: the current tab
     */
    fun showPreferencesTab(tabs: List<PreferencesTab>, tab: PreferencesTab, shouldRetry: Boolean = false) {
        createWebView(shouldRetry)?.load(
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
            logLevel
        )
    }

    /**
     * Dismiss the dialog
     */
    fun dismissDialog() {
        findDialogFragment()?.let {
            (it as? KetchDialogFragment)?.dismiss()
            this@Ketch.listener?.onDismiss(HideExperienceStatus.None)
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
        findDialogFragment()?.let { dialog ->
            (dialog as KetchDialogFragment).dismiss()
            this@Ketch.listener?.onDismiss(HideExperienceStatus.None)
        }
    }

    private fun getPreferences(): KetchSharedPreferences? = context.get()?.let {
        KetchSharedPreferences(it)
    }

    private fun createWebView(shouldRetry: Boolean = false): KetchWebView? {

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
                if (!isActivityActive()) {
                    Log.d(TAG, "Not showing as activity is not active")
                    return
                }

                if (findDialogFragment() != null) {
                    Log.d(TAG, "Not showing as dialog already exists")
                    return
                }

                val dialog = KetchDialogFragment.newInstance()

                fragmentManager.get()?.let {
                    dialog.show(it, webView)
                    this@Ketch.listener?.onShow()
                }
            }

            override fun onUSPrivacyUpdated(values: Map<String, Any?>) {
                getPreferences()?.saveUSPrivacy(values)
                this@Ketch.listener?.onUSPrivacyUpdated(values)
            }

            override fun onTCFUpdated(values: Map<String, Any?>) {
                getPreferences()?.saveTCFTC(values)
                this@Ketch.listener?.onTCFUpdated(values)
            }

            override fun onGPPUpdated(values: Map<String, Any?>) {
                getPreferences()?.saveGPP(values)
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
                // Dismiss dialog fragment
                findDialogFragment()?.let {
                    (it as? KetchDialogFragment)?.dismiss()
                }

                // Execute onDismiss event listener
                this@Ketch.listener?.onDismiss(status)
            }

            override fun onTapOutside() {
                // Dismiss dialog fragment
                findDialogFragment()?.let {
                    (it as? KetchDialogFragment)?.dismiss()

                    // Execute onDismiss event listener
                    this@Ketch.listener?.onDismiss(HideExperienceStatus.None)
                }
            }

            private fun showConsentPopup() {
                if (!isActivityActive()) {
                    Log.d(TAG, "Not showing as activity is not active")
                    return
                }

                if (findDialogFragment() != null) {
                    Log.d(TAG, "Not showing as dialog already exists")
                    return
                }

                val dialog = KetchDialogFragment.newInstance().apply {
                    val disableContentInteractions = getDisposableContentInteractions(
                        config?.experiences?.consent?.display ?: ContentDisplay.Banner
                    )
                    isCancelable = !disableContentInteractions
                }
                fragmentManager.get()?.let {
                    dialog.show(it, webView)
                    this@Ketch.listener?.onShow()
                }
                showConsent = false
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

    private fun findDialogFragment() =
        fragmentManager.get()?.findFragmentByTag(KetchDialogFragment.TAG)

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
