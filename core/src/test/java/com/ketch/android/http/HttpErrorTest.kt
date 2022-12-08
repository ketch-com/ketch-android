package com.ketch.android.http

import com.ketch.android.MockResponseFileReader
import com.ketch.android.api.KetchApi
import com.ketch.android.api.Repository
import com.ketch.android.api.response.ApiError
import com.ketch.android.api.response.ErrorResult
import com.ketch.android.api.response.Result
import com.ketch.android.usecase.OrganizationConfigUseCase
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.HttpURLConnection

private const val ERROR_FILE_NAME = "error.json"

class HttpErrorTest {
    private lateinit var mockWebServer: MockWebServer
    internal lateinit var organizationConfigUseCase: OrganizationConfigUseCase

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
        organizationConfigUseCase = OrganizationConfigUseCase(repository)
    }

    @After
    fun shutdown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `test error responces`() {
        mockWebServer.apply {
            enqueue(
                MockResponse()
                    .setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
                    .setBody(MockResponseFileReader(ERROR_FILE_NAME).content)
            )
        }

        runBlocking {
            organizationConfigUseCase.getFullConfiguration(
                organization = ORGANIZATION,
                property = PROPERTY,
                environment = ENVIRONMENT,
                hash = System.currentTimeMillis(),
                jurisdiction = JURISDICTION,
                language = LANGUAGE
            ).collect { result ->
                if (!(result is Result.Loading)) {
                    assertTrue(result is Result.Error)
                    assertEquals(
                        Result.Error<Nothing>(
                            ErrorResult.HttpError(
                                error = ApiError(
                                    code = "1",
                                    errMessage = "Internal Server Error",
                                    status = 500
                                )
                            )
                        ), result
                    )
                }
            }
        }
    }

    @Test
    fun `test IoError Offline`() {
        mockWebServer.apply {
            enqueue(
                MockResponse()
                    .setSocketPolicy(SocketPolicy.NO_RESPONSE)
            )
        }

        runBlocking {
            organizationConfigUseCase.getFullConfiguration(
                organization = ORGANIZATION,
                property = PROPERTY,
                environment = ENVIRONMENT,
                hash = System.currentTimeMillis(),
                jurisdiction = JURISDICTION,
                language = LANGUAGE
            ).collect { result ->
                if (!(result is Result.Loading)) {
                    assertTrue(result is Result.Error)
                    assertEquals(Result.Error<Nothing>(ErrorResult.Offline), result)
                }
            }
        }
    }

    @Test
    fun `test Other Error`() {
        mockWebServer.apply {
            enqueue(
                MockResponse()
                    .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
            )
        }

        runBlocking {
            organizationConfigUseCase.getFullConfiguration(
                organization = ORGANIZATION,
                property = PROPERTY,
                environment = ENVIRONMENT,
                hash = System.currentTimeMillis(),
                jurisdiction = JURISDICTION,
                language = LANGUAGE
            ).collect { result ->
                if (!(result is Result.Loading)) {
                    assertTrue(result is Result.Error)
                    assertTrue((result as Result.Error).error is ErrorResult.OtherError)
                }
            }
        }
    }

    companion object {
        private const val ORGANIZATION = "transcenda"
        private const val PROPERTY = "android_prop"
        private const val ENVIRONMENT = "stage"
        private const val JURISDICTION = "default"
        private const val LANGUAGE = "en-US"
    }
}