package com.ketch.android

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.ketch.android.data.Consent
import com.ketch.android.data.ContentDisplay
import com.ketch.android.data.KetchConfig
import com.ketch.android.ui.KetchDialogFragment
import com.ketch.android.ui.KetchWebView

/**
 * Main Ketch SDK class
 **/
class Ketch private constructor(
    context: Context,
    private val fragmentManager: FragmentManager,
    private val orgCode: String,
    private val property: String,
    private val environment: String?,
    private val listener: Listener?,
    private val ketchUrl: String?,
    private val logLevel: LogLevel
) {

    private val preferences: KetchSharedPreferences = KetchSharedPreferences(context)

    private var identities: Map<String, String> = mapOf()

    /**
     * Loads a web page and shows a popup if necessary
     */
    fun load() {
        webView.load(orgCode, property, environment, identities, ketchUrl, logLevel)
    }

    /**
     * Retrieve a String value from the preferences.
     *
     * @param key The name of the preference to retrieve.
     *
     * @return Returns the preference value if it exists
     */
    fun getSavedString(key: String) = preferences.getSavedValue(key)

    /**
     * Retrieve IABTCF_TCString value from the preferences.
     *
     * @return Returns the preference value if it exists
     */
    fun getTCFTCString() = preferences.getSavedValue(KetchSharedPreferences.IAB_TCF_TC_STRING)

    /**
     * Retrieve IABUSPrivacy_String value from the preferences.
     *
     * @return Returns the preference value if it exists
     */
    fun getUSPrivacyString() =
        preferences.getSavedValue(KetchSharedPreferences.IAB_US_PRIVACY_STRING)

    /**
     * Retrieve IABGPP_HDR_GppString value from the preferences.
     *
     * @return Returns the preference value if it exists
     */
    fun getGPPHDRGppString() =
        preferences.getSavedValue(KetchSharedPreferences.IAB_GPP_HDR_GPP_STRING)

    /**
     * Display the consent, adding the fragment dialog to the given FragmentManager.
     */
    fun showConsent() {
        webView.forceShow(KetchWebView.ExperienceType.CONSENT)
    }

    /**
     * Display the preferences, adding the fragment dialog to the given FragmentManager.
     */
    fun showPreferences() {
        webView.forceShow(KetchWebView.ExperienceType.PREFERENCES)
    }

    /**
     * Display the preferences tab, adding the fragment dialog to the given FragmentManager.
     *
     * @param tabs: list of preferences tab
     * @param tab: the current tab
     */
    fun showPreferencesTab(tabs: List<PreferencesTab>, tab: PreferencesTab) {
        webView.showPreferencesTab(tabs, tab)
    }

    /**
     * Dismiss the dialog
     */
    fun dismissDialog() {
        findDialogFragment()?.let {
            (it as? KetchDialogFragment)?.dismiss()
            this@Ketch.listener?.onDismiss()
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
    fun setLanguage(language: String) {
        webView.setLanguage(language)
    }

    /**
     * Set the jurisdiction
     *
     * @param jurisdiction: the jurisdiction value
     */
    fun setJurisdiction(jurisdiction: String?) {
        webView.setJurisdiction(jurisdiction)
    }

    /**
     * Set Region
     *
     * @param region: the region name
     */
    fun setRegion(region: String?) {
        webView.setRegion(region)
    }

    private lateinit var webView: KetchWebView

    init {
        findDialogFragment()?.let { dialog ->
            (dialog as KetchDialogFragment).dismiss()
            this@Ketch.listener?.onDismiss()
        }

        webView = KetchWebView(context).apply {
            listener = object : KetchWebView.WebViewListener {

                private var config: KetchConfig? = null
                private var showConsent: Boolean = false

                override fun onLoad() {
                    this@Ketch.listener?.onLoad()
                }

                override fun showConsent() {
                    if (config == null) {
                        showConsent = true
                        return
                    }
                    showConsentPopup()
                }

                override fun showPreferences() {
                    findDialogFragment()?.let {
                        (it as KetchDialogFragment).dismiss()
                        this@Ketch.listener?.onDismiss()
                    }
                    val dialog = KetchDialogFragment.newInstance()
                    fragmentManager.let {
                        dialog.show(it, webView)
                        this@Ketch.listener?.onShow()
                    }
                }

                override fun onUSPrivacyUpdated(values: Map<String, Any?>) {
                    preferences.saveUSPrivacy(values)
                    this@Ketch.listener?.onUSPrivacyUpdated(values)
                }

                override fun onTCFUpdated(values: Map<String, Any?>) {
                    preferences.saveTCFTC(values)
                    this@Ketch.listener?.onTCFUpdated(values)
                }

                override fun onGPPUpdated(values: Map<String, Any?>) {
                    preferences.saveGPP(values)
                    this@Ketch.listener?.onGPPUpdated(values)
                }

                override fun onConfigUpdated(config: KetchConfig?) {
                    this.config = config
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

                override fun onClose() {
                    findDialogFragment()?.let {
                        (it as? KetchDialogFragment)?.dismiss()
                        this@Ketch.listener?.onDismiss()
                    }
                }

                override fun onTapOutside() {
                    findDialogFragment()?.let {
                        (it as? KetchDialogFragment)?.let {
                            it.dismiss()
                            this@Ketch.listener?.onDismiss()
                        }
                    }
                }

                private fun showConsentPopup() {
                    if (findDialogFragment() != null) {
                        return
                    }

                    val dialog = KetchDialogFragment.newInstance().apply {
                        val disableContentInteractions = getDisposableContentInteractions(
                            config?.experiences?.consent?.display ?: ContentDisplay.Banner
                        )
                        isCancelable = !disableContentInteractions
                    }
                    fragmentManager.let {
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
        }
    }

    private fun findDialogFragment() =
        fragmentManager.findFragmentByTag(KetchDialogFragment.TAG)

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
         * Called when the page is loaded by the sdk
         */
        fun onLoad()

        /**
         * Called when a dialog is displayed
         */
        fun onShow()

        /**
         * Called when a dialog is dismissed
         */
        fun onDismiss()

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
        fun create(
            context: Context,
            fragmentManager: FragmentManager,
            orgCode: String,
            property: String,
            environment: String?,
            listener: Listener?,
            ketchUrl: String?,
            logLevel: LogLevel
        ) = Ketch(context, fragmentManager, orgCode, property, environment, listener, ketchUrl, logLevel)
    }
}
