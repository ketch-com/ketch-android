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

/**
 * Regression coverage for the actual URL getFullConfiguration() requests: a blank
 * environmentCode/jurisdictionCode/languageCode/hash must be treated the same as a null one
 * (matching ketch-tag), since Ketch's config cache key relies on this to never disagree with
 * the real request the HTTP client makes.
 */
class HeadlessFullConfigurationPathTest {
    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setUp() {
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
    fun nullEnvironment_omitsSegmentEntirely() = runBlocking {
        client().getFullConfiguration(
            FullConfigurationRequest(
                organizationCode = "org",
                propertyCode = "prop",
                environmentCode = null,
                jurisdictionCode = "us-ca",
                languageCode = "en-US",
            ),
        )
        assertEquals("/web/v3/config/org/prop/config.json", mockWebServer.takeRequest().path)
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
        assertEquals("/web/v3/config/org/prop/config.json", mockWebServer.takeRequest().path)
    }

    @Test
    fun blankHash_omitsHashQueryParam() = runBlocking {
        client().getFullConfiguration(
            FullConfigurationRequest(organizationCode = "org", propertyCode = "prop", hash = ""),
        )
        assertEquals("/web/v3/config/org/prop/config.json", mockWebServer.takeRequest().path)
    }

    @Test
    fun nonBlankHash_includesHashQueryParam() = runBlocking {
        client().getFullConfiguration(
            FullConfigurationRequest(organizationCode = "org", propertyCode = "prop", hash = "abc123"),
        )
        assertEquals("/web/v3/config/org/prop/config.json?hash=abc123", mockWebServer.takeRequest().path)
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
