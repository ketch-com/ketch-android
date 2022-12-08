package com.ketch.android.api.response

import com.google.gson.annotations.SerializedName

data class Consent(
    @SerializedName("purposes") var purposes: Map<String, String>?,
    @SerializedName("vendors") var vendors: List<String>?
)
