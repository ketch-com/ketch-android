package com.ketch.android.api.response

import com.google.gson.annotations.SerializedName

data class JurisdictionInfo(
    @SerializedName("code") val code: String,
    @SerializedName("defaultJurisdictionCode") val defaultJurisdictionCode: String,
    @SerializedName("variable") val variable: String?,
    @SerializedName("scopes") val scopes: Map<String, String>?
)
