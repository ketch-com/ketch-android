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
        val destinationUrl = WebResourceUrlOverrideResolver.resolve(sourceUrl, overrides) ?: return null
        return loadResponse(sourceUrl, destinationUrl)
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
