package com.ketch.android.sample.compose

import com.ketch.android.api.KetchDataCenter

/**
 * Single source of truth for both SDK init and the Info panel (mirrors iOS `SampleConfig` /
 * Flutter `sampleConfig` / React-Native `SAMPLE_CONFIG`).
 */
object SampleConfig {
    const val ORG_CODE = "ketch_samples"
    const val PROPERTY = "android"
    const val ENVIRONMENT = "production"
    const val LANGUAGE = "en"
    val dataCenter: KetchDataCenter = KetchDataCenter.US
    val identities: Map<String, String> = mapOf("aaid" to "sample-test-123")
}
