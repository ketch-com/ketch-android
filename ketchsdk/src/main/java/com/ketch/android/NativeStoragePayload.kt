package com.ketch.android

import com.google.gson.Gson
import com.google.gson.JsonParseException

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
