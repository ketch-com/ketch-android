package com.ketch.android.sample.standard

import com.ketch.android.data.Consent

/**
 * Formats consent for the event log (mirrors iOS `SampleLogging.swift` / React-Native
 * `consentLogging.ts`): allowed/denied purposes, vendors, and protocol strings.
 */
fun formatConsent(consent: Consent): String {
    val purposes = consent.purposes.orEmpty()
    val allowed = purposes.filterValues { it }.keys.sorted()
    val denied = purposes.filterValues { !it }.keys.sorted()
    return buildString {
        append("allowed=[").append(allowed.joinToString(",")).append("]")
        append(" denied=[").append(denied.joinToString(",")).append("]")
        consent.vendors?.takeIf { it.isNotEmpty() }?.let {
            append(" vendors=[").append(it.joinToString(",")).append("]")
        }
        consent.protocols?.takeIf { it.isNotEmpty() }?.let {
            append(" protocols=[").append(it.entries.joinToString(",") { e -> "${e.key}=${e.value}" }).append("]")
        }
    }
}
