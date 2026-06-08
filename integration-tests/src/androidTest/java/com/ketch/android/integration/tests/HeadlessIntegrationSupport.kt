package com.ketch.android.integration.tests

import com.ketch.android.api.KetchDataCenter
import com.ketch.android.data.ConsentConfig
import com.ketch.android.data.HeadlessConfiguration
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue

object HeadlessIntegrationSupport {
    const val ORG_CODE = "ketch_samples"
    const val PROPERTY = "android"
    const val ENVIRONMENT = "production"
    const val TIMEOUT_SECONDS = 45L

    fun uniqueEmailIdentity(): Map<String, String> =
        mapOf("email" to "headless-${UUID.randomUUID()}@integration.ketch.test")

    fun <T> awaitHeadless(block: (callback: (Result<T>) -> Unit) -> Unit): T {
        val latch = CountDownLatch(1)
        var result: Result<T>? = null
        block { value ->
            result = value
            latch.countDown()
        }
        assertTrue(
            "Headless call timed out after ${TIMEOUT_SECONDS}s",
            latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
        )
        return result!!.getOrElse { throw AssertionError("Headless call failed: ${it.message}", it) }
    }

    fun consentConfigFrom(
        config: HeadlessConfiguration,
        identities: Map<String, String>,
        organizationCode: String = ORG_CODE,
        propertyCode: String = PROPERTY,
        environmentCode: String = ENVIRONMENT,
    ): ConsentConfig {
        val jurisdiction = config.jurisdiction?.code
            ?: config.jurisdiction?.defaultJurisdictionCode
            ?: "us"
        val purposes = config.purposes
            ?.mapNotNull { purpose ->
                val code = purpose.code
                val legalBasis = purpose.legalBasisCode
                if (code != null && legalBasis != null) {
                    code to ConsentConfig.PurposeLegalBasis(legalBasis)
                } else {
                    null
                }
            }
            ?.toMap()
            ?: emptyMap()
        require(purposes.isNotEmpty()) { "Configuration returned no purposes" }
        return ConsentConfig(
            organizationCode = organizationCode,
            propertyCode = propertyCode,
            environmentCode = environmentCode,
            jurisdictionCode = jurisdiction,
            identities = identities,
            purposes = purposes,
        )
    }

    val dataCenter: KetchDataCenter = KetchDataCenter.US
}
