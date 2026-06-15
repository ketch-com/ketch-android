package com.ketch.android.ui

/**
 * Resolves a WebView resource URL against override rules.
 *
 * Keys may be:
 * - Exact URL (with or without query string)
 * - Path fragment (starts with `/`, matched via [String.contains])
 * - Filename suffix (e.g. `ketch-sdk.js`, matched via path ending)
 */
internal object WebResourceUrlOverrideResolver {
    fun resolve(sourceUrl: String, overrides: Map<String, String>): String? {
        if (overrides.isEmpty() || sourceUrl.isBlank()) return null

        overrides[sourceUrl]?.let { return it }

        val base = sourceUrl.substringBefore('#').substringBefore('?')
        if (base != sourceUrl) {
            overrides[base]?.let { return it }
        }

        for ((pattern, destination) in overrides) {
            if (pattern == sourceUrl || pattern == base) continue
            when {
                pattern.startsWith("/") && base.contains(pattern) -> return destination
                pattern.contains("://") -> Unit
                base.endsWith(pattern) || base.contains("/$pattern") -> return destination
            }
        }
        return null
    }
}
