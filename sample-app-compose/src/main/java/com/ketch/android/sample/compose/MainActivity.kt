package com.ketch.android.sample.compose

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateListOf
import com.ketch.android.Ketch
import com.ketch.android.TriggerName

class MainActivity : AppCompatActivity() {

    private lateinit var ketch: Ketch
    private lateinit var app: ComposeSampleApplication
    private val logEntries = mutableStateListOf<String>()

    companion object {
        private const val TAG = "KetchCompose"
        private val IAB_PREFERENCE_PREFIXES = listOf("IABTCF", "IABGPP", "IABUS")
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
                onLoadExperience = {
                    Log.d(TAG, "load() called")
                    logEntries.add("load() called")
                    ketch.load()
                },
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
                onOpenTransientLoadActivity = {
                    logEntries.add("Opening TransientLoadActivity")
                    startActivity(Intent(this, TransientLoadActivity::class.java))
                },
                onLogSharedPreferences = { logSharedPreferences() },
                onTriggerFunction = {
                    Log.d(TAG, "trigger('demoFunction') called")
                    logEntries.add("trigger('demoFunction') called")
                    ketch.trigger(TriggerName.CUSTOM, "demoFunction")
                },
                onGetJurisdiction = {
                    Log.d(TAG, "getJurisdiction() called")
                    logEntries.add("getJurisdiction() called")
                    ketch.getJurisdiction { result ->
                        result.onSuccess { jurisdiction ->
                            val message = "Jurisdiction: ${jurisdiction ?: "?"}"
                            Log.d(TAG, message)
                            logEntries.add(message)
                        }.onFailure { error ->
                            Log.e(TAG, "getJurisdiction() failed", error)
                            logEntries.add("Headless error: ${error.message}")
                        }
                    }
                },
                onGetRegion = {
                    Log.d(TAG, "getRegion() called")
                    logEntries.add("getRegion() called")
                    ketch.getRegion { result ->
                        result.onSuccess { region ->
                            val message = "Region: ${region ?: "?"}"
                            Log.d(TAG, message)
                            logEntries.add(message)
                        }.onFailure { error ->
                            Log.e(TAG, "getRegion() failed", error)
                            logEntries.add("Headless error: ${error.message}")
                        }
                    }
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
    }

    private fun logSharedPreferences() {
        fun emit(message: String) {
            Log.d(TAG, message)
            runOnUiThread { logEntries.add(message) }
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
}
