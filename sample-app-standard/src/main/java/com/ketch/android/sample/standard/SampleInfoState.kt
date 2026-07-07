package com.ketch.android.sample.standard

/**
 * Live SDK state bound to the Info panel (mirrors iOS `SampleInfoState` / React-Native
 * `InfoContext`). Only jurisdiction/region are surfaced on screen; every other callback goes to
 * logcat via the shared [com.ketch.android.Ketch.Listener] in [SampleApplication].
 */
class SampleInfoState {
    var jurisdiction: String = "Not set"
        private set
    var region: String = "Not set"
        private set

    /** Invoked on the main thread whenever [jurisdiction] or [region] changes. */
    var onChange: (() -> Unit)? = null

    fun updateJurisdiction(value: String?) {
        jurisdiction = value?.takeIf { it.isNotBlank() } ?: "Not set"
        onChange?.invoke()
    }

    fun updateRegion(value: String?) {
        region = value?.takeIf { it.isNotBlank() } ?: "Not set"
        onChange?.invoke()
    }
}
