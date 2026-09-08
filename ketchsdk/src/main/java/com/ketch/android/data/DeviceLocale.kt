package com.ketch.android.data

import java.util.Locale

/** Device-locale language default for headless config requests that omit an explicit language. */
object DeviceLocale {
    fun languageTag(): String = formatLanguageTag(Locale.getDefault().toLanguageTag())
}

/**
 * Normalizes a language tag to ketch-tag's `formatLanguage` convention (root lowercase, dialect
 * UPPERCASE, e.g. "fr-CA"), tolerant of the underscore separator Android/JVM locales use
 * ("fr_CA"). Blank input falls back to "en".
 */
fun formatLanguageTag(raw: String): String {
    if (raw.isBlank()) return "en"
    val parts = raw.split('-', '_')
    val root = parts[0].lowercase()
    val dialect = parts.getOrNull(1)?.takeIf { it.isNotBlank() }?.uppercase()
    return if (dialect != null) "$root-$dialect" else root
}
