package com.ketch.android.api

import org.junit.Assert.assertEquals
import org.junit.Test

class HeadlessApiClientTest {
    private val client = HeadlessApiClient(KetchDataCenter.US)

    @Test
    fun buildUrl_ip() {
        assertEquals(
            "https://global.ketchcdn.com/web/v3/ip",
            client.buildUrl("/ip"),
        )
    }

    @Test
    fun buildUrl_bootstrap() {
        assertEquals(
            "https://global.ketchcdn.com/web/v3/config/acme/prop/boot.json",
            client.buildUrl("/config/acme/prop/boot.json"),
        )
    }

    @Test
    fun buildUrl_fullConfigurationWithHash() {
        assertEquals(
            "https://global.ketchcdn.com/web/v3/config/acme/prop/prod/us-ca/en-US/config.json?hash=8913461971881236311",
            client.buildUrl(
                "/config/acme/prop/prod/us-ca/en-US/config.json",
                mapOf("hash" to "8913461971881236311"),
            ),
        )
    }

    @Test
    fun buildUrl_euDataCenter() {
        val eu = HeadlessApiClient(KetchDataCenter.EU)
        assertEquals("https://eu.ketchcdn.com/web/v3/ip", eu.buildUrl("/ip"))
    }

    @Test
    fun getPreferenceQRUrl_matchesContractFixture() {
        val url = client.getPreferenceQRUrl(
            com.ketch.android.data.PreferenceQRRequest(
                organizationCode = "switchbitcorp",
                propertyCode = "switchbit",
                environmentCode = "production",
                imageSize = 1024,
                path = "/policy.html",
                backgroundColor = "white",
                foregroundColor = "black",
                parameters = mapOf("foo" to "bar"),
            ),
        )
        assertEquals(
            "https://global.ketchcdn.com/web/v3/qr/switchbitcorp/switchbit/preferences.png?env=production&size=1024&path=%2Fpolicy.html&bgcolor=white&fgcolor=black&foo=bar",
            url,
        )
    }

    @Test
    fun buildUrl_rightsSubscriptions() {
        assertEquals(
            "https://global.ketchcdn.com/web/v3/rights/switchbitcorp/invoke",
            client.buildUrl("/rights/switchbitcorp/invoke"),
        )
        assertEquals(
            "https://global.ketchcdn.com/web/v3/subscriptions/acme/update",
            client.buildUrl("/subscriptions/acme/update"),
        )
    }

    @Test
    fun ketchDataCenterBaseUrls() {
        assertEquals("https://global.ketchcdn.com/web/v3", KetchDataCenter.US.baseUrl)
        assertEquals("https://eu.ketchcdn.com/web/v3", KetchDataCenter.EU.baseUrl)
        assertEquals("https://dev.ketchcdn.com/web/v3", KetchDataCenter.UAT.baseUrl)
    }
}
