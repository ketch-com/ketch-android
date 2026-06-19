package com.ketch.android

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.JsonParseException

private const val PREFS_NAME = "ketch_native_storage"

/**
 * Generic string key/value storage backed by app-private SharedPreferences.
 * Written via the `nativeStoragePut` WebView bridge event from ketch-tag.
 */
object NativeStorage {
    private var sharedPreferences: SharedPreferences? = null

    fun initialize(context: Context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun read(key: String, defaultValue: String = ""): String =
        prefs().getString(key, defaultValue) ?: defaultValue

    fun write(key: String, value: String) {
        prefs().edit { putString(key, value) }
    }

    fun remove(key: String) {
        prefs().edit { remove(key) }
    }

    fun removeValues(prefixes: List<String>): Int {
        val matching = prefs().all.keys.filter { key ->
            prefixes.any { key.startsWith(it) }
        }
        prefs().edit {
            matching.forEach { remove(it) }
        }
        return matching.size
    }

    @VisibleForTesting
    internal fun bindPreferencesForTesting(prefs: SharedPreferences) {
        sharedPreferences = prefs
    }

    @VisibleForTesting
    internal fun resetForTesting() {
        sharedPreferences = null
    }

    private fun prefs(): SharedPreferences =
        checkNotNull(sharedPreferences) {
            "NativeStorage.initialize must be called before read/write"
        }
}

internal data class NativeStoragePutPayload(
    val key: String,
    val value: String,
)

private data class NativeStoragePutPayloadDto(
    val key: String?,
    val value: String?,
)

internal fun parseNativeStoragePutPayload(json: String): NativeStoragePutPayload? {
    return try {
        val raw = Gson().fromJson(json, NativeStoragePutPayloadDto::class.java) ?: return null
        val key = raw.key?.trim().orEmpty()
        if (key.isEmpty()) {
            return null
        }
        NativeStoragePutPayload(key = key, value = raw.value.orEmpty())
    } catch (_: JsonParseException) {
        null
    }
}
