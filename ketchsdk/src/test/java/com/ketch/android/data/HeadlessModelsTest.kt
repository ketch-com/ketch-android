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
}
