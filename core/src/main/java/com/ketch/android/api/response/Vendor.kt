package com.ketch.android.api.response

import com.google.gson.annotations.SerializedName

data class Vendor(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("purposes") val purposes: List<VendorPurpose>?,
    @SerializedName("specialPurposes") val specialPurposes: List<VendorPurpose>?,
    @SerializedName("features") val features: List<VendorPurpose>?,
    @SerializedName("specialFeatures") val specialFeatures: List<VendorPurpose>?,
    @SerializedName("policyUrl") val policyUrl: String?,
    @SerializedName("cookieMaxAgeSeconds") val cookieMaxAgeSeconds: Long?,
    @SerializedName("usesCookies") val usesCookies: Boolean?,
    @SerializedName("usesNonCookieAccess") val usesNonCookieAccess: Boolean?,
)

data class VendorPurpose(
    @SerializedName("name") val name: String,
    @SerializedName("legalBasis") val legalBasis: String?
)
