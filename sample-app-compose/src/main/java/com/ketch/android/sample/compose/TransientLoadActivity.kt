package com.ketch.android.sample.compose

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

/**
 * Calls load() and finishes immediately, before the boot completes, to reproduce a
 * transient Activity's destroy silently killing an in-flight load() started elsewhere.
 */
class TransientLoadActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "KetchCompose"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as ComposeSampleApplication
        Log.d(TAG, "TransientLoadActivity: load() called, finishing immediately")
        app.ketch.load()

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
