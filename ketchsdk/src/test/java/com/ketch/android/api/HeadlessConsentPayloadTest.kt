package com.ketch.android.api

import com.google.gson.Gson
import com.ketch.android.data.ConsentConfig
import com.ketch.android.data.ConsentUpdate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeadlessConsentPayloadTest {
    private val gson = Gson()

    @Test
    fun setConsentPayloadOmitsProtocols() {
        val update = ConsentUpdate(
            organizationCode = "org",
            propertyCode = "prop",
            environmentCode = "production",
            identities = mapOf("id" to "1"),
            jurisdictionCode = "default",
            migrationOption = ConsentUpdate.MigrationOption.MIGRATE_DEFAULT,
            purposes = mapOf(
                "analytics" to ConsentUpdate.PurposeAllowedLegalBasis(
                    allowed = true,
                    legalBasisCode = "consent_optin",
                ),
            ),
            vendors = null,
            protocols = mapOf("gpp" to "DBABLA~"),
        )
        val payload = SetConsentPayloadForTesting.from(update.copy(protocols = null))
        val json = gson.toJsonTree(payload).asJsonObject
        assertNull(json.get("protocols"))
        assertEquals("org", json.get("organizationCode").asString)
    }

    @Test
    fun consentConfigPayloadOmitsCachedAt() {
        val config = ConsentConfig(
            organizationCode = "org",
            propertyCode = "prop",
            environmentCode = "production",
            jurisdictionCode = "default",
            identities = emptyMap(),
            purposes = emptyMap(),
        )
        val json = gson.toJsonTree(ConsentConfigPayloadForTesting.from(config)).asJsonObject
        assertNull(json.get("cachedAt"))
    }

    private data class SetConsentPayloadForTesting(
        val organizationCode: String,
        val propertyCode: String,
        val environmentCode: String,
        val identities: Map<String, String>,
        val jurisdictionCode: String,
        val migrationOption: ConsentUpdate.MigrationOption,
        val purposes: Map<String, ConsentUpdate.PurposeAllowedLegalBasis>,
        val vendors: List<String>?,
    ) {
        companion object {
            fun from(update: ConsentUpdate) = SetConsentPayloadForTesting(
                organizationCode = update.organizationCode,
                propertyCode = update.propertyCode,
                environmentCode = update.environmentCode,
                identities = update.identities,
                jurisdictionCode = update.jurisdictionCode,
                migrationOption = update.migrationOption,
                purposes = update.purposes,
                vendors = update.vendors,
            )
        }
    }

    private data class ConsentConfigPayloadForTesting(
        val organizationCode: String,
        val propertyCode: String,
        val environmentCode: String,
        val jurisdictionCode: String,
        val identities: Map<String, String>,
        val purposes: Map<String, ConsentConfig.PurposeLegalBasis>,
    ) {
        companion object {
            fun from(config: ConsentConfig) = ConsentConfigPayloadForTesting(
                organizationCode = config.organizationCode,
                propertyCode = config.propertyCode,
                environmentCode = config.environmentCode,
                jurisdictionCode = config.jurisdictionCode,
                identities = config.identities,
                purposes = config.purposes,
            )
        }
    }
}
