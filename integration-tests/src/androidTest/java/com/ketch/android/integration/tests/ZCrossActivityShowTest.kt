package com.ketch.android.integration.tests

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ketch.android.data.HideExperienceStatus
import com.ketch.android.data.WillShowExperienceType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ZCrossActivityShowTest : LeakCheckedInstrumentedTest() {

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
        try {
            pressBackUnconditionally()
        } catch (_: Exception) {
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            app.ketch.dismissDialog()
        }
        try {
            pressBackUnconditionally()
        } catch (_: Exception) {
        }
        try {
            pressBackUnconditionally()
        } catch (_: Exception) {
        }
        mainScenario.close()
    }

    @Test
    fun showConsentFromSecondActivityAfterInitInMainActivity() {
        var willShowEventReceived = false
        var hasShownEventReceived = false
        var receivedExperienceType: WillShowExperienceType? = null
        var webViewValidationReceived = false
        var webViewElementExists = false
        var errorMessage: String? = null
        val loadLatch = CountDownLatch(1)
        val showLatch = CountDownLatch(3)

        app.setTestMode(object : IntegrationTestApp.TestEventListener {
            override fun onConsentUpdated(consent: String) {
                loadLatch.countDown()
            }

            override fun onDismiss(status: HideExperienceStatus) {
                if (status == HideExperienceStatus.WillNotShow) {
                    loadLatch.countDown()
                }
            }

            override fun onWillShowExperience(type: WillShowExperienceType) {
                willShowEventReceived = true
                receivedExperienceType = type
                showLatch.countDown()
            }

            override fun onHasShownExperience() {
                hasShownEventReceived = true
                showLatch.countDown()

                app.checkForConsentBanner { exists ->
                    webViewValidationReceived = true
                    webViewElementExists = exists
                    showLatch.countDown()
                }
            }

            override fun onError(error: String) {
                errorMessage = error
                showLatch.countDown()
            }
        })

        onView(withId(R.id.loadButton)).perform(click())
        assertTrue("load() should complete within 30 seconds", loadLatch.await(30, TimeUnit.SECONDS))

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            app.ketch.dismissDialog()
        }

        onView(withId(R.id.openSecondActivityButton)).perform(click())
        onView(withId(R.id.secondShowConsentButton))
            .check(matches(isDisplayed()))
            .perform(click())

        val listenersFired = showLatch.await(30, TimeUnit.SECONDS)

        if (errorMessage != null && !hasShownEventReceived) {
            fail("Show consent from SecondActivity failed: $errorMessage")
        }

        assertTrue(
            "onWillShowExperience, onHasShownExperience, and WebView validation should fire within 30 seconds",
            listenersFired
        )
        assertTrue("onWillShowExperience should have been received", willShowEventReceived)
        assertTrue("onHasShownExperience should have been received", hasShownEventReceived)
        assertEquals(
            WillShowExperienceType.ConsentExperience,
            receivedExperienceType
        )
        assertTrue("WebView validation should have been received", webViewValidationReceived)
        assertTrue("WebView should contain element with id 'ketch-consent-banner'", webViewElementExists)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            app.ketch.dismissDialog()
        }
        repeat(2) {
            try {
                pressBackUnconditionally()
            } catch (_: Exception) {
            }
        }
    }

    @Test
    fun showPreferencesFromSecondActivityAfterInitInMainActivity() {
        var willShowEventReceived = false
        var hasShownEventReceived = false
        var receivedExperienceType: WillShowExperienceType? = null
        var webViewValidationReceived = false
        var webViewElementExists = false
        var errorMessage: String? = null
        val loadLatch = CountDownLatch(1)
        val showLatch = CountDownLatch(3)

        app.setTestMode(object : IntegrationTestApp.TestEventListener {
            override fun onConsentUpdated(consent: String) {
                loadLatch.countDown()
            }

            override fun onDismiss(status: HideExperienceStatus) {
                if (status == HideExperienceStatus.WillNotShow) {
                    loadLatch.countDown()
                }
            }

            override fun onWillShowExperience(type: WillShowExperienceType) {
                willShowEventReceived = true
                receivedExperienceType = type
                showLatch.countDown()
            }

            override fun onHasShownExperience() {
                hasShownEventReceived = true
                showLatch.countDown()

                app.checkForPreferencesCenter { exists ->
                    webViewValidationReceived = true
                    webViewElementExists = exists
                    showLatch.countDown()
                }
            }

            override fun onError(error: String) {
                errorMessage = error
                showLatch.countDown()
            }
        })

        app.ketch.setIdentities(mapOf("aaid" to "test-123"))

        onView(withId(R.id.loadButton)).perform(click())
        assertTrue("load() should complete within 30 seconds", loadLatch.await(30, TimeUnit.SECONDS))

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            app.ketch.dismissDialog()
        }

        onView(withId(R.id.openSecondActivityButton)).perform(click())
        onView(withId(R.id.secondShowPreferencesButton))
            .check(matches(isDisplayed()))
            .perform(click())

        val listenersFired = showLatch.await(30, TimeUnit.SECONDS)

        if (errorMessage != null && !hasShownEventReceived) {
            fail("Show preferences from SecondActivity failed: $errorMessage")
        }

        assertTrue(
            "onWillShowExperience, onHasShownExperience, and WebView validation should fire within 30 seconds",
            listenersFired
        )
        assertTrue("onWillShowExperience should have been received", willShowEventReceived)
        assertTrue("onHasShownExperience should have been received", hasShownEventReceived)
        assertEquals(
            WillShowExperienceType.PreferenceExperience,
            receivedExperienceType
        )
        assertTrue("WebView validation should have been received", webViewValidationReceived)
        assertTrue("WebView should contain element with id 'ketch-preferences'", webViewElementExists)
    }
}
