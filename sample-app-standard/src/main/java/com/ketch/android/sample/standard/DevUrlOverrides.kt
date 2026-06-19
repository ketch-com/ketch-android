package com.ketch.android.sample.standard

/**
 * Flip [ENABLED] to redirect UAT tag script URLs to local dev servers.
 * Android emulator: use [forEmulator] (localhost when using simulator/port-forward).
 */
object DevUrlOverrides {
    const val ENABLED = true

    val forEmulator: Map<String, String> = mapOf(
        "https://cdn.uat.ketchjs.com/ketchtag/stable/v2.12/ketch-sdk.js" to "http://localhost:9000/ketch-sdk.js",
        "ketch-sdk.js" to "http://localhost:9000/ketch-sdk.js",
    )

    /** Physical device — use host machine LAN IP if localhost is unreachable from device. */
    val forDevice: Map<String, String> = mapOf(
        "https://cdn.uat.ketchjs.com/ketchtag/stable/v2.12/ketch-sdk.js" to "http://localhost:9000/ketch-sdk.js",
        "ketch-sdk.js" to "http://localhost:9000/ketch-sdk.js",
    )
}
