package com.ketch.android.integration.tests

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ketch.android.KetchSdk
import com.ketch.android.data.ConsentUpdate
import com.ketch.android.data.FullConfigurationRequest
import com.ketch.android.data.SubscriptionStatus
import com.ketch.android.data.SubscriptionTopicContactMethodSetting
import com.ketch.android.data.SubscriptionTopicSetting
import com.ketch.android.data.SubscriptionsRequest
import com.ketch.android.integration.tests.HeadlessIntegrationSupport.ORG_CODE
import com.ketch.android.integration.tests.HeadlessIntegrationSupport.PROPERTY
import com.ketch.android.integration.tests.HeadlessIntegrationSupport.ENVIRONMENT
import com.ketch.android.integration.tests.HeadlessIntegrationSupport.SUBSCRIPTION_CONTACT_METHOD
import com.ketch.android.integration.tests.HeadlessIntegrationSupport.SUBSCRIPTION_TOPIC
import com.ketch.android.integration.tests.HeadlessIntegrationSupport.awaitHeadless
import com.ketch.android.integration.tests.HeadlessIntegrationSupport.consentConfigFrom
import com.ketch.android.integration.tests.HeadlessIntegrationSupport.dataCenter
import com.ketch.android.integration.tests.HeadlessIntegrationSupport.uniqueEmailIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Live CDN headless round-trip tests (web/v3, sandbox org).
 * Requires network on emulator/device. Does not use WebView.
 */
@RunWith(AndroidJUnit4::class)
class KetchHeadlessIntegrationTest {

    @Test
    fun testGetRegionReturnsRegionCode() {
        val region = awaitHeadless { callback -> KetchSdk.getRegion(dataCenter, callback) }
        assertFalse("Expected a region code from CDN GeoIP lookup", region.isNullOrBlank())
    }

    @Test
    fun testGetJurisdictionReturnsJurisdictionCode() {
        val request = FullConfigurationRequest(organizationCode = ORG_CODE, propertyCode = PROPERTY)
        val jurisdiction = awaitHeadless { callback -> KetchSdk.getJurisdiction(request, dataCenter, callback) }
        assertFalse("Expected a jurisdiction code from CDN full config", jurisdiction.isNullOrBlank())
    }

    @Test
    fun testGetJurisdictionPrefersLocallySetValue() {
        val ketch = KetchSdk.create(
            context = ApplicationProvider.getApplicationContext(),
            organization = ORG_CODE,
            property = PROPERTY,
            environment = ENVIRONMENT,
        )
        ketch.setJurisdiction("locally_set_jurisdiction")

        val jurisdiction = awaitHeadless { callback -> ketch.getJurisdiction(callback) }

        assertEquals("locally_set_jurisdiction", jurisdiction)
    }

    @Test
    fun testGetRegionPrefersLocallySetValue() {
        val ketch = KetchSdk.create(
            context = ApplicationProvider.getApplicationContext(),
            organization = ORG_CODE,
            property = PROPERTY,
            environment = ENVIRONMENT,
        )
        ketch.setRegion("ZZ-FAKE")

        val region = awaitHeadless { callback -> ketch.getRegion(callback) }

        assertEquals("ZZ-FAKE", region)
    }

    @Test
    fun testGetBootstrapConfiguration() {
        val boot = awaitHeadless { callback ->
            KetchSdk.getBootstrapConfiguration(ORG_CODE, PROPERTY, dataCenter, callback)
        }
        assertTrue(
            "Bootstrap should include experiences metadata",
            boot.experiences != null || boot.jurisdiction != null || !boot.purposes.isNullOrEmpty(),
        )
    }

    @Test
    fun testHeadlessColdStartConsentRoundTrip() {
        val identities = uniqueEmailIdentity()

        awaitHeadless { callback -> KetchSdk.getRegion(dataCenter, callback) }

        val boot = awaitHeadless { callback ->
            KetchSdk.getBootstrapConfiguration(ORG_CODE, PROPERTY, dataCenter, callback)
        }

        val fullConfig = awaitHeadless { callback ->
            KetchSdk.getFullConfiguration(
                FullConfigurationRequest(
                    organizationCode = ORG_CODE,
                    propertyCode = PROPERTY,
                ),
                dataCenter,
                callback,
            )
        }

        val consentConfig = consentConfigFrom(fullConfig, identities)

        val consent = awaitHeadless { callback ->
            KetchSdk.getConsent(consentConfig, dataCenter, callback)
        }
        assertTrue(
            "CDN consent get should return protocols and/or purposes",
            !consent.protocols.isNullOrEmpty() || !consent.purposes.isNullOrEmpty(),
        )

        val purposeCode = consentConfig.purposes.keys.first()
        val legalBasis = consentConfig.purposes[purposeCode]!!

        val update = ConsentUpdate(
            organizationCode = ORG_CODE,
            propertyCode = PROPERTY,
            environmentCode = ENVIRONMENT,
            identities = identities,
            jurisdictionCode = consentConfig.jurisdictionCode,
            migrationOption = ConsentUpdate.MigrationOption.MIGRATE_DEFAULT,
            purposes = mapOf(
                purposeCode to ConsentUpdate.PurposeAllowedLegalBasis(
                    allowed = true,
                    legalBasisCode = legalBasis.legalBasisCode,
                ),
            ),
        )

        val updated = awaitHeadless { callback ->
            KetchSdk.setConsent(update, dataCenter, callback)
        }
        assertNotNull("setConsent should return purposes", updated.purposes)
        assertTrue(
            "setConsent should include updated purpose",
            updated.purposes!!.containsKey(purposeCode),
        )
    }

    private fun subscriptionsRequest(
        identities: Map<String, String>,
        topics: Map<String, SubscriptionTopicSetting>? = null,
    ) = SubscriptionsRequest(
        organizationCode = ORG_CODE,
        propertyCode = PROPERTY,
        environmentCode = ENVIRONMENT,
        identities = identities,
        topics = topics,
    )

    private fun topicSetting(status: SubscriptionStatus) =
        mapOf(SUBSCRIPTION_TOPIC to mapOf(SUBSCRIPTION_CONTACT_METHOD to SubscriptionTopicContactMethodSetting(status)))

    /**
     * The get response carries no organizationCode and marks nothing required, so ordinary data
     * class use must not throw.
     */
    @Test
    fun testGetSubscriptionsForFreshIdentity() {
        val identities = uniqueEmailIdentity()

        val response = awaitHeadless { callback ->
            KetchSdk.getSubscriptions(subscriptionsRequest(identities), dataCenter, callback)
        }

        assertNotNull("Expected a topics map, even when empty", response.topics)
        assertEquals("Response must survive copy()", response, response.copy())
        assertEquals("Response must survive hashCode()", response.hashCode(), response.copy().hashCode())
    }

    /**
     * Exercises the three-level topic shape end to end. A two-level body was rejected with HTTP
     * 400, and decoding a populated response threw before the topic types were corrected.
     *
     * Each status uses its own fresh identity rather than flipping one. Flipping is subject to a
     * read-after-write race on this endpoint: a status change is not reliably visible to the next
     * get, and a repeated identical write is sometimes needed before it lands. Asserting a flip
     * here would be flaky for reasons unrelated to the type shape under test.
     */
    @Test
    fun testSubscriptionsRoundTrip() {
        for (status in listOf(SubscriptionStatus.GRANTED, SubscriptionStatus.DENIED)) {
            val identities = uniqueEmailIdentity()

            awaitHeadless<Unit> { callback ->
                KetchSdk.setSubscriptions(
                    subscriptionsRequest(identities, topicSetting(status)),
                    dataCenter,
                    callback,
                )
            }

            val response = awaitHeadless { callback ->
                KetchSdk.getSubscriptions(subscriptionsRequest(identities), dataCenter, callback)
            }

            assertEquals(
                "Topic should read back as $status",
                status,
                response.topics?.get(SUBSCRIPTION_TOPIC)?.get(SUBSCRIPTION_CONTACT_METHOD)?.status,
            )
        }
    }
}
