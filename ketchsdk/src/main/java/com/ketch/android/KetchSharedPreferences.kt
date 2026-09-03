package com.ketch.android

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.preference.PreferenceManager
import android.util.Log
import androidx.core.content.edit

/**
 * KetchSharedPreferences is a singleton object which handles writing to Android SharedPreferences.
 */
object KetchSharedPreferences {
    @Volatile
    private lateinit var sharedPreferences: SharedPreferences

    @Volatile
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
    @Synchronized
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
        val removed = removeValues(PREFIXES_TO_REMOVE)
        Log.d(TAG, "Cleared $removed keys while initializing KetchSharedPreferences")
    }

    /**
     * Whether [initialize] has run. KetchSdk's static headless methods work with no Ketch
     * instance, so callers reached that way cannot assume storage exists.
     */
    val isReady: Boolean
        get() = isInitialized

    /**
     * Retrieve some value from SharedPreferences
     */
    fun getSavedValue(key: String): String? = sharedPreferences.getString(key, null)

    /**
     * Read a string value, falling back to [defaultValue] when the key is absent.
     */
    fun read(key: String, defaultValue: String = ""): String =
        sharedPreferences.getString(key, defaultValue) ?: defaultValue

    /**
     * Persist a single string value. Backs the `nativeStoragePut` WebView bridge event.
     */
    fun write(key: String, value: String) {
        sharedPreferences.edit { putString(key, value) }
    }

    /**
     * Persist several values as one edit.
     *
     * [write] is one key per edit, so a caller storing a value and its timestamp could be killed
     * between the two and leave one without the other. A single edit updates the in-memory map
     * atomically, so readers never see half of it. Uses apply() rather than commit() because
     * callers may be on the main thread and commit() writes to disk synchronously.
     */
    fun writeAll(values: Map<String, String>) {
        sharedPreferences.edit {
            values.forEach { (key, value) -> putString(key, value) }
        }
    }

    /**
     * Remove a single key.
     */
    fun remove(key: String) {
        sharedPreferences.edit { remove(key) }
    }

    /**
     * Remove every stored key whose name begins with one of [prefixes]. Returns the count removed.
     */
    fun removeValues(prefixes: List<String>): Int {
        val keysToRemove = sharedPreferences.all.keys.filter { key ->
            prefixes.any { key.startsWith(it) }
        }
        sharedPreferences.edit {
            keysToRemove.forEach { remove(it) }
        }
        return keysToRemove.size
    }

    @VisibleForTesting
    internal fun bindPreferencesForTesting(prefs: SharedPreferences) {
        sharedPreferences = prefs
        isInitialized = true
    }

    @VisibleForTesting
    internal fun resetForTesting() {
        isInitialized = false
    }

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