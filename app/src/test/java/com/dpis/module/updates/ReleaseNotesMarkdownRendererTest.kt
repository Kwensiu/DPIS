package com.dpis.module

import com.dpis.module.updates.ReleaseNotesMarkdownRenderer
import org.junit.After
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
}

