package com.ketch.android.data

import com.google.gson.annotations.SerializedName

data class Consent(
    @SerializedName("purposes") var purposes: Map<String, Boolean>?,
    @SerializedName("vendors") var vendors: List<String>?,
    @SerializedName("protocols") var protocols: Map<String, String>?
)
