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

class ResolvedIdentityLookupTest {
    @Test
    fun aaidKey_routesToAaidCachedValue_neverStorage() {
        val result = resolvedIdentityLookup(
            key = KEY_AAID,
            aaidCachedValue = { "the-aaid" },
            storageValue = { org.junit.Assert.fail("should not read storage for the AAID key"); null },
        )

        assertEquals("the-aaid", result)
    }

    @Test
    fun aaidKey_whenUnresolved_returnsNull() {
        val result = resolvedIdentityLookup(
            key = KEY_AAID,
            aaidCachedValue = { null },
            storageValue = { org.junit.Assert.fail("should not read storage for the AAID key"); null },
        )

        assertNull(result)
    }

    @Test
    fun otherKey_routesToStorage_neverAaidCache() {
        val result = resolvedIdentityLookup(
            key = "swb_app1",
            aaidCachedValue = { org.junit.Assert.fail("should not read the AAID cache for other keys"); null },
            storageValue = { key -> if (key == "swb_app1") "the-uuid" else null },
        )

        assertEquals("the-uuid", result)
    }
}

class MergeResolvedIdentitiesTest {
    @Test
    fun noResolvedKeys_returnsIdentitiesUnchanged() {
        val result = mergeResolvedIdentities(
            identities = mapOf("email" to "a@b.test"),
            resolvedIdentityKeys = emptySet(),
            lookup = { null },
        )

        assertEquals(mapOf("email" to "a@b.test"), result)
    }

    @Test
    fun resolvedKeyWithAValue_isAdded() {
        val result = mergeResolvedIdentities(
            identities = mapOf("email" to "a@b.test"),
            resolvedIdentityKeys = setOf("swb_app1"),
            lookup = { key -> if (key == "swb_app1") "the-uuid" else null },
        )

        assertEquals(mapOf("email" to "a@b.test", "swb_app1" to "the-uuid"), result)
    }

    @Test
    fun resolvedKeyWithNoValueYet_isOmittedNotBlank() {
        val result = mergeResolvedIdentities(
            identities = emptyMap(),
            resolvedIdentityKeys = setOf("swb_app1"),
            lookup = { null },
        )

        assertEquals(emptyMap<String, String>(), result)
    }

    @Test
    fun resolvedValueWinsOnCollisionWithIdentities() {
        val result = mergeResolvedIdentities(
            identities = mapOf("swb_app1" to "stale"),
            resolvedIdentityKeys = setOf("swb_app1"),
            lookup = { "fresh" },
        )

        assertEquals(mapOf("swb_app1" to "fresh"), result)
    }
}
