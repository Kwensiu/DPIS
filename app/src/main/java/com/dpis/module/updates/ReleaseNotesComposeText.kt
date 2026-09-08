package com.dpis.module.updates

import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.noties.markwon.core.spans.BlockQuoteSpan
import io.noties.markwon.core.spans.BulletListItemSpan
import io.noties.markwon.core.spans.CodeBlockSpan
import io.noties.markwon.core.spans.CodeSpan
import io.noties.markwon.core.spans.EmphasisSpan
import io.noties.markwon.core.spans.HeadingSpan
import io.noties.markwon.core.spans.OrderedListItemSpan
import io.noties.markwon.core.spans.StrongEmphasisSpan

/**
 * Maps Markwon/Android spans used by GitHub release notes into Compose text.
 * Generic dialog interop still keeps URL-only conversion; this path owns
 * headings, emphasis, lists, quotes, and inline code.
 */
internal fun CharSequence.toReleaseNotesAnnotatedString(): AnnotatedString {
    val plainText = toString()
    if (this !is Spanned) {
        return AnnotatedString(plainText)
    }
    return try {
        buildAnnotatedString {
            append(plainText)
            getSpans(0, length, Any::class.java).forEach { span ->
                val start = getSpanStart(span).coerceAtLeast(0)
                val end = getSpanEnd(span).coerceAtMost(length)
                if (start >= end) return@forEach
                try {
                    applyReleaseNotesSpan(span, start, end, plainText)
                } catch (_: RuntimeException) {
                    // Skip a single unreadable Android span instead of dropping the whole body.
                }
            }
        }
    } catch (_: RuntimeException) {
        AnnotatedString(plainText)
    }
}

private fun AnnotatedString.Builder.applyReleaseNotesSpan(
    span: Any,
    start: Int,
    end: Int,
    plainText: String,
) {
    when (span) {
        is URLSpan -> {
            val url = sanitizedReleaseNotesUrl(span.url.orEmpty())
            if (url != null) {
                addLink(LinkAnnotation.Url(url), start, end)
                addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
            }
        }
        is StyleSpan -> addStyle(typefaceStyleToSpanStyle(span.style) ?: return, start, end)
        is StrongEmphasisSpan -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
        is EmphasisSpan -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
        is RelativeSizeSpan -> addStyle(SpanStyle(fontSize = span.sizeChange.em), start, end)
        is HeadingSpan -> addStyle(
            SpanStyle(fontWeight = FontWeight.Bold, fontSize = 1.12.em),
            start,
            end,
        )
        is TypefaceSpan -> {
            if (span.family.equals("monospace", ignoreCase = true)) {
                addStyle(SpanStyle(fontFamily = FontFamily.Monospace), start, end)
            }
        }
        is CodeSpan, is CodeBlockSpan -> addStyle(
            SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = Color(0x33808080),
            ),
            start,
            end,
        )
        is StrikethroughSpan -> addStyle(
            SpanStyle(textDecoration = TextDecoration.LineThrough),
            start,
            end,
        )
        is UnderlineSpan -> addStyle(
            SpanStyle(textDecoration = TextDecoration.Underline),
            start,
            end,
        )
        is ForegroundColorSpan -> addStyle(
            SpanStyle(color = Color(span.foregroundColor)),
            start,
            end,
        )
        is BackgroundColorSpan -> addStyle(
            SpanStyle(background = Color(span.backgroundColor)),
            start,
            end,
        )
        is BulletListItemSpan, is OrderedListItemSpan -> {
            // Markers are inserted as real text before Compose conversion. Extra leading
            // margin here would wrap the bullet onto its own line.
        }
        is BlockQuoteSpan, is LeadingMarginSpan -> addQuoteIndent(plainText, start, end)
    }
}

internal const val RELEASE_NOTES_QUOTE_TAG = "release_notes_quote"

internal fun typefaceStyleToSpanStyle(style: Int): SpanStyle? = when (style) {
    TYPEFACE_BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
    TYPEFACE_ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
    TYPEFACE_BOLD_ITALIC -> SpanStyle(
        fontWeight = FontWeight.Bold,
        fontStyle = FontStyle.Italic,
    )
    else -> null
}

// android.graphics.Typeface constants. Unit-test stubs may not expose the SDK fields.
private const val TYPEFACE_BOLD = 1
private const val TYPEFACE_ITALIC = 2
private const val TYPEFACE_BOLD_ITALIC = 3

private fun AnnotatedString.Builder.addQuoteIndent(
    plainText: String,
    start: Int,
    end: Int,
) {
    val paragraphStart = plainText.lastIndexOf('\n', start - 1).let { if (it < 0) 0 else it + 1 }
    val nextBreak = plainText.indexOf('\n', end)
    val paragraphEnd = if (nextBreak < 0) plainText.length else nextBreak
    if (paragraphStart >= paragraphEnd) return
    addStringAnnotation(RELEASE_NOTES_QUOTE_TAG, "", paragraphStart, paragraphEnd)
    var lineStart = paragraphStart
    while (lineStart < paragraphEnd) {
        val lineBreak = plainText.indexOf('\n', lineStart)
        val lineEnd = when {
            lineBreak < 0 -> paragraphEnd
            lineBreak > paragraphEnd -> paragraphEnd
            else -> lineBreak
        }
        if (lineStart < lineEnd) {
            try {
                addStyle(
                    ParagraphStyle(
                        textIndent = TextIndent(
                            firstLine = 12.sp,
                            restLine = 12.sp,
                        ),
                    ),
                    lineStart,
                    lineEnd,
                )
            } catch (_: IllegalArgumentException) {
                // Paragraph styles must align to paragraph bounds; skip rather than fail the dialog.
            }
        }
        if (lineBreak < 0 || lineBreak >= paragraphEnd) {
            break
        }
        lineStart = lineBreak + 1
    }
}
