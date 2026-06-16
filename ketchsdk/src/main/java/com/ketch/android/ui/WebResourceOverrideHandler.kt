package com.ketch.android.ui

import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.net.HttpURLConnection
import java.net.URL

/**
 * Redirects exact-match WebView resource URLs (e.g. UAT tag scripts) to local dev servers.
 */
internal object WebResourceOverrideHandler {
    private const val TAG = "KetchWebOverride"

    fun intercept(
        overrides: Map<String, String>,
        request: WebResourceRequest?,
    ): WebResourceResponse? {
        if (overrides.isEmpty() || request == null) return null
        val sourceUrl = request.url?.toString() ?: return null
        val destinationUrl = resolveOverrideUrl(sourceUrl, overrides) ?: return null
        return loadResponse(sourceUrl, destinationUrl)
    }

    private fun resolveOverrideUrl(sourceUrl: String, overrides: Map<String, String>): String? {
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

    private fun loadResponse(sourceUrl: String, destinationUrl: String): WebResourceResponse? {
        return try {
            val connection = URL(destinationUrl).openConnection() as HttpURLConnection
            connection.connect()
            Log.d(TAG, "Redirected $sourceUrl -> $destinationUrl")
            WebResourceResponse(
                mimeTypeFor(destinationUrl),
                connection.contentEncoding ?: "UTF-8",
                connection.inputStream,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed override $sourceUrl -> $destinationUrl: ${e.message}", e)
            null
        }
    }

    private fun mimeTypeFor(url: String): String = when {
        url.endsWith(".js", ignoreCase = true) -> "application/javascript"
        url.endsWith(".json", ignoreCase = true) -> "application/json"
        url.endsWith(".css", ignoreCase = true) -> "text/css"
        else -> "text/plain"
    }
}
