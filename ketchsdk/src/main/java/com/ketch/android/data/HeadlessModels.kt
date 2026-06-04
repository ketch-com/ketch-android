package com.ketch.android.data

import com.google.gson.annotations.SerializedName

/** GeoIP details from `GET /ip` (ketch-types `IPInfo`). */
data class IPInfo(
    @SerializedName("ip") val ip: String? = null,
    @SerializedName("hostname") val hostname: String? = null,
    @SerializedName("continentCode") val continentCode: String? = null,
    @SerializedName("continentName") val continentName: String? = null,
    @SerializedName("countryCode") val countryCode: String? = null,
    @SerializedName("countryName") val countryName: String? = null,
    @SerializedName("regionCode") val regionCode: String? = null,
    @SerializedName("regionName") val regionName: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("postalCode") val postalCode: String? = null,
    @SerializedName("timezone") val timezone: String? = null,
)

/** Response from headless `fetchLocation()`. */
data class LocationResponse(
    @SerializedName("location") val location: IPInfo? = null,
)

/** Parameters for v3 `getFullConfiguration` (ketch-types `GetFullConfigurationRequest`). */
data class FullConfigurationRequest(
    val organizationCode: String,
    val propertyCode: String,
    val environmentCode: String? = null,
    val jurisdictionCode: String? = null,
    val languageCode: String? = null,
    val hash: String? = null,
)

/** Request body for `POST /consent/{org}/get`. */
data class ConsentConfig(
    val organizationCode: String,
    val propertyCode: String,
    val environmentCode: String,
    val jurisdictionCode: String,
    val identities: Map<String, String>,
    val purposes: Map<String, PurposeLegalBasis>,
) {
    data class PurposeLegalBasis(
        @SerializedName("legalBasisCode") val legalBasisCode: String,
    )
}

/** Request body for `POST /consent/{org}/update`. */
data class ConsentUpdate(
    val organizationCode: String,
    val propertyCode: String,
    val environmentCode: String,
    val identities: Map<String, String>,
    val jurisdictionCode: String,
    val migrationOption: MigrationOption,
    val purposes: Map<String, PurposeAllowedLegalBasis>,
    val vendors: List<String>? = null,
    val protocols: Map<String, String>? = null,
) {
    enum class MigrationOption {
        @SerializedName("MIGRATE_DEFAULT")
        MIGRATE_DEFAULT,

        @SerializedName("MIGRATE_NEVER")
        MIGRATE_NEVER,

        @SerializedName("MIGRATE_FROM_ALLOW")
        MIGRATE_FROM_ALLOW,

        @SerializedName("MIGRATE_FROM_DENY")
        MIGRATE_FROM_DENY,

        @SerializedName("MIGRATE_ALWAYS")
        MIGRATE_ALWAYS,
    }

    data class PurposeAllowedLegalBasis(
        @SerializedName("allowed") val allowed: String,
        @SerializedName("legalBasisCode") val legalBasisCode: String,
    ) {
        constructor(allowed: Boolean, legalBasisCode: String) : this(
            allowed = allowed.toString(),
            legalBasisCode = legalBasisCode,
        )
    }
}

/** Full configuration from bootstrap / full-config CDN endpoints. */
data class HeadlessConfiguration(
    @SerializedName("experiences") val experiences: Experiences? = null,
    @SerializedName("theme") val theme: KetchTheme? = null,
    @SerializedName("rights") val rights: List<ConfigurationRight>? = null,
    @SerializedName("jurisdiction") val jurisdiction: ConfigurationJurisdiction? = null,
    @SerializedName("purposes") val purposes: List<ConfigurationPurpose>? = null,
)

data class ConfigurationRight(
    @SerializedName("code") val code: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
)

data class ConfigurationJurisdiction(
    @SerializedName("code") val code: String? = null,
    @SerializedName("defaultJurisdictionCode") val defaultJurisdictionCode: String? = null,
)

data class ConfigurationPurpose(
    @SerializedName("code") val code: String? = null,
    @SerializedName("legalBasisCode") val legalBasisCode: String? = null,
)

/** Errors from native headless HTTP calls. */
class HeadlessException(message: String, cause: Throwable? = null) : Exception(message, cause)
