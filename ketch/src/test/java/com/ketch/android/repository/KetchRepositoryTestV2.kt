package com.ketch.android.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.ketch.android.api.*
import com.ketch.android.api.model.Configuration
import com.ketch.android.api.model.ConfigurationV2
import com.ketch.android.cache.SharedPreferencesCacheProvider
import com.ketch.android.loadFromFile
import com.ketch.android.mock.MockResponses.Companion.mockGetConfigurationResponse
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.android.AndroidChannelBuilder
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.collect
import mobile.MobileGrpc
import java.io.IOException

class KetchRepositoryTestV2 : DescribeSpec() {

    private val cacheProvider = mockk<SharedPreferencesCacheProvider>(relaxed = true)
    private val client = mockk<MobileClient>(relaxed = true)
    val context = mockk<Context>(relaxed = true)
    val channel = mockk<ManagedChannel>(relaxed = true)
    val stub = mockk<MobileGrpc.MobileBlockingStub>(relaxed = true)
    val androidChannelBuilder = mockk<AndroidChannelBuilder>(relaxed = true)

    init {

        println(mockGetConfigurationResponse)
        val repo = KetchRepository("", "", context, cacheProvider, KetchApiClient())
        every { cacheProvider.generateSalt(*anyVararg()) } returns ""

        describe("test getFullConfiguration") {
            val configProto = mockGetConfigurationResponse
            val config = Gson().fromJson(loadJsonFromFile("config.json"), ConfigurationV2::class.java)
            val cachedConfig =
                Gson().fromJson(loadJsonFromFile("config_cache.json"), ConfigurationV2::class.java)

            mockkStatic(Base64::class.java.name)
            mockkStatic(Log::class.java.name)
            mockkStatic(AndroidChannelBuilder::class.java.name)
            mockkStatic(MobileGrpc::class.java.name)

            every { Base64.decode(any<ByteArray>(), any()) } returns "/".toByteArray()
            every { Base64.decode(any<String>(), any()) } returns "/".toByteArray()
            every { Log.d(any<String>(), any()) } returns 0
            every { Log.e(any<String>(), any()) } returns 0
            every {
                AndroidChannelBuilder.forAddress(any<String>(), any()).context(any()).build()
            } returns channel
            every { MobileGrpc.newBlockingStub(any()).withInterceptors(any()) } returns stub

            context("receive a proper response") {
                every { cacheProvider.obtain(any()) } returns null

                coEvery {
                    stub.getConfiguration(any())
                } returns configProto
                var result: Result<RequestError, ConfigurationV2>? = null
                repo.getConfigurationProto("", "", "")
                    .collect {
                        result = it
                    }
                println(">>!" + result)
                it("result should not be null") {
                    result shouldNotBe null
                }
                it("result should be successful, result $result") {
                    assert(result is Result.Success)
                }
                it("result value should not be null") {
                    (result as Result.Success).value shouldNotBe null
                }
                it("result value should not be cached") {
                    (result as Result.Success).value.isCached() shouldBe false
                }
                it("result value should be expected") {
                    (result as Result.Success).value shouldBe config
                }
            }

            context("receive failed response but has cache") {
                every { cacheProvider.obtain(any()) } returns loadJsonFromFile("config_cache.json")

                coEvery {
                    stub.getConfiguration(any())
                } throws IOException("Not available")
                var result: Result<RequestError, ConfigurationV2>? = null
                repo.getConfigurationProto("", "", "")
                    .collect {
                        result = it
                    }
                it("result should not be null") {
                    result shouldNotBe null
                }
                it("result should be successful") {
                    assert(result is Result.Success)
                }
                it("result value should not be null") {
                    (result as Result.Success).value shouldNotBe null
                }
                it("result value should be cached") {
                    (result as Result.Success).value.isCached() shouldBe true
                }
                it("result value should be expected") {
                    (result as Result.Success).value shouldBe cachedConfig
                }
            }

            context("receive failed response with no cache") {
                every { cacheProvider.obtain(any()) } returns null
                coEvery {
                    stub.getConfiguration(any())
                } throws StatusRuntimeException(Status.DEADLINE_EXCEEDED)
                var result: Result<RequestError, ConfigurationV2>? = null
                repo.getConfigurationProto("", "", "")
                    .collect {
                        result = it
                    }
                it("result should not be null") {
                    result shouldNotBe null
                }
                it("result should be failed") {
                    assert(result is Result.Error)
                }
                it("result error should not be null") {
                    (result as Result.Error).error shouldNotBe null
                }
                it("result error should be expected") {
                    assert((result as Result.Error).error is StatusError)
                }
                it("result error code should be expected") {
                    ((result as Result.Error).error as StatusError).code shouldBe Status.Code.DEADLINE_EXCEEDED
                }
            }
        }

 /*       describe("test getConsentStatus") {
            val config = Gson().fromJson(loadJsonFromFile("config.json"), Configuration::class.java)
            val status = Gson().fromJson(
                loadJsonFromFile("consentStatus.json"),
                GetConsentStatusResponse::class.java
            )

            context("receive a proper response for analytics") {
                every { cacheProvider.obtain(any()) } returns null
                coEvery {
                    client.getApiService(any()).getConsentStatus(any(), any())
                } returns Response.success(status)
                var result: Result<RequestError, Map<String, ConsentStatus>>? = null
                repo.getConsentStatus(
                    config,
                    mapOf("identity" to "identityValue"),
                    mapOf("analytics" to "disclosure")
                )
                    .collect {
                        result = it
                    }
                it("result should not be null") {
                    result shouldNotBe null
                }
                it("result should be successful") {
                    assert(result is Result.Success)
                }
                it("result value should not be null") {
                    (result as Result.Success).value shouldNotBe null
                }
                it("result value should be expected") {
                    (result as Result.Success).value shouldBe mapOf(
                        "analytics" to ConsentStatus(
                            false,
                            "disclosure"
                        )
                    )
                }
            }

            context("receive a proper response for analytics and data_sales") {
                every { cacheProvider.obtain(any()) } returns null
                coEvery {
                    client.getApiService(any()).getConsentStatus(any(), any())
                } returns Response.success(status)

                var result: Result<RequestError, Map<String, ConsentStatus>>? = null
                repo.getConsentStatus(
                    config,
                    mapOf("identity" to "identityValue"),
                    mapOf("analytics" to "disclosure", "data_sales" to "consent_optin")
                )
                    .collect {
                        result = it
                    }
                it("result should not be null") {
                    result shouldNotBe null
                }
                it("result should be successful") {
                    assert(result is Result.Success)
                }
                it("result value should not be null") {
                    (result as Result.Success).value shouldNotBe null
                }
                it("result value should be expected") {
                    (result as Result.Success).value shouldBe
                            mapOf(
                                "analytics" to ConsentStatus(false, "disclosure"),
                                "data_sales" to ConsentStatus(true, "consent_optin")
                            )
                }
            }

            context("receive failed response but has cache") {
                every { cacheProvider.obtain(any()) } returns loadJsonFromFile("consentStatus_cache.json")
                coEvery {
                    client.getApiService(any()).getConsentStatus(any(), any())
                } returns Response.error(404, ResponseBody.create(null, "{}"))

                var result: Result<RequestError, Map<String, ConsentStatus>>? = null
                repo.getConsentStatus(
                    config,
                    mapOf("identity" to "identityValue"),
                    mapOf("analytics" to "disclosure", "data_sales" to "consent_optin")
                )
                    .collect {
                        result = it
                    }
                it("result should not be null") {
                    result shouldNotBe null
                }
                it("result should be successful") {
                    assert(result is Result.Success)
                }
                it("result value should not be null") {
                    (result as Result.Success).value shouldNotBe null
                }
                it("result value should be expected") {
                    (result as Result.Success).value shouldBe
                            mapOf(
                                "analytics" to ConsentStatus(false, "disclosure"),
                                "data_sales" to ConsentStatus(true, "consent_optin")
                            )
                }
            }

            context("receive failed response with no cache") {
                every { cacheProvider.obtain(any()) } returns null
                coEvery {
                    client.getApiService(any()).getConsentStatus(any(), any())
                } returns Response.error(404, ResponseBody.create(null, "{}"))

                var result: Result<RequestError, Map<String, ConsentStatus>>? = null
                repo.getConsentStatus(
                    config,
                    mapOf("identity" to "identityValue"),
                    mapOf("analytics" to "disclosure", "data_sales" to "consent_optin")
                )
                    .collect {
                        result = it
                    }

                it("result should not be null") {
                    result shouldNotBe null
                }
                it("result should be failed") {
                    assert(result is Result.Error)
                }
                it("result error should not be null") {
                    (result as Result.Error).error shouldNotBe null
                }
                it("result error should be expected") {
                    assert((result as Result.Error).error is HttpError)
                }
                it("result error code should be expected") {
                    ((result as Result.Error).error as HttpError).code shouldBe 404
                }
            }
        }

        describe("test updateConsentStatus") {
            val config = Gson().fromJson(loadJsonFromFile("config.json"), Configuration::class.java)

            context("receive a proper response") {
                coEvery {
                    client.getApiService(any()).updateConsentStatus(any(), any())
                } returns Response.success(Unit)

                var result: Result<RequestError, Unit>? = null
                repo.updateConsentStatus(
                    config,
                    mapOf("identity" to "identityValue"),
                    mapOf(
                        "analytics" to ConsentStatus(false, "disclosure"),
                        "data_sales" to ConsentStatus(true, "consent_optin")
                    ),
                    MigrationOption.MIGRATE_ALWAYS
                )
                    .collect {
                        result = it
                    }
                it("result should not be null") {
                    result shouldNotBe null
                }
                it("result should be successful") {
                    assert(result is Result.Success)
                }
                it("result value should not be null") {
                    (result as Result.Success).value shouldNotBe null
                }
            }

            context("receive a failed response") {
                coEvery {
                    client.getApiService(any()).updateConsentStatus(any(), any())
                } returns Response.error(404, ResponseBody.create(null, "{}"))

                var result: Result<RequestError, Unit>? = null
                repo.updateConsentStatus(
                    config,
                    mapOf("identity" to "identityValue"),
                    mapOf(
                        "analytics" to ConsentStatus(false, "disclosure"),
                        "data_sales" to ConsentStatus(true, "consent_optin")
                    ),
                    MigrationOption.MIGRATE_ALWAYS
                )
                    .collect {
                        result = it
                    }
                it("result should not be null") {
                    result shouldNotBe null
                }
                it("result should be failed") {
                    assert(result is Result.Error)
                }
                it("result error should not be null") {
                    (result as Result.Error).error shouldNotBe null
                }
                it("result error should be expected") {
                    assert((result as Result.Error).error is HttpError)
                }
                it("result error code should be expected") {
                    ((result as Result.Error).error as HttpError).code shouldBe 404
                }
            }
        }

        describe("test invokeRights") {
            val config = Gson().fromJson(loadJsonFromFile("config.json"), Configuration::class.java)

            context("receive a proper response") {
                coEvery {
                    client.getApiService(any()).invokeRights(any(), any())
                } returns Response.success(Unit)

                var result: Result<RequestError, Unit>? = null
                repo.invokeRights(
                    config,
                    mapOf("identity" to "identityValue"),
                    UserData(email = "a@b.com"),
                    listOf("portability")
                )
                    .collect {
                        result = it
                    }
                it("result should not be null") {
                    result shouldNotBe null
                }
                it("result should be successful") {
                    assert(result is Result.Success)
                }
                it("result value should not be null") {
                    (result as Result.Success).value shouldNotBe null
                }
            }

            context("receive a failed response") {
                coEvery {
                    client.getApiService(any()).invokeRights(any(), any())
                } returns Response.error(404, ResponseBody.create(null, "{}"))

                var result: Result<RequestError, Unit>? = null
                repo.invokeRights(
                    config,
                    mapOf("identity" to "identityValue"),
                    UserData(email = "a@b.com"),
                    listOf("portabilitysssss")
                )
                    .collect {
                        result = it
                    }
                it("result should not be null") {
                    result shouldNotBe null
                }
                it("result should be failed") {
                    assert(result is Result.Error)
                }
                it("result error should not be null") {
                    (result as Result.Error).error shouldNotBe null
                }
                it("result error should be expected") {
                    assert((result as Result.Error).error is HttpError)
                }
                it("result error code should be expected") {
                    ((result as Result.Error).error as HttpError).code shouldBe 404
                }
        } }*/
    }

    private fun loadJsonFromFile(fileName: String): String {
        return javaClass.classLoader?.loadFromFile(fileName).orEmpty()
    }
}
