package com.ketch.android.api.response

import com.google.gson.annotations.SerializedName

data class Identity(
    @SerializedName("type") val type: String,
    @SerializedName("variable") val variable: String,
)
