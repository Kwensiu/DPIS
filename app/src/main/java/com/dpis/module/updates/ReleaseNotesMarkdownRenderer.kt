package com.dpis.module.updates

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.URLSpan
import java.net.URI
import java.net.URISyntaxException
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
        } catch (_: RuntimeException) {
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
        return renderReleaseNotesWithMarkwon(context, markdown)
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
        val links = parseReleaseNoteMarkdownLinks(markdown)
        if (links.isEmpty()) {
            return plainText
        }
        return try {
            val out = SpannableStringBuilder(plainText)
            var plainSearchStart = 0
            var appliedLink = false
            for (link in links) {
                val spanStart = plainText.indexOf(link.label, plainSearchStart)
                if (spanStart >= 0) {
                    val spanEnd = spanStart + link.label.length
                    out.setSpan(
                        URLSpan(link.url),
                        spanStart,
                        spanEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                    appliedLink = true
                    plainSearchStart = spanEnd
                }
            }
            if (appliedLink) out else plainText
        } catch (_: RuntimeException) {
            plainText
        }
    }

    private fun safe(value: String?): String = value ?: ""
}

internal fun headingScale(level: Int): Float = when (level) {
    1 -> 1.35f
    2 -> 1.22f
    3 -> 1.12f
    else -> 1.05f
}

internal data class ReleaseNotesListMarker(val marker: String)

internal data class ReleaseNotesPlainLink(val label: String, val url: String)

internal fun isAllowedReleaseNotesUrl(url: String): Boolean = sanitizedReleaseNotesUrl(url) != null

internal fun sanitizedReleaseNotesUrl(url: String): String? {
    return try {
        val uri = URI(url)
        val host = uri.host
        if (!uri.scheme.equals("https", ignoreCase = true) ||
            host.isNullOrBlank() ||
            uri.userInfo != null
        ) {
            return null
        }
        URI("https", null, host, uri.port, uri.path, uri.query, uri.fragment).toASCIIString()
    } catch (_: URISyntaxException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}

internal fun parseReleaseNoteMarkdownLinks(markdown: String): List<ReleaseNotesPlainLink> {
    val links = ArrayList<ReleaseNotesPlainLink>()
    var searchStart = 0
    while (searchStart < markdown.length) {
        val labelStart = markdown.indexOf('[', searchStart)
        if (labelStart < 0) {
            break
        }
        val labelEnd = markdown.indexOf("](", labelStart)
        val urlEnd = if (labelEnd >= 0) markdown.indexOf(')', labelEnd + 2) else -1
        if (labelEnd < 0 || urlEnd < 0) {
            break
        }
        val url = markdown.substring(labelEnd + 2, urlEnd)
        val label = markdown.substring(labelStart + 1, labelEnd)
        val sanitized = sanitizedReleaseNotesUrl(url)
        if (sanitized != null) {
            links.add(ReleaseNotesPlainLink(label, sanitized))
        }
        searchStart = urlEnd + 1
    }
    return links
}

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
