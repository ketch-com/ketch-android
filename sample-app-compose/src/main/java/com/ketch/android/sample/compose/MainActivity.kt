package com.ketch.android.sample.compose

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateListOf
import com.ketch.android.Ketch

class MainActivity : AppCompatActivity() {

    private lateinit var ketch: Ketch
    private lateinit var app: ComposeSampleApplication
    private val logEntries = mutableStateListOf<String>()

    companion object {
        private const val TAG = "KetchCompose"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        app = application as ComposeSampleApplication
        ketch = app.ketch
        app.logCallback = { message ->
            runOnUiThread { logEntries.add(message) }
        }

        initializeKetch()

        setContent {
            KetchSampleApp(
                logEntries = logEntries,
                onShowConsent = {
                    Log.d(TAG, "showConsent() called")
                    logEntries.add("showConsent() called")
                    ketch.showConsent()
                },
                onShowPreferences = {
                    Log.d(TAG, "showPreferences() called")
                    logEntries.add("showPreferences() called")
                    ketch.showPreferences()
                },
                onOpenSecondActivity = {
                    logEntries.add("Opening SecondActivity")
                    startActivity(Intent(this, SecondActivity::class.java))
                },
            )
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            app.logCallback = null
        }
        super.onDestroy()
    }

    private fun initializeKetch() {
        logEntries.add("Ketch initialized in Application")
        ketch.load()
        logEntries.add("load() called from MainActivity")
    }
}
