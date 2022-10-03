package com.ketch.android.api.response

import com.google.gson.annotations.SerializedName

data class Cookie(
    @SerializedName("name") val name: String,
    @SerializedName("code") val code: String,
    @SerializedName("host") val host: String,
    @SerializedName("duration") val duration: CookieDuration,
    @SerializedName("provenance") val provenance: CookieProvenance,
    @SerializedName("category") val category: CookieCategory,
    @SerializedName("description") val description: String,
    @SerializedName("serviceProvider") val serviceProvider: String,
    @SerializedName("latest") val latest: Boolean,
    @SerializedName("version") val version: Int
)

enum class CookieCategory {
    @SerializedName("0") UNSPECIFIED,
    @SerializedName("1") NECESSARY,
    @SerializedName("2") FUNCTIONAL,
    @SerializedName("3") PERFORMANCE,
    @SerializedName("4") MARKETING,
    @SerializedName("5") ANALYTICS
}

enum class CookieProvenance {
    @SerializedName("0") UNSPECIFIED,
    @SerializedName("1") FIRSTPARTY,
    @SerializedName("2") THIRDPARTY
}

enum class CookieDuration {
    @SerializedName("0") UNSPECIFIED,
    @SerializedName("1") SESSION,
    @SerializedName("2") PERSISTENT
}
