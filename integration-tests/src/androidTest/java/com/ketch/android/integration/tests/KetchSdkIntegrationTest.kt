package com.ketch.android.integration.tests

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.After
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class KetchSdkIntegrationTest {

    private lateinit var context: Context
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun testAppLaunchesSuccessfully() {
        // Test that the app launches and shows the main UI
        onView(withId(R.id.statusText))
            .check(matches(isDisplayed()))
            .check(matches(withText("Ketch initialized")))
    }

    @Test
    fun testKetchInitializationDisplaysCorrectStatus() {
        // Test that Ketch is initialized and shows correct status
        onView(withId(R.id.statusText))
            .check(matches(withText("Ketch initialized")))
    }

    @Test
    fun testBasicUIElementsAreDisplayed() {
        // Test that basic UI elements are visible
        onView(withId(R.id.statusText))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.loadButton))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.showConsentButton))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.showPreferencesButton))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testEnvironmentTextIsSet() {
        // Test that environment text is displayed
        onView(withId(R.id.environmentText))
            .check(matches(isDisplayed()))
            .check(matches(withText("Environment: Not set")))
    }

    @Test
    fun testConsentTextIsSet() {
        // Test that consent text is displayed
        onView(withId(R.id.consentText))
            .check(matches(isDisplayed()))
            .check(matches(withText("Consent: Not set")))
    }
} 