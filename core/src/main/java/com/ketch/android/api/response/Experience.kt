package com.ketch.android.api.response

import com.google.gson.annotations.SerializedName

data class Experience(
    @SerializedName("consent") val consentExperience: ConsentExperience?,
    @SerializedName("preference") val preference: PreferenceExperience?,
)
