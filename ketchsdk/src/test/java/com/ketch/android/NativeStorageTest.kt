package com.ketch.android

import android.content.SharedPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class NativeStoragePutPayloadTest {
    @Test
    fun parse_validPayload() {
        val payload = parseNativeStoragePutPayload("""{"key":"consent_id","value":"abc-123"}""")
        assertEquals(NativeStoragePutPayload("consent_id", "abc-123"), payload)
    }

    @Test
    fun parse_blankKey_returnsNull() {
        assertNull(parseNativeStoragePutPayload("""{"key":"   ","value":"x"}"""))
        assertNull(parseNativeStoragePutPayload("""{"key":"","value":"x"}"""))
    }

    @Test
    fun parse_missingKey_returnsNull() {
        assertNull(parseNativeStoragePutPayload("""{"value":"x"}"""))
    }

    @Test
    fun parse_malformedJson_returnsNull() {
        assertNull(parseNativeStoragePutPayload("not-json"))
    }

    @Test
    fun parse_missingValue_defaultsToEmptyString() {
        assertEquals(NativeStoragePutPayload("k", ""), parseNativeStoragePutPayload("""{"key":"k"}"""))
    }
}

class KetchSharedPreferencesStorageTest {
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
    fun write_thenRead_roundTrip() {
        KetchSharedPreferences.write("foo", "bar")
        assertEquals("bar", KetchSharedPreferences.read("foo"))
    }

    @Test
    fun read_missingKey_returnsDefault() {
        assertEquals("fallback", KetchSharedPreferences.read("missing", "fallback"))
    }

    @Test
    fun remove_deletesWrittenKey() {
        KetchSharedPreferences.write("gone", "bye")
        KetchSharedPreferences.remove("gone")
        assertEquals("missing", KetchSharedPreferences.read("gone", "missing"))
    }

    @Test
    fun removeValues_removesOnlyPrefixMatchingKeys() {
        KetchSharedPreferences.write("IABTCF_TCString", "abc")
        KetchSharedPreferences.write("IABGPP_HDR_Version", "1")
        KetchSharedPreferences.write("keep_me", "safe")

        val removed = KetchSharedPreferences.removeValues(listOf("IABTCF", "IABGPP", "IABUS"))

        assertEquals(2, removed)
        assertEquals("missing", KetchSharedPreferences.read("IABTCF_TCString", "missing"))
        assertEquals("missing", KetchSharedPreferences.read("IABGPP_HDR_Version", "missing"))
        assertEquals("safe", KetchSharedPreferences.read("keep_me"))
    }
}
