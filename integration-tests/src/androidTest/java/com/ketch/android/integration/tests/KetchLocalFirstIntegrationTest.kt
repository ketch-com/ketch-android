package com.ketch.android.integration.tests

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ketch.android.Ketch
import com.ketch.android.KetchSdk
import com.ketch.android.integration.tests.HeadlessIntegrationSupport.ENVIRONMENT
import com.ketch.android.integration.tests.HeadlessIntegrationSupport.ORG_CODE
import com.ketch.android.integration.tests.HeadlessIntegrationSupport.PROPERTY
import com.ketch.android.integration.tests.HeadlessIntegrationSupport.awaitHeadless
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A locally-set jurisdiction/region (via setJurisdiction/setRegion) must win over the
 * server-resolved value for the matching headless getter, mirroring ketch-tag's getter/setter
 * model. Requires network on emulator/device.
 */
@RunWith(AndroidJUnit4::class)
class KetchLocalFirstIntegrationTest {

    private fun newKetch(): Ketch =
        KetchSdk.create(
            context = ApplicationProvider.getApplicationContext(),
            organization = ORG_CODE,
            property = PROPERTY,
            environment = ENVIRONMENT,
        )

    @Test
    fun getJurisdiction_returnsLocallySetValue_withoutServerOverride() {
        val ketch = newKetch()
        ketch.setJurisdiction("locally_set_jurisdiction")

        val jurisdiction = awaitHeadless { callback -> ketch.getJurisdiction(callback) }

        assertEquals("locally_set_jurisdiction", jurisdiction)
    }

    @Test
    fun getJurisdiction_fallsBackToServer_whenNoneSetLocally() {
        val ketch = newKetch()

        val jurisdiction = awaitHeadless { callback -> ketch.getJurisdiction(callback) }

        assertFalse("Expected a server-resolved jurisdiction code", jurisdiction.isNullOrBlank())
    }

    @Test
    fun getRegion_returnsLocallySetValue_withoutServerOverride() {
        val ketch = newKetch()
        ketch.setRegion("ZZ-FAKE")

        val region = awaitHeadless { callback -> ketch.getRegion(callback) }

        assertEquals("ZZ-FAKE", region)
    }

    @Test
    fun getRegion_fallsBackToServer_whenNoneSetLocally() {
        val ketch = newKetch()

        val region = awaitHeadless { callback -> ketch.getRegion(callback) }

        assertFalse("Expected a GeoIP-resolved region code", region.isNullOrBlank())
    }
}
