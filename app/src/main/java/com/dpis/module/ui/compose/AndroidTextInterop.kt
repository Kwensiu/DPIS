package com.dpis.module.ui.compose

import android.text.Spanned
import android.text.style.URLSpan
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString

/** Preserves Android URL spans when controller-owned rich text crosses into Compose. */
internal fun CharSequence.toComposeAnnotatedString(): AnnotatedString {
    val plainText = toString()
    if (this !is Spanned) return AnnotatedString(plainText)
    return buildAnnotatedString {
        // Capture the source text before entering the builder receiver scope. Calling
        // toString() here would stringify AnnotatedString.Builder instead of the notes.
        append(plainText)
        getSpans(0, length, URLSpan::class.java).forEach { span ->
            val start = getSpanStart(span).coerceAtLeast(0)
            val end = getSpanEnd(span).coerceAtMost(length)
            if (start < end) addLink(LinkAnnotation.Url(span.url), start, end)
        }
    }
}
