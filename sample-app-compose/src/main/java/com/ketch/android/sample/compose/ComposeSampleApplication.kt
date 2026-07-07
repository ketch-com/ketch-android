package com.ketch.android.sample.compose

import android.app.Application
import android.util.Log
import com.ketch.android.Ketch
import com.ketch.android.KetchSdk
import com.ketch.android.data.Consent
import com.ketch.android.data.HideExperienceStatus
import com.ketch.android.data.KetchConfig
import com.ketch.android.data.WillShowExperienceType

class ComposeSampleApplication : Application() {

    lateinit var ketch: Ketch
        private set

    val infoState = SampleInfoState()

    var logCallback: ((String) -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        ketch = KetchSdk.create(
            context = this,
            organization = SampleConfig.ORG_CODE,
            property = SampleConfig.PROPERTY,
            environment = SampleConfig.ENVIRONMENT,
            listener = sharedListener,
            ketchUrl = SampleConfig.dataCenter.baseUrl,
            logLevel = Ketch.LogLevel.DEBUG,
            webResourceUrlOverrides = if (DevUrlOverrides.ENABLED) DevUrlOverrides.forEmulator else emptyMap(),
        )
        ketch.setLanguage(SampleConfig.LANGUAGE)
        ketch.setIdentities(SampleConfig.identities)
    }

    private val sharedListener = object : Ketch.Listener {
        override fun onShow() {
            Log.d(TAG, "onShow: Dialog shown")
            log("onShow: Dialog shown")
        }

        override fun onDismiss(status: HideExperienceStatus) {
            Log.d(TAG, "onDismiss: status=$status")
            log("onDismiss: status=$status")
        }

        override fun onConfigUpdated(config: KetchConfig?) {
            Log.d(TAG, "onConfigUpdated: $config")
            log("onConfigUpdated")
        }

        override fun onEnvironmentUpdated(environment: String?) {
            Log.d(TAG, "onEnvironmentUpdated: $environment")
            log("onEnvironmentUpdated: $environment")
        }

        override fun onRegionInfoUpdated(regionInfo: String?) {
            Log.d(TAG, "onRegionInfoUpdated: $regionInfo")
            log("onRegionInfoUpdated: $regionInfo")
            infoState.updateRegion(regionInfo)
        }

        override fun onJurisdictionUpdated(jurisdiction: String?) {
            Log.d(TAG, "onJurisdictionUpdated: $jurisdiction")
            log("onJurisdictionUpdated: $jurisdiction")
            infoState.updateJurisdiction(jurisdiction)
        }

        override fun onIdentitiesUpdated(identities: String?) {
            Log.d(TAG, "onIdentitiesUpdated: $identities")
            log("onIdentitiesUpdated: $identities")
        }

        override fun onConsentUpdated(consent: Consent) {
            val summary = formatConsent(consent)
            Log.d(TAG, "onConsentUpdated: $summary")
            log("onConsentUpdated: $summary")
        }

        override fun onError(errMsg: String?) {
            Log.e(TAG, "onError: $errMsg")
            log("ERROR: $errMsg")
        }

        override fun onUSPrivacyUpdated(values: Map<String, Any?>) {
            Log.d(TAG, "onUSPrivacyUpdated: $values")
            log("onUSPrivacyUpdated: ${values["IABUSPrivacy_String"]}")
        }

        override fun onTCFUpdated(values: Map<String, Any?>) {
            Log.d(TAG, "onTCFUpdated: $values")
            log("onTCFUpdated: ${values["IABTCF_TCString"]}")
        }

        override fun onGPPUpdated(values: Map<String, Any?>) {
            Log.d(TAG, "onGPPUpdated: $values")
            log("onGPPUpdated: ${values["IABGPP_HDR_GppString"]}")
        }

        override fun onWillShowExperience(type: WillShowExperienceType) {
            Log.d(TAG, "onWillShowExperience: $type")
            log("onWillShowExperience: $type")
        }

        override fun onHasShownExperience() {
            Log.d(TAG, "onHasShownExperience")
            log("onHasShownExperience")
        }
    }

    private fun log(message: String) {
        logCallback?.invoke(message)
    }

    companion object {
        private const val TAG = "KetchCompose"
    }
}
