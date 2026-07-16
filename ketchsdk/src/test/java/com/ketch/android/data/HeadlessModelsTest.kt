package com.ketch.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeadlessModelsTest {

    @Test
    fun toRegionCode_countryAndRegion() {
        val ipInfo = IPInfo(countryCode = "US", regionCode = "CA")
        assertEquals("US-CA", ipInfo.toRegionCode())
    }

    @Test
    fun toRegionCode_countryOnly() {
        val ipInfo = IPInfo(countryCode = "US", regionCode = null)
        assertEquals("US", ipInfo.toRegionCode())
    }

    @Test
    fun toRegionCode_countryOnly_blankRegion() {
        val ipInfo = IPInfo(countryCode = "US", regionCode = "")
        assertEquals("US", ipInfo.toRegionCode())
    }

    @Test
    fun toRegionCode_regionOnly_noCountry() {
        val ipInfo = IPInfo(countryCode = null, regionCode = "CA")
        assertEquals("CA", ipInfo.toRegionCode())
    }

    @Test
    fun toRegionCode_allBlank() {
        val ipInfo = IPInfo(countryCode = null, regionCode = null)
        assertNull(ipInfo.toRegionCode())
    }

    @Test
    fun toRegionCode_blankCountry_blankRegion() {
        val ipInfo = IPInfo(countryCode = "", regionCode = "")
        assertNull(ipInfo.toRegionCode())
    }

    @Test
    fun configPathSegment_allPresent() {
        val request = FullConfigurationRequest(
            organizationCode = "org",
            propertyCode = "prop",
            environmentCode = "production",
            jurisdictionCode = "us-ca",
            languageCode = "en-US",
        )
        assertEquals(Triple("production", "us-ca", "en-US"), request.configPathSegment())
    }

    @Test
    fun configPathSegment_nullEnvironment_isAbsent() {
        val request = FullConfigurationRequest(
            organizationCode = "org",
            propertyCode = "prop",
            environmentCode = null,
            jurisdictionCode = "us-ca",
            languageCode = "en-US",
        )
        assertNull(request.configPathSegment())
    }

    @Test
    fun configPathSegment_blankEnvironment_treatedSameAsNull() {
        val request = FullConfigurationRequest(
            organizationCode = "org",
            propertyCode = "prop",
            environmentCode = "",
            jurisdictionCode = "us-ca",
            languageCode = "en-US",
        )
        assertNull(request.configPathSegment())
    }

    @Test
    fun normalizedHash_blankIsNull() {
        assertNull(
            FullConfigurationRequest(organizationCode = "org", propertyCode = "prop", hash = "").normalizedHash(),
        )
        assertEquals(
            "abc123",
            FullConfigurationRequest(organizationCode = "org", propertyCode = "prop", hash = "abc123")
                .normalizedHash(),
        )
    }

    @Test
    fun jurisdictionCode_prefersSpecificOverDefault() {
        val config = HeadlessConfiguration(
            jurisdiction = ConfigurationJurisdiction(code = "us-ca", defaultJurisdictionCode = "us"),
        )
        assertEquals("us-ca", config.jurisdictionCode())
    }

    @Test
    fun jurisdictionCode_fallsBackToDefault_whenCodeAbsent() {
        val config = HeadlessConfiguration(
            jurisdiction = ConfigurationJurisdiction(code = null, defaultJurisdictionCode = "us"),
        )
        assertEquals("us", config.jurisdictionCode())
    }

    @Test
    fun jurisdictionCode_null_whenJurisdictionAbsent() {
        val config = HeadlessConfiguration(jurisdiction = null)
        assertNull(config.jurisdictionCode())
    }
}
