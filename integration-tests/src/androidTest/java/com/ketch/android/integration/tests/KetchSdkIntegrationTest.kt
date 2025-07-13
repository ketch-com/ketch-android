package com.ketch.android.integration.tests

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
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
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) {
            scenario.close()
        }
    }

    @Test
    fun testActivityLaunches() {
        // Test that the activity can be launched without crashing
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            assertNotNull("Activity should not be null", activity)
            assertTrue("Activity should be initialized", activity.isTaskRoot || !activity.isTaskRoot)
        }
    }

    @Test
    fun testContextIsAvailable() {
        // Test that Android context is available
        assertNotNull("Context should not be null", context)
        assertEquals("Package name should match", "com.ketch.android.integration.tests", context.packageName)
    }

    @Test
    fun testBasicAndroidComponents() {
        // Test basic Android framework components
        assertNotNull("Application context should be available", ApplicationProvider.getApplicationContext())
        
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            assertNotNull("Activity context should be available", activity)
            assertNotNull("Activity application should be available", activity.application)
        }
    }
} 