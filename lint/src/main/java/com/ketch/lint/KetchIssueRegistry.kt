package com.ketch.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

class KetchIssueRegistry : IssueRegistry() {

    override val issues: List<Issue>
        get() {
            return TodoDetector.getIssues() + UnescapedPercentDetector.getIssues()
        }

    override val api: Int = CURRENT_API
}
