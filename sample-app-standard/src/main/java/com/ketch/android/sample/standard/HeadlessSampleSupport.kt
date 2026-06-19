package com.ketch.android.sample.standard

import com.ketch.android.api.KetchDataCenter
import com.ketch.android.data.ConsentConfig
import com.ketch.android.data.HeadlessConfiguration
import java.util.UUID

object HeadlessSampleSupport {
    const val ORG_CODE = "ethansch061226"
    const val PROPERTY = "website_smart_tag"
    const val ENVIRONMENT = "production"
    val dataCenter: KetchDataCenter = KetchDataCenter.UAT

    fun uniqueEmailIdentity(): Map<String, String> =
        mapOf("email" to "headless-${UUID.randomUUID()}@integration.ketch.test")

    fun consentConfigFrom(
        config: HeadlessConfiguration,
        identities: Map<String, String>,
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
            organizationCode = ORG_CODE,
            propertyCode = PROPERTY,
            environmentCode = ENVIRONMENT,
            jurisdictionCode = jurisdiction,
            identities = identities,
            purposes = purposes,
        )
    }
}
