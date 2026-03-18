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
import com.ketch.android.data.Consent
import com.ketch.android.data.HideExperienceStatus
import com.ketch.android.data.KetchConfig
import com.ketch.android.data.WillShowExperienceType
import com.ketch.android.sample.standard.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var ketch: Ketch

    companion object {
        private const val TAG = "KetchSample"
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

    private fun setupClickListeners() {
        binding.showConsentButton.setOnClickListener {
            Log.d(TAG, "showConsent() called")
            appendLog("showConsent() called")
            ketch.showConsent()
        }

        binding.showPreferencesButton.setOnClickListener {
            Log.d(TAG, "showPreferences() called")
            appendLog("showPreferences() called")
            ketch.showPreferences()
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
