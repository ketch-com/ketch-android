package com.ketch.android.api.response

import com.google.gson.annotations.SerializedName

// 0: unspecified, 1: saveCurrentState, 2: acceptAll
enum class ExperienceButtonAction {
    @SerializedName("0") UNSPECIFIED,
    @SerializedName("1") SAVE_CURRENT_STATE,
    @SerializedName("2") ACCEPT_ALL,
}

// 0: unspecified, 1: modal, 2: preference
enum class ExperienceButtonDestination {
    @SerializedName("0") UNSPECIFIED,
    @SerializedName("1") MODAL,
    @SerializedName("2") PREFERENCE,
    @SerializedName("3") REJECT_ALL
}
