package com.ketch.android.data

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/** Summarize config.json for logging / sample-app debug UI. */
fun summarizeConfigJson(configJson: String?): String {
    if (configJson.isNullOrBlank()) return "config: empty"
    return try {
        val root = JsonParser.parseString(configJson)
        if (!root.isJsonObject) return "config: not an object"
        summarizeConfigElement(root.asJsonObject)
    } catch (e: Exception) {
        "config parse error: ${e.message}"
    }
}

fun summarizePurposesJson(configJson: String?): String {
    if (configJson.isNullOrBlank()) return "purposes: empty"
    return try {
        val root = JsonParser.parseString(configJson)
        if (!root.isJsonObject) return "purposes: n/a"
        summarizePurposesElement(root.asJsonObject)
    } catch (e: Exception) {
        "purposes parse error: ${e.message}"
    }
}

private fun summarizeConfigElement(root: JsonObject): String {
    val env = root.get("environment")?.asJsonObject?.get("code")?.asString
    val jurisdiction = root.get("jurisdiction")?.asJsonObject?.get("code")?.asString
        ?: root.get("policyScope")?.asJsonObject?.get("code")?.asString
    val language = root.get("language")?.asString
    val experiences = summarizeExperiences(root.get("experiences"))
    return buildString {
        append("env=").append(env ?: "?")
        append(" jurisdiction=").append(jurisdiction ?: "?")
        append(" lang=").append(language ?: "?")
        append(" ").append(experiences)
    }
}

private fun summarizePurposesElement(root: JsonObject): String {
    val canonical = root.getAsJsonObject("canonicalPurposes")
    if (canonical != null && canonical.size() > 0) {
        val codes = canonical.entrySet().map { it.key }.sorted()
        return "canonicalPurposes(${codes.size}): ${codes.joinToString(", ")}"
    }
    val purposes = root.getAsJsonArray("purposes")
    if (purposes != null && purposes.size() > 0) {
        val codes = purposes.mapNotNull { element ->
            element.asJsonObject.get("code")?.asString
        }.sorted()
        return "purposes(${codes.size}): ${codes.joinToString(", ")}"
    }
    return "purposes: none in config.json"
}

private fun summarizeExperiences(experiences: JsonElement?): String {
    if (experiences == null || experiences.isJsonNull) return "experiences=none"
    if (!experiences.isJsonObject) return "experiences=unexpected"
    val obj = experiences.asJsonObject
    val keys = obj.keySet().sorted()
    val autoInitiated = obj.getAsJsonObject("autoInitiated")
    val layout = autoInitiated?.getAsJsonObject("layout")
    val banner = layout?.has("banner") == true
    val modal = layout?.has("modal") == true
    val pref = layout?.has("preference") == true
    return buildString {
        append("experiences keys=[${keys.joinToString(",")}]")
        if (layout != null) {
            append(" layout banner=$banner modal=$modal pref=$pref")
        }
    }
}

private fun JsonArray.mapNotNull(transform: (JsonElement) -> String?): List<String> {
    val result = mutableListOf<String>()
    for (element in this) {
        transform(element)?.let { result.add(it) }
    }
    return result
}
