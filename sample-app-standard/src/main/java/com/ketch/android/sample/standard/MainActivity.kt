package com.ketch.android.sample.standard

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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        app = application as SampleApplication
        ketch = app.ketch
        app.logCallback = { message -> appendLog(message) }

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

    override fun onDestroy() {
        if (isFinishing) {
            app.logCallback = null
        }
        super.onDestroy()
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
            Log.d(TAG, "showPreferences() called")
            appendLog("showPreferences() called")
            ketch.showPreferences()
        }

        binding.openSecondActivityButton.setOnClickListener {
            appendLog("Opening SecondActivity")
            startActivity(Intent(this, SecondActivity::class.java))
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
