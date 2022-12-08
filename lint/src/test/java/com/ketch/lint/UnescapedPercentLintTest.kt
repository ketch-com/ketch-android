package com.ketch.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class UnescapedPercentLintTest : LintDetectorTest() {

    override fun getDetector(): Detector = UnescapedPercentDetector()

    override fun getIssues(): List<Issue> = UnescapedPercentDetector.getIssues()

    @Test
    fun expectPass() {
        lint()
            .files(
                xml(
                    "res/values/strings.xml",
                    """
<resources>
    <string name="escaped_percent">Ten %%</string>
    <string name="hire_a_professional_electrician" formatted="false">If you’re not 100% comfortable, hire\nan HVAC installer</string>
    <string name="charge_the_device_to_update">Charge device to 35% or higher to update</string>
</resources>
                """
                )
            )
            .allowMissingSdk()
            .run()
            .expectClean()
    }

    @Test
    fun expectFail() {
        lint()
            .files(
                xml(
                    "res/values/strings.xml",
                    """
<resources>
    <string name="test1">%s\'s battery is critically low. Less than 10%% battery remaining.</string>
    <string name="test2">%s\'s battery is critically low. Less than 10% battery remaining.</string>
    <string name="test3">%s\'s battery is critically low. Less than 10 % battery remaining.</string>
    <string name="test4">%1${'$'}s % available of %2${'$'}f %</string>
    <string name="test5">%1${'$'}s % available of %2${'$'}f %</string>
    <string name="test6">%1${'$'}s %% available of %2${'$'}f %</string>
    <string name="test7">%1${'$'}s % available of %2${'$'}f %%</string>
    <string name="test8">%1${'$'}s %% available of %2${'$'}f %%</string>
    <string name="test9">%d%%</string>
    <string name="test10">%d%</string>
    <string name="test11">%1${'$'}s  GB available of %2${'$'}f GB</string>
</resources>
                """,
                )
            )
            .allowMissingSdk()
            .run()
            .expect(
                """
res/values/strings.xml:4: Error: Percent symbol must be escaped to work properly [UnescapedPercent]
    <string name="test2">%s\'s battery is critically low. Less than 10% battery remaining.</string>
                                                                      ^
res/values/strings.xml:5: Error: Percent symbol must be escaped to work properly [UnescapedPercent]
    <string name="test3">%s\'s battery is critically low. Less than 10 % battery remaining.</string>
                                                                       ^
res/values/strings.xml:6: Error: Percent symbol must be escaped to work properly [UnescapedPercent]
    <string name="test4">%1＄s % available of %2＄f %</string>
                              ^
res/values/strings.xml:6: Error: Percent symbol must be escaped to work properly [UnescapedPercent]
    <string name="test4">%1＄s % available of %2＄f %</string>
                                                  ^
res/values/strings.xml:7: Error: Percent symbol must be escaped to work properly [UnescapedPercent]
    <string name="test5">%1＄s % available of %2＄f %</string>
                              ^
res/values/strings.xml:7: Error: Percent symbol must be escaped to work properly [UnescapedPercent]
    <string name="test5">%1＄s % available of %2＄f %</string>
                                                  ^
res/values/strings.xml:8: Error: Percent symbol must be escaped to work properly [UnescapedPercent]
    <string name="test6">%1＄s %% available of %2＄f %</string>
                                                   ^
res/values/strings.xml:9: Error: Percent symbol must be escaped to work properly [UnescapedPercent]
    <string name="test7">%1＄s % available of %2＄f %%</string>
                              ^
res/values/strings.xml:12: Error: Percent symbol must be escaped to work properly [UnescapedPercent]
    <string name="test10">%d%</string>
                            ^
9 errors, 0 warnings
            """
            )
    }
}
