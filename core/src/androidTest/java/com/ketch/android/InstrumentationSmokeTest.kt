package com.ketch.android

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class InstrumentationSmokeTest {

    @Test
    @DisplayName("Ketch-Sdk Module Instrumentation Smoke Test")
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.ketch.android.test", appContext.packageName)
    }
}
