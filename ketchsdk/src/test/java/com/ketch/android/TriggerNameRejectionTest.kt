package com.ketch.android

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TriggerNameRejectionTest {
    @Test
    fun knownTriggerNames_areAccepted() {
        assertNull(triggerNameRejection("privacyPrefUnknown"))
        assertNull(triggerNameRejection("preferencesOpened"))
        assertNull(triggerNameRejection("dnsOpened"))
    }

    @Test
    fun unknownWellFormedNames_areAccepted() {
        // Names the tag adds later must not need an SDK release.
        assertNull(triggerNameRejection("some_future-Trigger2"))
    }

    @Test
    fun custom_isRejected() {
        assertNotNull(triggerNameRejection("custom"))
    }

    @Test
    fun malformedNames_areRejected() {
        assertNotNull(triggerNameRejection(""))
        assertNotNull(triggerNameRejection("1dnsOpened"))
        assertNotNull(triggerNameRejection("dns.opened"))
        assertNotNull(triggerNameRejection("dns opened"))
        assertNotNull(triggerNameRejection("dnsOpened\n"))
        assertNotNull(triggerNameRejection("dns\"); alert(\"x"))
    }
}
