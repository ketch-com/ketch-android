package com.ketch.android.sample.compose

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ketch.android.Ketch
import com.ketch.android.KetchSdk
import com.ketch.android.data.Consent
import com.ketch.android.data.FullConfigurationRequest
import com.ketch.android.data.HideExperienceStatus
import com.ketch.android.data.KetchConfig
import com.ketch.android.data.WillShowExperienceType

class MainActivity : AppCompatActivity() {

    private lateinit var ketch: Ketch
    private var dashboard by mutableStateOf(SampleDashboardState())

    companion object {
        private const val TAG = "KetchCompose"
        private const val ORG_CODE = "ethansch061226"
        private const val PROPERTY = "website_smart_tag"
        private const val ENVIRONMENT = "production"
    }

    private val ketchListener = object : Ketch.Listener {
        override fun onShow() {
            Log.d(TAG, "onShow")
            updateDashboard {
                it.copy(experienceVisibility = "showing").appendLog("onShow")
            }
        }

        override fun onDismiss(status: HideExperienceStatus) {
            updateDashboard {
                it.copy(experienceVisibility = "dismissed", dismissReason = status.toString())
                    .appendLog("onDismiss: $status")
            }
        }

        override fun onConfigUpdated(config: KetchConfig?) {
            updateDashboard { it.appendLog("onConfigUpdated") }
        }

        override fun onEnvironmentUpdated(environment: String?) {
            updateDashboard {
                it.copy(
                    environment = environment ?: "Not set",
                    loadState = "loaded",
                ).appendLog("onEnvironmentUpdated: $environment")
            }
        }

        override fun onRegionInfoUpdated(regionInfo: String?) {
            updateDashboard {
                it.copy(region = regionInfo ?: "Not set").appendLog("onRegionInfoUpdated: $regionInfo")
            }
        }

        override fun onJurisdictionUpdated(jurisdiction: String?) {
            updateDashboard {
                it.copy(jurisdiction = jurisdiction ?: "Not set").appendLog("onJurisdictionUpdated: $jurisdiction")
            }
        }

        override fun onIdentitiesUpdated(identities: String?) {
            updateDashboard { it.appendLog("onIdentitiesUpdated: $identities") }
        }

        override fun onConsentUpdated(consent: Consent) {
            updateDashboard {
                it.copy(consent = consent.purposes.toString()).appendLog("onConsentUpdated")
            }
        }

        override fun onError(errMsg: String?) {
            updateDashboard {
                it.copy(loadState = "error", initState = "Error")
                    .setStatus("Error: ${errMsg ?: "unknown"}")
            }
        }

        override fun onUSPrivacyUpdated(values: Map<String, Any?>) {
            updateDashboard {
                it.copy(usPrivacy = values["IABUSPrivacy_String"]?.toString() ?: "Not set")
                    .appendLog("onUSPrivacyUpdated")
            }
        }

        override fun onTCFUpdated(values: Map<String, Any?>) {
            updateDashboard {
                it.copy(tcf = values["IABTCF_TCString"]?.toString() ?: "Not set").appendLog("onTCFUpdated")
            }
        }

        override fun onGPPUpdated(values: Map<String, Any?>) {
            updateDashboard {
                it.copy(gpp = values["IABGPP_HDR_GppString"]?.toString() ?: "Not set").appendLog("onGPPUpdated")
            }
        }

        override fun onWillShowExperience(type: WillShowExperienceType) {
            updateDashboard {
                it.copy(experienceVisibility = "will show: $type").appendLog("onWillShowExperience: $type")
            }
        }

        override fun onHasShownExperience() {
            updateDashboard {
                it.copy(experienceVisibility = "shown").appendLog("onHasShownExperience")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        initializeKetch()

        setContent {
            KetchSampleApp(
                dashboard = dashboard,
                connectionSummary = "$ORG_CODE / $PROPERTY / $ENVIRONMENT",
                onLoad = {
                    updateDashboard { it.copy(loadState = "loading").setStatus("Load called") }
                    ketch.load()
                },
                onShowConsent = {
                    updateDashboard { it.setStatus("showConsent() called") }
                    ketch.showConsent()
                },
                onShowPreferences = {
                    updateDashboard { it.setStatus("showPreferences() called") }
                    ketch.showPreferences()
                },
                onSetLanguage = {
                    ketch.setLanguage("EN")
                    updateDashboard { it.setStatus("Language set to EN") }
                },
                onSetJurisdiction = {
                    ketch.setJurisdiction("US")
                    updateDashboard { it.setStatus("Jurisdiction set to US") }
                },
                onSetRegion = {
                    ketch.setRegion("California")
                    updateDashboard { it.setStatus("Region set to California") }
                },
                onHeadlessLocation = { runHeadlessLocation() },
                onHeadlessBootstrap = { runHeadlessBootstrap() },
                onHeadlessConsent = { runHeadlessConsent() },
            )
        }
    }

    private fun initializeKetch() {
        ketch = KetchSdk.create(
            context = this,
            fragmentManager = supportFragmentManager,
            organization = ORG_CODE,
            property = PROPERTY,
            environment = ENVIRONMENT,
            listener = ketchListener,
            logLevel = Ketch.LogLevel.DEBUG,
            webResourceUrlOverrides = if (DevUrlOverrides.ENABLED) DevUrlOverrides.forEmulator else emptyMap(),
        )
        ketch.setIdentities(mapOf("aaid" to "sample-test-123"))
        dashboard = dashboard.setStatus("Ketch initialized")
    }

    private fun updateDashboard(block: (SampleDashboardState) -> SampleDashboardState) {
        runOnUiThread { dashboard = block(dashboard) }
    }

    private fun runHeadlessLocation() {
        updateDashboard { it.copy(headlessLocationResult = "Loading...") }
        KetchSdk.fetchLocation(HeadlessSampleSupport.dataCenter) { result ->
            val text = result.fold(
                onSuccess = { "OK: ${it.location?.countryCode ?: "?"}" },
                onFailure = { "Error: ${it.message}" },
            )
            updateDashboard { it.copy(headlessLocationResult = text).appendLog("headless location: $text") }
        }
    }

    private fun runHeadlessBootstrap() {
        updateDashboard { it.copy(headlessBootstrapResult = "Loading...") }
        KetchSdk.fetchBootstrapConfiguration(
            ORG_CODE,
            PROPERTY,
            HeadlessSampleSupport.dataCenter,
        ) { result ->
            val text = result.fold(
                onSuccess = { "OK: ${it.purposes?.size ?: 0} purpose(s)" },
                onFailure = { "Error: ${it.message}" },
            )
            updateDashboard { it.copy(headlessBootstrapResult = text).appendLog("headless bootstrap: $text") }
        }
    }

    private fun runHeadlessConsent() {
        updateDashboard { it.copy(headlessConsentResult = "Loading...") }
        val identities = HeadlessSampleSupport.uniqueEmailIdentity()
        KetchSdk.fetchLocation(HeadlessSampleSupport.dataCenter) { _ ->
            KetchSdk.fetchBootstrapConfiguration(
                ORG_CODE,
                PROPERTY,
                HeadlessSampleSupport.dataCenter,
            ) { bootResult ->
                bootResult.onSuccess { _ ->
                    KetchSdk.fetchFullConfiguration(
                        FullConfigurationRequest(
                            organizationCode = ORG_CODE,
                            propertyCode = PROPERTY,
                            environmentCode = ENVIRONMENT,
                        ),
                        HeadlessSampleSupport.dataCenter,
                    ) { fullResult ->
                        fullResult.onSuccess { full ->
                            val config = HeadlessSampleSupport.consentConfigFrom(full, identities)
                            KetchSdk.fetchConsent(config, HeadlessSampleSupport.dataCenter) { consentResult ->
                                val text = consentResult.fold(
                                    onSuccess = {
                                        val count = it.purposes?.size ?: it.protocols?.size ?: 0
                                        "OK: $count item(s)"
                                    },
                                    onFailure = { "Error: ${it.message}" },
                                )
                                updateDashboard {
                                    it.copy(headlessConsentResult = text).appendLog("headless consent: $text")
                                }
                            }
                        }.onFailure { err ->
                            updateDashboard { it.copy(headlessConsentResult = "Error: ${err.message}") }
                        }
                    }
                }.onFailure { err ->
                    updateDashboard { it.copy(headlessConsentResult = "Error: ${err.message}") }
                }
            }
        }
    }
}
