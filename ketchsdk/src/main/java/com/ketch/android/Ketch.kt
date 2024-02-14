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
    private val listener: Listener,
    private val url: String? = null
) {

    private val preferences: KetchSharedPreferences = KetchSharedPreferences(context)

    private var identities: Map<String, String> = mapOf()

    /**
     * Loads a web page and shows a popup if necessary
     */
    fun load() {
        webView.load(orgCode, property, identities, url)
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
    fun forceShowConsent() {
        webView.forceShow(KetchWebView.ExperienceType.CONSENT)
    }

    /**
     * Display the preferences, adding the fragment dialog to the given FragmentManager.
     */
    fun showPreferences() {
        webView.forceShow(KetchWebView.ExperienceType.PREFERENCES)
    }

    /**
     * Dismiss the dialog
     */
    fun dismissDialog() {
        findDialogFragment()?.let {
            (it as? KetchDialogFragment)?.dismiss()
        }
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
     * Set identifies
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
        }

        webView = KetchWebView(context).apply {
            listener = object : KetchWebView.KetchListener {

                private var config: KetchConfig? = null
                private var showConsent: Boolean = false

                override fun onLoad() {
                    this@Ketch.listener.onLoad()
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
                    }
                    val dialog = KetchDialogFragment.newInstance()
                    fragmentManager.let {
                        dialog.show(it, webView)
                    }
                }

                override fun onUSPrivacyUpdated(values: Map<String, Any?>) {
                    preferences.saveUSPrivacy(values)
                    this@Ketch.listener.onUSPrivacyUpdated(values)
                }

                override fun onTCFUpdated(values: Map<String, Any?>) {
                    preferences.saveTCFTC(values)
                    this@Ketch.listener.onTCFUpdated(values)
                }

                override fun onGPPUpdated(values: Map<String, Any?>) {
                    preferences.saveGPP(values)
                    this@Ketch.listener.onGPPUpdated(values)
                }

                override fun onConfigUpdated(config: KetchConfig?) {
                    this.config = config
                }

                override fun onEnvironmentUpdated(environment: String?) {
                    this@Ketch.listener.onEnvironmentUpdated(environment)
                }

                override fun onRegionInfoUpdated(regionInfo: String?) {
                    this@Ketch.listener.onRegionInfoUpdated(regionInfo)
                }

                override fun onJurisdictionUpdated(jurisdiction: String?) {
                    this@Ketch.listener.onJurisdictionUpdated(jurisdiction)
                }

                override fun onIdentitiesUpdated(identities: String?) {
                    this@Ketch.listener.onIdentitiesUpdated(identities)
                }

                override fun onConsentUpdated(consent: Consent) {
                    this@Ketch.listener.onConsentUpdated(consent)
                }

                override fun onError(errMsg: String?) {
                    this@Ketch.listener.onError(errMsg)
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
                    }
                }

                private fun showConsentPopup() {
                    if (findDialogFragment() != null) {
                        return
                    }

                    val dialog = KetchDialogFragment.newInstance().apply {
                        val disableContentInteractions = getDisposableContentInteractions(config?.experiences?.consent?.display ?: ContentDisplay.Banner)
                        isCancelable = !disableContentInteractions
                    }
                    fragmentManager.let {
                        dialog.show(it, webView)
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

    interface Listener {
        fun onLoad()
        fun onEnvironmentUpdated(environment: String?)
        fun onRegionInfoUpdated(regionInfo: String?)
        fun onJurisdictionUpdated(jurisdiction: String?)
        fun onIdentitiesUpdated(identities: String?)
        fun onConsentUpdated(consent: Consent)
        fun onError(errMsg: String?)
        fun onUSPrivacyUpdated(values: Map<String, Any?>)
        fun onTCFUpdated(values: Map<String, Any?>)
        fun onGPPUpdated(values: Map<String, Any?>)
    }

    class Builder private constructor(
        private val context: Context,
        private val fragmentManager: FragmentManager,
        private val orgCode: String,
        private val property: String,
        private val listener: Listener,
        private val url: String?
    ) {

        fun build(): Ketch =
            Ketch(
                context,
                fragmentManager,
                orgCode,
                property,
                listener,
                url
            )

        companion object {
            fun create(
                context: Context,
                fragmentManager: FragmentManager,
                orgCode: String,
                property: String,
                listener: Listener,
                url: String?
            ) = Builder(context, fragmentManager, orgCode, property, listener, url)
        }
    }
}
