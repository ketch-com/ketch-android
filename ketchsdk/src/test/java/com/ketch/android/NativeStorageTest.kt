package com.ketch.android

import android.content.SharedPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
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

class NativeStorageTest {
    private lateinit var prefs: SharedPreferences

    @Before
    fun setUp() {
        prefs = MemorySharedPreferences()
        NativeStorage.bindPreferencesForTesting(prefs)
    }

    @After
    fun tearDown() {
        NativeStorage.resetForTesting()
    }

    @Test
    fun read_withoutInitialize_throws() {
        NativeStorage.resetForTesting()
        assertThrows(IllegalStateException::class.java) {
            NativeStorage.read("missing")
        }
    }

    @Test
    fun write_thenRead_roundTrip() {
        NativeStorage.write("foo", "bar")
        assertEquals("bar", NativeStorage.read("foo"))
    }

    @Test
    fun read_missingKey_returnsDefault() {
        assertEquals("fallback", NativeStorage.read("missing", "fallback"))
    }
}

private class MemorySharedPreferences : SharedPreferences {
    private val store = linkedMapOf<String, String?>()

    override fun getAll(): Map<String, *> = store.toMap()

    override fun getString(key: String, defValue: String?): String? = store[key] ?: defValue

    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = null

    override fun getInt(key: String, defValue: Int): Int = defValue

    override fun getLong(key: String, defValue: Long): Long = defValue

    override fun getFloat(key: String, defValue: Float): Float = defValue

    override fun getBoolean(key: String, defValue: Boolean): Boolean = defValue

    override fun contains(key: String): Boolean = store.containsKey(key)

    override fun edit(): SharedPreferences.Editor = EditorImpl()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    private inner class EditorImpl : SharedPreferences.Editor {
        private val pending = linkedMapOf<String, String?>()

        override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply {
            if (key != null) pending[key] = value
        }

        override fun putStringSet(
            key: String?,
            values: MutableSet<String>?,
        ): SharedPreferences.Editor = this

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = this

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = this

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = this

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = this

        override fun remove(key: String?): SharedPreferences.Editor = apply {
            if (key != null) pending[key] = null
        }

        override fun clear(): SharedPreferences.Editor = apply { pending.clear() }

        override fun commit(): Boolean {
            applyPending()
            return true
        }

        override fun apply() {
            applyPending()
        }

        private fun applyPending() {
            pending.forEach { (key, value) ->
                if (value == null) {
                    store.remove(key)
                } else {
                    store[key] = value
                }
            }
            pending.clear()
        }
    }
}
