package com.ketch.android.integration.tests

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ketch.android.integration.tests.databinding.ActivitySecondBinding

class SecondActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySecondBinding
    private lateinit var app: IntegrationTestApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySecondBinding.inflate(layoutInflater)
        setContentView(binding.root)

        app = application as IntegrationTestApp

        binding.secondShowConsentButton.setOnClickListener {
            binding.statusText.text = "Show consent called"
            app.ketch.showConsent()
        }

        binding.secondShowPreferencesButton.setOnClickListener {
            binding.statusText.text = "Show preferences called"
            app.ketch.showPreferences()
        }
    }

    fun setTestMode(listener: TestEventListener) {
        app.setTestMode(listener)
    }

    fun clearTestMode() {
        app.clearTestMode()
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

    interface TestEventListener : IntegrationTestApp.TestEventListener
}
