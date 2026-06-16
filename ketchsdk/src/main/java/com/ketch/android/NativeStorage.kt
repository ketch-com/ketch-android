package com.ketch.android

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager

/**
 * Generic string key/value storage backed by default SharedPreferences.
 * Written via the `nativeStoragePut` WebView bridge event from ketch-tag.
 */
object NativeStorage {
    private lateinit var sharedPreferences: SharedPreferences

    fun initialize(context: Context) {
        if (!::sharedPreferences.isInitialized) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        }
    }

    fun read(key: String, defaultValue: String = ""): String =
        sharedPreferences.getString(key, defaultValue) ?: defaultValue

    fun write(key: String, value: String) {
        sharedPreferences.edit { putString(key, value) }
    }
}

internal data class NativeStoragePutPayload(
    val key: String,
    val value: String,
)
