package com.ketch.android.api

/**
 * Ketch CDN region for headless and WebView API calls (matches Flutter / React Native maps).
 */
enum class KetchDataCenter(val baseUrl: String) {
    US("https://global.ketchcdn.com/web/v3"),
    EU("https://eu.ketchcdn.com/web/v3"),
    UAT("https://dev.ketchcdn.com/web/v3"),
}
