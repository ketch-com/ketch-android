@file:OptIn(ExperimentalCoroutinesApi::class)

package com.ketch.android

import android.content.Context
import android.content.ContextWrapper
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

// getApplicationContext() is overridden directly so no inherited Android framework method is
// ever invoked on this fake.
private val fakeContext: Context = object : ContextWrapper(null) {
    override fun getApplicationContext(): Context = this
}

private class CountingReader(private val result: String?) : AaidReader {
    val callCount = AtomicInteger(0)

    override fun read(context: Context): String? {
        callCount.incrementAndGet()
        return result
    }
}

class AaidOrNullTest {
    @Test
    fun limitAdTrackingEnabled_isNullRegardlessOfId() {
        assertNull(aaidOrNull("some-real-id", isLimitAdTrackingEnabled = true))
    }

    @Test
    fun zeroedId_isNullEvenWhenLimitAdTrackingIsFalse() {
        // Play Services returns the zeroed UUID when the app hasn't declared the AD_ID
        // permission, independent of isLimitAdTrackingEnabled — must not pass through either way.
        assertNull(aaidOrNull("00000000-0000-0000-0000-000000000000", isLimitAdTrackingEnabled = false))
    }

    @Test
    fun nullId_isNull() {
        assertNull(aaidOrNull(null, isLimitAdTrackingEnabled = false))
    }

    @Test
    fun realId_isReturned() {
        assertEquals("some-real-id", aaidOrNull("some-real-id", isLimitAdTrackingEnabled = false))
    }
}

class AaidResolverTest {
    @Before
    fun setUp() {
        AaidResolver.resetForTesting()
    }

    @After
    fun tearDown() {
        AaidResolver.resetForTesting()
    }

    @Test
    fun dependencyAbsent_alwaysReturnsNull_readerNeverCalled() {
        val reader = CountingReader("some-id")
        AaidResolver.reader = reader
        AaidResolver.isAvailable = { false }

        assertNull(AaidResolver.resolve(fakeContext))
        assertNull(AaidResolver.resolve(fakeContext))
        assertEquals(0, reader.callCount.get())
    }

    @Test
    fun limitAdTrackingEnabled_resolvesToNull() = runTest {
        val reader = CountingReader(result = null)
        AaidResolver.reader = reader
        AaidResolver.isAvailable = { true }
        AaidResolver.scope = CoroutineScope(StandardTestDispatcher(testScheduler))

        AaidResolver.resolve(fakeContext)
        advanceUntilIdle()

        assertNull(AaidResolver.resolve(fakeContext))
        assertEquals(1, reader.callCount.get())
    }

    @Test
    fun limitAdTrackingDisabled_resolvesToTheId() = runTest {
        val reader = CountingReader(result = "the-aaid")
        AaidResolver.reader = reader
        AaidResolver.isAvailable = { true }
        AaidResolver.scope = CoroutineScope(StandardTestDispatcher(testScheduler))

        AaidResolver.resolve(fakeContext)
        advanceUntilIdle()

        assertEquals("the-aaid", AaidResolver.resolve(fakeContext))
        assertEquals(1, reader.callCount.get())
    }

    @Test
    fun resolveInFlight_secondCallAlsoReturnsNull_readerCalledOnce() = runTest {
        val reader = CountingReader(result = "the-aaid")
        AaidResolver.reader = reader
        AaidResolver.isAvailable = { true }
        AaidResolver.scope = CoroutineScope(StandardTestDispatcher(testScheduler))

        val first = AaidResolver.resolve(fakeContext)
        val second = AaidResolver.resolve(fakeContext)

        assertNull(first)
        assertNull(second)
        // The background read hasn't run yet on the (paused) test dispatcher — only one coroutine
        // was launched, for the first call; the second call saw InFlight and returned directly.
        assertEquals(0, reader.callCount.get())

        advanceUntilIdle()

        assertEquals("the-aaid", AaidResolver.resolve(fakeContext))
        assertEquals(1, reader.callCount.get())
    }

    @Test
    fun resolveComplete_cachedValueReturnedWithoutRereading() = runTest {
        val reader = CountingReader(result = "the-aaid")
        AaidResolver.reader = reader
        AaidResolver.isAvailable = { true }
        AaidResolver.scope = CoroutineScope(StandardTestDispatcher(testScheduler))

        AaidResolver.resolve(fakeContext)
        advanceUntilIdle()

        assertEquals("the-aaid", AaidResolver.resolve(fakeContext))
        assertEquals("the-aaid", AaidResolver.resolve(fakeContext))
        assertEquals(1, reader.callCount.get())
    }

    @Test
    fun reset_restartsTheResolveCycle() = runTest {
        val reader = CountingReader(result = "the-aaid")
        AaidResolver.reader = reader
        AaidResolver.isAvailable = { true }
        AaidResolver.scope = CoroutineScope(StandardTestDispatcher(testScheduler))

        AaidResolver.resolve(fakeContext)
        advanceUntilIdle()
        assertEquals("the-aaid", AaidResolver.resolve(fakeContext))

        AaidResolver.reset()
        assertNull(AaidResolver.resolve(fakeContext))
        advanceUntilIdle()

        assertEquals("the-aaid", AaidResolver.resolve(fakeContext))
        assertEquals(2, reader.callCount.get())
    }
}

// Exercises the same identities map Ketch.getIdentities() builds — resolvedIdentityKeys plus
// resolvedIdentityLookup — against the real AaidResolver state machine, without needing a real
// Ketch/Context.
class AaidInGetIdentitiesTest {
    @Before
    fun setUp() {
        AaidResolver.resetForTesting()
    }

    @After
    fun tearDown() {
        AaidResolver.resetForTesting()
    }

    private fun identities(resolvedIdentityKeys: Set<String>) =
        mergeResolvedIdentities(emptyMap(), resolvedIdentityKeys) { key ->
            resolvedIdentityLookup(key, AaidResolver::cachedValue) { null }
        }

    @Test
    fun aaidRegisteredButUnresolved_isOmitted() {
        assertEquals(emptyMap<String, String>(), identities(setOf(KEY_AAID)))
    }

    @Test
    fun aaidResolved_appearsInIdentities() = runTest {
        AaidResolver.reader = CountingReader(result = "the-aaid")
        AaidResolver.isAvailable = { true }
        AaidResolver.scope = CoroutineScope(StandardTestDispatcher(testScheduler))

        AaidResolver.resolve(fakeContext)
        advanceUntilIdle()

        assertEquals(mapOf(KEY_AAID to "the-aaid"), identities(setOf(KEY_AAID)))
    }

    @Test
    fun afterReset_aaidIsGoneFromIdentities() = runTest {
        AaidResolver.reader = CountingReader(result = "the-aaid")
        AaidResolver.isAvailable = { true }
        AaidResolver.scope = CoroutineScope(StandardTestDispatcher(testScheduler))

        AaidResolver.resolve(fakeContext)
        advanceUntilIdle()
        assertEquals(mapOf(KEY_AAID to "the-aaid"), identities(setOf(KEY_AAID)))

        // Mirrors Ketch.clearIdentities(): resets the resolver (resolvedIdentityKeys would also
        // be cleared in the real Ketch instance, but the resolver alone is what makes AAID
        // disappear from a lookup keyed on KEY_AAID).
        AaidResolver.reset()

        assertEquals(emptyMap<String, String>(), identities(setOf(KEY_AAID)))
    }
}
