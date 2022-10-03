package com.ketch.android.repository

import com.ketch.android.MockResponseFileReader
import com.ketch.android.api.KetchApi
import com.ketch.android.api.Repository
import com.ketch.android.api.response.Consent
import com.ketch.android.api.response.Result
import com.ketch.android.usecase.ConsentUseCase
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.HttpURLConnection

class GetConsentTest {
    private lateinit var mockWebServer: MockWebServer

    private lateinit var consentUseCase: ConsentUseCase

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        val ketchApi = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .addInterceptor {
                    val requestBuilder = it.request().newBuilder()
                    requestBuilder.addHeader("accept", "application/json")
                    it.proceed(requestBuilder.build())
                }
                .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KetchApi::class.java)

        val repository = Repository(ketchApi)
        consentUseCase = ConsentUseCase(repository)
    }

    @After
    fun shutdown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getConsent API request`() {
        mockWebServer.apply {
            enqueue(
                MockResponse()
                    .setResponseCode(HttpURLConnection.HTTP_OK)
                    .setBody(MockResponseFileReader("get_consent.json").content)
            )
        }

        runBlocking {
            consentUseCase.getConsent(
                organization = ORGANIZATION,
                controller = null,
                property = PROPERTY,
                environment = ENVIRONMENT,
                jurisdiction = JURISDICTION,
                identities = emptyMap(),
                purposes = emptyMap()
            ).collect { result ->
                if (!(result is Result.Loading)) {
                    assertTrue(result is Result.Success, "Is the result successful?")

                    val data = (result as Result.Success).data

                    val consent = Consent(
                        purposes = mapOf(
                            Pair("analytics", "true"),
                            Pair("behavioral_advertising", "true"),
                            Pair("data_broking", "true"),
                            Pair("email_marketing", "true"),
                            Pair("essential_services", "true"),
                            Pair("somepurpose_key", "true"),
                            Pair("tcf.purpose_1", "true")
                        ),
                        vendors = null
                    )

                    assertEquals(consent.purposes, data.purposes)
                    assertEquals(consent.vendors, data.vendors)

                    assertEquals(consent, data)
                }
            }
        }
    }

    companion object {
        private const val ORGANIZATION = "transcenda"
        private const val PROPERTY = "android_prop"
        private const val ENVIRONMENT = "stage"
        private const val JURISDICTION = "default"
    }
}