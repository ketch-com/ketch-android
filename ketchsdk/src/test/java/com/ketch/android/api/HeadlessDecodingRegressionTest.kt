package com.ketch.android.api

import com.google.gson.Gson
import com.ketch.android.data.Consent
import com.ketch.android.data.ConsentUpdate
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Response bodies here are verbatim captures from web/v3, org ketch_samples / jurisdiction
 * california, not hand-written fixtures.
 */
class HeadlessDecodingRegressionTest {

    private companion object {
        /** `POST /consent/{org}/update` — purposes are objects carrying the value under `allowed`. */
        const val UPDATE_BODY = """
            {"controllerCode":"","environmentCode":"production","identities":{"aaid":"hl-regress-001"},
             "jurisdictionCode":"california","propertyCode":"android","protocols":{},
             "purposes":{"analytics_900":{"allowed":"false","collectedAt":0,"issuedAt":0,
             "legalBasisCode":"consent_optout","source":""}}}
        """

        /** `POST /consent/{org}/get` — purposes are stringified booleans. A different shape. */
        const val GET_BODY = """
            {"collectedAt":1787795557,"environmentCode":"production","jurisdictionCode":"california",
             "propertyCode":"android","protocols":{"usps":"1---"},
             "purposes":{"analytics_900":"false","data_broking":"true"}}
        """
    }

    private lateinit var mockWebServer: MockWebServer

    @Before fun setUp() { mockWebServer = MockWebServer(); mockWebServer.start() }
    @After fun tearDown() { mockWebServer.shutdown() }

    /** The get path already worked; this guards against the adapter regressing it. */
    @Test
    fun getShapeDecodesStringifiedBooleans() {
        val consent = Gson().fromJson(GET_BODY, Consent::class.java)
        assertEquals(mapOf("analytics_900" to false, "data_broking" to true), consent.purposes)
    }

    @Test
    fun updateShapeDecodesObjectPurposes() {
        val consent = Gson().fromJson(UPDATE_BODY, Consent::class.java)
        assertEquals(mapOf("analytics_900" to false), consent.purposes)
    }

    /**
     * The one that matters: we send allowed=true, the server replies allowed=false. Before the
     * adapter the caller was told true — its own request, echoed back.
     */
    @Test
    fun setConsentReportsServerValueNotRequestEcho() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody(UPDATE_BODY))

        val consent = client().setConsent(
            ConsentUpdate(
                organizationCode = "org",
                propertyCode = "prop",
                environmentCode = "production",
                identities = mapOf("aaid" to "hl-regress-001"),
                jurisdictionCode = "california",
                migrationOption = ConsentUpdate.MigrationOption.MIGRATE_DEFAULT,
                purposes = mapOf(
                    "analytics_900" to ConsentUpdate.PurposeAllowedLegalBasis(
                        allowed = true, legalBasisCode = "consent_optout",
                    ),
                ),
            ),
        )

        assertEquals(mapOf("analytics_900" to false), consent.purposes)
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
