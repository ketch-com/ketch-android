package com.ketch.android.integration.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ketch.android.KetchSdk
import com.ketch.android.data.ConsentUpdate
import com.ketch.android.data.FullConfigurationRequest
import com.ketch.android.integration.tests.HeadlessIntegrationSupport.ORG_CODE
import com.ketch.android.integration.tests.HeadlessIntegrationSupport.PROPERTY
import com.ketch.android.integration.tests.HeadlessIntegrationSupport.ENVIRONMENT
import com.ketch.android.integration.tests.HeadlessIntegrationSupport.awaitHeadless
import com.ketch.android.integration.tests.HeadlessIntegrationSupport.consentConfigFrom
import com.ketch.android.integration.tests.HeadlessIntegrationSupport.dataCenter
import com.ketch.android.integration.tests.HeadlessIntegrationSupport.uniqueEmailIdentity
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
    fun testFetchLocationReturnsGeoIP() {
        val location = awaitHeadless { callback ->
            KetchSdk.fetchLocation(dataCenter, callback)
        }
        assertNotNull("Expected location payload", location.location)
        assertFalse(
            "Expected countryCode from CDN",
            location.location?.countryCode.isNullOrBlank(),
        )
    }

    @Test
    fun testFetchBootstrapConfiguration() {
        val boot = awaitHeadless { callback ->
            KetchSdk.fetchBootstrapConfiguration(ORG_CODE, PROPERTY, dataCenter, callback)
        }
        assertTrue(
            "Bootstrap should include experiences metadata",
            boot.experiences != null || boot.jurisdiction != null || !boot.purposes.isNullOrEmpty(),
        )
    }

    @Test
    fun testHeadlessColdStartConsentRoundTrip() {
        val identities = uniqueEmailIdentity()

        awaitHeadless { callback ->
            KetchSdk.fetchLocation(dataCenter, callback)
        }

        val boot = awaitHeadless { callback ->
            KetchSdk.fetchBootstrapConfiguration(ORG_CODE, PROPERTY, dataCenter, callback)
        }

        val fullConfig = awaitHeadless { callback ->
            KetchSdk.fetchFullConfiguration(
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
            KetchSdk.fetchConsent(consentConfig, dataCenter, callback)
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
}
