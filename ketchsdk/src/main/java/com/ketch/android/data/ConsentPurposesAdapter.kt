package com.ketch.android.data

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

/**
 * Reads consent purposes from any shape web/v3 returns: a boolean, a stringified boolean, or the
 * object `POST /consent/{org}/update` replies with, which carries the value under `allowed`.
 *
 * Without this the update shape fails to decode and the caller silently receives an echo of its
 * own request instead of what the server actually recorded.
 */
internal class ConsentPurposesAdapter : TypeAdapter<Map<String, Boolean>?>() {

    override fun write(out: JsonWriter, value: Map<String, Boolean>?) {
        if (value == null) {
            out.nullValue()
            return
        }
        out.beginObject()
        value.forEach { (code, allowed) -> out.name(code).value(allowed) }
        out.endObject()
    }

    override fun read(reader: JsonReader): Map<String, Boolean>? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        val purposes = mutableMapOf<String, Boolean>()
        reader.beginObject()
        while (reader.hasNext()) {
            val code = reader.nextName()
            readAllowed(reader)?.let { purposes[code] = it }
        }
        reader.endObject()
        return purposes
    }

    private fun readAllowed(reader: JsonReader): Boolean? = when (reader.peek()) {
        JsonToken.BOOLEAN -> reader.nextBoolean()
        JsonToken.STRING -> reader.nextString().toBoolean()
        JsonToken.BEGIN_OBJECT -> readAllowedFromObject(reader)
        JsonToken.NULL -> {
            reader.nextNull()
            null
        }
        else -> {
            reader.skipValue()
            null
        }
    }

    /** A purpose object carries other bookkeeping fields; only `allowed` is the consent value. */
    private fun readAllowedFromObject(reader: JsonReader): Boolean? {
        var allowed: Boolean? = null
        reader.beginObject()
        while (reader.hasNext()) {
            if (reader.nextName() == "allowed") {
                allowed = readAllowed(reader)
            } else {
                reader.skipValue()
            }
        }
        reader.endObject()
        return allowed
    }
}
