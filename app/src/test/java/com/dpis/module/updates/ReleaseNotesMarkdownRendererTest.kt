package com.dpis.module

import android.text.Spanned
import com.dpis.module.updates.ReleaseNotesListMarker
import com.dpis.module.updates.ReleaseNotesMarkerEdit
import com.dpis.module.updates.ReleaseNotesMarkdownRenderer
import com.dpis.module.updates.ReleaseNotesPlainLink
import com.dpis.module.updates.parseReleaseNoteMarkdownLinks
import com.dpis.module.updates.headingScale
import com.dpis.module.updates.insertMarkerText
import com.dpis.module.updates.insertVisibleListMarkers
import com.dpis.module.updates.isAllowedReleaseNotesUrl
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class ReleaseNotesMarkdownRendererTest {
    @After
    fun tearDown() {
        ReleaseNotesMarkdownRenderer.setRendererForTesting(null)
    }

    @Test
    fun emptyMarkdownRendersEmpty() {
        val rendered = ReleaseNotesMarkdownRenderer.render(
            null,
            "   ",
            Locale.ENGLISH,
        )
        assertEquals("", rendered.toString())
    }

    @Test
    fun filtersLocaleBeforeRendering() {
        ReleaseNotesMarkdownRenderer.setRendererForTesting { _, markdown -> markdown }
        val markdown = "### 中文\n- 一\n\n---\n\n### English\n- one"

        val rendered = ReleaseNotesMarkdownRenderer.render(
            null,
            markdown,
            Locale.SIMPLIFIED_CHINESE,
        )

        assertTrue(rendered.toString().contains("中文"))
        assertFalse(rendered.toString().contains("English"))
    }

    @Test
    fun fallsBackToPlainTextWhenRendererThrows() {
        ReleaseNotesMarkdownRenderer.setRendererForTesting { _, _ ->
            throw RuntimeException("boom")
        }
        val markdown = "### Fixes\n- **Crash** on [release](https://github.com/Kwensiu/DPIS)"

        val rendered = ReleaseNotesMarkdownRenderer.render(
            null,
            markdown,
            Locale.ENGLISH,
        )

        assertTrue(rendered.toString().contains("Fixes"))
        assertTrue(rendered.toString().contains("Crash"))
        assertTrue(rendered.toString().contains("release"))
        assertFalse(rendered.toString().contains("**"))
    }

    @Test
    fun toleratesCommonGithubMarkdownShapes() {
        ReleaseNotesMarkdownRenderer.setRendererForTesting { _, markdown -> markdown }
        val markdown = """
            ## [1.0.0](https://example.com) (2026-06-06)

            ### 中文
            - [x] 修复崩溃
            > 引用
            | A | B |
            |---|---|
            | 1 | 2 |
            ![image](https://example.com/a.png)
            ```java
            broken fence
        """.trimIndent()

        val rendered = ReleaseNotesMarkdownRenderer.render(
            null,
            markdown,
            Locale.SIMPLIFIED_CHINESE,
        )

        assertTrue(rendered.toString().contains("修复崩溃"))
        assertFalse(rendered.toString().contains("1.0.0"))
    }

    @Test
    fun recentReleaseNotesStayReadableWhenMarkwonCannotRun() {
        val markdown = """
            ## [2.0.0](https://github.com/Kwensiu/DPIS/compare/v1.15.0...v2.0.0) (2026-08-30)

            > 务必导出配置备份后更新
            > 全新界面和自适应体验为核心的大版本更新。

            - **全新 Compose 界面**：主界面采用统一的 Material 3 设计
            - 字体库支持 `.ttc` 字体集合

            ### 升级提示

            - 建议优先使用 **Modern** 版本

            ---

            ### Upgrade Notes

            - Use the **Modern** version first
        """.trimIndent()

        val rendered = ReleaseNotesMarkdownRenderer.render(
            null,
            markdown,
            Locale.SIMPLIFIED_CHINESE,
        )

        val text = rendered.toString()
        assertTrue(text.contains("务必导出配置备份后更新"))
        assertTrue(text.contains("全新 Compose 界面"))
        assertTrue(text.contains("升级提示"))
        assertTrue(text.contains(".ttc"))
        assertFalse(text.contains("Upgrade Notes"))
        assertFalse(text.contains("**"))
        assertFalse(text.contains("2.0.0"))
    }

    @Test
    fun stripsFencesHeadingsAndNullLines() {
        assertEquals("", ReleaseNotesMarkdownRenderer.stripMarkdownLine(null))
        assertEquals("", ReleaseNotesMarkdownRenderer.stripMarkdownLine("```java"))
        assertEquals("Fixes", ReleaseNotesMarkdownRenderer.stripMarkdownLine("### Fixes"))
        assertEquals("• Crash", ReleaseNotesMarkdownRenderer.stripMarkdownLine("- Crash"))
    }

    @Test
    fun onlyHttpsReleaseNoteUrlsAreClickable() {
        assertTrue(isAllowedReleaseNotesUrl("https://github.com/Kwensiu/DPIS"))
        assertFalse(isAllowedReleaseNotesUrl("http://example.com"))
        assertFalse(isAllowedReleaseNotesUrl("javascript:alert(1)"))
        assertFalse(isAllowedReleaseNotesUrl("https://user:pass@example.com/path"))
        assertEquals(
            listOf(ReleaseNotesPlainLink("safe", "https://github.com/Kwensiu/DPIS")),
            parseReleaseNoteMarkdownLinks(
                "See [safe](https://github.com/Kwensiu/DPIS) and [skip](javascript:alert(1)) and [broken](not-a-url",
            ),
        )

        val rendered = ReleaseNotesMarkdownRenderer.render(
            null,
            "See [safe](https://github.com/Kwensiu/DPIS) and [skip](javascript:alert(1))",
            Locale.ENGLISH,
        )
        assertTrue(rendered.toString().contains("safe"))
        assertTrue(rendered.toString().contains("[skip](javascript:alert(1))"))
    }

    @Test
    fun insertsListMarkersWithoutWrappingOntoTheirOwnLine() {
        assertEquals("plain", insertVisibleListMarkers("plain").toString())
        assertEquals(1.35f, headingScale(1), 0.001f)
        assertEquals(1.22f, headingScale(2), 0.001f)
        assertEquals(1.12f, headingScale(3), 0.001f)
        assertEquals(1.05f, headingScale(6), 0.001f)
        assertEquals(
            "\n• 全新 Compose 界面",
            insertMarkerText(
                "\n全新 Compose 界面",
                ReleaseNotesMarkerEdit(0, 10, "• "),
            ).toString(),
        )
        assertEquals(
            "2. item",
            insertMarkerText("item", ReleaseNotesMarkerEdit(0, 4, "2. ")).toString(),
        )
        assertEquals(
            "• item",
            insertMarkerText("• item", ReleaseNotesMarkerEdit(0, 6, "• ")).toString(),
        )

        val loose = MarkerSpanned(
            "\n全新 Compose 界面",
            ReleaseNotesListMarker("• "),
            0,
            10,
        )
        assertEquals("\n• 全新 Compose 界面", insertVisibleListMarkers(loose).toString())
    }

    private class MarkerSpanned(
        private val text: String,
        private val marker: ReleaseNotesListMarker,
        private val start: Int,
        private val end: Int,
    ) : Spanned, CharSequence by text {
        override fun toString(): String = text

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any> getSpans(start: Int, end: Int, type: Class<T>): Array<T> {
            if (!type.isInstance(marker)) {
                val empty = java.lang.reflect.Array.newInstance(type, 0)
                return empty as Array<T>
            }
            val array = java.lang.reflect.Array.newInstance(type, 1)
            java.lang.reflect.Array.set(array, 0, marker)
            return array as Array<T>
        }

        override fun getSpanStart(tag: Any): Int = if (tag === marker) start else -1

        override fun getSpanEnd(tag: Any): Int = if (tag === marker) end else -1

        override fun getSpanFlags(tag: Any): Int = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

        override fun nextSpanTransition(start: Int, limit: Int, type: Class<out Any>?): Int = limit
    }
}

