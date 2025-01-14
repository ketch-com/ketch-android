package com.ketch.android

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import android.util.Log
import androidx.core.content.edit

/**
 * KetchSharedPreferences is a singleton object which handles writing to Android SharedPreferences.
 */
object KetchSharedPreferences {
    private lateinit var sharedPreferences: SharedPreferences
    private var isInitialized = false

    // Prefixes to remove during initialization
    private val PREFIXES_TO_REMOVE = listOf("IABTCF", "IABGPP", "IABUS")

    // Key names for retrieving each string
    const val IAB_TCF_TC_STRING = "IABTCF_TCString"
    const val IAB_US_PRIVACY_STRING = "IABUSPrivacy_String"
    const val IAB_GPP_HDR_GPP_STRING = "IABGPP_HDR_GppString"

    // Logging tag
    private val TAG = KetchSharedPreferences::class.java.simpleName

    /**
     * Initialize SharedPreferences if it doesn't already exist
     */
    fun initialize(context: Context) {
        if (!isInitialized) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            isInitialized = true

            // Clear entries with specific prefixes
            clearEntriesWithPrefixes()

            Log.d(TAG, "Initialized KetchSharedPreferences")
        }
    }

    /**
     * Clear all SharedPreferences keys with the prefixes in PREFIXES_TO_REMOVE
     */
    private fun clearEntriesWithPrefixes() {
        val keysToRemove = sharedPreferences.all.keys.filter { key ->
            PREFIXES_TO_REMOVE.any { key.startsWith(it) }
        }

        sharedPreferences.edit().apply {
            keysToRemove.forEach { remove(it) }
            apply()
        }

        // Log the result
        Log.d(TAG, "Cleared ${keysToRemove.size} keys while initializing KetchSharedPreferences")
    }

    /**
     * Retrieve some value from SharedPreferences
     */
    fun getSavedValue(key: String): String? = sharedPreferences.getString(key, null)

    /**
     * Save a map of values in SharedPreferences, using either apply (async) or commit (sync)
     */
    fun saveValues(values: Map<String, Any?>, logTag: String, synchronousPreferences: Boolean = false) {
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
                Log.d(TAG, "$logTag - Saved ${values.size} keys. Commit result: $result")
            } else {
                apply()
                Log.d(TAG, "$logTag - Saved ${values.size} keys. Changes applied asynchronously.")
            }
        }
    }
}