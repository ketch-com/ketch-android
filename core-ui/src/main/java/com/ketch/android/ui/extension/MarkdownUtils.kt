package com.ketch.android.ui.extension

import android.content.Context
import android.view.View
import android.widget.TextView
import com.ketch.android.api.response.FullConfiguration
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.LinkResolver
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import io.noties.markwon.core.CoreProps
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.core.spans.LinkSpan
import io.noties.markwon.linkify.LinkifyPlugin
import org.commonmark.node.Link

internal object MarkdownUtils {
    private const val PRIVACY_POLICY = "privacyPolicy"
    private const val TERMS_OF_SERVICE = "termsOfService"
    private const val TRIGGER_MODAL = "triggerModal"

    fun markdown(
        context: Context,
        textView: TextView,
        string: String,
        fullConfiguration: FullConfiguration,
        markdownTriggerListener: MarkdownTriggerListener? = null
    ) {
        val markwon = Markwon.builder(context)
            .usePlugin(SoftBreakAddsNewLinePlugin.create())
            .usePlugin(LinkifyPlugin.create())
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                    builder.setFactory(Link::class.java) { configuration, props ->
                        ClickLinkSpan(
                            fullConfiguration,
                            configuration.theme(),
                            CoreProps.LINK_DESTINATION.require(props),
                            configuration.linkResolver(),
                            markdownTriggerListener
                        )
                    }
                }
            })
            .build()

        markwon.setMarkdown(textView, string)
    }

    private class ClickLinkSpan(
        private val configuration: FullConfiguration,
        theme: MarkwonTheme,
        link: String,
        private val resolver: LinkResolver,
        private val markdownTriggerListener: MarkdownTriggerListener?
    ) : LinkSpan(theme, link, resolver) {

        override fun onClick(widget: View) {
            if (link == TRIGGER_MODAL) {
                markdownTriggerListener?.showModal()
            } else {
                val link = replaceLink(link, configuration)
                resolver.resolve(widget, link)
            }
        }

        private fun replaceLink(link: String, configuration: FullConfiguration): String =
            when (link) {
                PRIVACY_POLICY -> configuration.privacyPolicy?.url
                TERMS_OF_SERVICE -> configuration.termsOfService?.url
                else -> link
            } ?: ""
    }

    interface MarkdownTriggerListener {
        fun showModal()
    }
}