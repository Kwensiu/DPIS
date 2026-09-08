package com.dpis.module.updates

import android.text.Spanned
import android.text.style.LeadingMarginSpan
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import io.noties.markwon.core.spans.StrongEmphasisSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseNotesComposeTextTest {
    @Test
    fun mapsBoldConstantAndKeepsReleaseNoteText() {
        val text = "升级提示 Crash .ttc release"
        val annotated = text.toReleaseNotesAnnotatedString()

        assertEquals(text, annotated.text)
        assertEquals(FontWeight.Bold, typefaceStyleToSpanStyle(1)?.fontWeight)
        assertEquals(FontStyle.Italic, typefaceStyleToSpanStyle(2)?.fontStyle)
        assertEquals(FontWeight.Bold, typefaceStyleToSpanStyle(3)?.fontWeight)
        assertEquals(null, typefaceStyleToSpanStyle(0))
    }

    @Test
    fun listMarkerStaysOnSameLineAsLooseListItemBody() {
        assertEquals(1, firstContentIndex("\n全新 Compose 界面", 0, 10))
        assertEquals(0, firstContentIndex("全新 Compose 界面", 0, 4))
        assertTrue(hasPrefix("• 全新", 0, "• "))
        assertFalse(hasPrefix("全新", 0, "• "))
        assertFalse(hasPrefix("•", 0, "• "))
    }

    @Test
    fun mapsMarkwonStrongEmphasisToBold() {
        val text = "Crash"
        val spanned = TestSpanned(
            text,
            listOf(TestSpan(StrongEmphasisSpan(), 0, text.length)),
        )
        val annotated = spanned.toReleaseNotesAnnotatedString()
        assertTrue(
            annotated.spanStyles.any {
                it.start == 0 &&
                    it.end == text.length &&
                    it.item.fontWeight == FontWeight.Bold
            },
        )
    }

    @Test
    fun keepsVisibleListMarkers() {
        val annotated = "• 全新 Compose 界面".toReleaseNotesAnnotatedString()
        assertTrue(annotated.text.startsWith("• "))
    }

    @Test
    fun indentsQuotedReleaseWarning() {
        val text = "务必导出配置备份后更新"
        val spanned = TestSpanned(
            text,
            listOf(TestSpan(LeadingMarginSpan.Standard(24), 0, text.length)),
        )

        val annotated = spanned.toReleaseNotesAnnotatedString()

        assertEquals(text, annotated.text)
        assertFalse(annotated.paragraphStyles.isEmpty())
        assertFalse(
            annotated.getStringAnnotations(RELEASE_NOTES_QUOTE_TAG, 0, text.length).isEmpty(),
        )
    }

    @Test
    fun malformedMarkdownStaysReadablePlainText() {
        val text = "### 中文\n```java\nline\n[broken](not-a-url"
        val annotated = text.toReleaseNotesAnnotatedString()
        assertEquals(text, annotated.text)
        assertTrue(annotated.spanStyles.isEmpty())
    }

    private data class TestSpan(val span: Any, val start: Int, val end: Int)

    private class TestSpanned(
        private val text: String,
        private val spans: List<TestSpan>,
    ) : Spanned, CharSequence by text {
        override fun toString(): String = text

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any> getSpans(start: Int, end: Int, type: Class<T>): Array<T> {
            val matched = spans.map { it.span }.filter { type.isInstance(it) }
            val array = java.lang.reflect.Array.newInstance(type, matched.size)
            matched.forEachIndexed { index, span ->
                java.lang.reflect.Array.set(array, index, span)
            }
            return array as Array<T>
        }

        override fun getSpanStart(tag: Any): Int = spans.firstOrNull { it.span === tag }?.start ?: -1

        override fun getSpanEnd(tag: Any): Int = spans.firstOrNull { it.span === tag }?.end ?: -1

        override fun getSpanFlags(tag: Any): Int = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

        override fun nextSpanTransition(start: Int, limit: Int, type: Class<out Any>?): Int = limit
    }
}
