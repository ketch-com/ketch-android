package com.ketch.android.api.response

import com.google.gson.annotations.SerializedName

data class PolicyDocument(
    @SerializedName("code") val code: String?,
    @SerializedName("version") val version: Int,
    @SerializedName("url") val url: String?,
)
