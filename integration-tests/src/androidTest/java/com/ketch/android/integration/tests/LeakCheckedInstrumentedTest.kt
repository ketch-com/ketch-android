package com.ketch.android.integration.tests

import leakcanary.AppWatcher
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Rule

/**
 * Base for instrumented tests that should be checked for Activity/Fragment/View leaks.
 * On test success (after @After has run), LeakCanary dumps the heap and fails the test if any
 * watched object (a destroyed Activity/Fragment, or one registered via [watchExpectedGone]) is
 * still retained. Subclasses keep their own @Before/@After; this only adds the leak-check rule.
 *
 * Opt a single test method out with @leakcanary.SkipLeakDetection("reason") when retention is
 * deliberate for that test.
 */
abstract class LeakCheckedInstrumentedTest {

    @get:Rule
    val detectLeaksRule = DetectLeaksAfterTestSuccess()

    /**
     * Registers [instance] to be asserted weakly-reachable, in addition to the auto-watched
     * Activities/Fragments. Useful for asserting a specific retained object (e.g. the SDK's
     * internal WebView) is released, independent of which Activity happens to own it.
     */
    protected fun watchExpectedGone(instance: Any?, reason: String) {
        if (instance != null) {
            AppWatcher.objectWatcher.expectWeaklyReachable(instance, reason)
        }
    }
}
