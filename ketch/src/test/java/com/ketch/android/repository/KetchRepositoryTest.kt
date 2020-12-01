package com.ketch.android.repository

import android.util.Base64
import com.google.gson.Gson
import com.ketch.android.api.KetchApiClient
import com.ketch.android.api.HttpError
import com.ketch.android.api.MigrationOption
import com.ketch.android.api.RequestError
import com.ketch.android.api.Result
import com.ketch.android.api.model.BootstrapConfiguration
import com.ketch.android.api.model.Configuration
import com.ketch.android.api.model.GetConsentStatusResponse
import com.ketch.android.cache.SharedPreferencesCacheProvider
import com.ketch.android.loadFromFile
import com.ketch.android.model.ConsentStatus
import com.ketch.android.model.UserData
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.collect
import okhttp3.ResponseBody
import retrofit2.Response

class KetchRepositoryTest : DescribeSpec() {

    private val cacheProvider = mockk<SharedPreferencesCacheProvider>(relaxed = true)
    private val client = mockk<KetchApiClient>(relaxed = true)

    init {
        val repo = KetchRepository("", "", null, cacheProvider, client)
        every { cacheProvider.generateSalt(*anyVararg()) } returns ""
        describe("test getBootstrapConfiguration") {
            val bootConfig =
                Gson().fromJson(loadJsonFromFile("boot.json"), BootstrapConfiguration::class.java)
            val cachedBootConfig = Gson().fromJson(
                loadJsonFromFile("boot_cache.json"),
                BootstrapConfiguration::class.java
            )

            context("receive a proper response") {
                every { cacheProvider.obtain(any()) } returns null
                coEvery {
                    client.getApiService(any()).getBootstrapConfiguration(any(), any())
                } returns Response.success(bootConfig)
                var result: Result<RequestError, BootstrapConfiguration>? = null
                repo.getBootstrapConfiguration()
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
                it("result value should not be cached") {
                    (result as Result.Success).value.isCached() shouldBe false
                }
                it("result value should be expected") {
                    (result as Result.Success).value shouldBe bootConfig
                }
            }

            context("receive failed response but has cache") {
                every { cacheProvider.obtain(any()) } returns loadJsonFromFile("boot_cache.json")
                coEvery {
                    client.getApiService(any()).getBootstrapConfiguration(any(), any())
                } returns Response.error(404, ResponseBody.create(null, "{}"))

                var result: Result<RequestError, BootstrapConfiguration>? = null
                repo.getBootstrapConfiguration()
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
                    (result as Result.Success).value shouldBe cachedBootConfig
                }
            }

            context("receive failed response with no cache") {
                every { cacheProvider.obtain(any()) } returns null
                coEvery {
                    client.getApiService(any()).getBootstrapConfiguration(any(), any())
                } returns Response.error(404, ResponseBody.create(null, "{}"))

                var result: Result<RequestError, BootstrapConfiguration>? = null
                repo.getBootstrapConfiguration()
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

        describe("test getFullConfiguration") {
            val bootConfig =
                Gson().fromJson(loadJsonFromFile("boot.json"), BootstrapConfiguration::class.java)
            val config = Gson().fromJson(loadJsonFromFile("config.json"), Configuration::class.java)
            val cachedConfig =
                Gson().fromJson(loadJsonFromFile("config_cache.json"), Configuration::class.java)
            mockkStatic(Base64::class.java.name)
            every { Base64.decode(any<ByteArray>(), any()) } returns "/".toByteArray()
            every { Base64.decode(any<String>(), any()) } returns "/".toByteArray()

            context("receive a proper response") {
                every { cacheProvider.obtain(any()) } returns null
                coEvery {
                    client.getApiService(any())
                        .getFullConfiguration(any(), any(), any(), any(), any(), any())
                } returns Response.success(config)
                var result: Result<RequestError, Configuration>? = null
                repo.getFullConfiguration(bootConfig, "", "")
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
                    client.getApiService(any())
                        .getFullConfiguration(any(), any(), any(), any(), any(), any())
                } returns Response.error(404, ResponseBody.create(null, "{}"))

                var result: Result<RequestError, Configuration>? = null
                repo.getFullConfiguration(bootConfig, "", "")
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
                    client.getApiService(any()).getBootstrapConfiguration(any(), any())
                } returns Response.error(404, ResponseBody.create(null, "{}"))

                var result: Result<RequestError, Configuration>? = null
                repo.getFullConfiguration(bootConfig, "", "")
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

        describe("test getConsentStatus") {
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
            }
        }
    }

    private fun loadJsonFromFile(fileName: String): String {
        return javaClass.classLoader?.loadFromFile(fileName).orEmpty()
    }
}
