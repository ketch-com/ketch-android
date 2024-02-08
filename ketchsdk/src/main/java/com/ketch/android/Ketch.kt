package com.ketch.android

import android.content.Context
import android.os.Parcelable
import android.view.Gravity
import androidx.annotation.FloatRange
import androidx.annotation.GravityInt
import androidx.annotation.StyleRes
import androidx.fragment.app.FragmentManager
import com.ketch.android.data.Consent
import com.ketch.android.data.ContentDisplay
import com.ketch.android.data.Identity
import com.ketch.android.data.KetchConfig
import com.ketch.android.ui.KetchDialogFragment
import com.ketch.android.ui.KetchWebView
import kotlinx.parcelize.Parcelize

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

    private var bannerWindowPosition: WindowPosition? = null
    private var modalWindowPosition: WindowPosition? = null

    fun getSavedString(key: String) = preferences.getSavedValue(key)

    fun getTCFTCString() = preferences.getSavedValue(IAB_TCF_TC_STRING)

    fun getUSPrivacyString() = preferences.getSavedValue(IAB_US_PRIVACY_STRING)

    fun getGPPHDRGppString() = preferences.getSavedValue(IAB_GPP_HDR_GPP_STRING)

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
                }

                override fun showConsent() {
                    showConsent = true
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
                    showConsentPopup()
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
                }

                override fun onClose() {
                    findDialogFragment()?.let {
                        (it as? KetchDialogFragment)?.dismiss()
                    }
                }

                private fun showConsentPopup(forceContentDisplay: ContentDisplay? = null) {
                    if (findDialogFragment() != null) {
                        return
                    }

                    if (config == null) return
                    if (!showConsent) return

                    var consentWindowType = ContentDisplay.Banner
                    var modalPosition = modalWindowPosition
                    var bannerPosition = bannerWindowPosition

                    config?.let {
                        it.experiences?.consent?.let {
                            consentWindowType = it.display
                        }

                        it.theme?.banner?.container?.position?.let { position ->
                            if (bannerPosition == null) {
                                bannerPosition = position.mapToDialogPosition()
                            }
                        }

                        it.theme?.modal?.container?.position?.let { position ->
                            if (modalPosition == null) {
                                modalPosition = position.mapToDialogPosition()
                            }
                        }
                    }

                    forceContentDisplay?.let {
                        consentWindowType = it
                    }

                    val windowPosition = if (consentWindowType == ContentDisplay.Modal) {
                        modalPosition ?: DEFAULT_MODAL_POSITION
                    } else {
                        bannerPosition ?: DEFAULT_BANNER_POSITION
                    }

                    val dialog = KetchDialogFragment.newInstance(windowPosition)
                    fragmentManager.let {
                        dialog.show(it, webView)
                    }
                    showConsent = false
                }
            }
        }
    }

    private fun findDialogFragment() = fragmentManager.findFragmentByTag(KetchDialogFragment.TAG)

    fun load() {
        webView.let {
            it.listener?.onConfigUpdated(null)
            it.load(orgCode, property, identities.map {
                Identity(it.key, it.value)
            }, url)
        }
    }

    fun forceShowConsent() {
        webView.forceShow(ExperienceType.CONSENT)
    }

    fun showPreferences() {
        webView.forceShow(ExperienceType.PREFERENCES)
    }

    fun showPreferencesTab(tabs: List<PreferencesTab>, tab: PreferencesTab) {
        webView.showPreferencesTab(tabs, tab)
    }

    fun setIdentities(identities: Map<String, String>) {
        this.identities = identities
    }

    fun setLanguage(language: String) {
        webView.setLanguage(language)
    }

    fun setJurisdiction(jurisdiction: String?) {
        webView.setJurisdiction(jurisdiction)
    }

    fun setRegion(region: String?) {
        webView.setRegion(region)
    }

    fun setBannerWindowPosition(position: WindowPosition?) {
        this.bannerWindowPosition = position
    }

    fun setModalWindowPosition(position: WindowPosition?) {
        this.modalWindowPosition = position
    }

    enum class ExperienceType {
        CONSENT,
        PREFERENCES;

        fun getUrlParameter(): String = when (this) {
            CONSENT -> "cd"
            PREFERENCES -> "preferences"
        }
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

    interface Listener {
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

    enum class WindowPosition(@StyleRes val animId: Int, @GravityInt val gravity: Int) {
        TOP(R.style.SlideFromTopAnimation, Gravity.CENTER_HORIZONTAL.or(Gravity.TOP)),
        BOTTOM(R.style.SlideFromBottomAnimation, Gravity.CENTER_HORIZONTAL.or(Gravity.BOTTOM)),
        BOTTOM_LEFT(R.style.SlideFromLeftAnimation, Gravity.LEFT.or(Gravity.BOTTOM)),
        BOTTOM_RIGHT(R.style.SlideFromRightAnimation, Gravity.RIGHT.or(Gravity.BOTTOM)),
        BOTTOM_MIDDLE(
            R.style.SlideFromBottomAnimation,
            Gravity.CENTER_HORIZONTAL.or(Gravity.BOTTOM)
        ),
        CENTER(R.style.FadeInCenterAnimation, Gravity.CENTER)
    }

    @Parcelize
    data class WindowSize(
        @FloatRange(from = 0.1, to = 1.0) val width: Float,
        @FloatRange(from = 0.1, to = 1.0) val height: Float
    ) : Parcelable

    companion object {
        const val IAB_TCF_TC_STRING = "IABTCF_TCString"
        const val IAB_US_PRIVACY_STRING = "IABUSPrivacy_String"
        const val IAB_GPP_HDR_GPP_STRING = "IABGPP_HDR_GppString"

        private val DEFAULT_BANNER_POSITION = WindowPosition.BOTTOM_MIDDLE
        private val DEFAULT_MODAL_POSITION = WindowPosition.BOTTOM_MIDDLE
    }
}
