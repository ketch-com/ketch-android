package com.ketch.android.sample.standard

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SampleDashboardState(
    val initState: String = "Initialized",
    val statusText: String = "Ketch initialized",
    val loadState: String = "idle",
    val experienceVisibility: String = "hidden",
    val dismissReason: String = "—",
    val environment: String = "Not set",
    val jurisdiction: String = "Not set",
    val region: String = "Not set",
    val consent: String = "Not set",
    val usPrivacy: String = "Not set",
    val tcf: String = "Not set",
    val gpp: String = "Not set",
    val headlessLocationResult: String = "—",
    val headlessBootstrapResult: String = "—",
    val headlessConsentResult: String = "—",
    val eventLog: List<String> = emptyList(),
) {
    fun appendLog(message: String): SampleDashboardState {
        val line = "[${timestamp()}] $message"
        return copy(eventLog = (eventLog + line).takeLast(50))
    }

    fun setStatus(message: String): SampleDashboardState =
        appendLog(message).copy(statusText = message)

    private fun timestamp(): String =
        SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
}
