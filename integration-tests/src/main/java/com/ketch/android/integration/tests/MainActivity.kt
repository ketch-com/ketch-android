package com.ketch.android.integration.tests

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ketch.android.Ketch
import com.ketch.android.data.Consent
import com.ketch.android.data.HideExperienceStatus
import com.ketch.android.data.WillShowExperienceType
import com.ketch.android.integration.tests.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var app: IntegrationTestApp
    private lateinit var ketch: Ketch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        app = application as IntegrationTestApp
        ketch = app.ketch

        initializeTextViews()
        setupClickListeners()

        binding.statusText.text = "Ketch initialized"
    }

    private fun initializeTextViews() {
        binding.environmentText.text = "Environment: Not set"
        binding.consentText.text = "Consent: Not set"
        binding.usPrivacyText.text = "US Privacy: Not set"
        binding.tcfText.text = "TCF: Not set"
        binding.gppText.text = "GPP: Not set"
    }

    private fun setupClickListeners() {
        binding.loadButton.setOnClickListener {
            ketch.load()
            binding.statusText.text = "Load called"
        }

        binding.showConsentButton.setOnClickListener {
            ketch.showConsent()
            binding.statusText.text = "Show consent called"
        }

        binding.showPreferencesButton.setOnClickListener {
            ketch.showPreferences()
            binding.statusText.text = "Show preferences called"
        }

        binding.openSecondActivityButton.setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
        }

        binding.setLanguageButton.setOnClickListener {
            ketch.setLanguage("EN")
            binding.statusText.text = "Language set to EN"
        }

        binding.setJurisdictionButton.setOnClickListener {
            ketch.setJurisdiction("US")
            binding.statusText.text = "Jurisdiction set to US"
        }

        binding.setRegionButton.setOnClickListener {
            ketch.setRegion("California")
            binding.statusText.text = "Region set to California"
        }
    }

    fun setTestMode(listener: TestEventListener) {
        app.setTestMode(object : TestEventListener {
            override fun onConsentUpdated(consent: String) {
                runOnUiThread { binding.consentText.text = "Consent: $consent" }
                listener.onConsentUpdated(consent)
            }

            override fun onConsentUpdated(consent: Consent) {
                listener.onConsentUpdated(consent)
            }

            override fun onEnvironmentUpdated(environment: String) {
                runOnUiThread { binding.environmentText.text = "Environment: $environment" }
                listener.onEnvironmentUpdated(environment)
            }

            override fun onConfigUpdated() {
                runOnUiThread { binding.statusText.text = "Config updated" }
                listener.onConfigUpdated()
            }

            override fun onShow() {
                runOnUiThread { binding.statusText.text = "Dialog shown" }
                listener.onShow()
            }

            override fun onDismiss(status: HideExperienceStatus) {
                runOnUiThread { binding.statusText.text = "Dialog dismissed: $status" }
                listener.onDismiss(status)
            }

            override fun onError(error: String) {
                runOnUiThread { binding.statusText.text = "Error: $error" }
                listener.onError(error)
            }

            override fun onWillShowExperience(type: WillShowExperienceType) {
                runOnUiThread { binding.statusText.text = "Will show experience: $type" }
                listener.onWillShowExperience(type)
            }

            override fun onHasShownExperience() {
                runOnUiThread { binding.statusText.text = "Experience has been shown" }
                listener.onHasShownExperience()
            }

            override fun onWebViewContentValidated(elementId: String, exists: Boolean) {
                listener.onWebViewContentValidated(elementId, exists)
            }
        })
    }

    fun clearTestMode() {
        app.clearTestMode()
    }

    fun validateWebViewContent(expectedInnerElementId: String, callback: (Boolean) -> Unit) {
        app.validateWebViewContent(expectedInnerElementId, callback)
    }

    fun checkForConsentBanner(callback: (Boolean) -> Unit) {
        app.checkForConsentBanner(callback)
    }

    fun checkForPreferencesCenter(callback: (Boolean) -> Unit) {
        app.checkForPreferencesCenter(callback)
    }

    fun updateIdentitiesWithUniqueValue() {
        app.updateIdentitiesWithUniqueValue()
    }

    fun clickButtonById(buttonId: String, callback: (Boolean) -> Unit) {
        app.clickButtonById(buttonId, callback)
    }

    interface TestEventListener : IntegrationTestApp.TestEventListener
}
