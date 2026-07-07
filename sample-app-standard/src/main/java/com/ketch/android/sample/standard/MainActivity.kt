package com.ketch.android.sample.standard

import android.content.Context
import android.content.Intent
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
import com.ketch.android.sample.standard.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var ketch: Ketch
    private lateinit var app: SampleApplication

    companion object {
        private const val TAG = "KetchSample"
        private val IAB_PREFERENCE_PREFIXES = listOf("IABTCF", "IABGPP", "IABUS")
        private const val SAMPLE_CSS_OVERRIDE = "body { --ketch-brand-color: #6B4EE6; }"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        app = application as SampleApplication
        ketch = app.ketch
        app.logCallback = { message -> appendLog(message) }
        app.infoState.onChange = { runOnUiThread { renderInfoPanel() } }

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
        renderStaticInfo()
        renderInfoPanel()
        initializeKetch()
        setupClickListeners()
    }

    override fun onDestroy() {
        if (isFinishing) {
            app.logCallback = null
            app.infoState.onChange = null
        }
        super.onDestroy()
    }

    private fun renderStaticInfo() {
        binding.infoOrgCodeValue.text = SampleConfig.ORG_CODE
        binding.infoPropertyValue.text = SampleConfig.PROPERTY
        binding.infoEnvironmentValue.text = SampleConfig.ENVIRONMENT
        binding.infoLanguageValue.text = SampleConfig.LANGUAGE
    }

    private fun renderInfoPanel() {
        binding.infoJurisdictionValue.text = app.infoState.jurisdiction
        binding.infoRegionValue.text = app.infoState.region
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
        appendLog("Ketch initialized in Application")
        ketch.load()
        appendLog("load() called from MainActivity")
    }

    private fun setupClickListeners() {
        binding.showConsentButton.setOnClickListener {
            Log.d(TAG, "showConsent() called")
            appendLog("showConsent() called")
            ketch.showConsent()
        }

        binding.showPreferencesButton.setOnClickListener {
            val allowedTabs = allowedTabs()
            val initialTab = initialTab()
            Log.d(TAG, "showPreferencesTab() called: tabs=$allowedTabs, initial=$initialTab")
            appendLog("showPreferencesTab() called: tabs=$allowedTabs, initial=$initialTab")
            ketch.showPreferencesTab(allowedTabs, initialTab)
        }

        binding.reloadButton.setOnClickListener {
            Log.d(TAG, "load() called")
            appendLog("load() called")
            ketch.load()
        }

        binding.applyCssButton.setOnClickListener {
            Log.d(TAG, "setCssStyle() called")
            appendLog("setCssStyle() called")
            ketch.setCssStyle(SAMPLE_CSS_OVERRIDE)
        }

        binding.openSecondActivityButton.setOnClickListener {
            appendLog("Opening SecondActivity")
            startActivity(Intent(this, SecondActivity::class.java))
        }

        binding.logSharedPreferencesButton.setOnClickListener {
            logSharedPreferences()
        }
    }

    private fun allowedTabs(): List<Ketch.PreferencesTab> = buildList {
        if (binding.prefTabOverviewCheck.isChecked) add(Ketch.PreferencesTab.OVERVIEW)
        if (binding.prefTabConsentCheck.isChecked) add(Ketch.PreferencesTab.CONSENTS)
        if (binding.prefTabRightsCheck.isChecked) add(Ketch.PreferencesTab.RIGHTS)
        if (binding.prefTabSubscriptionsCheck.isChecked) add(Ketch.PreferencesTab.SUBSCRIPTIONS)
    }

    private fun initialTab(): Ketch.PreferencesTab = when (binding.prefInitialTabGroup.checkedRadioButtonId) {
        binding.prefTabConsentRadio.id -> Ketch.PreferencesTab.CONSENTS
        binding.prefTabRightsRadio.id -> Ketch.PreferencesTab.RIGHTS
        binding.prefTabSubscriptionsRadio.id -> Ketch.PreferencesTab.SUBSCRIPTIONS
        else -> Ketch.PreferencesTab.OVERVIEW
    }

    private fun logSharedPreferences() {
        fun emit(message: String) {
            Log.d(TAG, message)
            appendLog(message)
        }

        val tcf = ketch.getTCFTCString()
        val usPrivacy = ketch.getUSPrivacyString()
        val gpp = ketch.getGPPHDRGppString()

        emit("SharedPreferences — IABTCF_TCString: ${tcf ?: "(not set)"}")
        emit("SharedPreferences — IABUSPrivacy_String: ${usPrivacy ?: "(not set)"}")
        emit("SharedPreferences — IABGPP_HDR_GppString: ${gpp ?: "(not set)"}")

        val prefs = getSharedPreferences(
            "${applicationContext.packageName}_preferences",
            Context.MODE_PRIVATE,
        )
        val iabEntries = prefs.all
            .filterKeys { key -> IAB_PREFERENCE_PREFIXES.any { key.startsWith(it) } }
            .toSortedMap()

        if (iabEntries.isEmpty()) {
            emit("SharedPreferences — no IAB-prefixed keys found")
            return
        }

        emit("SharedPreferences — all IAB keys (${iabEntries.size}):")
        iabEntries.forEach { (key, value) ->
            emit("  $key = $value")
        }
    }

    private fun appendLog(message: String) {
        runOnUiThread {
            val current = binding.eventLogText.text.toString()
            val prefix = if (current == "Waiting for events...") "" else "$current\n"
            binding.eventLogText.text = "$prefix$message"
            binding.eventLogScroll.post {
                binding.eventLogScroll.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }
}
