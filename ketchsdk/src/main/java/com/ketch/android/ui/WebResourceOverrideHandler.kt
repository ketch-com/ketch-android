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
        var connection: HttpURLConnection? = null
        return try {
            connection = URL(destinationUrl).openConnection() as HttpURLConnection
            when (val responseCode = connection.responseCode) {
                in 200..299 -> {
                    Log.d(TAG, "Redirected $sourceUrl -> $destinationUrl")
                    WebResourceResponse(
                        mimeTypeFor(destinationUrl),
                        connection.contentEncoding ?: "UTF-8",
                        connection.inputStream,
                    )
                }
                else -> {
                    Log.e(
                        TAG,
                        "Override destination HTTP $responseCode for $sourceUrl -> $destinationUrl",
                    )
                    connection.errorStream?.close()
                    connection.disconnect()
                    connection = null
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed override $sourceUrl -> $destinationUrl: ${e.message}", e)
            connection?.disconnect()
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
