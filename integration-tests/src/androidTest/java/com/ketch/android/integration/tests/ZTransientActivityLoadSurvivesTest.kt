package com.ketch.android.integration.tests

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * A transient Activity calls load() and finishes before the boot completes, while a
 * different (still-alive) Activity is the one that actually owns the in-flight WebView.
 * Verifies the boot survives the transient Activity's destroy instead of being killed.
 */
@RunWith(AndroidJUnit4::class)
class ZTransientActivityLoadSurvivesTest : LeakCheckedInstrumentedTest() {

    private lateinit var app: IntegrationTestApp
    private lateinit var mainScenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext() as IntegrationTestApp
        repeat(3) {
            try {
                pressBackUnconditionally()
            } catch (_: Exception) {
            }
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            app.ketch.dismissDialog()
            app.ketch.setIdentities(mapOf("aaid" to "test-123"))
        }
        mainScenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        app.clearTestMode()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            app.ketch.dismissDialog()
            app.ketch.setIdentities(mapOf("aaid" to "test-123"))
        }
        repeat(3) {
            try {
                pressBackUnconditionally()
            } catch (_: Exception) {
            }
        }
        mainScenario.close()
    }

    @Test
    fun transientActivityFinishing_doesNotKillLoadStartedWhileMainActivityWasCurrent() {
        val configUpdatedLatch = CountDownLatch(1)
        app.setTestMode(object : IntegrationTestApp.TestEventListener {
            override fun onConfigUpdated() {
                configUpdatedLatch.countDown()
            }
        })

        mainScenario.onActivity { it.updateIdentitiesWithUniqueValue() }
        mainScenario.onActivity { activity ->
            activity.startActivity(Intent(activity, TransientLoadActivity::class.java))
        }

        assertTrue(
            "onConfigUpdated should fire within 30s even though TransientLoadActivity " +
                "finished before the boot completed",
            configUpdatedLatch.await(30, TimeUnit.SECONDS)
        )
    }
}
