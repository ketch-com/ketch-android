package com.ketch.android.api

import com.ketch.android.data.Consent
import com.ketch.android.data.ConsentConfig
import com.ketch.android.data.ConsentUpdate
import com.ketch.android.data.HeadlessException
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.HttpURLConnection

class HeadlessConsentTest {
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

    @Test
    fun fetchConsentPropagatesHttpFailure() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR))

        val client = headlessClient()
        try {
            client.fetchConsent(sampleConsentConfig())
            fail("Expected fetchConsent to fail on HTTP 500")
        } catch (error: HeadlessException) {
            assertTrue(error.message?.contains("HTTP 500") == true)
        }
    }

    @Test
    fun setConsentPropagatesNetworkFailure() = runBlocking {
        val failingClient = OkHttpClient.Builder()
            .addInterceptor { throw IOException("not connected to internet") }
            .build()
        val client = HeadlessApiClient(
            dataCenter = KetchDataCenter.US,
            okHttpClient = failingClient,
        )

        try {
            client.setConsent(sampleConsentUpdate())
            fail("Expected setConsent to fail on network error")
        } catch (error: HeadlessException) {
            assertTrue(error.message?.contains("Network error") == true)
            assertTrue(error.cause != null)
        }
    }

    @Test
    fun fetchConsentReturnsEmptyConsentOn200NullBody() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody("null")
                .addHeader("Content-Type", "application/json"),
        )

        val client = headlessClient()
        val consent = client.fetchConsent(sampleConsentConfig())

        assertEquals(emptyMap<String, Boolean>(), consent.purposes)
        assertNull(consent.vendors)
        assertNull(consent.protocols)
    }

    @Test
    fun setConsentAcceptsProtocolsOnlyResponse() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody("""{"protocols":{"gpp":"DBABLA~BVQqAAAAAAJY.QA"}}""")
                .addHeader("Content-Type", "application/json"),
        )

        val client = headlessClient()
        val consent = client.setConsent(sampleConsentUpdate())

        assertNull(consent.purposes)
        assertEquals("DBABLA~BVQqAAAAAAJY.QA", consent.protocols?.get("gpp"))
    }

    private fun headlessClient(): HeadlessApiClient {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val redirected = original.newBuilder()
                    .url(
                        original.url.newBuilder()
                            .scheme("http")
                            .host(mockWebServer.hostName)
                            .port(mockWebServer.port)
                            .build(),
                    )
                    .build()
                chain.proceed(redirected)
            }
            .build()
        return HeadlessApiClient(
            dataCenter = KetchDataCenter.US,
            okHttpClient = okHttpClient,
        )
    }

    private fun sampleConsentConfig(): ConsentConfig =
        ConsentConfig(
            organizationCode = "org",
            propertyCode = "prop",
            environmentCode = "production",
            jurisdictionCode = "default",
            identities = mapOf("email" to "user@example.com"),
            purposes = mapOf(
                "analytics" to ConsentConfig.PurposeLegalBasis(legalBasisCode = "consent_optin"),
            ),
        )

    private fun sampleConsentUpdate(): ConsentUpdate =
        ConsentUpdate(
            organizationCode = "org",
            propertyCode = "prop",
            environmentCode = "production",
            identities = mapOf("email" to "user@example.com"),
            jurisdictionCode = "default",
            migrationOption = ConsentUpdate.MigrationOption.MIGRATE_DEFAULT,
            purposes = mapOf(
                "analytics" to ConsentUpdate.PurposeAllowedLegalBasis(
                    allowed = true,
                    legalBasisCode = "consent_optin",
                ),
            ),
            vendors = null,
            protocols = null,
        )
}
