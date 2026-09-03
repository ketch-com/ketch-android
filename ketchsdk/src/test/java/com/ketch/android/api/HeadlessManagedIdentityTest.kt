package com.ketch.android.api

import com.ketch.android.KetchSharedPreferences
import com.ketch.android.ManagedIdentity
import com.ketch.android.MemorySharedPreferences
import com.ketch.android.data.ConsentConfig
import com.ketch.android.data.SubscriptionsRequest
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.HttpURLConnection

/**
 * The managed identifier must reach headless request bodies once config says `queryString`, and
 * must be completely absent while config still says `managedCookie` — that inertness is what lets
 * this ship before the backend change.
 */
class HeadlessManagedIdentityTest {
    private lateinit var server: MockWebServer
    private lateinit var prefs: MemorySharedPreferences

    private val liveManagedCookieConfig =
        """{"identities":{"swb_android":{"type":"managedCookie","variable":"_swb"}}}"""
    private val activatedConfig =
        """{"identities":{"swb_android":{"type":"queryString","variable":"swb_android"}}}"""

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        prefs = MemorySharedPreferences()
        KetchSharedPreferences.bindPreferencesForTesting(prefs)
        ManagedIdentity.resetForTesting()
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

    private fun enqueue(body: String) {
        server.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(body)
                .addHeader("Content-Type", "application/json"),
        )
    }

    private fun config(identities: Map<String, String>) = ConsentConfig(
        organizationCode = "acme",
        propertyCode = "android",
        environmentCode = "production",
        jurisdictionCode = "default",
        identities = identities,
        purposes = emptyMap(),
    )

    @Suppress("UNCHECKED_CAST")
    private fun sentIdentities(): Map<String, String> {
        server.takeRequest() // the identity config fetch
        val body = server.takeRequest().body.readUtf8()
        val parsed = Gson().fromJson(body, Map::class.java) as Map<String, Any>
        return (parsed["identities"] as Map<String, String>)
    }

    @Test
    fun managedCookieConfig_sendsTheCallersIdentitiesUnchanged() = runBlocking {
        enqueue(liveManagedCookieConfig)
        enqueue("""{"purposes":{}}""")
        val callerIdentities = mapOf("email" to "user@example.com")

        client().getConsent(config(callerIdentities))

        assertEquals(callerIdentities, sentIdentities())
    }

    @Test
    fun managedCookieConfig_mintsNothingAtAll() = runBlocking {
        enqueue(liveManagedCookieConfig)
        enqueue("""{"purposes":{}}""")

        client().getConsent(config(mapOf("email" to "user@example.com")))

        // A value minted but withheld would still be a behaviour change, and would surface
        // through getIdentities().
        assertTrue("no managed identifier may be stored", prefs.all.isEmpty())
    }

    @Test
    fun queryStringConfig_addsTheIdentifierUnderTheSpaceCode() = runBlocking {
        enqueue(activatedConfig)
        enqueue("""{"purposes":{}}""")

        client().getConsent(config(mapOf("email" to "user@example.com")))

        val sent = sentIdentities()
        assertEquals("user@example.com", sent["email"])
        assertEquals(prefs.getString(ManagedIdentity.STORAGE_KEY, null), sent["swb_android"])
    }

    @Test
    fun headlessUsesTheSpaceCodeEvenWhenTheVariableDiffers() = runBlocking {
        // The variable names a WebView query parameter; headless bodies are keyed by space code.
        enqueue("""{"identities":{"swb_android":{"type":"queryString","variable":"_swb"}}}""")
        enqueue("""{"purposes":{}}""")

        client().getConsent(config(emptyMap()))

        val sent = sentIdentities()
        assertTrue("must be keyed by space code", sent.containsKey("swb_android"))
        assertTrue("must not be keyed by the query-string variable", !sent.containsKey("_swb"))
    }

    @Test
    fun callerSuppliedValueWinsOnCollision() = runBlocking {
        enqueue(activatedConfig)
        enqueue("""{"purposes":{}}""")

        client().getConsent(config(mapOf("swb_android" to "caller-owned")))

        assertEquals("caller-owned", sentIdentities()["swb_android"])
    }

    @Test
    fun requestWithoutPropertyCode_usesTheAlreadyResolvedIdentifier() = runBlocking {
        // A real resolve first, so lastResolved() has something to fall back to.
        enqueue(activatedConfig)
        enqueue("""{"purposes":{}}""")
        client().getConsent(config(emptyMap()))
        server.takeRequest()
        server.takeRequest()
        val minted = prefs.getString(ManagedIdentity.STORAGE_KEY, null)

        // SubscriptionsRequest is the one request type whose propertyCode is nullable, so it
        // cannot name a space to look up.
        enqueue("""{"identities":{}}""")
        client().getSubscriptions(SubscriptionsRequest(organizationCode = "acme"))

        val body = server.takeRequest().body.readUtf8()
        assertTrue(
            "a missing property code must not drop an already-resolved identifier",
            body.contains(minted!!),
        )
    }
}
