package com.ketch.android.data

// Status that accompanies a hideExperience event
enum class HideExperienceStatus(val value: String?) {
    SetConsent("setConsent"),
    InvokeRight("invokeRight"),
    Close("close"),
    CloseWithoutSettingConsent("closeWithoutSettingConsent"),
    WillNotShow("willNotShow"),
    None(null);

    companion object {
        fun fromValue(value: String?): HideExperienceStatus {
            return entries.find { it.value == value } ?: None
        }
    }
}

// Determine which enum value corresponds to some string
fun parseHideExperienceStatus(status: String?): HideExperienceStatus {
    return HideExperienceStatus.fromValue(status)
}

// Type the accompanies a willShowExperience event
enum class WillShowExperienceType(val value: String?) {
    ConsentExperience("experiences.consent"),
    PreferenceExperience("experiences.preference"),
    None(null);

    companion object {
        fun fromValue(value: String?): WillShowExperienceType {
            return entries.find { it.value == value } ?: None
        }
    }
}

// Determine which enum value corresponds to some string
fun parseWillShowExperienceType(type: String?): WillShowExperienceType {
    return WillShowExperienceType.fromValue(type)
}