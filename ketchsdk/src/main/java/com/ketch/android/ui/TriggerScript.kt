package com.ketch.android.ui

import com.ketch.android.data.jsonEscape

/**
 * Script for a custom-function trigger: `ketch('trigger', '<triggerName>', '<functionName>', <options>)`.
 * Callers pass a [functionName] already restricted to characters that are safe in a single-quoted literal.
 */
internal fun buildCustomTriggerScript(triggerName: String, functionName: String, optionsJson: String): String =
    "ketch('trigger', '$triggerName', '$functionName', $optionsJson)"

/**
 * Script for a named trigger: `ketch('trigger', "<name>", <options>)`, with [name] escaped as a JSON string.
 */
internal fun buildNamedTriggerScript(name: String, optionsJson: String): String =
    "ketch('trigger', \"${jsonEscape(name)}\", $optionsJson)"
