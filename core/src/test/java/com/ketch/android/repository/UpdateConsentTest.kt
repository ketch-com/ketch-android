package com.ketch.android.repository

import com.ketch.android.api.KetchApi
import com.ketch.android.api.Repository
import com.ketch.android.api.request.MigrationOption
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
import org.junit.jupiter.api.Assertions.assertTrue
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.HttpURLConnection

class UpdateConsentTest {
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
    fun `updateConsent request`() {
        mockWebServer.apply {
            enqueue(
                MockResponse()
                    .setResponseCode(HttpURLConnection.HTTP_OK)
            )
        }

        runBlocking {
            consentUseCase.updateConsent(
                organization = ORGANIZATION,
                controller = null,
                property = PROPERTY,
                environment = ENVIRONMENT,
                jurisdiction = JURISDICTION,
                identities = emptyMap(),
                purposes = emptyMap(),
                migrationOption = MigrationOption.MIGRATE_NEVER,
                vendors = listOf()
            ).collect { result ->
                if (!(result is Result.Loading)) {
                    assertTrue(result is Result.Success, "Is the result successful?")
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