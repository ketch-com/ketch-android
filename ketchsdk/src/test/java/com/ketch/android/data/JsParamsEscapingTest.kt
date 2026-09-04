package com.ketch.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The serialized params object is spliced into a script block, so anything a caller can set
 * through setIdentities/setJurisdiction/setLanguage/setRegion/setEnvironment has to survive
 * escaping without terminating the string literal or the script.
 */
class JsParamsEscapingTest {

    @Test
    fun doubleQuote_isEscaped() {
        assertEquals("a\\\"b", jsonEscape("a\"b"))
    }

    @Test
    fun backslash_isEscaped() {
        assertEquals("a\\\\b", jsonEscape("a\\b"))
    }

    @Test
    fun scriptClose_cannotTerminateTheBlock() {
        val escaped = jsonEscape("</script>")

        assertEquals("<\\/script>", escaped)
        assertTrue("must not contain a literal script terminator", !escaped.contains("</"))
    }

    @Test
    fun forwardSlash_isOnlyEscapedAfterLessThan() {
        // Escaping every slash would bloat every URL-valued param for no benefit.
        assertEquals("https://example.test/a", jsonEscape("https://example.test/a"))
    }

    @Test
    fun namedControlCharacters_areEscaped() {
        assertEquals("a\\nb", jsonEscape("a\nb"))
        assertEquals("a\\rb", jsonEscape("a\rb"))
        assertEquals("a\\tb", jsonEscape("a\tb"))
    }

    @Test
    fun unnamedControlCharacters_areEscapedAsUnicode() {
        assertEquals("a\\u0001b", jsonEscape("a\u0001b"))
    }

    @Test
    fun lineSeparators_areEscaped() {
        // Legal in JSON, but a pre-ES2019 JavaScript parser treats these as line terminators,
        // and Android org.json leaves them raw.
        assertEquals("a\\u2028b", jsonEscape("a\u2028b"))
        assertEquals("a\\u2029b", jsonEscape("a\u2029b"))
    }

    @Test
    fun injectionAttempt_staysInsideTheStringLiteral() {
        val literal = toJsObjectLiteral(mapOf("ketch_jurisdiction" to "x\"});alert(1);//"))

        assertEquals("{\"ketch_jurisdiction\":\"x\\\"});alert(1);//\"}", literal)
    }

    @Test
    fun keysAreQuoted_soPunctuationIsNotASyntaxError() {
        // A bare identifier key would not compile for a space code containing a hyphen.
        assertEquals("{\"a-b\":\"c\"}", toJsObjectLiteral(mapOf("a-b" to "c")))
    }

    @Test
    fun emptyMap_isAnEmptyObject() {
        assertEquals("{}", toJsObjectLiteral(emptyMap()))
    }

    @Test
    fun iterationOrderIsPreserved() {
        // The rendered document feeds buildLoadSignature, so ordering has to be stable.
        val params = linkedMapOf("b" to "1", "a" to "2", "c" to "3")

        assertEquals("{\"b\":\"1\",\"a\":\"2\",\"c\":\"3\"}", toJsObjectLiteral(params))
    }
}
