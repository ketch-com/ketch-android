package com.ketch.android

import org.junit.Assert.assertEquals
import org.junit.Test

class PendingTriggerTest {
    @Test
    fun namedPendingTrigger_firesNamedForm() {
        val pending = PendingTrigger("dnsOpened", null, "{\"tab\":\"rightsTab\"}")

        assertEquals("ketch('trigger', \"dnsOpened\", {\"tab\":\"rightsTab\"})", pending.script)
    }

    @Test
    fun customPendingTrigger_firesFunctionNameForm() {
        val pending = PendingTrigger(TriggerName.CUSTOM.value, "managePrivacy", "{}")

        assertEquals("ketch('trigger', 'custom', 'managePrivacy', {})", pending.script)
    }
}
