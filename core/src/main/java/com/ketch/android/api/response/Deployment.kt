package com.ketch.android.api.response

import com.google.gson.annotations.SerializedName

data class Deployment(
    @SerializedName("code") val code: String,
    @SerializedName("version") val version: Long,
)
