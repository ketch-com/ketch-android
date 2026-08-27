package com.ketch.android.data

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

data class Consent(
    @field:JsonAdapter(ConsentPurposesAdapter::class)
    @SerializedName("purposes") var purposes: Map<String, Boolean>?,
    @SerializedName("vendors") var vendors: List<String>?,
    @SerializedName("protocols") var protocols: Map<String, String>?
)
