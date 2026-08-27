package com.ketch.android.api

import com.google.gson.Gson
import com.ketch.android.data.Consent
import com.ketch.android.data.ConsentConfig
import com.ketch.android.data.ConsentUpdate
import com.ketch.android.data.VendorConsents
import com.ketch.android.data.VendorStatus
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    /** The deprecated `vendors` list is opt-out only, so grants can only travel here. */
    @Test
    fun setConsentSendsVendorConsentsToTheWire() = runBlocking {
        mockWebServer.enqueue(MockResponse().setBody(UPDATE_BODY))

        client().setConsent(
            ConsentUpdate(
                organizationCode = "org",
                propertyCode = "prop",
                environmentCode = "production",
                identities = mapOf("aaid" to "hl-vendor-003"),
                jurisdictionCode = "california",
                migrationOption = ConsentUpdate.MigrationOption.MIGRATE_DEFAULT,
                purposes = mapOf(
                    "marketing" to ConsentUpdate.PurposeAllowedLegalBasis(
                        allowed = true, legalBasisCode = "consent_optin",
                    ),
                ),
                vendorConsents = VendorConsents(
                    tcf = mapOf("747" to VendorStatus.GRANTED, "755" to VendorStatus.DENIED),
                ),
            ),
        )

        val body = mockWebServer.takeRequest().body.readUtf8()
        assertTrue(
            "a vendor grant must reach the wire, was: $body",
            body.contains("\"vendorConsents\":{\"tcf\":{\"747\":\"granted\",\"755\":\"denied\"}}"),
        )
    }

    /** A vendorConsents-only body must not be discarded and replaced with empty consent. */
    @Test
    fun getConsentKeepsVendorConsentsOnlyResponse() = runBlocking {
        mockWebServer.enqueue(
            MockResponse().setBody("""{"vendorConsents":{"tcf":{"747":"granted"}}}"""),
        )

        val consent = client().getConsent(
            ConsentConfig(
                organizationCode = "org",
                propertyCode = "prop",
                environmentCode = "production",
                jurisdictionCode = "california",
                identities = mapOf("aaid" to "hl-vendor-003"),
                purposes = emptyMap(),
            ),
        )

        assertEquals(VendorStatus.GRANTED, consent.vendorConsents?.tcf?.get("747"))
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
