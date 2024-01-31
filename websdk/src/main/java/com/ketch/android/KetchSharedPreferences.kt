package com.ketch.android

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.ketch.android.Ketch.Companion.IAB_GPP_HDR_GPP_STRING
import com.ketch.android.Ketch.Companion.IAB_TCF_TC_STRING
import com.ketch.android.Ketch.Companion.IAB_US_PRIVACY_STRING


internal class KetchSharedPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences

    init {
        sharedPreferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
    }

    fun getSavedValue(key: String): String? = sharedPreferences.getString(key, null)

    fun saveTCFTC(values: Map<String, Any?>) {
        sharedPreferences.edit {
            values.onEach { map ->
                map.value?.let {
                    putString(map.key, it.toString())
                } ?: remove(map.key)
            }
            apply()
            Log.d(TAG, "$IAB_TCF_TC_STRING is saved")
        }
    }

    fun saveUSPrivacy(values: Map<String, Any?>) {
        sharedPreferences.edit {
            values.onEach { map ->
                map.value?.let {
                    putString(map.key, it.toString())
                } ?: remove(map.key)
            }
            apply()
            Log.d(TAG, "$IAB_US_PRIVACY_STRING is saved")
        }
    }

    fun saveGPP(values: Map<String, Any?>) {
        sharedPreferences.edit {
            values.onEach { map ->
                map.value?.let {
                    putString(map.key, it.toString())
                } ?: remove(map.key)
            }
            apply()
            Log.d(TAG, "$IAB_GPP_HDR_GPP_STRING is saved")
        }
    }

    companion object {
        private val TAG = KetchSharedPreferences::class.java.simpleName
    }
}