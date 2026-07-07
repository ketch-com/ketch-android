package com.ketch.android.sample.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Live SDK state bound to the Info panel (mirrors iOS `SampleInfoState` / React-Native
 * `InfoContext`). Only jurisdiction/region are surfaced on screen; every other callback goes to
 * logcat via the shared [Ketch.Listener] in [ComposeSampleApplication].
 */
class SampleInfoState {
    var jurisdiction: String by mutableStateOf("Not set")
        private set
    var region: String by mutableStateOf("Not set")
        private set

    fun updateJurisdiction(value: String?) {
        jurisdiction = value?.takeIf { it.isNotBlank() } ?: "Not set"
    }

    fun updateRegion(value: String?) {
        region = value?.takeIf { it.isNotBlank() } ?: "Not set"
    }
}
