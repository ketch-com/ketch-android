package com.ketch.lint

import com.android.resources.ResourceFolderType
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.ResourceXmlDetector
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.TextFormat
import com.android.tools.lint.detector.api.XmlContext
import java.util.regex.Pattern
import org.w3c.dom.Element
import org.w3c.dom.Node

private const val PRIORITY = 5
private val ISSUE_UNESCAPED_PERCENT = Issue.create(
    "UnescapedPercent",
    "Unescaped Percent",
    """Percent symbol must be escaped to work properly""".trimIndent(),
    Category.CORRECTNESS,
    PRIORITY,
    Severity.ERROR,
    Implementation(UnescapedPercentDetector::class.java, Scope.RESOURCE_FILE_SCOPE)
)
private val patternPercent = "(?<!%)%(?![%dsf\\d])".toPattern(Pattern.CASE_INSENSITIVE)
private val patternPlaceholder = "%(\\d+\\\$)?([-+#,0(<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z])".toPattern()

class UnescapedPercentDetector : ResourceXmlDetector() {

    override fun appliesTo(folderType: ResourceFolderType): Boolean {
        // Return true if we want to analyze resource files in the specified resource folder type.
        return folderType == ResourceFolderType.VALUES
    }

    override fun getApplicableElements(): Collection<String> = setOf("string")

    override fun visitElement(context: XmlContext, element: Element) {
        if (!element.hasChildNodes()) {
            // <string> elements should always have a single child node
            // (the string text), but double check just to be safe.
            return
        }

        if (element.getAttribute("formatted") == "false") {
            // No need to verify non formatted string
            return
        }

        val textNode = element.firstChild
        if (textNode.nodeType != Node.TEXT_NODE) {
            // The first child of a `<string>` element should always be a text
            // node, but double check just to be safe.
            return
        }

        val stringText = textNode.nodeValue

        if (!patternPlaceholder.matcher(stringText).find()) {
            // String doesn't contain any placeholders no need to enforce percent symbol escape
            return
        }

        val matcher = patternPercent.matcher(stringText)

        while (matcher.find()) {
            context.report(
                issue = ISSUE_UNESCAPED_PERCENT,
                scope = element,
                location = context.getLocation(textNode, matcher.start(), matcher.end()),
                message = ISSUE_UNESCAPED_PERCENT.getExplanation(TextFormat.RAW),
                quickfixData = LintFix.create()
                    .replace()
                    .text("%")
                    .with("%%")
                    .build()
            )
        }
    }

    companion object {
        fun getIssues(): List<Issue> = listOf(ISSUE_UNESCAPED_PERCENT)
    }
}
