package com.ketch.android.data

import org.junit.Assert.assertEquals
import org.junit.Test

class EventsTest {

    @Test
    fun parseHideExperienceStatus_mapsKnownReasons() {
        assertEquals(HideExperienceStatus.SetConsent, parseHideExperienceStatus("setConsent"))
        assertEquals(HideExperienceStatus.InvokeRight, parseHideExperienceStatus("invokeRight"))
        assertEquals(HideExperienceStatus.Close, parseHideExperienceStatus("close"))
        assertEquals(
            HideExperienceStatus.CloseWithoutSettingConsent,
            parseHideExperienceStatus("closeWithoutSettingConsent")
        )
        assertEquals(HideExperienceStatus.WillNotShow, parseHideExperienceStatus("willNotShow"))
        assertEquals(HideExperienceStatus.SetSubscriptions, parseHideExperienceStatus("setSubscriptions"))
    }

    @Test
    fun parseHideExperienceStatus_unknownReasonFallsBackToNone() {
        assertEquals(HideExperienceStatus.None, parseHideExperienceStatus("unknownReason"))
        assertEquals(HideExperienceStatus.None, parseHideExperienceStatus(null))
        assertEquals(HideExperienceStatus.None, parseHideExperienceStatus(""))
    }
}
