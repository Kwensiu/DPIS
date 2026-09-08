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
import org.commonmark.node.Heading
import org.commonmark.node.ListItem
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

    internal fun stripMarkdownLine(line: String?): String {
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
            if (!isAllowedReleaseNotesUrl(url)) {
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
                if (isAllowedReleaseNotesUrl(url)) {
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
        builder.setFactory(ListItem::class.java) { _, props ->
            ReleaseNotesListMarker(listMarkerText(props))
        }
    }

    override fun configureVisitor(builder: MarkwonVisitor.Builder) {
        builder.on(SoftLineBreak::class.java) { visitor, _ ->
            visitor.builder().append('\n')
        }
    }
}

internal fun headingScale(level: Int): Float = when (level) {
    1 -> 1.35f
    2 -> 1.22f
    3 -> 1.12f
    else -> 1.05f
}

internal fun listMarkerText(props: io.noties.markwon.RenderProps): String {
    return if (CoreProps.LIST_ITEM_TYPE.get(props) == CoreProps.ListItemType.ORDERED) {
        val number = CoreProps.ORDERED_LIST_ITEM_NUMBER.get(props) ?: 1
        "$number. "
    } else {
        "• "
    }
}

internal data class ReleaseNotesListMarker(val marker: String)

internal fun isAllowedReleaseNotesUrl(url: String): Boolean = url.startsWith("https://")

internal data class ReleaseNotesMarkerEdit(val start: Int, val end: Int, val marker: String)

internal fun insertVisibleListMarkers(rendered: CharSequence): CharSequence {
    if (rendered !is Spanned) {
        return rendered
    }
    val edits = rendered.getSpans(0, rendered.length, ReleaseNotesListMarker::class.java)
        .map { ReleaseNotesMarkerEdit(rendered.getSpanStart(it), rendered.getSpanEnd(it), it.marker) }
        .sortedByDescending { it.start }
    if (edits.isEmpty()) {
        return rendered
    }
    val builder = try {
        SpannableStringBuilder(rendered).also { built ->
            built.getSpans(0, built.length, ReleaseNotesListMarker::class.java)
                .forEach { built.removeSpan(it) }
        }
    } catch (_: RuntimeException) {
        null
    }
    var text: CharSequence = builder ?: rendered.toString()
    for (edit in edits) {
        text = insertMarkerText(text, edit)
    }
    return text
}

internal fun insertMarkerText(text: CharSequence, edit: ReleaseNotesMarkerEdit): CharSequence {
    val index = firstContentIndex(text, edit.start, edit.end)
    if (hasPrefix(text, index, edit.marker)) {
        return text
    }
    return if (text is SpannableStringBuilder) {
        try {
            text.insert(index, edit.marker)
            text
        } catch (_: RuntimeException) {
            insertMarkerIntoString(text.toString(), index, edit.marker)
        }
    } else {
        insertMarkerIntoString(text.toString(), index, edit.marker)
    }
}

private fun insertMarkerIntoString(text: String, index: Int, marker: String): String {
    val safeIndex = index.coerceIn(0, text.length)
    return text.substring(0, safeIndex) + marker + text.substring(safeIndex)
}

internal fun hasPrefix(text: CharSequence, index: Int, prefix: String): Boolean {
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
