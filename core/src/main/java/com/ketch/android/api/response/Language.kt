package com.ketch.android.api.response

import com.google.gson.annotations.SerializedName

data class Language(
    @SerializedName("code") val code: String,
    @SerializedName("englishName") val englishName: String,
    @SerializedName("nativeName") val nativeName: String?,
)
