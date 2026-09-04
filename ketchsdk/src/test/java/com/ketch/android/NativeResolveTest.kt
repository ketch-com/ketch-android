package com.ketch.android

import android.content.SharedPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ParseNativeResolveKeyTest {
    @Test
    fun trimsWhitespaceFromAValidKey() {
        assertEquals("swb_app1", parseNativeResolveKey("  swb_app1  "))
    }

    @Test
    fun nullKey_returnsNull() {
        assertNull(parseNativeResolveKey(null))
    }

    @Test
    fun emptyKey_returnsNull() {
        assertNull(parseNativeResolveKey(""))
    }

    @Test
    fun blankKey_returnsNull() {
        assertNull(parseNativeResolveKey("   "))
    }
}

class KetchNativeResolveLookupTest {
    private lateinit var prefs: SharedPreferences

    @Before
    fun setUp() {
        prefs = MemorySharedPreferences()
        KetchSharedPreferences.bindPreferencesForTesting(prefs)
    }

    @After
    fun tearDown() {
        KetchSharedPreferences.resetForTesting()
    }

    @Test
    fun storedValue_isReturned() {
        KetchSharedPreferences.write("swb_app1", "the-uuid")

        assertEquals("the-uuid", KetchSharedPreferences.getSavedValue("swb_app1"))
    }

    @Test
    fun nothingStored_returnsNull() {
        assertNull(KetchSharedPreferences.getSavedValue("swb_app1"))
    }
}
