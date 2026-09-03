package com.ketch.android

import com.ketch.android.api.HeadlessApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit

class ManagedIdentityTest {
    private lateinit var server: MockWebServer
    private lateinit var prefs: MemorySharedPreferences
    private var now = 1_000_000L

    private val queryStringConfig =
        """{"identities":{"swb_android":{"type":"queryString","variable":"swb_android","ttl":34560000}}}"""
    private val managedCookieConfig =
        """{"identities":{"swb_android":{"type":"managedCookie","variable":"_swb"}}}"""

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        prefs = MemorySharedPreferences()
        KetchSharedPreferences.bindPreferencesForTesting(prefs)
        ManagedIdentity.resetForTesting()
        ManagedIdentity.clock = { now }
    }

    @After
    fun tearDown() {
        server.shutdown()
        ManagedIdentity.resetForTesting()
        KetchSharedPreferences.resetForTesting()
    }

    private fun client() = HeadlessApiClient(
        baseUrl = server.url("/").toString().trimEnd('/'),
        okHttpClient = OkHttpClient(),
    )

    private fun respond(body: String, times: Int = 1) {
        repeat(times) {
            server.enqueue(
                MockResponse()
                    .setResponseCode(HttpURLConnection.HTTP_OK)
                    .setBody(body)
                    .addHeader("Content-Type", "application/json"),
            )
        }
    }

    private fun resolve(property: String = "android") =
        runBlocking { ManagedIdentity.await("acme", property, client()) }

    // --- matching ---------------------------------------------------------------

    @Test
    fun queryString_mintsAndPersists() {
        respond(queryStringConfig)

        val resolved = resolve()

        assertEquals("swb_android", resolved?.code)
        assertEquals("swb_android", resolved?.variable)
        assertTrue(
            "expected a lowercase uuid v4",
            resolved?.value?.matches(
                Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"),
            ) == true,
        )
        assertEquals(resolved?.value, prefs.getString(ManagedIdentity.STORAGE_KEY, null))
        assertEquals("1000000", prefs.getString(ManagedIdentity.MINTED_AT_KEY, null))
    }

    @Test
    fun managedCookie_suppliesNothingAndMintsNothing() {
        respond(managedCookieConfig)

        assertNull(resolve())
        // The inertness guarantee: a value minted but not sent would still be a behaviour change.
        assertTrue("nothing may be written to prefs", prefs.all.isEmpty())
    }

    @Test
    fun typeMatchIsCaseAndWhitespaceInsensitive() {
        respond("""{"identities":{"swb_android":{"type":" QueryString ","variable":"swb_android"}}}""")

        assertEquals("swb_android", resolve()?.variable)
    }

    @Test
    fun nonManagedPrefix_isIgnored() {
        respond("""{"identities":{"email":{"type":"queryString","variable":"email"}}}""")

        assertNull(resolve())
        assertTrue(prefs.all.isEmpty())
    }

    @Test
    fun missingIdentitiesKey_resolvesToNothing() {
        respond("""{"bogus":null}""")

        assertNull(resolve())
    }

    @Test
    fun emptyIdentities_resolvesToNothing() {
        respond("""{"identities":{}}""")

        assertNull(resolve())
    }

    @Test
    fun missingVariable_fallsBackToTheSpaceCode() {
        respond("""{"identities":{"swb_android":{"type":"queryString"}}}""")

        val resolved = resolve()

        assertEquals("swb_android", resolved?.code)
        assertEquals("swb_android", resolved?.variable)
    }

    // --- ttl --------------------------------------------------------------------

    @Test
    fun withinTtl_reusesStoredValueAndDoesNotRewriteTheTimestamp() {
        respond(queryStringConfig, times = 2)
        val first = resolve()?.value

        now += 34_559_999_000L
        ManagedIdentity.resetForTesting()
        ManagedIdentity.clock = { now }

        assertEquals(first, resolve()?.value)
        assertEquals("1000000", prefs.getString(ManagedIdentity.MINTED_AT_KEY, null))
    }

    @Test
    fun exactTtlBoundary_expires() {
        respond(queryStringConfig, times = 2)
        val first = resolve()?.value

        now += 34_560_000_000L
        ManagedIdentity.resetForTesting()
        ManagedIdentity.clock = { now }

        assertNotEquals(first, resolve()?.value)
    }

    @Test
    fun missingTtl_fallsBackTo400Days() {
        respond("""{"identities":{"swb_android":{"type":"queryString"}}}""", times = 2)
        val first = resolve()?.value

        now += ManagedIdentity.DEFAULT_TTL_SECONDS * 1_000 - 1
        ManagedIdentity.resetForTesting()
        ManagedIdentity.clock = { now }

        assertEquals(first, resolve()?.value)
    }

    @Test
    fun unparseableTimestamp_forcesAReMint() {
        respond(queryStringConfig, times = 2)
        val first = resolve()?.value

        prefs.edit().putString(ManagedIdentity.MINTED_AT_KEY, "not-a-number").commit()
        ManagedIdentity.resetForTesting()
        ManagedIdentity.clock = { now }

        assertNotEquals(first, resolve()?.value)
    }

    // --- caching, sharing, clearing ---------------------------------------------

    @Test
    fun secondResolveForSameProperty_usesTheCacheAndDoesNotRefetch() {
        respond(queryStringConfig)

        val first = resolve()
        val second = resolve()

        assertEquals(first, second)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun twoPropertiesShareOneStoredIdentifier() {
        respond(queryStringConfig, times = 2)

        val android = resolve("android")
        val other = resolve("other")

        // One value per app, surfaced under whatever space code each property names.
        assertEquals(android?.value, other?.value)
        assertEquals(1, prefs.all.keys.count { it == ManagedIdentity.STORAGE_KEY })
    }

    // resolve() (the callback form) has no unit coverage: HeadlessApiClient.launchAsync delivers
    // on Dispatchers.Main, which does not exist in a JVM unit test, so its callback never runs.
    @Test
    fun concurrentResolves_shareASingleConfigFetch() = runBlocking {
        // Held open so the second caller definitely arrives mid-flight and has to join rather
        // than hit the cache; only one response is enqueued, so a second fetch would block.
        server.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(queryStringConfig)
                .addHeader("Content-Type", "application/json")
                .setBodyDelay(750, TimeUnit.MILLISECONDS),
        )
        val shared = client()

        val first = async(Dispatchers.IO) { ManagedIdentity.await("acme", "android", shared) }
        val second = async(Dispatchers.IO) { ManagedIdentity.await("acme", "android", shared) }

        assertEquals(first.await(), second.await())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun peek_returnsNothingUntilResolved() {
        respond(queryStringConfig)
        assertNull(ManagedIdentity.peek("acme", "android"))

        val resolved = resolve()

        assertEquals(resolved, ManagedIdentity.peek("acme", "android"))
    }

    @Test
    fun failedFetch_backsOffInsteadOfRefetching() {
        server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR))

        assertNull(resolve())
        assertEquals(1, server.requestCount)

        // Inside the backoff window a second attempt must not hit the network again.
        assertNull(resolve())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun backoffExpires_andAFreshAttemptIsMade() {
        server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR))
        respond(queryStringConfig)

        assertNull(resolve())
        now += 60_001

        assertEquals("swb_android", resolve()?.variable)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun clear_dropsStorageAndCache_andTheNextResolveMintsAfresh() {
        respond(queryStringConfig, times = 2)
        val first = resolve()?.value

        ManagedIdentity.clear()

        assertNull(prefs.getString(ManagedIdentity.STORAGE_KEY, null))
        assertNull(prefs.getString(ManagedIdentity.MINTED_AT_KEY, null))
        assertNull("the memoised resolution must go too", ManagedIdentity.peek("acme", "android"))
        assertNotEquals(first, resolve()?.value)
    }

    @Test
    fun lastResolved_isAvailableForRequestsWithNoPropertyCode() {
        respond(queryStringConfig)
        assertNull(ManagedIdentity.lastResolved())

        val resolved = resolve()

        assertEquals(resolved, ManagedIdentity.lastResolved())
    }
}
