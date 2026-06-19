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

    var logCallback: ((String) -> Unit)? = null

    /**
     * Optional richer [Ketch.Listener] that MainActivity plugs in to drive its
     * SDK Health Dashboard. Ketch itself only supports a single listener (set at
     * construction time), so [sharedListener] below always fires first and then
     * forwards every callback here when a delegate is registered.
     */
    var dashboardListener: Ketch.Listener? = null

    override fun onCreate() {
        super.onCreate()
        ketch = KetchSdk.create(
            context = this,
            organization = ORG_CODE,
            property = PROPERTY,
            environment = ENVIRONMENT,
            listener = sharedListener,
            ketchUrl = null,
            logLevel = Ketch.LogLevel.DEBUG,
        )
        ketch.setIdentities(mapOf("aaid" to "sample-test-123"))
    }

    private val sharedListener = object : Ketch.Listener {
        override fun onShow() {
            Log.d(TAG, "onShow: Dialog shown")
            log("onShow: Dialog shown")
            dashboardListener?.onShow()
        }

        override fun onDismiss(status: HideExperienceStatus) {
            Log.d(TAG, "onDismiss: status=$status")
            log("onDismiss: status=$status")
            dashboardListener?.onDismiss(status)
        }

        override fun onConfigUpdated(config: KetchConfig?) {
            Log.d(TAG, "onConfigUpdated: $config")
            log("onConfigUpdated")
            dashboardListener?.onConfigUpdated(config)
        }

        override fun onConfigDebugInfo(configSummary: String, purposesSummary: String) {
            Log.d(TAG, "onConfigDebugInfo: $configSummary / $purposesSummary")
            dashboardListener?.onConfigDebugInfo(configSummary, purposesSummary)
        }

        override fun onEnvironmentUpdated(environment: String?) {
            Log.d(TAG, "onEnvironmentUpdated: $environment")
            log("onEnvironmentUpdated: $environment")
            dashboardListener?.onEnvironmentUpdated(environment)
        }

        override fun onRegionInfoUpdated(regionInfo: String?) {
            Log.d(TAG, "onRegionInfoUpdated: $regionInfo")
            log("onRegionInfoUpdated: $regionInfo")
            dashboardListener?.onRegionInfoUpdated(regionInfo)
        }

        override fun onJurisdictionUpdated(jurisdiction: String?) {
            Log.d(TAG, "onJurisdictionUpdated: $jurisdiction")
            log("onJurisdictionUpdated: $jurisdiction")
            dashboardListener?.onJurisdictionUpdated(jurisdiction)
        }

        override fun onIdentitiesUpdated(identities: String?) {
            Log.d(TAG, "onIdentitiesUpdated: $identities")
            log("onIdentitiesUpdated: $identities")
            dashboardListener?.onIdentitiesUpdated(identities)
        }

        override fun onConsentUpdated(consent: Consent) {
            Log.d(TAG, "onConsentUpdated: purposes=${consent.purposes}")
            log("onConsentUpdated: ${consent.purposes}")
            dashboardListener?.onConsentUpdated(consent)
        }

        override fun onError(errMsg: String?) {
            Log.e(TAG, "onError: $errMsg")
            log("ERROR: $errMsg")
            dashboardListener?.onError(errMsg)
        }

        override fun onUSPrivacyUpdated(values: Map<String, Any?>) {
            Log.d(TAG, "onUSPrivacyUpdated: $values")
            log("onUSPrivacyUpdated: ${values["IABUSPrivacy_String"]}")
            dashboardListener?.onUSPrivacyUpdated(values)
        }

        override fun onTCFUpdated(values: Map<String, Any?>) {
            Log.d(TAG, "onTCFUpdated: $values")
            log("onTCFUpdated: ${values["IABTCF_TCString"]}")
            dashboardListener?.onTCFUpdated(values)
        }

        override fun onGPPUpdated(values: Map<String, Any?>) {
            Log.d(TAG, "onGPPUpdated: $values")
            log("onGPPUpdated: ${values["IABGPP_HDR_GppString"]}")
            dashboardListener?.onGPPUpdated(values)
        }

        override fun onWillShowExperience(type: WillShowExperienceType) {
            Log.d(TAG, "onWillShowExperience: $type")
            log("onWillShowExperience: $type")
            dashboardListener?.onWillShowExperience(type)
        }

        override fun onHasShownExperience() {
            Log.d(TAG, "onHasShownExperience")
            log("onHasShownExperience")
            dashboardListener?.onHasShownExperience()
        }
    }

    private fun log(message: String) {
        logCallback?.invoke(message)
    }

    companion object {
        private const val TAG = "KetchCompose"
        const val ORG_CODE = "ethansch061226"
        const val PROPERTY = "website_smart_tag"
        const val ENVIRONMENT = "production"
    }
}
