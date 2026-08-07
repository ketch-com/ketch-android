package com.ketch.android.integration.tests

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Calls load() and finishes immediately, before the boot completes, to reproduce a
 * transient Activity's destroy silently killing an in-flight load() started elsewhere.
 */
class TransientLoadActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as IntegrationTestApp
        app.ketch.load()

        startActivity(Intent(this, SecondActivity::class.java))
        finish()
    }
}
