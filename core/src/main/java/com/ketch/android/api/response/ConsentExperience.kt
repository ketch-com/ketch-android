package com.ketch.android.api.response

import com.google.gson.annotations.SerializedName

data class ConsentExperience(
    @SerializedName("code") val code: String,
    @SerializedName("version") val version: Int,
    @SerializedName("banner") val banner: Banner,
    @SerializedName("modal") val modal: Modal,
    @SerializedName("jit") val jit: Jit?,
    @SerializedName("experienceDefault") val experienceDefault: ExperienceDefault,

    // additional extensions
    @SerializedName("extensions") val extensions: Map<String, String>?
)

data class Banner(
    @SerializedName("title") val title: String?,
    @SerializedName("footerDescription") val footerDescription: String,
    @SerializedName("buttonText") val buttonText: String,
    @SerializedName("primaryButtonAction") val primaryButtonAction: ExperienceButtonAction?,
    @SerializedName("secondaryButtonText") val secondaryButtonText: String?,
    @SerializedName("secondaryButtonDestination") val secondaryButtonDestination: ExperienceButtonDestination?,

    // showCloseIcon determines whether the x out icon appears in the experience. Default do not show
    @SerializedName("showCloseIcon") val showCloseIcon: Boolean?,

    // additional extensions
    @SerializedName("extensions") val extensions: Map<String, String>?
)

data class Jit(
    @SerializedName("title") val title: String?,
    @SerializedName("bodyDescription") val bodyDescription: String?,
    @SerializedName("acceptButtonText") val acceptButtonText: String,
    @SerializedName("declineButtonText") val declineButtonText: String,
    @SerializedName("moreInfoText") val moreInfoText: String?,
    @SerializedName("moreInfoDestination") val moreInfoDestination: ExperienceButtonDestination?,

    // showCloseIcon determines whether the x out icon appears in the experience. Default do not show
    @SerializedName("showCloseIcon") val showCloseIcon: Boolean?,

    // additional extensions
    @SerializedName("extensions") val extensions: Map<String, String>?
)

data class Modal(
    @SerializedName("title") val title: String,
    @SerializedName("bodyTitle") val bodyTitle: String?,
    @SerializedName("bodyDescription") val bodyDescription: String?,
    @SerializedName("buttonText") val buttonText: String,

    // showCloseIcon determines whether the x out icon appears in the experience. Default do not show
    @SerializedName("showCloseIcon") val showCloseIcon: Boolean?,

    // consentTitle is the heading that goes above the list of purposes
    // if not
    @SerializedName("consentTitle") val consentTitle: String?,

    // hideConsentTitle determines whether the consent title should be hidden. Default is to show
    @SerializedName("hideConsentTitle") val hideConsentTitle: Boolean?,

    // hideLegalBases determines whether the legal bases should be hidden. Default is to show
    @SerializedName("hideLegalBases") val hideLegalBases: Boolean?,

    // additional extensions
    @SerializedName("extensions") val extensions: Map<String, String>?
)

