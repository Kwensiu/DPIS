package com.dpis.module.ui.compose

import android.text.Spanned
import android.text.style.URLSpan
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString

/** Preserves Android URL spans when controller-owned rich text crosses into Compose. */
internal fun CharSequence.toComposeAnnotatedString(): AnnotatedString {
    if (this !is Spanned) return AnnotatedString(toString())
    return buildAnnotatedString {
        append(toString())
        getSpans(0, length, URLSpan::class.java).forEach { span ->
            val start = getSpanStart(span).coerceAtLeast(0)
            val end = getSpanEnd(span).coerceAtMost(length)
            if (start < end) addLink(LinkAnnotation.Url(span.url), start, end)
        }
    }
}
