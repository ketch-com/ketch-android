package com.ketch.android.api.response

import com.google.gson.annotations.SerializedName

data class PurposeCategory(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("retentionPeriod") val retentionPeriod: String,
    @SerializedName("externalTransfers") val externalTransfers: String,
)