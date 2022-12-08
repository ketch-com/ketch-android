package com.ketch.android.api.response

import com.google.gson.annotations.SerializedName

data class PreferenceExperience(
    @SerializedName("code") val code: String,
    @SerializedName("version") val version: Int,
    @SerializedName("title") val title: String?,
    @SerializedName("rights") val rights: RightsTab,
    @SerializedName("consents") val consents: ConsentsTab,
    @SerializedName("overview") val overview: OverviewTab,
)

data class ConsentsTab(
    @SerializedName("tabName") override val tabName: String,
    @SerializedName("bodyTitle") val bodyTitle: String?,
    @SerializedName("bodyDescription") val bodyDescription: String?,
    @SerializedName("buttonText") val buttonText: String,

    // additional extensions
    @SerializedName("extensions") override val extensions: Map<String, String>?
): ExperienceTab

data class OverviewTab(
    @SerializedName("tabName") override val tabName: String,
    @SerializedName("bodyTitle") val bodyTitle: String?,
    @SerializedName("bodyDescription") val bodyDescription: String?,

    // additional extensions
    @SerializedName("extensions") override val extensions: Map<String, String>?
): ExperienceTab

data class RightsTab(
    @SerializedName("tabName") override val tabName: String,
    @SerializedName("bodyTitle") val bodyTitle: String?,
    @SerializedName("bodyDescription") val bodyDescription: String?,
    @SerializedName("buttonText") val buttonText: String,

    // additional extensions
    @SerializedName("extensions") override val extensions: Map<String, String>?
): ExperienceTab

interface ExperienceTab {
    val tabName: String
    val extensions: Map<String, String>?
}

