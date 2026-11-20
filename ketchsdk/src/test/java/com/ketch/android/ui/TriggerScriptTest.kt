package com.ketch.android.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TriggerScriptTest {
    @Test
    fun namedTrigger_passesNameAsJsonStringWithOptions() {
        assertEquals(
            "ketch('trigger', \"dnsOpened\", {\"tab\":\"rightsTab\"})",
            buildNamedTriggerScript("dnsOpened", "{\"tab\":\"rightsTab\"}"),
        )
    }

    @Test
    fun namedTrigger_escapesQuotesInName() {
        assertEquals(
            "ketch('trigger', \"a\\\"b\", {})",
            buildNamedTriggerScript("a\"b", "{}"),
        )
    }

    @Test
    fun customTrigger_keepsSingleQuotedFunctionNameForm() {
        assertEquals(
            "ketch('trigger', 'custom', 'managePrivacy', {\"a\":1})",
            buildCustomTriggerScript("custom", "managePrivacy", "{\"a\":1}"),
        )
    }
}
