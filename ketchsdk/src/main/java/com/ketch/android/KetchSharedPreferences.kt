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

    private fun saveValues(values: Map<String, Any?>, logMessage: String, synchronousPreferences: Boolean = false) {
        sharedPreferences.edit {
            values.forEach { (key, value) ->
                when (value) {
                    is Int -> putInt(key, value)
                    is Long -> putInt(key, value.toInt())
                    is Float -> putInt(key, value.toInt())
                    is Double -> putInt(key, value.toInt())
                    is Boolean -> putInt(key, if (value) 1 else 0)
                    is String -> putString(key, value)
                    else -> putString(key, value.toString())
                }
            }
            if (synchronousPreferences) {
                val result = commit()
                Log.d(TAG, "$logMessage - $result")
            } else {
                apply()
            }
        }
    }

    fun saveTCFTC(values: Map<String, Any?>, synchronousPreferences: Boolean = false) {
        saveValues(values, "$IAB_TCF_TC_STRING is saved", synchronousPreferences)
    }

    fun saveUSPrivacy(values: Map<String, Any?>, synchronousPreferences: Boolean = false) {
        saveValues(values, "$IAB_US_PRIVACY_STRING is saved", synchronousPreferences)
    }

    fun saveGPP(values: Map<String, Any?>, synchronousPreferences: Boolean = false) {
        saveValues(values, "$IAB_GPP_HDR_GPP_STRING is saved", synchronousPreferences)
    }

    companion object {
        private val TAG = KetchSharedPreferences::class.java.simpleName
        internal const val IAB_TCF_TC_STRING = "IABTCF_TCString"
        internal const val IAB_US_PRIVACY_STRING = "IABUSPrivacy_String"
        internal const val IAB_GPP_HDR_GPP_STRING = "IABGPP_HDR_GppString"
    }
}