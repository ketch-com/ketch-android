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

/** Response from headless `getLocation()`. */
data class LocationResponse(
    @SerializedName("location") val location: IPInfo? = null,
)

/** Combined ISO region code, e.g. "US-CA", or "US" when no subdivision is known. */
fun IPInfo.toRegionCode(): String? {
    val country = countryCode?.takeIf { it.isNotBlank() } ?: return regionCode?.takeIf { it.isNotBlank() }
    return regionCode?.takeIf { it.isNotBlank() }?.let { "$country-$it" } ?: country
}

/** Parameters for v3 `getFullConfiguration` (ketch-types `GetFullConfigurationRequest`). */
data class FullConfigurationRequest(
    val organizationCode: String,
    val propertyCode: String,
    val environmentCode: String? = null,
    val jurisdictionCode: String? = null,
    val languageCode: String? = null,
    val hash: String? = null,
)

/**
 * The env/jurisdiction/language segment appended to the full-config path, or null if any of the
 * three is absent or blank — matching ketch-tag, which treats a blank value the same as unset.
 * The single source of truth for this so the actual HTTP path (see [HeadlessApiClient]) and any
 * cache keyed on it (see `Ketch.buildConfigCacheKey`) can never disagree about what "the same
 * request" means.
 */
fun FullConfigurationRequest.configPathSegment(): Triple<String, String, String>? {
    val env = environmentCode?.takeIf { it.isNotBlank() } ?: return null
    val jurisdiction = jurisdictionCode?.takeIf { it.isNotBlank() } ?: return null
    val language = languageCode?.takeIf { it.isNotBlank() } ?: return null
    return Triple(env, jurisdiction, language)
}

/** Non-blank hash, or null — a blank hash is treated the same as an absent one. */
fun FullConfigurationRequest.normalizedHash(): String? = hash?.takeIf { it.isNotBlank() }

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

/** Resolved jurisdiction code: the CDN's specific jurisdiction if set, else its default. */
fun HeadlessConfiguration.jurisdictionCode(): String? =
    jurisdiction?.code ?: jurisdiction?.defaultJurisdictionCode

data class ConfigurationPurpose(
    @SerializedName("code") val code: String? = null,
    @SerializedName("legalBasisCode") val legalBasisCode: String? = null,
)

/** ketch-types `DataSubject` */
data class DataSubject(
    @SerializedName("email") val email: String,
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName") val lastName: String,
    @SerializedName("country") val country: String? = null,
    @SerializedName("stateRegion") val stateRegion: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("postalCode") val postalCode: String? = null,
    @SerializedName("addressLine1") val addressLine1: String? = null,
    @SerializedName("addressLine2") val addressLine2: String? = null,
)

/** ketch-types `InvokeRightRequest` */
data class InvokeRightRequest(
    val organizationCode: String,
    val propertyCode: String,
    val environmentCode: String,
    val identities: Map<String, String>,
    val jurisdictionCode: String,
    val rightCode: String,
    val user: DataSubject,
    val controllerCode: String? = null,
    val invokedAt: Long? = null,
    val recaptchaToken: String? = null,
    val regionCode: String? = null,
    val isAuthenticated: Boolean? = null,
)

/** ketch-types `GetPreferenceQRRequest` */
data class PreferenceQRRequest(
    val organizationCode: String,
    val propertyCode: String,
    val environmentCode: String? = null,
    val imageSize: Int? = null,
    val path: String? = null,
    val backgroundColor: String? = null,
    val foregroundColor: String? = null,
    val parameters: Map<String, String> = emptyMap(),
)

/** ketch-types `GetSubscriptionsRequest` / `SetSubscriptionsRequest` */
data class SubscriptionsRequest(
    val organizationCode: String,
    val controllerCode: String? = null,
    val propertyCode: String? = null,
    val environmentCode: String? = null,
    val identities: Map<String, String>? = null,
    val topics: Map<String, Map<String, String>>? = null,
    val controls: Map<String, Map<String, String>>? = null,
    val collectedAt: Long? = null,
    val jurisdictionCode: String? = null,
    val regionCode: String? = null,
)

/** ketch-types `GetSubscriptionsResponse` */
typealias SubscriptionsResponse = SubscriptionsRequest

/** Errors from native headless HTTP calls. */
class HeadlessException(message: String, cause: Throwable? = null) : Exception(message, cause)
