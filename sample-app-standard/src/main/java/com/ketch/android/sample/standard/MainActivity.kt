package com.ketch.android.sample.standard

import android.os.Bundle
import android.util.Log
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.ketch.android.Ketch
import com.ketch.android.KetchSdk
import com.ketch.android.api.KetchDataCenter
import com.ketch.android.data.Consent
import com.ketch.android.data.ConsentConfig
import com.ketch.android.data.FullConfigurationRequest
import com.ketch.android.data.HeadlessConfiguration
import com.ketch.android.data.HideExperienceStatus
import com.ketch.android.data.KetchConfig
import com.ketch.android.data.WillShowExperienceType
import com.ketch.android.sample.standard.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private object DevUrlOverrides {
    const val ENABLED = true

    val forEmulator: Map<String, String> = mapOf(
        "https://cdn.uat.ketchjs.com/ketchtag/stable/v2.12/ketch-sdk.js" to "http://localhost:9000/ketch-sdk.js",
        "ketch-sdk.js" to "http://localhost:9000/ketch-sdk.js",
    )

    val forDevice: Map<String, String> = mapOf(
        "https://cdn.uat.ketchjs.com/ketchtag/stable/v2.12/ketch-sdk.js" to "http://localhost:9000/ketch-sdk.js",
        "ketch-sdk.js" to "http://localhost:9000/ketch-sdk.js",
    )
}

private data class SampleDashboardState(
    val initState: String = "Initialized",
    val statusText: String = "Ketch initialized",
    val loadState: String = "idle",
    val experienceVisibility: String = "hidden",
    val dismissReason: String = "—",
    val environment: String = "Not set",
    val jurisdiction: String = "Not set",
    val region: String = "Not set",
    val consent: String = "Not set",
    val usPrivacy: String = "Not set",
    val tcf: String = "Not set",
    val gpp: String = "Not set",
    val configSummary: String = "Not loaded",
    val purposesSummary: String = "Not loaded",
    val headlessLocationResult: String = "—",
    val headlessBootstrapResult: String = "—",
    val headlessConsentResult: String = "—",
    val eventLog: List<String> = emptyList(),
) {
    fun appendLog(message: String): SampleDashboardState {
        val line = "[${SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())}] $message"
        return copy(eventLog = (eventLog + line).takeLast(50))
    }

    fun setStatus(message: String): SampleDashboardState =
        appendLog(message).copy(statusText = message)
}

private object HeadlessSampleSupport {
    const val ORG_CODE = "ethansch061226"
    const val PROPERTY = "website_smart_tag"
    const val ENVIRONMENT = "production"
    val dataCenter: KetchDataCenter = KetchDataCenter.UAT

    fun uniqueEmailIdentity(): Map<String, String> =
        mapOf("email" to "headless-${UUID.randomUUID()}@integration.ketch.test")

    fun consentConfigFrom(
        config: HeadlessConfiguration,
        identities: Map<String, String>,
    ): ConsentConfig {
        val jurisdiction = config.jurisdiction?.code
            ?: config.jurisdiction?.defaultJurisdictionCode
            ?: "us"
        val purposes = config.purposes
            ?.mapNotNull { purpose ->
                val code = purpose.code
                val legalBasis = purpose.legalBasisCode
                if (code != null && legalBasis != null) {
                    code to ConsentConfig.PurposeLegalBasis(legalBasis)
                } else {
                    null
                }
            }
            ?.toMap()
            ?: emptyMap()
        require(purposes.isNotEmpty()) { "Configuration returned no purposes" }
        return ConsentConfig(
            organizationCode = ORG_CODE,
            propertyCode = PROPERTY,
            environmentCode = ENVIRONMENT,
            jurisdictionCode = jurisdiction,
            identities = identities,
            purposes = purposes,
        )
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var ketch: Ketch
    private var dashboard = SampleDashboardState()

    companion object {
        private const val TAG = "KetchSample"
        private const val ORG_CODE = "ethansch061226"
        private const val PROPERTY = "website_smart_tag"
        private const val ENVIRONMENT = "production"
        private const val LANGUAGE = "en"
    }

    private val ketchListener = object : Ketch.Listener {
        override fun onShow() {
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
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.headerBar.setPadding(
                binding.headerBar.paddingLeft,
                insets.top + 16,
                binding.headerBar.paddingRight,
                binding.headerBar.paddingBottom
            )
            view.updatePadding(bottom = insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        setupDarkModeToggle()
        initializeKetch()
        setupClickListeners()
        renderDashboard()
    }

    private fun setupDarkModeToggle() {
        val isNightMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        binding.darkModeSwitch.isChecked = isNightMode
        binding.darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
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
            dataCenter = HeadlessSampleSupport.dataCenter,
            logLevel = Ketch.LogLevel.DEBUG,
            webResourceUrlOverrides = if (DevUrlOverrides.ENABLED) DevUrlOverrides.forEmulator else emptyMap(),
        )
        ketch.setIdentities(mapOf("email" to "sample-test@integration.ketch.test"))
        ketch.setLanguage("en")
        dashboard = dashboard.setStatus("Ketch initialized")
    }

    private fun setupClickListeners() {
        val showConsent = {
            Log.d(TAG, "showConsent() called")
            updateDashboard { it.setStatus("showConsent() called") }
            ketch.showConsent()
        }
        val showPreferences = {
            Log.d(TAG, "showPreferences() called")
            updateDashboard { it.setStatus("showPreferences() called") }
            ketch.showPreferences()
        }

        binding.loadButton.setOnClickListener {
            updateDashboard { it.copy(loadState = "loading").setStatus("Load called") }
            ketch.load()
        }
        binding.showConsentButton.setOnClickListener { showConsent() }
        binding.showPreferencesButton.setOnClickListener { showPreferences() }
        binding.showConsentCardButton.setOnClickListener { showConsent() }
        binding.showPreferencesCardButton.setOnClickListener { showPreferences() }

        binding.setLanguageButton.setOnClickListener {
            ketch.setLanguage("EN")
            updateDashboard { it.setStatus("Language set to EN") }
        }
        binding.setJurisdictionButton.setOnClickListener {
            ketch.setJurisdiction("US")
            updateDashboard { it.setStatus("Jurisdiction set to US") }
        }
        binding.setRegionButton.setOnClickListener {
            ketch.setRegion("California")
            updateDashboard { it.setStatus("Region set to California") }
        }

        binding.headlessLocationButton.setOnClickListener { runHeadlessLocation() }
        binding.headlessBootstrapButton.setOnClickListener { runHeadlessBootstrap() }
        binding.headlessConsentButton.setOnClickListener { runHeadlessConsent() }
    }

    private fun updateDashboard(block: (SampleDashboardState) -> SampleDashboardState) {
        runOnUiThread {
            dashboard = block(dashboard)
            renderDashboard()
        }
    }

    private fun renderDashboard() {
        binding.initText.text = "Init: ${dashboard.initState}"
        binding.statusText.text = "Status: ${dashboard.statusText}"
        binding.connectionText.text = "Connection: $ORG_CODE / $PROPERTY / $ENVIRONMENT"
        binding.loadStateText.text = "Load: ${dashboard.loadState}"
        binding.visibilityText.text = "Visibility: ${dashboard.experienceVisibility}"
        binding.dismissText.text = "Dismiss: ${dashboard.dismissReason}"
        binding.environmentText.text = "Environment: ${dashboard.environment}"
        binding.jurisdictionText.text = "Jurisdiction: ${dashboard.jurisdiction}"
        binding.regionText.text = "Region: ${dashboard.region}"
        binding.consentText.text = "Consent: ${truncate(dashboard.consent)}"
        binding.usPrivacyText.text = "US Privacy: ${truncate(dashboard.usPrivacy)}"
        binding.tcfText.text = "TCF: ${truncate(dashboard.tcf)}"
        binding.gppText.text = "GPP: ${truncate(dashboard.gpp)}"
        binding.headlessLocationText.text = "Location: ${dashboard.headlessLocationResult}"
        binding.headlessBootstrapText.text = "Bootstrap: ${dashboard.headlessBootstrapResult}"
        binding.headlessConsentText.text = "Consent: ${dashboard.headlessConsentResult}"

        binding.eventLogText.text = if (dashboard.eventLog.isEmpty()) {
            "Waiting for events..."
        } else {
            dashboard.eventLog.joinToString("\n")
        }
        binding.eventLogScroll.post {
            binding.eventLogScroll.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    private fun truncate(value: String, max: Int = 80): String =
        if (value.length <= max) value else "${value.take(max)}…"

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
        KetchSdk.fetchBootstrapConfiguration(ORG_CODE, PROPERTY, HeadlessSampleSupport.dataCenter) { result ->
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
            KetchSdk.fetchBootstrapConfiguration(ORG_CODE, PROPERTY, HeadlessSampleSupport.dataCenter) { bootResult ->
                bootResult.onSuccess { boot ->
                    val jurisdiction = boot.jurisdiction?.code
                        ?: boot.jurisdiction?.defaultJurisdictionCode
                    KetchSdk.fetchFullConfiguration(
                        FullConfigurationRequest(
                            organizationCode = ORG_CODE,
                            propertyCode = PROPERTY,
                            environmentCode = ENVIRONMENT,
                            jurisdictionCode = jurisdiction,
                            languageCode = LANGUAGE,
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
