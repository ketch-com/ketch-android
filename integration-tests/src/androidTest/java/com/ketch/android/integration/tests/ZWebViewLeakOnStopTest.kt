package com.ketch.android.integration.tests

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ketch.android.data.HideExperienceStatus
import com.ketch.android.data.WillShowExperienceType
import leakcanary.LeakAssertions
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Regression test for the retained-WebView Activity leak: a WebView kept alive after
 * hideExperience holds the Activity context that built it, and must be released when its owning
 * Activity leaves the foreground for another Activity — without waiting for onDestroy, which
 * Android does not guarantee. Runs after core suite (Z prefix).
 */
@RunWith(AndroidJUnit4::class)
class ZWebViewLeakOnStopTest : LeakCheckedInstrumentedTest() {

    private lateinit var app: IntegrationTestApp
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext() as IntegrationTestApp
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            app.ketch.dismissDialog()
            app.ketch.setIdentities(mapOf("aaid" to "test-123"))
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
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
        scenario.close()
    }

    private fun showConsentBannerAndWait() {
        val showLatch = CountDownLatch(2) // willShow(Consent) + hasShown
        scenario.onActivity { it.updateIdentitiesWithUniqueValue() }
        app.setTestMode(object : IntegrationTestApp.TestEventListener {
            override fun onWillShowExperience(type: WillShowExperienceType) {
                if (type == WillShowExperienceType.ConsentExperience) showLatch.countDown()
            }

            override fun onHasShownExperience() {
                showLatch.countDown()
            }

            override fun onError(error: String) {
                throw AssertionError("Show consent failed: $error")
            }
        })
        onView(withId(R.id.showConsentButton)).perform(click())
        assertTrue("Consent experience should show within 30s", showLatch.await(30, TimeUnit.SECONDS))
    }

    private fun dismissBannerViaPrimaryButton() {
        val dismissLatch = CountDownLatch(1)
        app.setTestMode(object : IntegrationTestApp.TestEventListener {
            override fun onDismiss(status: HideExperienceStatus) {
                dismissLatch.countDown()
            }
        })
        val clickLatch = CountDownLatch(1)
        scenario.onActivity { activity ->
            activity.clickButtonById("ketch-banner-button-primary") { clicked ->
                if (clicked) clickLatch.countDown()
            }
        }
        assertTrue("Primary button should be clicked", clickLatch.await(15, TimeUnit.SECONDS))
        assertTrue("Dismiss should fire after button click", dismissLatch.await(15, TimeUnit.SECONDS))
    }

    private fun awaitWebViewReleased(timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (!app.hasActiveWebView()) return true
            Thread.sleep(200)
        }
        return !app.hasActiveWebView()
    }

    @Test
    fun idleRetainedWebViewReleasedWhenOwnerStopsForDifferentActivity() {
        showConsentBannerAndWait()
        dismissBannerViaPrimaryButton()

        // hideExperience retains the WebView (holding MainActivity's context) for warm re-show.
        assertTrue("WebView should be retained after hideExperience", app.hasActiveWebView())

        // Navigate to SecondActivity WITHOUT finishing MainActivity: it goes to the back stack —
        // stopped, a different Activity foreground, not destroyed (the leak-trace scenario).
        scenario.onActivity { activity ->
            activity.startActivity(Intent(activity, SecondActivity::class.java))
        }

        assertTrue(
            "Retained WebView must be released when its owning Activity stops for another Activity",
            awaitWebViewReleased(10_000),
        )
        assertFalse("WebView should be released", app.hasActiveWebView())
        assertNull("Loaded signature should be cleared", app.getLoadedSignature())

        // Heap-verify the WebView is actually collectible now. Deliberately does not capture the
        // WebView instance in a local: a named local stays live on this method's stack for the
        // rest of the call, which would itself be a GC root and mask a real release with a false
        // "still reachable" positive. LeakCanary's automatic FragmentWatcher (KetchDialogFragment
        // owns the WebView as a child view) already watches this instance with no test code
        // needed; this assertion just forces the heap dump/analysis at this exact checkpoint.
        LeakAssertions.assertNoLeaks()
    }
}
