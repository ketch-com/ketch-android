package com.ketch.android.integration.tests

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ketch.android.Ketch
import com.ketch.android.data.WillShowExperienceType
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Verifies the [Ketch.showPreferencesTab] arguments drive the correct preference center:
 *  - the `tab` argument selects which tab is active (asserted via the active tab's
 *    content element id rendered by lanyard, e.g. `ketch-preferences-purposes-tab`).
 *  - the `tabs` argument controls which tabs are available; selecting a tab that was
 *    excluded falls back to the overview tab.
 *
 * Assertions inspect the live WebView DOM by element id (matching lanyard's
 * `ketch-preferences-*-tab` ids), the same approach used by the other suites.
 * Only the active tab's content div is mounted, so its presence/absence is a
 * reliable signal of which tab is displayed across both mobile and desktop layouts.
 *
 * Runs after the core suite (Z prefix).
 */
@RunWith(AndroidJUnit4::class)
class ZPreferenceTabArgsTest {

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

    private fun showPreferencesTabAndWait(
        tabs: List<Ketch.PreferencesTab>,
        tab: Ketch.PreferencesTab,
    ) {
        val showLatch = CountDownLatch(1)
        app.setTestMode(object : IntegrationTestApp.TestEventListener {
            override fun onWillShowExperience(type: WillShowExperienceType) {
                if (type == WillShowExperienceType.PreferenceExperience) {
                    showLatch.countDown()
                }
            }

            override fun onError(error: String) {
                throw AssertionError("showPreferencesTab failed: $error")
            }
        })

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            app.ketch.showPreferencesTab(tabs = tabs, tab = tab)
        }
        assertTrue(
            "Preference center should show within 30s",
            showLatch.await(30, TimeUnit.SECONDS),
        )
    }

    private fun isTabContentPresent(elementId: String): Boolean {
        val latch = CountDownLatch(1)
        val result = AtomicReference(false)
        app.validateWebViewContent(elementId) { exists ->
            result.set(exists)
            latch.countDown()
        }
        assertTrue(
            "WebView content check for '$elementId' should complete within 15s",
            latch.await(15, TimeUnit.SECONDS),
        )
        return result.get()
    }

    @Test
    fun consentsTabArgDisplaysPurposesTab() {
        showPreferencesTabAndWait(
            tabs = listOf(
                Ketch.PreferencesTab.OVERVIEW,
                Ketch.PreferencesTab.CONSENTS,
                Ketch.PreferencesTab.RIGHTS,
                Ketch.PreferencesTab.SUBSCRIPTIONS,
            ),
            tab = Ketch.PreferencesTab.CONSENTS,
        )

        assertTrue(
            "Consents tab arg should render the purposes tab content",
            isTabContentPresent(PURPOSES_TAB_ID),
        )
        assertFalse(
            "Overview tab content should not be rendered when consents tab is selected",
            isTabContentPresent(WELCOME_TAB_ID),
        )
    }

    @Test
    fun overviewTabArgDisplaysWelcomeTab() {
        showPreferencesTabAndWait(
            tabs = listOf(
                Ketch.PreferencesTab.OVERVIEW,
                Ketch.PreferencesTab.CONSENTS,
                Ketch.PreferencesTab.RIGHTS,
                Ketch.PreferencesTab.SUBSCRIPTIONS,
            ),
            tab = Ketch.PreferencesTab.OVERVIEW,
        )

        assertTrue(
            "Overview tab arg should render the welcome tab content",
            isTabContentPresent(WELCOME_TAB_ID),
        )
        assertFalse(
            "Purposes tab content should not be rendered when overview tab is selected",
            isTabContentPresent(PURPOSES_TAB_ID),
        )
    }

    @Test
    fun consentsOnlyTabsShowsPurposesWhenSelected() {
        // tabs excludes OVERVIEW, so the overview tab must be unavailable and the
        // selected consents tab must be displayed.
        showPreferencesTabAndWait(
            tabs = listOf(Ketch.PreferencesTab.CONSENTS),
            tab = Ketch.PreferencesTab.CONSENTS,
        )

        assertTrue(
            "Consents tab should be displayed when it is the only tab requested",
            isTabContentPresent(PURPOSES_TAB_ID),
        )
        assertFalse(
            "Overview tab content should be absent when overview is excluded from tabs",
            isTabContentPresent(WELCOME_TAB_ID),
        )
    }

    @Test
    fun selectedTabExcludedFromTabsFallsBackToOverview() {
        // tabs only allows OVERVIEW but the caller selects CONSENTS; since consents is
        // not an available tab, lanyard falls back to the overview tab.
        showPreferencesTabAndWait(
            tabs = listOf(Ketch.PreferencesTab.OVERVIEW),
            tab = Ketch.PreferencesTab.CONSENTS,
        )

        assertTrue(
            "Excluded selected tab should fall back to the overview tab content",
            isTabContentPresent(WELCOME_TAB_ID),
        )
        assertFalse(
            "Purposes tab content should be absent when consents is excluded from tabs",
            isTabContentPresent(PURPOSES_TAB_ID),
        )
    }

    private companion object {
        // lanyard preference tab content element ids (only the active tab is mounted)
        const val WELCOME_TAB_ID = "ketch-preferences-welcome-tab"
        const val PURPOSES_TAB_ID = "ketch-preferences-purposes-tab"
    }
}
