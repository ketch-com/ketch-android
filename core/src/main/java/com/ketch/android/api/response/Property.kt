package com.ketch.android.api.response

import com.google.gson.annotations.SerializedName

data class Property(
    @SerializedName("code") val code: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("platform") val platform: String?,
)
