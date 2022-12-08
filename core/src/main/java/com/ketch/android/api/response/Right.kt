package com.ketch.android.api.response

import com.google.gson.annotations.SerializedName

data class Right(
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,

    // the data subject types for which the right is relevant. If this list is empty then the right applies to all data subject types
    @SerializedName("dataSubjectTypeCodes") val dataSubjectTypeCodes: List<String>?
)
