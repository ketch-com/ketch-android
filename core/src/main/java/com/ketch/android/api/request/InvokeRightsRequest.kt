package com.ketch.android.api.request

import com.google.gson.annotations.SerializedName

data class InvokeRightsRequest(
    @SerializedName("controllerCode") val controller: String?,
    @SerializedName("propertyCode") val property: String,
    @SerializedName("environmentCode") val environment: String,
    @SerializedName("jurisdictionCode") val jurisdiction: String,
    @SerializedName("invokedAt") val invokedAt: Long?,
    @SerializedName("identities") val identities: Map<String, String>,
    @SerializedName("rightCode") val right: String?,
    @SerializedName("user") val user: User,
)

data class User(
    @SerializedName("email") val email: String,
    @SerializedName("first") val first: String,
    @SerializedName("last") val last: String,
    @SerializedName("country") val country: String?,
    @SerializedName("stateRegion") val stateRegion: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("postalCode") val postalCode: String?,
    @SerializedName("addressLine1") val addressLine1: String?,
    @SerializedName("addressLine2") val addressLine2: String?,
)
