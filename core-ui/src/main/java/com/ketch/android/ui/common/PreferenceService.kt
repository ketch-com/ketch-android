package com.ketch.android.ui.common

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit


internal class PreferenceService(context: Context) {
    private val sharedPreferences: SharedPreferences

    init {
        sharedPreferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
    }

    fun updateConsentVersion(version: Int?) {
        if (version != null && getConsentVersion() == version) {
            return
        }

        sharedPreferences.edit {
            version?.let {
                putInt(CONSENT_VERSION, it)
            } ?: remove(CONSENT_VERSION)

            apply()
        }
    }

    fun getConsentVersion(): Int? = if (sharedPreferences.contains(CONSENT_VERSION)) {
        sharedPreferences.getInt(CONSENT_VERSION, 0)
    } else null

    fun updatePreferenceVersion(version: Int?) {
        if (version != null && getPreferenceVersion() == version) {
            return
        }

        sharedPreferences.edit {
            version?.let {
                putInt(PREFERENCE_VERSION, it)
            } ?: remove(PREFERENCE_VERSION)

            apply()
        }
    }

    fun getPreferenceVersion(): Int? = if (sharedPreferences.contains(PREFERENCE_VERSION)) {
        sharedPreferences.getInt(PREFERENCE_VERSION, 0)
    } else null

    companion object {
        private const val CONSENT_VERSION = "consent_version"
        private const val PREFERENCE_VERSION = "preference_version"
    }
}
