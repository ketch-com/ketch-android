package com.ketch.android.api.response

import com.google.gson.annotations.SerializedName

data class FullConfiguration(
    @SerializedName("language") val language: String?,
    @SerializedName("organization") val organization: Organization,
    @SerializedName("property") val property: Property?,
    @SerializedName("environments") val environments: List<Environment>?,
    @SerializedName("environment") val environment: Environment?,
    @SerializedName("jurisdiction") val jurisdiction: JurisdictionInfo?,
    @SerializedName("identities") val identities: Map<String, Identity>?,
    @SerializedName("deployment") val deployment: Deployment,
    @SerializedName("regulations") val regulations: List<String>?,
    @SerializedName("rights") val rights: List<Right>?,
    @SerializedName("purposes") val purposes: List<Purpose>?,
    @SerializedName("canonicalPurposes") val canonicalPurposes: Map<String, CanonicalPurpose>?,
    @SerializedName("experiences") val experiences: Experience?,
    @SerializedName("services") val services: Map<String, String>?,
    @SerializedName("options") val options: Map<String, String>?,
    @SerializedName("privacyPolicy") val privacyPolicy: PolicyDocument?,
    @SerializedName("termsOfService") val termsOfService: PolicyDocument?,
    @SerializedName("theme") val theme: Theme?,
    @SerializedName("scripts") val scripts: List<String>?,
    @SerializedName("vendors") val vendors: List<Vendor>?,
)