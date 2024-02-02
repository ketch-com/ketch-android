package com.ketch.android.data

import com.google.gson.annotations.SerializedName
import com.ketch.android.Ketch
import com.ketch.android.R

/*
"experiences":{
      "consent":{
         "experienceDefault":1
      },
      "preference":{
         "code":"preference"
      }
   },
 */
data class KetchConfig(
    @SerializedName("experiences") val experiences: Experiences?,
    @SerializedName("theme") val theme: KetchTheme?
)

data class Experiences(
    @SerializedName("content") val consent: ContentConfig?
)

data class ContentConfig(
    @SerializedName("display") val display: ContentDisplay
)

enum class ContentDisplay {
    @SerializedName("banner")
    Banner,

    @SerializedName("modal")
    Modal;

    fun getStyle(position: Ketch.WindowPosition): Int =
        when (this) {
            Modal -> when (position) {
                Ketch.WindowPosition.TOP -> R.style.KetchModalTopBottom
                Ketch.WindowPosition.BOTTOM -> R.style.KetchModalTopBottom
                Ketch.WindowPosition.BOTTOM_LEFT -> R.style.KetchModalLeftRightCenter
                Ketch.WindowPosition.BOTTOM_RIGHT -> R.style.KetchModalLeftRightCenter
                Ketch.WindowPosition.BOTTOM_MIDDLE -> R.style.KetchModalLeftRightCenter
                Ketch.WindowPosition.CENTER -> R.style.KetchModalLeftRightCenter
            }

            Banner -> when (position) {
                Ketch.WindowPosition.TOP -> R.style.KetchBannerTopBottom
                Ketch.WindowPosition.BOTTOM -> R.style.KetchBannerTopBottom
                Ketch.WindowPosition.BOTTOM_LEFT -> R.style.KetchBannerLeftRightCenter
                Ketch.WindowPosition.BOTTOM_RIGHT -> R.style.KetchBannerLeftRightCenter
                Ketch.WindowPosition.BOTTOM_MIDDLE -> R.style.KetchBannerLeftRightCenter
                Ketch.WindowPosition.CENTER -> R.style.KetchBannerLeftRightCenter
            }
        }
}

data class KetchTheme(
    @SerializedName("banner") val banner: Banner.Config?,
    @SerializedName("modal") val modal: Modal.Config?
)

sealed class Banner {
    data class Config(
        @SerializedName("container") val container: ContainerConfig?
    )

    data class ContainerConfig(
        @SerializedName("position") val position: ContainerPosition = ContainerPosition.BottomMiddle,
        @SerializedName("size") val size: Size = Size.Standard
    )

    enum class ContainerPosition {
        @SerializedName("bottom")
        Bottom,

        @SerializedName("top")
        Top,

        @SerializedName("leftCorner")
        LeftCorner,

        @SerializedName("rightCorner")
        RightCorner,

        @SerializedName("bottomMiddle")
        BottomMiddle,

        @SerializedName("center")
        Center;

        fun mapToDialogPosition(): Ketch.WindowPosition = when (this) {
            Bottom -> Ketch.WindowPosition.BOTTOM
            Top -> Ketch.WindowPosition.TOP
            LeftCorner -> Ketch.WindowPosition.BOTTOM_LEFT
            RightCorner -> Ketch.WindowPosition.BOTTOM_RIGHT
            BottomMiddle -> Ketch.WindowPosition.BOTTOM_MIDDLE
            Center -> Ketch.WindowPosition.CENTER
        }
    }

    enum class Size {
        @SerializedName("standard")
        Standard,

        @SerializedName("compact")
        Compact
    }
}

sealed class Modal {
    data class Config(
        @SerializedName("container") val container: ContainerConfig?
    )

    data class ContainerConfig(
        @SerializedName("position") val position: ContainerPosition = ContainerPosition.Center
    )

    enum class ContainerPosition {
        @SerializedName("center")
        Center,

        @SerializedName("left")
        Left,

        @SerializedName("right")
        Right;

        fun mapToDialogPosition(): Ketch.WindowPosition = when (this) {
            Center -> Ketch.WindowPosition.BOTTOM_MIDDLE
            Left -> Ketch.WindowPosition.BOTTOM_LEFT
            Right -> Ketch.WindowPosition.BOTTOM_RIGHT
        }
    }
}

