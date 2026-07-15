package com.ketch.android

/**
 * The trigger name for [Ketch.trigger] — mirrors ketch-tag's
 * `ketch('trigger', <triggerName>, ...)` call shape. `CUSTOM` is the only supported value today.
 */
enum class TriggerName(val value: String) {
    CUSTOM("custom"),
}
