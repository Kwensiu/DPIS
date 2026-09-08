package com.dpis.module.updates

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.core.CoreProps
import io.noties.markwon.core.spans.BulletListItemSpan
import io.noties.markwon.core.spans.OrderedListItemSpan
import org.commonmark.node.Heading
import org.commonmark.node.SoftLineBreak
import java.util.Locale

object ReleaseNotesMarkdownRenderer {
    fun interface Renderer {
        fun render(context: Context?, markdown: String): CharSequence
    }

    @Volatile
    private var renderer: Renderer = Renderer { context, markdown ->
        renderWithMarkwon(context, markdown)
    }

    @JvmStatic
    fun render(context: Context?, markdown: String?, locale: Locale?): CharSequence {
        val filtered = ReleaseNotesMarkdownLite.filterBodyForLocale(safe(markdown), locale)
        if (filtered.trim().isEmpty()) {
            return ""
        }
        return try {
            renderer.render(context, filtered)
        } catch (_: Throwable) {
            // Release notes come from GitHub text controlled outside the app. Keep the UI
            // alive even if a Markdown extension or malformed input trips the renderer.
            fallbackPlainText(filtered)
        }
    }

    @JvmStatic
    fun setRendererForTesting(testRenderer: Renderer?) {
        renderer = testRenderer ?: Renderer { context, markdown ->
            renderWithMarkwon(context, markdown)
        }
    }

    private fun renderWithMarkwon(context: Context?, markdown: String): CharSequence {
        if (context == null) {
            return fallbackPlainText(markdown)
        }
        val rendered = Markwon.builder(context)
            .usePlugin(ReleaseNotesComposeCompatiblePlugin())
            .build()
            .toMarkdown(markdown)
        return insertVisibleListMarkers(rendered)
    }

    private fun fallbackPlainText(markdown: String): CharSequence {
        val plain = StringBuilder()
        val lines = safe(markdown).replace("\r\n", "\n").split('\n')
        for (i in lines.indices) {
            val line = stripMarkdownLine(lines[i])
            plain.append(toPlainLinkLabels(line))
            if (i < lines.lastIndex) {
                plain.append('\n')
            }
        }
        return applyPlainLinkSpans(plain.toString(), markdown)
    }

    private fun stripMarkdownLine(line: String?): String {
        if (line == null) {
            return ""
        }
        var stripped = line.trim()
        if (stripped.startsWith("```")) {
            return ""
        }
        stripped = stripped.replaceFirst("^#{1,6}\\s+".toRegex(), "")
        stripped = stripped.replaceFirst("^[-*+]\\s+".toRegex(), "• ")
        stripped = stripped.replace("**", "")
        stripped = stripped.replace("__", "")
        stripped = stripped.replace("`", "")
        return stripped
    }

    private fun toPlainLinkLabels(line: String): String {
        val out = StringBuilder()
        var index = 0
        while (index < line.length) {
            val labelStart = line.indexOf('[', index)
            if (labelStart < 0) {
                out.append(line, index, line.length)
                return out.toString()
            }
            val labelEnd = line.indexOf("](", labelStart)
            val urlEnd = if (labelEnd >= 0) line.indexOf(')', labelEnd + 2) else -1
            if (labelEnd < 0 || urlEnd < 0) {
                out.append(line, index, line.length)
                return out.toString()
            }
            val url = line.substring(labelEnd + 2, urlEnd)
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                out.append(line, index, urlEnd + 1)
                index = urlEnd + 1
                continue
            }
            out.append(line, index, labelStart)
            out.append(line, labelStart + 1, labelEnd)
            index = urlEnd + 1
        }
        return out.toString()
    }

    private fun applyPlainLinkSpans(plainText: String, markdown: String): CharSequence {
        return try {
            val out = SpannableStringBuilder(plainText)
            val markdownText = safe(markdown)
            var searchStart = 0
            var plainSearchStart = 0
            var appliedLink = false
            while (searchStart < markdownText.length) {
                val labelStart = markdownText.indexOf('[', searchStart)
                if (labelStart < 0) {
                    break
                }
                val labelEnd = markdownText.indexOf("](", labelStart)
                val urlEnd = if (labelEnd >= 0) markdownText.indexOf(')', labelEnd + 2) else -1
                if (labelEnd < 0 || urlEnd < 0) {
                    break
                }
                val url = markdownText.substring(labelEnd + 2, urlEnd)
                val label = markdownText.substring(labelStart + 1, labelEnd)
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    val spanStart = plainText.indexOf(label, plainSearchStart)
                    if (spanStart >= 0) {
                        val spanEnd = spanStart + label.length
                        out.setSpan(
                            URLSpan(url),
                            spanStart,
                            spanEnd,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                        appliedLink = true
                        plainSearchStart = spanEnd
                    }
                }
                searchStart = urlEnd + 1
            }
            if (appliedLink) out else plainText
        } catch (_: RuntimeException) {
            plainText
        }
    }

    private fun safe(value: String?): String = value ?: ""
}

private class ReleaseNotesComposeCompatiblePlugin : AbstractMarkwonPlugin() {
    override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
        builder.setFactory(Heading::class.java) { _, props ->
            val level = CoreProps.HEADING_LEVEL.require(props)
            arrayOf<Any>(StyleSpan(Typeface.BOLD), RelativeSizeSpan(headingScale(level)))
        }
    }

    override fun configureVisitor(builder: MarkwonVisitor.Builder) {
        builder.on(SoftLineBreak::class.java) { visitor, _ ->
            visitor.builder().append('\n')
        }
    }

    private fun headingScale(level: Int): Float = when (level) {
        1 -> 1.35f
        2 -> 1.22f
        3 -> 1.12f
        else -> 1.05f
    }
}

internal fun insertVisibleListMarkers(rendered: CharSequence): CharSequence {
    if (rendered !is Spanned) {
        return rendered
    }
    val builder = try {
        SpannableStringBuilder(rendered)
    } catch (_: RuntimeException) {
        return rendered
    }
    return try {
        insertMarkers(builder, BulletListItemSpan::class.java) { "• " }
        insertMarkers(builder, OrderedListItemSpan::class.java, ::orderedListMarker)
        builder
    } catch (_: RuntimeException) {
        rendered
    }
}

private fun <T : Any> insertMarkers(
    builder: SpannableStringBuilder,
    type: Class<T>,
    markerFor: (T) -> String,
) {
    builder.getSpans(0, builder.length, type)
        .sortedByDescending { builder.getSpanStart(it) }
        .forEach { span ->
            val start = builder.getSpanStart(span)
            val index = firstContentIndex(builder, start, builder.getSpanEnd(span))
            val marker = markerFor(span)
            if (!hasPrefix(builder, index, marker)) {
                builder.insert(index, marker)
            }
            builder.removeSpan(span)
        }
}

private fun orderedListMarker(span: OrderedListItemSpan): String {
    return try {
        val field = OrderedListItemSpan::class.java.getDeclaredField("number")
        field.isAccessible = true
        (field.get(span) as? String)?.takeIf { it.isNotEmpty() } ?: "1. "
    } catch (_: Exception) {
        "1. "
    }
}

private fun hasPrefix(text: CharSequence, index: Int, prefix: String): Boolean {
    if (index < 0 || index + prefix.length > text.length) {
        return false
    }
    return text.subSequence(index, index + prefix.length).toString() == prefix
}

internal fun firstContentIndex(text: CharSequence, start: Int, end: Int): Int {
    var index = start.coerceAtLeast(0)
    val limit = end.coerceAtMost(text.length)
    while (index < limit && text[index] == '\n') {
        index++
    }
    return index
}
