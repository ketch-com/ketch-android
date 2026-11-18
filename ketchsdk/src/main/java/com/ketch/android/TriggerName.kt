package com.ketch.android

/**
 * The trigger name for [Ketch.trigger] — mirrors ketch-tag's
 * `ketch('trigger', <triggerName>, ...)` call shape. Fire other triggers by name with
 * `Ketch.trigger(name, options)`.
 */
enum class TriggerName(val value: String) {
    CUSTOM("custom"),
}
