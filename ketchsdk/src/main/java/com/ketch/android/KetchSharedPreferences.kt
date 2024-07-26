package com.ketch.android

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import android.util.Log
import androidx.core.content.edit

internal class KetchSharedPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences

    init {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun getSavedValue(key: String): String? = sharedPreferences.getString(key, null)

    private fun saveValues(values: Map<String, Any?>, logMessage: String) {
        sharedPreferences.edit {
            values.forEach { (key, value) ->
                when (value) {
                    is Int, is Long, is Float, is Double -> putString(key, value.toString().replace(".0", ""))
                    is Boolean -> putString(key, value.toString())
                    is String -> putString(key, value)
                    null -> remove(key)
                    else -> putString(key, value.toString())
                }
            }
            apply()
            Log.d(TAG, logMessage)
        }
    }

    fun saveTCFTC(values: Map<String, Any?>) {
        saveValues(values, "$IAB_TCF_TC_STRING is saved")
    }

    fun saveUSPrivacy(values: Map<String, Any?>) {
        saveValues(values, "$IAB_US_PRIVACY_STRING is saved")
    }

    fun saveGPP(values: Map<String, Any?>) {
        saveValues(values, "$IAB_GPP_HDR_GPP_STRING is saved")
    }

    companion object {
        private val TAG = KetchSharedPreferences::class.java.simpleName
        internal const val IAB_TCF_TC_STRING = "IABTCF_TCString"
        internal const val IAB_US_PRIVACY_STRING = "IABUSPrivacy_String"
        internal const val IAB_GPP_HDR_GPP_STRING = "IABGPP_HDR_GppString"
    }
}