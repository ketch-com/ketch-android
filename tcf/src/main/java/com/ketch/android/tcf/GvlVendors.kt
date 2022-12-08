package com.ketch.android.api.response

import com.google.gson.annotations.SerializedName
import java.time.Instant

internal data class GvlVendors(
    @SerializedName("gvlSpecificationVersion") val gvlSpecificationVersion: Int,
    @SerializedName("vendorListVersion") val vendorListVersion: Int,
    @SerializedName("tcfPolicyVersion") val tcfPolicyVersion: Int,
    @SerializedName("lastUpdated") val lastUpdated: String?,
    @SerializedName("purposes") val purposes: Map<String, GvlPurpose>?,
    @SerializedName("specialPurposes") val specialPurposes: Map<String, GvlPurpose>?,
    @SerializedName("features") val features: Map<String, GvlPurpose>?,
    @SerializedName("specialFeatures") val specialFeatures: Map<String, GvlPurpose>?,
    @SerializedName("stacks") val stacks: Map<String, GvlStack>?,
    @SerializedName("vendors") val vendors: Map<String, GvlVendor>?
)

internal data class GvlPurpose(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("descriptionLegal") val descriptionLegal: String?
)

internal data class GvlStack(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("purposes") val purposes: List<Int>?,
    @SerializedName("specialPurposes") val specialPurposes: List<Int>?,
    @SerializedName("features") val features: List<Int>?,
    @SerializedName("specialFeatures") val specialFeatures: List<Int>?,
)

internal data class GvlVendor(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("purposes") val purposes: List<Int>?,
    @SerializedName("legIntPurposes") val legIntPurposes: List<Int>?,
    @SerializedName("flexiblePurposes") val flexiblePurposes: List<Int>?,
    @SerializedName("specialPurposes") val specialPurposes: List<Int>?,
    @SerializedName("features") val features: List<Int>?,
    @SerializedName("specialFeatures") val specialFeatures: List<Int>?,
    @SerializedName("policyUrl") val policyUrl: String?
)