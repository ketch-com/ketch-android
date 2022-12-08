package com.ketch.android.api.request

import com.google.gson.annotations.SerializedName

data class GetConsentRequest(
    @SerializedName("controllerCode") val controller: String?,
    @SerializedName("propertyCode") val property: String,
    @SerializedName("environmentCode") val environment: String,
    @SerializedName("jurisdictionCode") val jurisdiction: String,
    @SerializedName("identities") val identities: Map<String, String>,
    @SerializedName("purposes") val purposes: Map<String, PurposeLegalBasis>,
)

data class PurposeLegalBasis(
    @SerializedName("legalBasisCode") val legalBasisCode: String,
)