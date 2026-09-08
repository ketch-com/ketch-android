package com.ketch.android.integration.tests

import android.content.Intent
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Verifies that navigating to a different Activity while an experience is showing
 * automatically dismisses the orphaned experience (no integrator call required) and
 * leaves the SDK able to show experiences from the new Activity.
 */
@RunWith(AndroidJUnit4::class)
class ZAutoDismissOnNavigationTest : LeakCheckedInstrumentedTest() {

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

    // Poll the WebView for the expected element, since a freshly re-loaded experience may take
    // longer than a single fixed delay to render its inner DOM.
    private fun awaitPreferencesContent(timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val cdl = CountDownLatch(1)
            var exists = false
            app.checkForPreferencesCenter {
                exists = it
                cdl.countDown()
            }
            cdl.await(8, TimeUnit.SECONDS)
            if (exists) return true
            Thread.sleep(1000)
        }
        return false
    }

    @Test
    fun navigatingWhileBannerShowing_autoDismisses_andNewActivityCanShow() {
        val bannerShownLatch = CountDownLatch(1)
        val autoDismissLatch = CountDownLatch(1)
        val secondShowLatch = CountDownLatch(2) // willShow + hasShown
        var dismissStatus: HideExperienceStatus? = null
        var secondShowType: WillShowExperienceType? = null
        var errorMessage: String? = null

        val phase = AtomicReference("init")

        app.setTestMode(object : IntegrationTestApp.TestEventListener {
            override fun onHasShownExperience() {
                when (phase.get()) {
                    "main" -> bannerShownLatch.countDown()
                    "second" -> secondShowLatch.countDown()
                }
            }

            override fun onWillShowExperience(type: WillShowExperienceType) {
                if (phase.get() == "second") {
                    secondShowType = type
                    secondShowLatch.countDown()
                }
            }

            override fun onDismiss(status: HideExperienceStatus) {
                if (phase.get() == "navigating") {
                    dismissStatus = status
                    autoDismissLatch.countDown()
                }
            }

            override fun onError(error: String) {
                errorMessage = error
            }
        })

        // Phase 1: force the consent banner to show in MainActivity.
        phase.set("main")
        mainScenario.onActivity { it.updateIdentitiesWithUniqueValue() }
        onView(withId(R.id.loadButton)).perform(click())
        assertTrue("Banner should show in MainActivity within 30s", bannerShownLatch.await(30, TimeUnit.SECONDS))

        // Phase 2: navigate to SecondActivity WHILE the banner is showing.
        // The banner is a full-window dialog over MainActivity, so navigation here is code-driven
        // (as it would be for a deep link / notification). Expect automatic dismissal.
        phase.set("navigating")
        mainScenario.onActivity { activity ->
            activity.startActivity(Intent(activity, SecondActivity::class.java))
        }
        assertTrue(
            "Navigating away while showing should auto-dismiss the experience (no manual call)",
            autoDismissLatch.await(10, TimeUnit.SECONDS)
        )
        assertEquals("Auto-dismiss should report status ActivityChanged", HideExperienceStatus.ActivityChanged, dismissStatus)

        // Phase 3: the SDK must no longer be stuck — SecondActivity can show an experience.
        phase.set("second")
        onView(withId(R.id.secondShowPreferencesButton))
            .check(matches(isDisplayed()))
            .perform(click())

        val secondShown = secondShowLatch.await(30, TimeUnit.SECONDS)
        if (errorMessage != null && secondShowType == null) {
            org.junit.Assert.fail("Show from SecondActivity after auto-dismiss failed: $errorMessage")
        }
        assertTrue(
            "SecondActivity should be able to show after auto-dismiss (will/has-shown)",
            secondShown
        )
        assertEquals(WillShowExperienceType.PreferenceExperience, secondShowType)
        assertTrue(
            "WebView in SecondActivity should contain element with id 'ketch-preferences'",
            awaitPreferencesContent(15_000)
        )
    }
}
