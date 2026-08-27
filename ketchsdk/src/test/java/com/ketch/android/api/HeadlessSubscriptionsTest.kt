package com.ketch.android.api

import com.ketch.android.data.SubscriptionStatus
import com.ketch.android.data.SubscriptionTopicContactMethodSetting
import com.ketch.android.data.SubscriptionsRequest
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Bodies are verbatim captures from web/v3, org ketch_samples, not hand-written fixtures. */
class HeadlessSubscriptionsTest {
    private lateinit var mockWebServer: MockWebServer

    @Before fun setUp() { mockWebServer = MockWebServer(); mockWebServer.start() }
    @After fun tearDown() { mockWebServer.shutdown() }

    /**
     * A two-level body was rejected with
     * `expected=subscriptions.SubscriptionTopicContactMethodSetting, got=string`.
     */
    @Test
    fun setSubscriptionsSendsThreeLevelTopics() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody("{}"))

        client().setSubscriptions(
            SubscriptionsRequest(
                organizationCode = "org",
                propertyCode = "prop",
                environmentCode = "production",
                identities = mapOf("email" to "user@example.com"),
                topics = mapOf(
                    "marketing_emails" to mapOf(
                        "email" to SubscriptionTopicContactMethodSetting(SubscriptionStatus.GRANTED),
                    ),
                ),
            ),
        )

        val body = mockWebServer.takeRequest().body.readUtf8()
        assertTrue(
            "topics must nest contact method under a status object, was: $body",
            body.contains("\"topics\":{\"marketing_emails\":{\"email\":{\"status\":\"granted\"}}}"),
        )
    }

    /** Decoding this response previously threw `Expected a string but was BEGIN_OBJECT`. */
    @Test
    fun getSubscriptionsDecodesThreeLevelTopics() = runBlocking {
        mockWebServer.enqueue(
            MockResponse().setBody(
                """{"propertyCode":"android",
                    "topics":{"marketing_emails":{"email":{"status":"granted"}}},
                    "controls":{}}""",
            ),
        )

        val response = client().getSubscriptions(
            SubscriptionsRequest(organizationCode = "org", propertyCode = "prop"),
        )

        assertEquals(
            SubscriptionStatus.GRANTED,
            response.topics?.get("marketing_emails")?.get("email")?.status,
        )
    }

    private fun client(): HeadlessApiClient {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                chain.proceed(
                    original.newBuilder().url(
                        original.url.newBuilder()
                            .scheme("http").host(mockWebServer.hostName).port(mockWebServer.port)
                            .build(),
                    ).build(),
                )
            }.build()
        return HeadlessApiClient(dataCenter = KetchDataCenter.US, okHttpClient = okHttpClient)
    }
}
