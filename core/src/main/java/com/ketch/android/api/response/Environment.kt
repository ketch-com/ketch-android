package com.ketch.android.api.response

import com.google.gson.annotations.SerializedName

data class Environment(
    @SerializedName("code") val code: String,
    @SerializedName("pattern") val pattern: String?,
    @SerializedName("hash") val hash: String?,
)
