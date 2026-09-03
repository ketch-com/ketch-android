package com.ketch.android.api

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.HttpURLConnection

/**
 * The identity config call gates the WebView, so both the URL it requests and its handling of a
 * response with no `identities` key are load-bearing.
 */
class HeadlessIdentityConfigurationTest {
    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    private fun client() = HeadlessApiClient(
        baseUrl = mockWebServer.url("/").toString().trimEnd('/'),
        okHttpClient = OkHttpClient(),
    )

    private fun respond(body: String) {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(body)
                .addHeader("Content-Type", "application/json"),
        )
    }

    @Test
    fun requestsTheShortPathWithTheIncludeParam() = runBlocking {
        respond("""{"identities":{}}""")

        client().getIdentityConfiguration("acme", "android")

        val path = mockWebServer.takeRequest().path
        // The variant path ignores `include` and returns the whole ~128KB config.
        assertEquals("/config/acme/android/config.json?include=identities", path)
    }

    @Test
    fun parsesTheIdentitiesBlock() = runBlocking {
        respond("""{"identities":{"swb_android":{"type":"queryString","variable":"swb_android","ttl":34560000}}}""")

        val config = client().getIdentityConfiguration("acme", "android")

        val identity = config.identities?.get("swb_android")
        assertEquals("queryString", identity?.type)
        assertEquals("swb_android", identity?.variable)
        assertEquals(34_560_000L, identity?.ttl)
    }

    @Test
    fun parsesTheLiveManagedCookieShape() = runBlocking {
        respond("""{"identities":{"swb_android":{"type":"managedCookie","variable":"_swb"}}}""")

        val identity = client().getIdentityConfiguration("acme", "android").identities?.get("swb_android")

        assertEquals("managedCookie", identity?.type)
        assertEquals("_swb", identity?.variable)
        assertNull("ttl is absent until supercargo emits it", identity?.ttl)
    }

    @Test
    fun unrecognisedInclude_yieldsNullRatherThanAnEmptyMap() = runBlocking {
        // The server answers 200 with the key simply absent, which must stay distinguishable
        // from a property that genuinely declares no identities.
        respond("""{"bogus":null}""")

        assertNull(client().getIdentityConfiguration("acme", "android").identities)
    }

    @Test
    fun declaredButEmpty_isAnEmptyMapNotNull() = runBlocking {
        respond("""{"identities":{}}""")

        val identities = client().getIdentityConfiguration("acme", "android").identities

        assertEquals(emptyMap<String, Any>(), identities)
    }

    @Test
    fun stringTtl_doesNotBreakTheWholeParse() = runBlocking {
        // Gson coerces a numeric string; the point is that one odd field cannot take out config.
        respond("""{"identities":{"swb_android":{"type":"queryString","ttl":"34560000"}}}""")

        val identity = client().getIdentityConfiguration("acme", "android").identities?.get("swb_android")

        assertTrue("identity should still parse", identity != null)
        assertEquals("queryString", identity?.type)
    }
}
