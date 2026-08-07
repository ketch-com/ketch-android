package com.ketch.android.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IndexWebResourceOverridesTest {

    private fun html(webResourceUrlOverrides: String?): String =
        getIndexHtml(
            orgCode = "org",
            propertyName = "prop",
            logLevel = "ERROR",
            ketchMobileSdkUrl = "https://global.ketchcdn.com/web/v3",
            identities = "",
            webResourceUrlOverrides = webResourceUrlOverrides,
        )

    @Test
    fun overridesJson_isInstalled() {
        val json = """{"/lanyard.js":"https://cdn.example.test/lanyard.js"}"""
        val result = html(json)

        assertTrue(result.contains("installWebResourceUrlOverrides($json)"))
    }

    @Test
    fun installRunsBeforeTheTagIsAppended() {
        val result = html("""{"a":"b"}""")

        val install = result.indexOf("installWebResourceUrlOverrides({")
        val initTag = result.indexOf("initKetchTag({")

        assertTrue("install call should be present", install >= 0)
        // The override patches HTMLScriptElement.prototype.src, so it is only effective
        // if it runs before initKetchTag appends boot.js.
        assertTrue("install must run before initKetchTag", install < initTag)
    }

    @Test
    fun noOverrides_doesNotInvokeInstall() {
        val result = html(null)

        assertFalse(result.contains("installWebResourceUrlOverrides({"))
        // The helper is always defined; only the invocation is conditional.
        assertTrue(result.contains("function installWebResourceUrlOverrides(overrides)"))
    }

    @Test
    fun blankOverrides_doesNotInvokeInstall() {
        assertFalse(html("   ").contains("installWebResourceUrlOverrides({"))
    }
}
