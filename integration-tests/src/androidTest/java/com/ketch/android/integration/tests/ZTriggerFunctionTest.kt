package com.ketch.android.integration.tests

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ketch.android.TriggerName
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
 * Verifies [com.ketch.android.Ketch.trigger] (the `onFunction` rule trigger) dispatches on
 * both a warm and a cold WebView, and that invalid function names are rejected before
 * anything is sent to the tag.
 *
 * These tests cover dispatch only, not that a configured experience shows — that requires a
 * backend `onFunction|<functionName>` rule on the `ketch_samples`/`android` test property.
 *
 * Runs after the core suite (Z prefix).
 */
@RunWith(AndroidJUnit4::class)
class ZTriggerFunctionTest {

    private lateinit var app: IntegrationTestApp
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext() as IntegrationTestApp
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            app.ketch.dismissDialog()
            app.ketch.setIdentities(mapOf("aaid" to "test-123"))
            app.ketch.setLanguage(null)
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        app.clearTestMode()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            app.ketch.dismissDialog()
            app.ketch.setIdentities(mapOf("aaid" to "test-123"))
            app.ketch.setLanguage(null)
        }
        scenario.close()
    }

    private fun showConsentBannerAndWait() {
        val showLatch = CountDownLatch(1)
        app.setTestMode(object : IntegrationTestApp.TestEventListener {
            override fun onWillShowExperience(type: WillShowExperienceType) {
                if (type == WillShowExperienceType.ConsentExperience) {
                    showLatch.countDown()
                }
            }
        })
        onView(withId(R.id.showConsentButton)).perform(click())
        assertTrue("Consent banner should show within 30s", showLatch.await(30, TimeUnit.SECONDS))
    }

    @Test
    fun invalidFunctionName_isRejectedWithoutDispatch() {
        var result: Boolean? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            result = app.ketch.trigger(TriggerName.CUSTOM, "not a valid name!")
        }
        assertFalse("trigger() should reject an invalid functionName", result!!)
    }

    @Test
    fun warmWebView_dispatchesTriggerWithoutError() {
        showConsentBannerAndWait()

        val errorLatch = CountDownLatch(1)
        app.setTestMode(object : IntegrationTestApp.TestEventListener {
            override fun onError(error: String) {
                throw AssertionError("trigger() on a warm WebView raised an error: $error")
            }
        })

        var dispatched = false
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            dispatched = app.ketch.trigger(TriggerName.CUSTOM, "integrationTestFunction", mapOf("source" to "androidTest"))
        }
        assertTrue("trigger() should dispatch on a warm WebView", dispatched)

        // An unmatched custom function is a silent no-op in ketch-tag's rule engine, not an error.
        assertFalse("Unexpected onError after warm trigger()", errorLatch.await(3, TimeUnit.SECONDS))
        assertTrue("WebView should remain intact after trigger()", app.hasActiveWebView())
    }

    @Test
    fun coldWebView_bootsAndDispatchesDeferredTrigger() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            app.ketch.dismissDialog()
        }
        Thread.sleep(500)
        assertFalse("Precondition: no active WebView before cold trigger", app.hasActiveWebView())

        val configLatch = CountDownLatch(1)
        app.setTestMode(object : IntegrationTestApp.TestEventListener {
            override fun onConfigUpdated() {
                configLatch.countDown()
            }

            override fun onError(error: String) {
                throw AssertionError("trigger() cold boot raised an error: $error")
            }
        })

        var dispatched = false
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            dispatched = app.ketch.trigger(TriggerName.CUSTOM, "integrationTestFunction")
        }
        assertTrue("trigger() should dispatch (cold boot) even with no warm WebView", dispatched)
        assertTrue("Cold-booted tag should finish loading within 30s", configLatch.await(30, TimeUnit.SECONDS))
    }

    @Test
    fun warmTrigger_clearsDeferredColdTrigger() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            app.ketch.dismissDialog()
        }
        Thread.sleep(500)
        assertFalse("Precondition: no active WebView before cold trigger", app.hasActiveWebView())

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            assertTrue("Cold trigger should start WebView load", app.ketch.trigger(TriggerName.CUSTOM, "integrationTestFunction"))
        }
        assertTrue("Cold trigger should be deferred while tag loads", app.hasPendingTrigger())

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            assertTrue("Warm trigger should dispatch on the active WebView", app.ketch.trigger(TriggerName.CUSTOM, "integrationTestFunction"))
        }
        assertFalse("Warm trigger should clear the deferred cold trigger", app.hasPendingTrigger())
    }
}
