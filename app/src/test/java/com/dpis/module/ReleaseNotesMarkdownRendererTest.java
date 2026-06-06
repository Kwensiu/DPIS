package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.After;
import org.junit.Test;

public class ReleaseNotesMarkdownRendererTest {
    @After
    public void tearDown() {
        ReleaseNotesMarkdownRenderer.setRendererForTesting(null);
    }

    @Test
    public void filtersLocaleBeforeRendering() {
        ReleaseNotesMarkdownRenderer.setRendererForTesting((context, markdown) -> markdown);
        String markdown = "### 中文\n- 一\n\n---\n\n### English\n- one";

        CharSequence rendered = ReleaseNotesMarkdownRenderer.render(
                null,
                markdown,
                Locale.SIMPLIFIED_CHINESE);

        assertTrue(rendered.toString().contains("中文"));
        assertFalse(rendered.toString().contains("English"));
    }

    @Test
    public void fallsBackToPlainTextWhenRendererThrows() {
        ReleaseNotesMarkdownRenderer.setRendererForTesting((context, markdown) -> {
            throw new RuntimeException("boom");
        });
        String markdown = "### Fixes\n- **Crash** on [release](https://github.com/Kwensiu/DPIS)";

        CharSequence rendered = ReleaseNotesMarkdownRenderer.render(
                null,
                markdown,
                Locale.ENGLISH);

        assertTrue(rendered.toString().contains("Fixes"));
        assertTrue(rendered.toString().contains("Crash"));
        assertTrue(rendered.toString().contains("release"));
        assertFalse(rendered.toString().contains("**"));
    }

    @Test
    public void toleratesCommonGithubMarkdownShapes() {
        ReleaseNotesMarkdownRenderer.setRendererForTesting((context, markdown) -> markdown);
        String markdown = "## [1.0.0](https://example.com) (2026-06-06)\n"
                + "\n"
                + "### 中文\n"
                + "- [x] 修复崩溃\n"
                + "> 引用\n"
                + "| A | B |\n"
                + "|---|---|\n"
                + "| 1 | 2 |\n"
                + "![image](https://example.com/a.png)\n"
                + "```java\n"
                + "broken fence\n";

        CharSequence rendered = ReleaseNotesMarkdownRenderer.render(
                null,
                markdown,
                Locale.SIMPLIFIED_CHINESE);

        assertTrue(rendered.toString().contains("修复崩溃"));
        assertFalse(rendered.toString().contains("1.0.0"));
    }
}
