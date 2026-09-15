package com.ketch.android.sample.standard

/**
 * Flip [ENABLED] to redirect UAT tag script URLs to a local dev server (e.g. `ketch-js` on :9000).
 * Android emulator: use [forEmulator] — `10.0.2.2` is the emulator's alias for the host machine's
 * loopback interface (plain `localhost` from inside the emulator resolves to the emulator itself).
 * Physical device: substitute the host machine's LAN IP for `10.0.2.2` below.
 */
object DevUrlOverrides {
    const val ENABLED = false

    val forEmulator: Map<String, String> = mapOf(
        "https://cdn.uat.ketchjs.com/ketchtag/stable/v2.12/ketch-sdk.js" to "http://10.0.2.2:9000/ketch-sdk.js",
        "ketch-sdk.js" to "http://10.0.2.2:9000/ketch-sdk.js",
    )
}
