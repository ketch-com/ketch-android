package com.ketch.android.api.request

import com.google.gson.annotations.SerializedName

data class UpdateConsentRequest(
    @SerializedName("controllerCode") val controller: String?,
    @SerializedName("propertyCode") val property: String,
    @SerializedName("environmentCode") val environment: String,
    @SerializedName("jurisdictionCode") val jurisdiction: String,
    @SerializedName("identities") val identities: Map<String, String>,
    @SerializedName("purposes") val purposes: Map<String, PurposeAllowedLegalBasis>,
    @SerializedName("migrationOption") val migrationOption: MigrationOption?,
    @SerializedName("vendors") val vendors: List<String>?
)

data class PurposeAllowedLegalBasis(
    @SerializedName("legalBasisCode") val legalBasisCode: String,
    @SerializedName("allowed") val allowed: String,
)

enum class MigrationOption {
    @SerializedName("0") MIGRATE_DEFAULT,
    @SerializedName("1") MIGRATE_NEVER,
    @SerializedName("2") MIGRATE_FROM_ALLOW,
    @SerializedName("3") MIGRATE_FROM_DENY,
    @SerializedName("4") MIGRATE_ALWAYS,
}
