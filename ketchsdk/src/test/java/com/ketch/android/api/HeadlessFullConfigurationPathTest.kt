package com.ketch.android.api

import com.ketch.android.data.FullConfigurationRequest
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.net.HttpURLConnection
import java.util.Locale

/**
 * Regression coverage for the actual URL getFullConfiguration() requests, including device-locale
 * language defaulting on the short path — the JVM default locale is pinned per test.
 */
class HeadlessFullConfigurationPathTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("en-US"))
        mockWebServer = MockWebServer()
        mockWebServer.start()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody("{}")
                .addHeader("Content-Type", "application/json"),
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        Locale.setDefault(originalLocale)
    }

    @Test
    fun allFieldsPresent_includesEnvJurisdictionLanguage() = runBlocking {
        client().getFullConfiguration(
            FullConfigurationRequest(
                organizationCode = "org",
                propertyCode = "prop",
                environmentCode = "production",
                jurisdictionCode = "us-ca",
                languageCode = "en-US",
            ),
        )
        assertEquals("/web/v3/config/org/prop/production/us-ca/en-US/config.json", mockWebServer.takeRequest().path)
    }

    @Test
    fun nullEnvironment_omitsSegmentEntirely_includesDefaultedLanguage() = runBlocking {
        client().getFullConfiguration(
            FullConfigurationRequest(
                organizationCode = "org",
                propertyCode = "prop",
                environmentCode = null,
                jurisdictionCode = "us-ca",
                languageCode = "en-US",
            ),
        )
        assertEquals(
            "/web/v3/config/org/prop/config.json?language=en-US&jurisdiction=us-ca",
            mockWebServer.takeRequest().path,
        )
    }

    @Test
    fun blankEnvironment_treatedSameAsNull_omitsSegmentEntirely() = runBlocking {
        client().getFullConfiguration(
            FullConfigurationRequest(
                organizationCode = "org",
                propertyCode = "prop",
                environmentCode = "",
                jurisdictionCode = "us-ca",
                languageCode = "en-US",
            ),
        )
        assertEquals(
            "/web/v3/config/org/prop/config.json?language=en-US&jurisdiction=us-ca",
            mockWebServer.takeRequest().path,
        )
    }

    @Test
    fun blankHash_omitsHashQueryParam() = runBlocking {
        client().getFullConfiguration(
            FullConfigurationRequest(organizationCode = "org", propertyCode = "prop", hash = ""),
        )
        assertEquals("/web/v3/config/org/prop/config.json?language=en-US", mockWebServer.takeRequest().path)
    }

    @Test
    fun nonBlankHash_includesHashQueryParam() = runBlocking {
        client().getFullConfiguration(
            FullConfigurationRequest(organizationCode = "org", propertyCode = "prop", hash = "abc123"),
        )
        assertEquals(
            "/web/v3/config/org/prop/config.json?language=en-US&hash=abc123",
            mockWebServer.takeRequest().path,
        )
    }

    @Test
    fun nothingSet_shortPath_includesDeviceLanguage() = runBlocking {
        client().getFullConfiguration(
            FullConfigurationRequest(organizationCode = "org", propertyCode = "prop"),
        )
        assertEquals("/web/v3/config/org/prop/config.json?language=en-US", mockWebServer.takeRequest().path)
    }

    @Test
    fun jurisdictionOnly_shortPath_includesJurisdictionAndDeviceLanguage() = runBlocking {
        client().getFullConfiguration(
            FullConfigurationRequest(organizationCode = "org", propertyCode = "prop", jurisdictionCode = "us-ca"),
        )
        assertEquals(
            "/web/v3/config/org/prop/config.json?language=en-US&jurisdiction=us-ca",
            mockWebServer.takeRequest().path,
        )
    }

    @Test
    fun regionOnly_shortPath_includesRegionAndDeviceLanguage() = runBlocking {
        client().getFullConfiguration(
            FullConfigurationRequest(organizationCode = "org", propertyCode = "prop", regionCode = "US-CA"),
        )
        assertEquals(
            "/web/v3/config/org/prop/config.json?language=en-US&region=US-CA",
            mockWebServer.takeRequest().path,
        )
    }

    @Test
    fun explicitLanguage_winsOverDeviceLocale() = runBlocking {
        client().getFullConfiguration(
            FullConfigurationRequest(organizationCode = "org", propertyCode = "prop", languageCode = "de-DE"),
        )
        assertEquals("/web/v3/config/org/prop/config.json?language=de-DE", mockWebServer.takeRequest().path)
    }

    @Test
    fun shortPath_includesAcceptLanguageHeaderFromDeviceLocale() = runBlocking {
        client().getFullConfiguration(
            FullConfigurationRequest(organizationCode = "org", propertyCode = "prop"),
        )
        assertEquals("en-US", mockWebServer.takeRequest().getHeader("Accept-Language"))
    }

    private fun client(): HeadlessApiClient {
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
        return HeadlessApiClient(dataCenter = KetchDataCenter.US, okHttpClient = okHttpClient)
    }
}
