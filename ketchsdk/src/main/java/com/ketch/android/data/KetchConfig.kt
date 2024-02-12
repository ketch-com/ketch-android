package com.ketch.android.data

import com.google.gson.annotations.SerializedName

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
    }
}

