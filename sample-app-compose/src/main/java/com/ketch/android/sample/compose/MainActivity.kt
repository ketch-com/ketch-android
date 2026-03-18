package com.ketch.android.sample.compose

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateListOf
import com.ketch.android.Ketch
import com.ketch.android.KetchSdk
import com.ketch.android.data.Consent
import com.ketch.android.data.HideExperienceStatus
import com.ketch.android.data.KetchConfig
import com.ketch.android.data.WillShowExperienceType

class MainActivity : AppCompatActivity() {

    private lateinit var ketch: Ketch
    private val logEntries = mutableStateListOf<String>()

    companion object {
        private const val TAG = "KetchCompose"
        private const val ORG_CODE = "ketch_samples"
        private const val PROPERTY = "android"
        private const val ENVIRONMENT = "production"
    }

    private val ketchListener = object : Ketch.Listener {
        override fun onShow() {
            Log.d(TAG, "onShow: Dialog shown")
            appendLog("onShow: Dialog shown")
        }

        override fun onDismiss(status: HideExperienceStatus) {
            Log.d(TAG, "onDismiss: status=$status")
            appendLog("onDismiss: status=$status")
        }

        override fun onConfigUpdated(config: KetchConfig?) {
            Log.d(TAG, "onConfigUpdated: $config")
            appendLog("onConfigUpdated")
        }

        override fun onEnvironmentUpdated(environment: String?) {
            Log.d(TAG, "onEnvironmentUpdated: $environment")
            appendLog("onEnvironmentUpdated: $environment")
        }

        override fun onRegionInfoUpdated(regionInfo: String?) {
            Log.d(TAG, "onRegionInfoUpdated: $regionInfo")
            appendLog("onRegionInfoUpdated: $regionInfo")
        }

        override fun onJurisdictionUpdated(jurisdiction: String?) {
            Log.d(TAG, "onJurisdictionUpdated: $jurisdiction")
            appendLog("onJurisdictionUpdated: $jurisdiction")
        }

        override fun onIdentitiesUpdated(identities: String?) {
            Log.d(TAG, "onIdentitiesUpdated: $identities")
            appendLog("onIdentitiesUpdated: $identities")
        }

        override fun onConsentUpdated(consent: Consent) {
            Log.d(TAG, "onConsentUpdated: purposes=${consent.purposes}")
            appendLog("onConsentUpdated: ${consent.purposes}")
        }

        override fun onError(errMsg: String?) {
            Log.e(TAG, "onError: $errMsg")
            appendLog("ERROR: $errMsg")
        }

        override fun onUSPrivacyUpdated(values: Map<String, Any?>) {
            Log.d(TAG, "onUSPrivacyUpdated: $values")
            appendLog("onUSPrivacyUpdated: ${values["IABUSPrivacy_String"]}")
        }

        override fun onTCFUpdated(values: Map<String, Any?>) {
            Log.d(TAG, "onTCFUpdated: $values")
            appendLog("onTCFUpdated: ${values["IABTCF_TCString"]}")
        }

        override fun onGPPUpdated(values: Map<String, Any?>) {
            Log.d(TAG, "onGPPUpdated: $values")
            appendLog("onGPPUpdated: ${values["IABGPP_HDR_GppString"]}")
        }

        override fun onWillShowExperience(type: WillShowExperienceType) {
            Log.d(TAG, "onWillShowExperience: $type")
            appendLog("onWillShowExperience: $type")
        }

        override fun onHasShownExperience() {
            Log.d(TAG, "onHasShownExperience")
            appendLog("onHasShownExperience")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        initializeKetch()

        setContent {
            KetchSampleApp(
                logEntries = logEntries,
                onShowConsent = {
                    Log.d(TAG, "showConsent() called")
                    appendLog("showConsent() called")
                    ketch.showConsent()
                },
                onShowPreferences = {
                    Log.d(TAG, "showPreferences() called")
                    appendLog("showPreferences() called")
                    ketch.showPreferences()
                },
            )
        }
    }

    private fun initializeKetch() {
        ketch = KetchSdk.create(
            this,
            supportFragmentManager,
            ORG_CODE,
            PROPERTY,
            ENVIRONMENT,
            ketchListener,
            null,
            Ketch.LogLevel.DEBUG
        )

        ketch.setIdentities(mapOf("aaid" to "sample-test-123"))

        appendLog("Ketch initialized")

        ketch.load()
        appendLog("load() called")
    }

    private fun appendLog(message: String) {
        runOnUiThread {
            logEntries.add(message)
        }
    }
}
