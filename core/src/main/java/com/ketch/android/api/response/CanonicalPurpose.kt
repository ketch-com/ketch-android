package com.ketch.android.api.response

import com.google.gson.annotations.SerializedName

data class CanonicalPurpose(
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String,
    @SerializedName("purposeCodes") val purposeCodes: List<String>?,
)
