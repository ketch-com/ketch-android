package com.ketch.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TriggerFunctionNameTest {
    @Test
    fun validNames_areAccepted() {
        assertTrue(isValidTriggerFunctionName("managePrivacy"))
        assertTrue(isValidTriggerFunctionName("manage_privacy"))
        assertTrue(isValidTriggerFunctionName("manage-privacy"))
        assertTrue(isValidTriggerFunctionName("manage.privacy.v2"))
        assertTrue(isValidTriggerFunctionName("123"))
    }

    @Test
    fun blankOrEmpty_isRejected() {
        assertFalse(isValidTriggerFunctionName(""))
        assertFalse(isValidTriggerFunctionName("   "))
    }

    @Test
    fun quoteInjection_isRejected() {
        // Would otherwise break out of the single-quoted JS literal in
        // KetchWebView.trigger(): ketch('trigger', 'custom', '<functionName>', ...)
        assertFalse(isValidTriggerFunctionName("foo'); alert('xss"))
        assertFalse(isValidTriggerFunctionName("foo'"))
    }

    @Test
    fun backslashOrControlChars_areRejected() {
        assertFalse(isValidTriggerFunctionName("foo\\bar"))
        assertFalse(isValidTriggerFunctionName("foo\nbar"))
        assertFalse(isValidTriggerFunctionName("foo\tbar"))
    }

    @Test
    fun whitespaceOrSpecialChars_areRejected() {
        assertFalse(isValidTriggerFunctionName("foo bar"))
        assertFalse(isValidTriggerFunctionName("foo/bar"))
        assertFalse(isValidTriggerFunctionName("foo|bar"))
    }
}
