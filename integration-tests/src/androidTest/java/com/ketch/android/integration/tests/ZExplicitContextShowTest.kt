package com.ketch.android.integration.tests

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ketch.android.Ketch
import com.ketch.android.KetchSdk
import com.ketch.android.data.Consent
import com.ketch.android.data.HideExperienceStatus
import com.ketch.android.data.KetchConfig
import com.ketch.android.data.WillShowExperienceType
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Covers KD-17712 §D: a [Ketch] instance created from a non-Activity context (e.g. a ViewModel
 * holding `applicationContext`, as Kroger does) has neither a seeded Activity nor a tracked one
 * until an Activity resumes — `registerActivityLifecycleCallbacks` does not replay past events.
 * A no-arg `show*` called in that window fails with "no active Activity" (§C's gap). Passing the
 * host explicitly via the new `context` param resolves it deterministically instead.
 *
 * Each test builds its own [Ketch] instance (rather than reusing the shared [IntegrationTestApp]
 * one) so its [com.ketch.android.KetchLifecycleTracker] is guaranteed fresh, independent of
 * whatever Activities other tests left resumed.
 */
@RunWith(AndroidJUnit4::class)
class ZExplicitContextShowTest {

    private lateinit var ketch: Ketch

    @Before
    fun setUp() {
        repeat(3) {
            try {
                pressBackUnconditionally()
            } catch (_: Exception) {
            }
        }
    }

    @After
    fun tearDown() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            ketch.dismissDialog()
        }
        repeat(3) {
            try {
                pressBackUnconditionally()
            } catch (_: Exception) {
            }
        }
    }

    private fun newAppContextKetch(listener: Ketch.Listener): Ketch =
        KetchSdk.create(
            context = ApplicationProvider.getApplicationContext(),
            organization = IntegrationTestApp.ORG_CODE,
            property = IntegrationTestApp.PROPERTY,
            environment = IntegrationTestApp.ENVIRONMENT,
            listener = listener,
            ketchUrl = null,
            logLevel = Ketch.LogLevel.DEBUG,
        ).also { it.setIdentities(mapOf("aaid" to "test-123")) }

    @Test
    fun noArgShowConsent_withNoResumedActivity_reportsNoHostError() {
        val outcomeLatch = CountDownLatch(1)
        var errorMessage: String? = null
        var dialogShown = false

        ketch = newAppContextKetch(object : NoOpListener() {
            override fun onError(errMsg: String?) {
                errorMessage = errMsg
                outcomeLatch.countDown()
            }

            override fun onShow() {
                dialogShown = true
                outcomeLatch.countDown()
            }
        })

        // No ActivityScenario is launched here: seedActivity is null (an applicationContext
        // isn't a FragmentActivity) and this tracker has never observed a resumed Activity.
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            ketch.showConsent()
        }

        assertTrue("Expected onError or onShow within 30s", outcomeLatch.await(30, TimeUnit.SECONDS))
        assertFalse("Dialog should not show with no resolvable host", dialogShown)
        assertTrue(
            "Expected the no-host error message, got: $errorMessage",
            errorMessage?.contains("No active Activity") == true
        )
    }

    @Test
    fun showConsentWithExplicitContext_resolvesHost_withNoTrackedActivity() {
        val outcomeLatch = CountDownLatch(1)
        var errorMessage: String? = null
        var dialogShown = false

        ketch = newAppContextKetch(object : NoOpListener() {
            override fun onShow() {
                dialogShown = true
                outcomeLatch.countDown()
            }

            override fun onError(errMsg: String?) {
                errorMessage = errMsg
                outcomeLatch.countDown()
            }
        })

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            // Same tracker state as the test above (nothing resumed yet on THIS Ketch's
            // tracker) — the only difference is passing the host explicitly.
            scenario.onActivity { activity ->
                ketch.showConsent(context = activity)
            }

            assertTrue("Expected onShow or onError within 30s", outcomeLatch.await(30, TimeUnit.SECONDS))
            assertTrue("Explicit context should resolve the host; got error: $errorMessage", dialogShown)
        } finally {
            scenario.close()
        }
    }

    private abstract class NoOpListener : Ketch.Listener {
        override fun onShow() {}
        override fun onDismiss(status: HideExperienceStatus) {}
        override fun onConfigUpdated(config: KetchConfig?) {}
        override fun onEnvironmentUpdated(environment: String?) {}
        override fun onRegionInfoUpdated(regionInfo: String?) {}
        override fun onJurisdictionUpdated(jurisdiction: String?) {}
        override fun onIdentitiesUpdated(identities: String?) {}
        override fun onConsentUpdated(consent: Consent) {}
        override fun onError(errMsg: String?) {}
        override fun onUSPrivacyUpdated(values: Map<String, Any?>) {}
        override fun onTCFUpdated(values: Map<String, Any?>) {}
        override fun onGPPUpdated(values: Map<String, Any?>) {}
        override fun onWillShowExperience(type: WillShowExperienceType) {}
        override fun onHasShownExperience() {}
    }
}
