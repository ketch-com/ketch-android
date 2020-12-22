package com.ketch.android.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.ketch.android.api.*
import com.ketch.android.api.model.*
import com.ketch.android.cache.SharedPreferencesCacheProvider
import com.ketch.android.loadFromFile
import com.ketch.android.mock.MockResponses.Companion.mockGetConfigurationResponse
import com.ketch.android.mock.MockResponses.Companion.mockGetConsentResponse
import com.ketch.android.model.Consent
import com.ketch.android.model.UserDataV2
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
import mobile.MobileOuterClass
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
            val config =
                Gson().fromJson(loadJsonFromFile("config.json"), ConfigurationV2::class.java)
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

        describe("test getConsentStatus") {
            val config =
                Gson().fromJson(loadJsonFromFile("config.json"), ConfigurationV2::class.java)
            val status = mockGetConsentResponse

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
                    stub.getConsent(any())
                } returns mockGetConsentResponse
                var result: Result<RequestError, GetConsentStatusResponseV2>? = null
                repo.getConsentStatusProto(
                    config,
                    arrayListOf(IdentityV2("id1", "id1")),
                    arrayListOf(PurposeV2("id1", "id1", true))
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
                    (result as Result.Success).value shouldBe GetConsentStatusResponseV2(
                        arrayListOf(
                            Consent(purpose = "id1", legalBasis = "opt_in", allowed = true),
                            Consent(purpose = "id2", legalBasis = "opt_out", allowed = false)
                        )
                    )
                }
            }

             context("receive failed response but has cache") {
                          every { cacheProvider.obtain(any()) } returns loadJsonFromFile("consentStatus_cache.json")
                          coEvery {
                              stub.getConsent(any())
                          } throws StatusRuntimeException(Status.DEADLINE_EXCEEDED)

                          var result: Result<RequestError, GetConsentStatusResponseV2>? = null
                          repo.getConsentStatusProto(
                              config,
                              arrayListOf(IdentityV2("id1", "id1")),
                              arrayListOf(PurposeV2("id1", "id1", true))
                          )
                              .collect {
                                  result = it
                              }
                          it("result should not be null") {
                              result shouldNotBe null
                          }
                          it("result should be successful $result") {
                              assert(result is Result.Success)
                          }
                          it("result value should not be null") {
                              (result as Result.Success).value shouldNotBe null
                          }
                          it("result value should be expected") {
                              (result as Result.Success).value shouldBe
                                      GetConsentStatusResponseV2(
                                          arrayListOf(
                                              Consent(purpose = "id1", legalBasis = "opt_in", allowed = true),
                                              Consent(purpose = "id2", legalBasis = "opt_out", allowed = false)
                                          ),
                                          cachedAt = 1111111
                                      )
                          }
                      }

                      context("receive failed response with no cache") {
                          every { cacheProvider.obtain(any()) } returns null
                          coEvery {
                              stub.getConsent(any())
                          } throws  StatusRuntimeException(Status.DEADLINE_EXCEEDED)
                          var result: Result<RequestError, GetConsentStatusResponseV2>? = null
                          repo.getConsentStatusProto(
                              config,
                              arrayListOf(IdentityV2("id1", "id1")),
                              arrayListOf(PurposeV2("id1", "id1", true))
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
                              assert((result as Result.Error).error is StatusError)
                          }
                          it("result error code should be expected") {
                              ((result as Result.Error).error as StatusError).code shouldBe Status.Code.DEADLINE_EXCEEDED
                          }
                      }
        }
        describe("test setConsentStatus") {
            val config: ConfigurationV2 =
                Gson().fromJson(loadJsonFromFile("config.json"), ConfigurationV2::class.java)

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
                    stub.setConsent(any())
                } returns MobileOuterClass.SetConsentResponse.newBuilder()
                    .setReceivedTime(System.currentTimeMillis()).build()
                var result: Result<RequestError, Long>? = null
                repo.updateConsentStatusProto(
                    config,
                    arrayListOf(
                        IdentityV2("swb_dinghy", "swb_dinghy")
                    ),
                    arrayListOf(
                        PurposeV2("data_sales", "consent_opt_in", true),
                        PurposeV2("test", "consent_opt_out", false)
                    )
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
                    stub.setConsent(any())
                } throws StatusRuntimeException(Status.DEADLINE_EXCEEDED)

                var result: Result<RequestError, Long>? = null
                repo.updateConsentStatusProto(
                    config,
                    arrayListOf(
                        IdentityV2("swb_dinghy", "swb_dinghy")
                    ),
                    arrayListOf(
                        PurposeV2("data_sales", "consent_opt_in", true),
                        PurposeV2("test", "consent_opt_out", false)
                    )
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
                    assert((result as Result.Error).error is StatusError)
                }
                it("result error code should be expected") {
                    ((result as Result.Error).error as StatusError).code shouldBe Status.Code.DEADLINE_EXCEEDED
                }
            }
        }

        describe("test invokeRights") {
            val config: ConfigurationV2 =
                Gson().fromJson(loadJsonFromFile("config.json"), ConfigurationV2::class.java)

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
                coEvery {
                    stub.invokeRight(any())
                } returns MobileOuterClass.InvokeRightResponse.getDefaultInstance()

                var result: Result<RequestError, Unit>? = null
                repo.invokeRightsProto(
                    config,
                    arrayListOf(
                        IdentityV2("id", "_id")
                    ),
                    UserDataV2(
                        email = "a@b.com",
                        first = "First", last = "Last", region = "CA", country = "US"
                    ),
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
                    stub.invokeRight(any())
                } throws StatusRuntimeException(Status.DEADLINE_EXCEEDED)

                var result: Result<RequestError, Unit>? = null
                repo.invokeRightsProto(
                    config,
                    arrayListOf(
                        IdentityV2("id", "_id")
                    ),
                    UserDataV2(
                        email = "a@b.com",
                        first = "First", last = "Last", region = "CA", country = "US"
                    ),
                    listOf("portability")
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
                    assert((result as Result.Error).error is StatusError)
                }
                it("result error code should be expected") {
                    ((result as Result.Error).error as StatusError).code shouldBe Status.Code.DEADLINE_EXCEEDED
                }
            }
        }
    }

    private fun loadJsonFromFile(fileName: String): String {
        return javaClass.classLoader?.loadFromFile(fileName).orEmpty()
    }
}
