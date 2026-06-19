package com.ketch.android.integration.tests

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ketch.android.data.HideExperienceStatus
import com.ketch.android.data.WillShowExperienceType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Validates WebView retention on hideExperience, warm re-show without reload,
 * cache invalidation, and teardown paths. Runs after core suite (Z prefix).
 */
@RunWith(AndroidJUnit4::class)
class ZWebViewRetentionTest {

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

    private fun showConsentBannerAndWait(): Int {
        val showLatch = CountDownLatch(2)
        val pageLoadCountBeforeShow = AtomicReference(0)

        scenario.onActivity { activity ->
            activity.updateIdentitiesWithUniqueValue()
        }
        app.setTestMode(object : IntegrationTestApp.TestEventListener {
            override fun onWillShowExperience(type: WillShowExperienceType) {
                if (type == WillShowExperienceType.ConsentExperience) {
                    showLatch.countDown()
                }
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

        val initialLoadCount = app.getPageLoadCount()
        pageLoadCountBeforeShow.set(initialLoadCount)
        assertTrue("Initial page load should occur", initialLoadCount >= 1)
        return initialLoadCount
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

    @Test
    fun hideExperienceRetainsWebViewForWarmReshow() {
        showConsentBannerAndWait()
        val webViewHashBeforeHide = app.getActiveWebViewIdentityHash()
        assertNotNull("WebView should exist while showing", webViewHashBeforeHide)

        dismissBannerViaPrimaryButton()

        assertTrue("WebView should be retained after hideExperience", app.hasActiveWebView())
        assertNotNull("Loaded signature should be retained", app.getLoadedSignature())
        assertEquals(
            "Same WebView instance should be retained",
            webViewHashBeforeHide,
            app.getActiveWebViewIdentityHash(),
        )
        assertFalse("Experience should not be showing after hide", app.isShowingExperience())

        val reshowLatch = CountDownLatch(2)
        app.setTestMode(object : IntegrationTestApp.TestEventListener {
            override fun onWillShowExperience(type: WillShowExperienceType) {
                if (type == WillShowExperienceType.ConsentExperience) {
                    reshowLatch.countDown()
                }
            }

            override fun onHasShownExperience() {
                reshowLatch.countDown()
            }
        })

        onView(withId(R.id.showConsentButton)).perform(click())
        assertTrue("Warm re-show should complete within 25s", reshowLatch.await(25, TimeUnit.SECONDS))
        assertEquals(
            "Warm re-show should reuse the same WebView instance",
            webViewHashBeforeHide,
            app.getActiveWebViewIdentityHash(),
        )
    }

    @Test
    fun warmReshowDoesNotReloadPage() {
        val initialLoadCount = showConsentBannerAndWait()
        dismissBannerViaPrimaryButton()

        assertTrue("WebView should be retained", app.hasActiveWebView())

        val reshowLatch = CountDownLatch(1)
        app.setTestMode(object : IntegrationTestApp.TestEventListener {
            override fun onHasShownExperience() {
                reshowLatch.countDown()
            }
        })

        onView(withId(R.id.showConsentButton)).perform(click())
        assertTrue("Warm re-show should complete within 25s", reshowLatch.await(25, TimeUnit.SECONDS))
        assertEquals(
            "Warm re-show should not trigger another page load",
            initialLoadCount,
            app.getPageLoadCount(),
        )
    }

    @Test
    fun dismissDialogDestroysWebView() {
        showConsentBannerAndWait()
        assertTrue("WebView should exist while showing", app.hasActiveWebView())

        val dismissLatch = CountDownLatch(1)
        app.setTestMode(object : IntegrationTestApp.TestEventListener {
            override fun onDismiss(status: HideExperienceStatus) {
                dismissLatch.countDown()
            }
        })

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            app.ketch.dismissDialog()
        }
        assertTrue("dismissDialog should notify listener", dismissLatch.await(10, TimeUnit.SECONDS))
        assertFalse("WebView should be destroyed after dismissDialog", app.hasActiveWebView())
        assertNull("Loaded signature should be cleared", app.getLoadedSignature())
    }

    @Test
    fun cacheInvalidationOnLanguageChangeReloadsWebView() {
        showConsentBannerAndWait()
        val webViewHashBeforeInvalidation = app.getActiveWebViewIdentityHash()
        dismissBannerViaPrimaryButton()
        assertTrue("WebView retained after hide", app.hasActiveWebView())

        onView(withId(R.id.setLanguageButton)).perform(click())

        val reshowLatch = CountDownLatch(1)
        app.setTestMode(object : IntegrationTestApp.TestEventListener {
            override fun onHasShownExperience() {
                reshowLatch.countDown()
            }
        })

        onView(withId(R.id.showConsentButton)).perform(click())
        assertTrue("Re-show after language change should complete within 30s", reshowLatch.await(30, TimeUnit.SECONDS))
        assertTrue("WebView should exist after cold reload", app.hasActiveWebView())
        assertTrue(
            "Language change should create a new WebView instance",
            app.getActiveWebViewIdentityHash() != webViewHashBeforeInvalidation,
        )
    }

    @Test
    fun warmReshowPreferencesWorks() {
        val showLatch = CountDownLatch(2)
        app.setTestMode(object : IntegrationTestApp.TestEventListener {
            override fun onWillShowExperience(type: WillShowExperienceType) {
                if (type == WillShowExperienceType.PreferenceExperience) {
                    showLatch.countDown()
                }
            }

            override fun onHasShownExperience() {
                showLatch.countDown()
            }
        })

        onView(withId(R.id.showPreferencesButton)).perform(click())
        assertTrue("Preferences should show within 30s", showLatch.await(30, TimeUnit.SECONDS))

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            app.ketch.dismissDialog()
        }
        Thread.sleep(500)

        // Cold start again for preferences since dismissDialog destroys
        val secondShowLatch = CountDownLatch(2)
        app.setTestMode(object : IntegrationTestApp.TestEventListener {
            override fun onWillShowExperience(type: WillShowExperienceType) {
                if (type == WillShowExperienceType.PreferenceExperience) {
                    secondShowLatch.countDown()
                }
            }

            override fun onHasShownExperience() {
                secondShowLatch.countDown()
            }
        })

        onView(withId(R.id.showPreferencesButton)).perform(click())
        assertTrue("Second preferences show should complete within 30s", secondShowLatch.await(30, TimeUnit.SECONDS))
        assertNotNull("WebView should exist after second show", app.getActiveWebViewIdentityHash())
    }

    @Test
    fun tapOutsideBridgeRemoved() {
        showConsentBannerAndWait()

        val bridgeCheckLatch = CountDownLatch(1)
        val tapOutsideExists = AtomicReference(false)
        scenario.onActivity { activity ->
            val ketchClass = app.ketch.javaClass
            val activeWebViewField = ketchClass.getDeclaredField("activeWebView")
            activeWebViewField.isAccessible = true
            val webView = activeWebViewField.get(app.ketch) as android.webkit.WebView
            webView.evaluateJavascript("'tapOutside' in window.androidListener") { result ->
                tapOutsideExists.set(result == "true")
                bridgeCheckLatch.countDown()
            }
        }
        assertTrue("Bridge check should complete", bridgeCheckLatch.await(10, TimeUnit.SECONDS))
        assertFalse("tapOutside bridge method should be removed", tapOutsideExists.get())
    }

    @Test
    fun postHideEventsStillDeliveredWhenRetained() {
        showConsentBannerAndWait()
        dismissBannerViaPrimaryButton()
        assertTrue("WebView should be retained for trailing events", app.hasActiveWebView())

        val consentLatch = CountDownLatch(1)
        app.setTestMode(object : IntegrationTestApp.TestEventListener {
            override fun onConsentUpdated(consent: com.ketch.android.data.Consent) {
                consentLatch.countDown()
            }
        })

        val jsLatch = CountDownLatch(1)
        scenario.onActivity {
            val ketchClass = app.ketch.javaClass
            val activeWebViewField = ketchClass.getDeclaredField("activeWebView")
            activeWebViewField.isAccessible = true
            val webView = activeWebViewField.get(app.ketch) as android.webkit.WebView
            webView.evaluateJavascript(
                "window.androidListener.consent('{\"purposes\":{\"analytics\":false},\"vendors\":[]}')"
            ) {
                jsLatch.countDown()
            }
        }
        assertTrue("JS bridge invoke should complete", jsLatch.await(10, TimeUnit.SECONDS))
        assertTrue(
            "Events should still be delivered through the retained WebView bridge",
            consentLatch.await(10, TimeUnit.SECONDS),
        )
    }
}
