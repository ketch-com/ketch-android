package com.ketch.android.api.response

import com.google.gson.annotations.SerializedName

// 0: unspecified, 1: banner, 2: modal
enum class ExperienceDefault {
    @SerializedName("0") UNSPECIFIED,
    @SerializedName("1") BANNER,
    @SerializedName("2") MODAL
}