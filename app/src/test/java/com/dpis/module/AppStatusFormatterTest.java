package com.dpis.module;

import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;

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
            "Not installed",
            "No value",
            "System",
            "Compat",
            "Interface",
            "Interface",
            "Font",
            "WeChat DPI",
            Locale.US);

    private final AppStatusFormatter.Labels chineseLabels = new AppStatusFormatter.Labels(
            "\u5DF2\u6CE8\u5165",
            "\u672A\u6CE8\u5165",
            "\u5DF2\u542F\u7528",
            "\u5DF2\u7981\u7528",
            "\u672A\u542F\u7528",
            "\u672A\u5B89\u88C5",
            "\u7A7A\u6570\u503C",
            "\u7CFB\u7EDF",
            "\u517C\u5BB9",
            "\u754C\u9762",
            "\u754C\u9762",
            "\u5B57\u4F53",
            "\u5FAE\u4FE1 DPI",
            Locale.CHINA);

    @Test
    public void formatsOutOfScopeDisabledStateWithLabels() {
        assertEquals("Not injected | Not enabled",
                AppStatusFormatter.format(englishLabels,
                        input(false, null, null, null, FontApplyMode.OFF, null, true)));
    }

    @Test
    public void formatsInScopeEnabledStateWithLabels() {
        assertEquals("Injected | Interface 320dp(System) | Font[C] 115%(System)",
                AppStatusFormatter.format(
                        englishLabels,
                        input(true, 320, ViewportApplyMode.SYSTEM_EMULATION, 115,
                                FontApplyMode.SYSTEM_EMULATION, "font_roboto", true)));
    }

    @Test
    public void formatsFontOnlyStateWithChineseLabels() {
        assertEquals("\u672A\u6CE8\u5165 | \u672A\u542F\u7528 | \u5B57\u4F53[C] 110%(\u517C\u5BB9)",
                AppStatusFormatter.format(
                        chineseLabels,
                        input(false, null, ViewportApplyMode.OFF, 110,
                                FontApplyMode.FIELD_REWRITE, "font_noto", true)));
    }

    @Test
    public void formatsDpisDisabledStateWithLabels() {
        assertEquals("Injected | Disabled",
                AppStatusFormatter.format(
                        englishLabels,
                        input(true, 360, ViewportApplyMode.SYSTEM_EMULATION, 120,
                                FontApplyMode.SYSTEM_EMULATION, "font_roboto", false)));
    }

    @Test
    public void formatsCompactStatusWithoutStringStripping() {
        assertEquals("Injected | Interface 320dp | Font[C] 115%",
                AppStatusFormatter.formatCompact(
                        englishLabels,
                        input(true, 320, ViewportApplyMode.SYSTEM_EMULATION, 115,
                                FontApplyMode.SYSTEM_EMULATION, "font_roboto", true)));
    }

    @Test
    public void hidesStatusScopeSegmentWhenScopeCannotBeRead() {
        assertEquals("Interface 320dp",
                AppStatusFormatter.formatCompact(
                        englishLabels,
                        input(false, false, 320, ViewportApplyMode.FIELD_REWRITE, null,
                                FontApplyMode.OFF, null, true)));
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
        String text = "Injected | Interface 360dp | Font[C] 120%";
        int[][] ranges = AppStatusFormatter.resolveWarnSegmentRanges(text, true, false);
        assertEquals(1, ranges.length);
        assertArrayEquals(resolveExpectedRange(text, "Interface 360dp"), ranges[0]);
    }

    @Test
    public void warnsOnlyFontSegmentWhenOnlyFontEmulationFails() {
        String text = "Injected | Interface 360dp | Font[C] 120%";
        int[][] ranges = AppStatusFormatter.resolveWarnSegmentRanges(text, false, true);
        assertEquals(1, ranges.length);
        assertArrayEquals(resolveExpectedRange(text, "Font[C] 120%"), ranges[0]);
    }

    @Test
    public void warnsBothSegmentsWhenBothEmulationsFail() {
        String text = "Injected | Interface 360dp | Font[C] 120%";
        int[][] ranges = AppStatusFormatter.resolveWarnSegmentRanges(text, true, true);
        assertEquals(2, ranges.length);
        assertArrayEquals(resolveExpectedRange(text, "Interface 360dp"), ranges[0]);
        assertArrayEquals(resolveExpectedRange(text, "Font[C] 120%"), ranges[1]);
    }

    @Test
    public void warnsSegmentsWhenScopeSegmentIsHidden() {
        String text = "Interface 106% | Font 115%";
        int[][] ranges = AppStatusFormatter.resolveWarnSegmentRanges(text, true, true);
        assertEquals(2, ranges.length);
        assertArrayEquals(resolveExpectedRange(text, "Interface 106%"), ranges[0]);
        assertArrayEquals(resolveExpectedRange(text, "Font 115%"), ranges[1]);
    }

    @Test
    public void warnsOnlyViewportSegmentWhenScopeAndFontSegmentsAreHidden() {
        String text = "Interface 320dp";
        int[][] ranges = AppStatusFormatter.resolveWarnSegmentRanges(text, true, false);
        assertEquals(1, ranges.length);
        assertArrayEquals(resolveExpectedRange(text, "Interface 320dp"), ranges[0]);
    }

    @Test
    public void returnsNoRangesWhenNoSegmentNeedsWarning() {
        String text = "Injected | Interface 360dp | Font 120%";
        int[][] ranges = AppStatusFormatter.resolveWarnSegmentRanges(text, false, false);
        assertEquals(0, ranges.length);
    }

    @Test
    public void formatsRelativeScaleCompactStatus() {
        assertEquals("Injected | Interface 106% | Font 115%",
                AppStatusFormatter.formatCompact(
                        englishLabels,
                        input(true, true, ViewportTargetSpec.relativeScale(106000),
                                ViewportApplyMode.AUTO, 115, FontApplyMode.SYSTEM_EMULATION,
                                null, true)));
    }

    @Test
    public void compactHidesCustomTypefaceWithoutFontScaleWhenViewportHasValue() {
        assertEquals("Injected | Interface 100%",
                AppStatusFormatter.formatCompact(
                        englishLabels,
                        input(true, true, ViewportTargetSpec.relativeScale(100000),
                                ViewportApplyMode.AUTO, null, FontApplyMode.OFF,
                                "font_roboto", true)));
    }

    @Test
    public void compactInstalledConfiguredWithoutNumericValueShowsNoValue() {
        assertEquals("Injected | No value",
                AppStatusFormatter.formatCompact(
                        englishLabels,
                        input(true, true, true, ViewportTargetSpec.off(),
                                ViewportApplyMode.SYSTEM_EMULATION, null,
                                FontApplyMode.SYSTEM_EMULATION, null, true, false, null)));
    }

    @Test
    public void compactUninstalledConfiguredWithoutNumericValueShowsNotInstalledNoValue() {
        assertEquals("\u672A\u5B89\u88C5 | \u7A7A\u6570\u503C",
                AppStatusFormatter.formatCompact(
                        chineseLabels,
                        input(false, true, false, ViewportTargetSpec.off(),
                                ViewportApplyMode.SYSTEM_EMULATION, null,
                                FontApplyMode.SYSTEM_EMULATION, null, true, false, null)));
    }

    @Test
    public void compactWechatDpiOnlyShowsAppSpecificValue() {
        assertEquals("\u5DF2\u6CE8\u5165 | \u5FAE\u4FE1 DPI 440",
                AppStatusFormatter.formatCompact(
                        chineseLabels,
                        input(true, true, true, ViewportTargetSpec.off(),
                                ViewportApplyMode.OFF, null, FontApplyMode.OFF,
                                null, true, true, 440)));
    }

    @Test
    public void compactHidesModeOnlyFontWhenViewportHasValue() {
        assertEquals("Injected | Interface 360dp",
                AppStatusFormatter.formatCompact(
                        englishLabels,
                        input(true, true, ViewportTargetSpec.absoluteDp(360),
                                ViewportApplyMode.AUTO, null, FontApplyMode.SYSTEM_EMULATION,
                                null, true)));
    }

    @Test
    public void compactHidesModeOnlyViewportWhenFontHasValue() {
        assertEquals("Injected | Font 115%",
                AppStatusFormatter.formatCompact(
                        englishLabels,
                        input(true, true, (ViewportTargetSpec) null,
                                ViewportApplyMode.SYSTEM_EMULATION, 115,
                                FontApplyMode.SYSTEM_EMULATION, null, true)));
    }

    private static int[] resolveExpectedRange(String fullText, String segmentText) {
        int expectedStart = fullText.indexOf(segmentText);
        assertTrue(expectedStart >= 0);
        int expectedEnd = expectedStart + segmentText.length();
        return new int[] { expectedStart, expectedEnd };
    }

    private static AppStatusFormatter.StatusInput input(boolean inScope,
            Integer viewportWidthDp,
            String viewportMode,
            Integer fontScalePercent,
            String fontMode,
            String typefaceId,
            boolean dpisEnabled) {
        return new AppStatusFormatter.StatusInput(inScope, viewportWidthDp, viewportMode,
                fontScalePercent, fontMode, typefaceId, dpisEnabled);
    }

    private static AppStatusFormatter.StatusInput input(boolean inScope,
            boolean scopeKnown,
            Integer viewportWidthDp,
            String viewportMode,
            Integer fontScalePercent,
            String fontMode,
            String typefaceId,
            boolean dpisEnabled) {
        return new AppStatusFormatter.StatusInput(inScope, scopeKnown, viewportWidthDp,
                viewportMode, fontScalePercent, fontMode, typefaceId, dpisEnabled);
    }

    private static AppStatusFormatter.StatusInput input(boolean inScope,
            boolean scopeKnown,
            ViewportTargetSpec viewportTargetSpec,
            String viewportMode,
            Integer fontScalePercent,
            String fontMode,
            String typefaceId,
            boolean dpisEnabled) {
        return new AppStatusFormatter.StatusInput(inScope, scopeKnown, viewportTargetSpec,
                viewportMode, fontScalePercent, fontMode, typefaceId, dpisEnabled);
    }

    private static AppStatusFormatter.StatusInput input(boolean inScope,
            boolean scopeKnown,
            boolean installed,
            ViewportTargetSpec viewportTargetSpec,
            String viewportMode,
            Integer fontScalePercent,
            String fontMode,
            String typefaceId,
            boolean dpisEnabled,
            boolean appSpecificConfigActive,
            Integer wechatDpi) {
        return new AppStatusFormatter.StatusInput(inScope, scopeKnown, installed,
                viewportTargetSpec, viewportMode, fontScalePercent, fontMode, typefaceId,
                dpisEnabled, appSpecificConfigActive, wechatDpi);
    }
}

