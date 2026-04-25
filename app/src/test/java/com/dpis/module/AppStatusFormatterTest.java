package com.dpis.module;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AppStatusFormatterTest {
    private final AppStatusFormatter.Labels englishLabels = new AppStatusFormatter.Labels(
            "Injected",
            "Not injected",
            "Enabled",
            "Disabled",
            "Not enabled",
            "Emulation",
            "Replace",
            "Font",
            Locale.US);

    private final AppStatusFormatter.Labels chineseLabels = new AppStatusFormatter.Labels(
            "\u5DF2\u6CE8\u5165",
            "\u672A\u6CE8\u5165",
            "\u5DF2\u542F\u7528",
            "\u5DF2\u7981\u7528",
            "\u672A\u542F\u7528",
            "\u4F2A\u88C5",
            "\u66FF\u6362",
            "\u5B57\u4F53",
            Locale.CHINA);

    @Test
    public void formatsOutOfScopeDisabledStateWithLabels() {
        assertEquals("Not injected | Not enabled",
                AppStatusFormatter.format(englishLabels,
                        false, null, null, null, FontApplyMode.OFF, true));
    }

    @Test
    public void formatsInScopeEnabledStateWithLabels() {
        assertEquals("Injected | 320dp(Emulation) | Font115%(Emulation)",
                AppStatusFormatter.format(
                        englishLabels,
                        true,
                        320,
                        ViewportApplyMode.SYSTEM_EMULATION,
                        115,
                        FontApplyMode.SYSTEM_EMULATION,
                        true));
    }

    @Test
    public void formatsFontOnlyStateWithChineseLabels() {
        assertEquals("\u672A\u6CE8\u5165 | \u672A\u542F\u7528 | \u5B57\u4F53110%(\u66FF\u6362)",
                AppStatusFormatter.format(
                        chineseLabels,
                        false,
                        null,
                        ViewportApplyMode.OFF,
                        110,
                        FontApplyMode.FIELD_REWRITE,
                        true));
    }

    @Test
    public void formatsDpisDisabledStateWithLabels() {
        assertEquals("Injected | Disabled",
                AppStatusFormatter.format(
                        englishLabels,
                        true,
                        360,
                        ViewportApplyMode.SYSTEM_EMULATION,
                        120,
                        FontApplyMode.SYSTEM_EMULATION,
                        false));
    }

    @Test
    public void formatsCompactStatusWithoutStringStripping() {
        assertEquals("Injected | 320dp | 115%",
                AppStatusFormatter.formatCompact(
                        englishLabels,
                        true,
                        320,
                        ViewportApplyMode.SYSTEM_EMULATION,
                        115,
                        FontApplyMode.SYSTEM_EMULATION,
                        true));
    }

    @Test
    public void warnsViewportEmulationWhenSystemHooksDisabled() {
        assertTrue(AppStatusFormatter.shouldWarnViewportEmulation(
                360,
                ViewportApplyMode.SYSTEM_EMULATION,
                false,
                true));
    }

    @Test
    public void doesNotWarnViewportEmulationWhenSystemHooksEnabled() {
        assertFalse(AppStatusFormatter.shouldWarnViewportEmulation(
                360,
                ViewportApplyMode.SYSTEM_EMULATION,
                true,
                true));
    }

    @Test
    public void warnsFontEmulationWhenSystemHooksDisabled() {
        assertTrue(AppStatusFormatter.shouldWarnFontEmulation(
                120,
                FontApplyMode.SYSTEM_EMULATION,
                false,
                true));
    }

    @Test
    public void doesNotWarnAnyEmulationWhenDpisDisabled() {
        assertFalse(AppStatusFormatter.shouldWarnViewportEmulation(
                360,
                ViewportApplyMode.SYSTEM_EMULATION,
                false,
                false));
        assertFalse(AppStatusFormatter.shouldWarnFontEmulation(
                120,
                FontApplyMode.SYSTEM_EMULATION,
                false,
                false));
    }

    @Test
    public void warnsOnlyViewportSegmentWhenOnlyViewportEmulationFails() {
        String text = "Injected | 360dp | 120%";
        int[][] ranges = AppStatusFormatter.resolveWarnSegmentRanges(text, true, false);
        assertEquals(1, ranges.length);
        assertArrayEquals(resolveExpectedRange(text, "360dp"), ranges[0]);
    }

    @Test
    public void warnsOnlyFontSegmentWhenOnlyFontEmulationFails() {
        String text = "Injected | 360dp | 120%";
        int[][] ranges = AppStatusFormatter.resolveWarnSegmentRanges(text, false, true);
        assertEquals(1, ranges.length);
        assertArrayEquals(resolveExpectedRange(text, "120%"), ranges[0]);
    }

    @Test
    public void warnsBothSegmentsWhenBothEmulationsFail() {
        String text = "Injected | 360dp | 120%";
        int[][] ranges = AppStatusFormatter.resolveWarnSegmentRanges(text, true, true);
        assertEquals(2, ranges.length);
        assertArrayEquals(resolveExpectedRange(text, "360dp"), ranges[0]);
        assertArrayEquals(resolveExpectedRange(text, "120%"), ranges[1]);
    }

    @Test
    public void returnsNoRangesWhenNoSegmentNeedsWarning() {
        String text = "Injected | 360dp | 120%";
        int[][] ranges = AppStatusFormatter.resolveWarnSegmentRanges(text, false, false);
        assertEquals(0, ranges.length);
    }

    private static int[] resolveExpectedRange(String fullText, String segmentText) {
        int expectedStart = fullText.indexOf(segmentText);
        assertTrue(expectedStart >= 0);
        int expectedEnd = expectedStart + segmentText.length();
        return new int[]{expectedStart, expectedEnd};
    }
}
