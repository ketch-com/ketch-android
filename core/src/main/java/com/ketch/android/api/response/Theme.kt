package com.ketch.android.api.response

import com.google.gson.annotations.SerializedName

data class Theme(
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String?,
    @SerializedName("description") val description: String?,

    @SerializedName("watermark") val watermark: Boolean?,

    @SerializedName("buttonBorderRadius") val buttonBorderRadius: Int,

    @SerializedName("bannerBackgroundColor") val bannerBackgroundColor: String?,
    @SerializedName("bannerContentColor") val bannerContentColor: String?,
    @SerializedName("bannerButtonColor") val bannerButtonColor: String?,
    @SerializedName("bannerSecondaryButtonColor") val bannerSecondaryButtonColor: String?,
    @SerializedName("bannerPosition") val bannerPosition: BannerPosition?,

    @SerializedName("modalHeaderBackgroundColor") val modalHeaderBackgroundColor: String?,
    @SerializedName("modalHeaderContentColor") val modalHeaderContentColor: String?,
    @SerializedName("modalContentColor") val modalContentColor: String?,
    @SerializedName("modalButtonColor") val modalButtonColor: String?,
    @SerializedName("modalPosition") val modalPosition: ModalPosition?,
    @SerializedName("modalSwitchOffColor") val modalSwitchOffColor: String?,
    @SerializedName("modalSwitchOnColor") val modalSwitchOnColor: String?,

    @SerializedName("formHeaderBackgroundColor") val formHeaderBackgroundColor: String?,
    @SerializedName("formHeaderContentColor") val formHeaderContentColor: String?,
    @SerializedName("formContentColor") val formContentColor: String?,
    @SerializedName("formButtonColor") val formButtonColor: String?,
    @SerializedName("formSwitchOffColor") val formSwitchOffColor: String?,
    @SerializedName("formSwitchOnColor") val formSwitchOnColor: String?,
)

enum class BannerPosition {
    @SerializedName("0")
    UNKNOWN,
    @SerializedName("1")
    BOTTOM,
    @SerializedName("2")
    TOP,
    @SerializedName("3")
    BOTTOM_LEFT,
    @SerializedName("4")
    BOTTOM_RIGHT
}

enum class ModalPosition {
    @SerializedName("0")
    UNKNOWN,
    @SerializedName("1")
    CENTER,
    @SerializedName("2")
    LEFT_FULL_HEIGHT,
    @SerializedName("3")
    RIGHT_FULL_HEIGHT
}
