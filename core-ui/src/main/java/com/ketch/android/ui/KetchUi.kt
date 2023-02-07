package com.ketch.android.ui

import android.content.Context
import android.util.Log
import com.ketch.android.Ketch
import com.ketch.android.api.request.PurposeAllowedLegalBasis
import com.ketch.android.api.request.User
import com.ketch.android.api.response.Consent
import com.ketch.android.api.response.ExperienceButtonAction
import com.ketch.android.api.response.ExperienceButtonDestination
import com.ketch.android.api.response.ExperienceDefault
import com.ketch.android.api.response.FullConfiguration
import com.ketch.android.api.response.Purpose
import com.ketch.android.api.response.Right
import com.ketch.android.ui.common.PreferenceService
import com.ketch.android.ui.dialog.BannerDialog
import com.ketch.android.ui.dialog.JitDialog
import com.ketch.android.ui.dialog.ModalDialog
import com.ketch.android.ui.dialog.PreferenceDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn

/**
 * Represents Ketch UI Components
 *
 * @param context - [android.content.Context]
 * @param ketch - [com.ketch.android.Ketch]
 */
class KetchUi(private val context: Context, private val ketch: Ketch) {

    /**
     * Show dialogs automatically
     */
    var showDialogsIfNeeded: Boolean = false

    private val scope = CoroutineScope(Dispatchers.Main)

    private val preferenceService = PreferenceService(context)

    init {
        combine(
            ketch.configuration,
            ketch.consent
        ) { configuration, consent ->
            if (showDialogsIfNeeded && configuration != null && consent != null) {
                showConsentExperience(configuration, consent)
            }
        }.launchIn(scope)
    }

    private fun showConsentExperience(
        configuration: FullConfiguration,
        consent: Consent,
    ) {
        configuration.experiences?.consentExperience?.experienceDefault?.let { experienceDefault ->
            val dialogListener = object : DialogListener {
                override fun onShow() {
                }

                override fun onHide() {
                    if (shouldShowPreference(configuration, consent)) {
                        showPreference(configuration, consent)
                    }
                }

                override fun onFirstButtonClick() {
                }

                override fun onSecondButtonClick() {
                }

                override fun onThirdButtonClick() {
                }
            }

            when (experienceDefault) {
                ExperienceDefault.BANNER -> if (shouldShowBanner(configuration)) {
                    showBanner(configuration, consent, dialogListener)
                } else if (shouldShowPreference(configuration, consent)) {
                    showPreference(configuration, consent)
                }
                ExperienceDefault.MODAL -> if (shouldShowModal(configuration, consent)) {
                    showModal(configuration, consent, dialogListener)
                } else if (shouldShowPreference(configuration, consent)) {
                    showPreference(configuration, consent)
                }
                else -> {}
            }
        }
    }

    /**
     * Show Banner
     * @param configuration - [com.ketch.android.api.response.FullConfiguration]
     * @param consent - [com.ketch.android.api.response.Consent]
     * @param listener - [com.ketch.android.ui.KetchUi.DialogListener]
     */
    fun showBanner(configuration: FullConfiguration, consent: Consent, listener: DialogListener? = null) {
        configuration.experiences?.consentExperience?.banner?.let { banner ->
            val dialog = BannerDialog(context, configuration, object : BannerDialog.BannerDialogListener {
                override fun onShow(bannerDialog: BannerDialog) {
                    listener?.onShow()
                }

                override fun onHide(bannerDialog: BannerDialog) {
                    listener?.onHide()
                }

                override fun onFirstButtonClick(bannerDialog: BannerDialog) {
                    listener?.onFirstButtonClick()
                    banner.primaryButtonAction?.let {
                        buttonAction(configuration, consent, it)
                        bannerDialog.dismiss()
                    }
                }

                override fun onSecondButtonClick(bannerDialog: BannerDialog) {
                    listener?.onSecondButtonClick()
                    banner.secondaryButtonDestination?.let {
                        buttonDestination(configuration, consent, it)
                        bannerDialog.dismiss()
                    }
                }

                override fun showModal(bannerDialog: BannerDialog) {
                    showModal(configuration, consent)
                }

            })

            dialog.show()
        }
    }

    /**
     * Show Modal Popup
     * @param configuration - [com.ketch.android.api.response.FullConfiguration]
     * @param consent - [com.ketch.android.api.response.Consent]
     * @param listener - [com.ketch.android.ui.KetchUi.DialogListener]
     */
    fun showModal(configuration: FullConfiguration, consent: Consent, listener: DialogListener? = null) {
        configuration.experiences?.consentExperience?.modal?.let {
            val dialog = ModalDialog(context, configuration, consent, object : ModalDialog.ModalDialogListener {
                override fun onShow(modalDialog: ModalDialog) {
                    listener?.onShow()
                }

                override fun onHide(modalDialog: ModalDialog) {
                    listener?.onHide()
                }

                override fun onButtonClick(modalDialog: ModalDialog, consent: Consent) {
                    listener?.onFirstButtonClick()
                    buttonAction(configuration, consent, ExperienceButtonAction.SAVE_CURRENT_STATE)
                    modalDialog.dismiss()
                }
            })

            dialog.show()
        }
    }

    /**
     * Show Just in Time Popup
     * @param configuration - [com.ketch.android.api.response.FullConfiguration]
     * @param consent - [com.ketch.android.api.response.Consent]
     * @param purpose - [com.ketch.android.api.response.Purpose]
     * @param listener - [com.ketch.android.ui.KetchUi.DialogListener]
     */
    fun showJit(
        configuration: FullConfiguration,
        consent: Consent,
        purpose: Purpose,
        listener: DialogListener? = null
    ) {
        configuration.experiences?.consentExperience?.jit?.let { jit ->
            val dialog = JitDialog(context, configuration, consent, purpose, object : JitDialog.JitDialogListener {

                override fun onShow(jitDialog: JitDialog) {
                    listener?.onShow()
                }

                override fun onHide(jitDialog: JitDialog) {
                    listener?.onHide()
                }

                override fun onFirstButtonClick(jitDialog: JitDialog, consent: Consent) {
                    listener?.onFirstButtonClick()
                    buttonAction(configuration, consent, ExperienceButtonAction.SAVE_CURRENT_STATE)
                    jitDialog.dismiss()
                }

                override fun onSecondButtonClick(jitDialog: JitDialog, consent: Consent) {
                    listener?.onSecondButtonClick()
                    buttonAction(configuration, consent, ExperienceButtonAction.SAVE_CURRENT_STATE)
                    jitDialog.dismiss()
                }

                override fun onThirdButtonClick(jitDialog: JitDialog) {
                    listener?.onThirdButtonClick()
                    jit.moreInfoDestination?.let {
                        buttonDestination(configuration, consent, it)
                    }
                }

                override fun showModal(jitDialog: JitDialog) {
                    showModal(configuration, consent)
                }
            })

            dialog.show()
        }
    }

    /**
     * Show Preference Popup
     * @param configuration - [com.ketch.android.api.response.FullConfiguration]
     * @param consent - [com.ketch.android.api.response.Consent]
     * @param listener - [com.ketch.android.ui.KetchUi.DialogListener]
     */
    fun showPreference(configuration: FullConfiguration, consent: Consent, listener: DialogListener? = null) {
        configuration.experiences?.preference?.let {
            val dialog =
                PreferenceDialog(context, configuration, consent, object : PreferenceDialog.PreferencesDialogListener {
                    override fun onShow(preferenceDialog: PreferenceDialog) {
                        preferenceService.updatePreferenceVersion(version = it.version)
                        listener?.onShow()
                    }

                    override fun onHide(preferenceDialog: PreferenceDialog) {
                        listener?.onHide()
                    }

                    override fun onConsentsButtonClick(preferenceDialog: PreferenceDialog, consent: Consent) {
                        listener?.onFirstButtonClick()
                        buttonAction(configuration, consent, ExperienceButtonAction.SAVE_CURRENT_STATE)
                        preferenceDialog.dismiss()
                    }

                    override fun onRightButtonClick(preferenceDialog: PreferenceDialog, right: Right, user: User) {
                        listener?.onSecondButtonClick()
                        invokeRight(right, user)
                    }

                    override fun showModal(preferenceDialog: PreferenceDialog) {
                        showModal(configuration, consent)
                    }
                })

            dialog.show()
        }
    }

    private fun shouldShowConsent(configuration: FullConfiguration, consent: Consent): Boolean {
        configuration.purposes?.onEach {
            if (consent.purposes?.get(it.code) == null) {
                Log.d(TAG, "shouldShowConsent: true")
                return true
            }
        }

        Log.d(TAG, "shouldShowConsent: false")
        return false
    }

    private fun shouldShowBanner(configuration: FullConfiguration): Boolean {
        if (preferenceService.getConsentVersion() != configuration.experiences?.consentExperience?.version) {
            Log.d(TAG, "shouldShowBanner: true")
            return true
        }

        Log.d(TAG, "shouldShowBanner: false")
        return false
    }

    private fun shouldShowModal(configuration: FullConfiguration, consent: Consent): Boolean {
        if (preferenceService.getConsentVersion() != configuration.experiences?.consentExperience?.version) {
            Log.d(TAG, "shouldShowModal: true")
            return true
        }

        if (shouldShowConsent(configuration, consent)) {
            Log.d(TAG, "shouldShowModal: true")
            return true
        }

        Log.d(TAG, "shouldShowModal: false")
        return false
    }

    private fun shouldShowPreference(configuration: FullConfiguration, consent: Consent): Boolean {
        if (preferenceService.getPreferenceVersion() != configuration.experiences?.preference?.version) {
            Log.d(TAG, "shouldShowPreference: true")
            return true
        }

        if (preferenceService.getConsentVersion() != configuration.experiences?.consentExperience?.version) {
            Log.d(TAG, "shouldShowPreference: true")
            return true
        }

        if (shouldShowConsent(configuration, consent)) {
            Log.d(TAG, "shouldShowPreference: true")
            return true
        }

        Log.d(TAG, "shouldShowPreference: false")
        return false
    }

    private fun buttonAction(
        configuration: FullConfiguration,
        consent: Consent,
        primaryButtonAction: ExperienceButtonAction,
    ) {
        when (primaryButtonAction) {
            ExperienceButtonAction.ACCEPT_ALL -> acceptAll(configuration)
            ExperienceButtonAction.SAVE_CURRENT_STATE -> saveConsentState(configuration, consent)
            else -> {}
        }
    }

    private fun buttonDestination(
        configuration: FullConfiguration,
        consent: Consent,
        secondaryButtonDestination: ExperienceButtonDestination
    ) {
        when (secondaryButtonDestination) {
            ExperienceButtonDestination.MODAL -> showModal(configuration, consent)
            ExperienceButtonDestination.PREFERENCE -> showPreference(configuration, consent)
            ExperienceButtonDestination.REJECT_ALL -> rejectAll(configuration)
            else -> {}
        }
    }

    private fun acceptAll(configuration: FullConfiguration) {
        val purposes: Map<String, PurposeAllowedLegalBasis> = configuration.purposes?.map {
            it.code to PurposeAllowedLegalBasis(
                it.legalBasisCode,
                true.toString()
            )
        }?.toMap() ?: emptyMap()
        val vendors: List<String> = configuration.vendors?.map {
            it.id
        } ?: emptyList()
        ketch.updateConsent(purposes, vendors)
        preferenceService.updateConsentVersion(configuration.experiences?.consentExperience?.version)
    }

    private fun rejectAll(configuration: FullConfiguration) {
        val purposes: Map<String, PurposeAllowedLegalBasis> = configuration.purposes?.map {
            it.code to PurposeAllowedLegalBasis(
                it.legalBasisCode,
                false.toString()
            )
        }?.toMap() ?: emptyMap()
        val vendors: List<String> = emptyList()
        ketch.updateConsent(purposes, vendors)
        preferenceService.updateConsentVersion(configuration.experiences?.consentExperience?.version)
    }

    private fun saveConsentState(configuration: FullConfiguration, consent: Consent) {
        val purposes: Map<String, PurposeAllowedLegalBasis> = configuration.purposes?.map {
            it.code to PurposeAllowedLegalBasis(
                it.legalBasisCode,
                consent.purposes?.get(it.code) ?: true.toString()
            )
        }?.toMap() ?: emptyMap()
        val vendors: List<String> = consent.vendors ?: emptyList()
        ketch.updateConsent(purposes, vendors)
        preferenceService.updateConsentVersion(configuration.experiences?.consentExperience?.version)
    }

    private fun invokeRight(right: Right, user: User) {
        ketch.invokeRights(right.code, user)
    }

    interface DialogListener {
        fun onShow()
        fun onHide()
        fun onFirstButtonClick()
        fun onSecondButtonClick()
        fun onThirdButtonClick()
    }

    companion object {
        private val TAG = KetchUi::class.java.simpleName
    }
}