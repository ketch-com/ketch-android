package com.ketch.android.api.response

import com.google.gson.annotations.SerializedName

data class Organization(
    @SerializedName("code") val code: String,
)
