package com.ketch.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.XmlContext
import java.util.regex.Pattern
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.w3c.dom.Document

private const val PRIORITY = 6
private const val COMMENT = "TODO"
private val pattern = Pattern.compile("[\\t]*[//].*$COMMENT.*", Pattern.CASE_INSENSITIVE)

private val ISSUE_TODO = Issue.create(
    "UnresolvedTodo",
    "Unresolved todo",
    """This check highlights comments indicating that some part of the code is unresolved.
        Please address and remove all **TODO** comments before ship!
    """.trimIndent(),
    Category.CORRECTNESS,
    PRIORITY,
    Severity.WARNING,
    Implementation(TodoDetector::class.java, Scope.JAVA_FILE_SCOPE)
)

class TodoDetector :
    Detector(),
    Detector.UastScanner,
    Detector.GradleScanner,
    Detector.OtherFileScanner,
    Detector.XmlScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>>? = listOf(UClass::class.java)

    override fun visitDocument(context: XmlContext, document: Document) {
        // Do nothing, work done in afterCheckFile
    }

    override fun afterCheckFile(context: Context) {
        val source = context.getContents().toString()
        val matcher = pattern.matcher(source)

        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            val location = Location.create(context.file, source, start, end)
            context.report(ISSUE_TODO, location, "Unresolved TODO comment found")
        }
    }

    companion object {
        fun getIssues(): List<Issue> {
            return listOf(ISSUE_TODO)
        }
    }
}
