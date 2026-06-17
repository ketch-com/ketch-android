package com.ketch.android.integration.tests

import android.content.pm.ActivityInfo
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ketch.android.data.HideExperienceStatus
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Verifies that benign lifecycle events (rotation, backgrounding) do NOT auto-dismiss
 * an active experience or kill the WebView.
 */
@RunWith(AndroidJUnit4::class)
class ZBenignLifecycleNoDismissTest {

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

    private fun showConsentBannerInMainActivity(bannerShownLatch: CountDownLatch) {
        mainScenario.onActivity { it.updateIdentitiesWithUniqueValue() }
        onView(withId(R.id.loadButton)).perform(click())
        assertTrue("Banner should show in MainActivity within 30s", bannerShownLatch.await(30, TimeUnit.SECONDS))
    }

    @Test
    fun rotationWhileShowing_doesNotDismissOrKillWebView() {
        val bannerShownLatch = CountDownLatch(1)
        var activityChangedDismissFired = false
        app.setTestMode(object : IntegrationTestApp.TestEventListener {
            override fun onHasShownExperience() {
                bannerShownLatch.countDown()
            }

            override fun onDismiss(status: HideExperienceStatus) {
                if (status == HideExperienceStatus.ActivityChanged) {
                    activityChangedDismissFired = true
                }
            }
        })

        showConsentBannerInMainActivity(bannerShownLatch)

        mainScenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        Thread.sleep(2000)
        mainScenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        Thread.sleep(2000)

        assertFalse(
            "Rotation should not fire onDismiss(ActivityChanged)",
            activityChangedDismissFired
        )
        assertTrue(
            "isShowingExperience should remain true after rotation",
            app.isShowingExperience()
        )
        assertTrue(
            "activeWebView should remain alive after rotation",
            app.hasActiveWebView()
        )
    }

    @Test
    fun backgroundingWhileShowing_doesNotDismiss() {
        val bannerShownLatch = CountDownLatch(1)
        var activityChangedDismissFired = false
        app.setTestMode(object : IntegrationTestApp.TestEventListener {
            override fun onHasShownExperience() {
                bannerShownLatch.countDown()
            }

            override fun onDismiss(status: HideExperienceStatus) {
                if (status == HideExperienceStatus.ActivityChanged) {
                    activityChangedDismissFired = true
                }
            }
        })

        showConsentBannerInMainActivity(bannerShownLatch)

        mainScenario.moveToState(Lifecycle.State.CREATED)
        Thread.sleep(1000)

        assertFalse(
            "Backgrounding should not fire onDismiss(ActivityChanged)",
            activityChangedDismissFired
        )
        assertTrue(
            "isShowingExperience should remain true while backgrounded",
            app.isShowingExperience()
        )
        assertTrue(
            "activeWebView should remain alive while backgrounded",
            app.hasActiveWebView()
        )

        mainScenario.moveToState(Lifecycle.State.RESUMED)
        Thread.sleep(2500)

        assertFalse(
            "Returning from background should not fire onDismiss(ActivityChanged)",
            activityChangedDismissFired
        )
        assertTrue(
            "isShowingExperience should remain true after returning from background",
            app.isShowingExperience()
        )
        assertTrue(
            "activeWebView should remain alive after returning from background",
            app.hasActiveWebView()
        )

        val bannerContentLatch = CountDownLatch(1)
        var bannerContentExists = false
        app.checkForConsentBanner { exists ->
            bannerContentExists = exists
            bannerContentLatch.countDown()
        }
        bannerContentLatch.await(10, TimeUnit.SECONDS)
        assertTrue(
            "Consent banner content should still be present after returning from background",
            bannerContentExists
        )
    }
}
