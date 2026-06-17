package com.ketch.android.sample.standard

import android.os.Bundle
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import com.ketch.android.sample.standard.databinding.ActivitySecondBinding

class SecondActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySecondBinding
    private lateinit var app: SampleApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySecondBinding.inflate(layoutInflater)
        setContentView(binding.root)

        app = application as SampleApplication
        app.logCallback = { message -> appendLog(message) }

        binding.showConsentButton.setOnClickListener {
            appendLog("showConsent() called from SecondActivity")
            app.ketch.showConsent()
        }

        binding.showPreferencesButton.setOnClickListener {
            appendLog("showPreferences() called from SecondActivity")
            app.ketch.showPreferences()
        }

        appendLog("SecondActivity started (shared Ketch instance)")
    }

    override fun onDestroy() {
        if (isFinishing) {
            app.logCallback = null
        }
        super.onDestroy()
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
