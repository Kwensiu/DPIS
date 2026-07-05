package com.dpis.module;

import com.dpis.module.updates.ReleaseNotesMarkdownLite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Test;

public class ReleaseNotesMarkdownLiteTest {
    private static final String SAMPLE = "## [1.7.0](https://github.com/Kwensiu/DPIS/compare/v1.6.3...v1.7.0) (2026-05-09)\n"
            + "\n"
            + "### 功能\n"
            + "* 增加 HyperOS 支持 ([#37](https://github.com/Kwensiu/DPIS/issues/37))\n"
            + "\n"
            + "---\n"
            + "\n"
            + "### Features\n"
            + "* add HyperOS support ([#37](https://github.com/Kwensiu/DPIS/issues/37))\n";

    @Test
    public void stripsVersionHeadingAndShowsChineseSectionForChineseLocale() {
        String rendered = ReleaseNotesMarkdownLite.filterBodyForLocale(
                SAMPLE,
                Locale.SIMPLIFIED_CHINESE);

        assertFalse(rendered.contains("1.7.0"));
        assertTrue(rendered.contains("功能"));
        assertTrue(rendered.contains("增加 HyperOS 支持"));
        assertFalse(rendered.contains("Features"));
    }

    @Test
    public void showsEnglishSectionForNonChineseLocale() {
        String rendered = ReleaseNotesMarkdownLite.filterBodyForLocale(SAMPLE, Locale.ENGLISH);

        assertTrue(rendered.contains("Features"));
        assertTrue(rendered.contains("add HyperOS support"));
        assertFalse(rendered.contains("功能"));
    }

    @Test
    public void acceptsDividerLineWithExtraDashesAndSpaces() {
        String markdown = "### 中文\n* 你好\n\n  ----  \n\n### English\n* hello";

        String rendered = ReleaseNotesMarkdownLite.filterBodyForLocale(
                markdown,
                Locale.SIMPLIFIED_CHINESE);

        assertEquals("### 中文\n* 你好", rendered.trim());
    }

    @Test
    public void fallsBackWhenPreferredLanguageSectionIsEmpty() {
        String markdown = "\n\n---\n\n### Features\n* hello\n";

        String rendered = ReleaseNotesMarkdownLite.filterBodyForLocale(
                markdown,
                Locale.SIMPLIFIED_CHINESE);

        assertTrue(rendered.contains("Features"));
        assertTrue(rendered.contains("hello"));
    }

    @Test
    public void keepsMarkdownLinksWhenOnlyFilteringByLocale() {
        String rendered = ReleaseNotesMarkdownLite.filterBodyForLocale(
                "* fix ([abc123](https://github.com/Kwensiu/DPIS/commit/abc123))",
                Locale.ENGLISH);

        assertTrue(rendered.contains("abc123"));
        assertTrue(rendered.contains("https://github.com"));
    }

    @Test
    public void usesChineseTopAndEnglishBottomAroundDivider() {
        String markdown = "### 中文\n- 一\n\n---\n\n### English\n- one";

        String zh = ReleaseNotesMarkdownLite.filterBodyForLocale(markdown, Locale.SIMPLIFIED_CHINESE);
        String en = ReleaseNotesMarkdownLite.filterBodyForLocale(markdown, Locale.ENGLISH);

        assertTrue(zh.contains("中文"));
        assertFalse(zh.contains("English"));
        assertTrue(en.contains("English"));
        assertFalse(en.contains("中文"));
    }

    @Test
    public void unclosedCodeFenceFallsBackToNormalText() {
        String markdown = "### 中文\n```java\nline\nstill text";

        String rendered = ReleaseNotesMarkdownLite.filterBodyForLocale(markdown, Locale.SIMPLIFIED_CHINESE);

        assertTrue(rendered.contains("```java"));
        assertTrue(rendered.contains("still text"));
    }
}
