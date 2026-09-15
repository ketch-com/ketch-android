package com.ketch.android

import android.content.Context
import android.content.ContextWrapper
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

/**
 * Reader that parks inside read() until released, so the timeout and single-read paths can be
 * driven deterministically rather than by sleeping.
 */
private class BlockingReader(private val result: String?) : AaidReader {
    val callCount = AtomicInteger(0)
    val started = CountDownLatch(1)
    private val release = CountDownLatch(1)

    override fun read(context: Context): String? {
        callCount.incrementAndGet()
        started.countDown()
        release.await(5, TimeUnit.SECONDS)
        return result
    }

    fun release() = release.countDown()
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

    /** A real dispatcher, since resolve() blocks on the read rather than deferring it. */
    private fun useRealScope() {
        AaidResolver.scope = CoroutineScope(Dispatchers.IO)
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
    fun firstCallWaitsForTheReadAndReturnsTheId() {
        val reader = CountingReader("the-aaid")
        AaidResolver.reader = reader
        AaidResolver.isAvailable = { true }
        useRealScope()

        // The behaviour this class exists for: the very first resolve answers with the value, so
        // the tag's first identity collection carries ketch_aaid into getConsent.
        assertEquals("the-aaid", AaidResolver.resolve(fakeContext))
        assertEquals(1, reader.callCount.get())
    }

    @Test
    fun limitAdTrackingEnabled_resolvesToNull() {
        val reader = CountingReader(result = null)
        AaidResolver.reader = reader
        AaidResolver.isAvailable = { true }
        useRealScope()

        assertNull(AaidResolver.resolve(fakeContext))
        assertEquals(1, reader.callCount.get())
    }

    @Test
    fun resolvedValueIsCachedAndTheReaderIsNotCalledAgain() {
        val reader = CountingReader("the-aaid")
        AaidResolver.reader = reader
        AaidResolver.isAvailable = { true }
        useRealScope()

        assertEquals("the-aaid", AaidResolver.resolve(fakeContext))
        assertEquals("the-aaid", AaidResolver.resolve(fakeContext))
        assertEquals(1, reader.callCount.get())
    }

    @Test
    fun reset_restartsTheResolveCycle() {
        val reader = CountingReader("the-aaid")
        AaidResolver.reader = reader
        AaidResolver.isAvailable = { true }
        useRealScope()

        assertEquals("the-aaid", AaidResolver.resolve(fakeContext))
        AaidResolver.reset()
        assertEquals("the-aaid", AaidResolver.resolve(fakeContext))
        assertEquals(2, reader.callCount.get())
    }

    @Test
    fun readSlowerThanTheTimeoutReturnsNullRatherThanBlockingOn() {
        val reader = BlockingReader("the-aaid")
        AaidResolver.reader = reader
        AaidResolver.isAvailable = { true }
        useRealScope()

        assertNull(AaidResolver.resolve(fakeContext, timeoutMs = 50))
        assertTrue(reader.started.await(5, TimeUnit.SECONDS))
        reader.release()
    }

    @Test
    fun valueFromATimedOutReadIsPickedUpByTheNextCall() {
        val reader = BlockingReader("the-aaid")
        AaidResolver.reader = reader
        AaidResolver.isAvailable = { true }
        useRealScope()

        assertNull(AaidResolver.resolve(fakeContext, timeoutMs = 50))
        assertTrue(reader.started.await(5, TimeUnit.SECONDS))
        reader.release()

        // Joins the read already in flight instead of starting a second one.
        assertEquals("the-aaid", AaidResolver.resolve(fakeContext, timeoutMs = 5_000))
        assertEquals(1, reader.callCount.get())
    }

    @Test
    fun timedOutReadStillReachesCachedValue() {
        val reader = BlockingReader("the-aaid")
        AaidResolver.reader = reader
        AaidResolver.isAvailable = { true }
        useRealScope()

        assertNull(AaidResolver.resolve(fakeContext, timeoutMs = 50))
        assertTrue(reader.started.await(5, TimeUnit.SECONDS))
        reader.release()
        AaidResolver.resolve(fakeContext, timeoutMs = 5_000)

        // getIdentities() reads cachedValue(), not resolve(). A read that landed after a timeout
        // has to be promoted out of the in-flight state or the AAID is invisible to it forever.
        assertEquals("the-aaid", AaidResolver.cachedValue())
    }

    @Test
    fun concurrentCallersShareASingleRead() {
        val reader = BlockingReader("the-aaid")
        AaidResolver.reader = reader
        AaidResolver.isAvailable = { true }
        useRealScope()

        val results = Collections.synchronizedList(mutableListOf<String?>())
        val callers = (1..4).map {
            thread { results.add(AaidResolver.resolve(fakeContext, timeoutMs = 5_000)) }
        }

        assertTrue(reader.started.await(5, TimeUnit.SECONDS))
        reader.release()
        callers.forEach { it.join(10_000) }

        // filterNotNull is safe here: expecting four values also asserts none came back null.
        assertEquals(List(4) { "the-aaid" }, results.filterNotNull().sorted())
        // Losing the race must not start a second read — the ad ID is read once per process.
        assertEquals(1, reader.callCount.get())
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
    fun aaidResolved_appearsInIdentities() {
        AaidResolver.reader = CountingReader("the-aaid")
        AaidResolver.isAvailable = { true }
        AaidResolver.scope = CoroutineScope(Dispatchers.IO)

        AaidResolver.resolve(fakeContext)

        assertEquals(mapOf(KEY_AAID to "the-aaid"), identities(setOf(KEY_AAID)))
    }

    @Test
    fun afterReset_aaidIsGoneFromIdentities() {
        AaidResolver.reader = CountingReader("the-aaid")
        AaidResolver.isAvailable = { true }
        AaidResolver.scope = CoroutineScope(Dispatchers.IO)

        AaidResolver.resolve(fakeContext)
        assertEquals(mapOf(KEY_AAID to "the-aaid"), identities(setOf(KEY_AAID)))

        // Mirrors Ketch.clearIdentities(): resets the resolver (resolvedIdentityKeys would also
        // be cleared in the real Ketch instance, but the resolver alone is what makes AAID
        // disappear from a lookup keyed on KEY_AAID).
        AaidResolver.reset()

        assertEquals(emptyMap<String, String>(), identities(setOf(KEY_AAID)))
    }

    @Test
    fun aaidFromATimedOutReadAppearsOnceItLands() {
        val reader = BlockingReader("the-aaid")
        AaidResolver.reader = reader
        AaidResolver.isAvailable = { true }
        AaidResolver.scope = CoroutineScope(Dispatchers.IO)

        assertNull(AaidResolver.resolve(fakeContext, timeoutMs = 50))
        assertEquals(emptyMap<String, String>(), identities(setOf(KEY_AAID)))

        assertTrue(reader.started.await(5, TimeUnit.SECONDS))
        reader.release()
        AaidResolver.resolve(fakeContext, timeoutMs = 5_000)

        assertEquals(mapOf(KEY_AAID to "the-aaid"), identities(setOf(KEY_AAID)))
    }
}
