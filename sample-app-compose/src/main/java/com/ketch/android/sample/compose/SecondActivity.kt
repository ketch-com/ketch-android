package com.ketch.android.sample.compose

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateListOf
import com.ketch.android.sample.compose.ui.theme.KetchTheme

class SecondActivity : AppCompatActivity() {

    private val logEntries = mutableStateListOf<String>()
    private lateinit var app: ComposeSampleApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        app = application as ComposeSampleApplication
        app.logCallback = { message ->
            runOnUiThread { logEntries.add(message) }
        }

        setContent {
            KetchTheme(darkTheme = false) {
                SecondActivityScreen(
                    logEntries = logEntries,
                    onShowConsent = {
                        logEntries.add("showConsent() called from SecondActivity")
                        app.ketch.showConsent()
                    },
                    onShowPreferences = {
                        logEntries.add("showPreferences() called from SecondActivity")
                        app.ketch.showPreferences()
                    },
                )
            }
        }

        logEntries.add("SecondActivity started (shared Ketch instance)")
    }

    override fun onDestroy() {
        if (isFinishing) {
            app.logCallback = null
        }
        super.onDestroy()
    }
}
