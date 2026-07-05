package com.dpis.module;
import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;
import com.dpis.module.viewport.ViewportTargetType;

import com.dpis.module.templates.TemplateConfigValueAdapters;

import com.dpis.module.templates.TemplateCustomSemantics;

import com.dpis.module.templates.TemplateConfigSummaryFormatter;

import com.dpis.module.templates.TemplateConfigValue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TemplateConfigSummaryFormatterTest {
    @Test
    public void formatsConfiguredTemplateSummary() {
        TemplateConfigSummaryFormatter formatter = newFormatter(id ->
                TemplateConfigSummaryFormatter.TypefaceStatus.resolved(id, "Demo Font"));
        TemplateConfigValue value = TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.relativeScale(112500),
                ViewportApplyMode.AUTO,
                120,
                FontApplyMode.FIELD_REWRITE,
                "font_demo",
                "resources_font");

        TemplateConfigSummaryFormatter.Result result = formatter.format(value);

        assertEquals(
                "Interface 112.5% · Font 120% · Compat · Demo Font · Custom hook chain",
                result.summary());
        assertEquals(3, result.summaryParts.size());
        assertEquals("Interface 112.5%", result.summaryParts.get(0));
        assertEquals("Font 120% · Compat · Demo Font",
                result.summaryParts.get(1));
        assertEquals("Custom hook chain", result.summaryParts.get(2));
        assertFalse(result.typefaceStatus.missing);
    }

    @Test
    public void surfacesMissingTypefaceWithoutDroppingOtherSummaryParts() {
        TemplateConfigSummaryFormatter formatter = newFormatter(id ->
                TemplateConfigSummaryFormatter.TypefaceStatus.missing(id));
        TemplateConfigValue value = TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.absoluteDp(411),
                ViewportApplyMode.COMPAT,
                null,
                FontApplyMode.OFF,
                "font_missing",
                null);

        TemplateConfigSummaryFormatter.Result result = formatter.format(value);

        assertEquals("Interface 411dp · Compat", result.summary());
        assertEquals(1, result.summaryParts.size());
        assertEquals("Interface 411dp · Compat", result.summaryParts.get(0));
        assertTrue(result.typefaceStatus.missing);
        assertEquals("font_missing", result.typefaceStatus.typefaceId);
    }

    @Test
    public void modeOnlyValuesShowNoValueSummaryParts() {
        TemplateConfigValue value = TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.off(),
                ViewportTargetType.ABSOLUTE_DP,
                ViewportApplyMode.COMPAT,
                null,
                FontApplyMode.FIELD_REWRITE,
                null,
                null);

        TemplateConfigSummaryFormatter.Result result = newFormatter(id -> null).format(value);

        assertEquals("Interface No value · Width · Compat · Font No value · Compat",
                result.summary());
        assertEquals(2, result.summaryParts.size());
    }

    @Test
    public void viewportApplyModeOnlyShowsConfiguredStrategy() {
        TemplateConfigValue value = TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.off(),
                ViewportTargetType.OFF,
                ViewportApplyMode.SYSTEM,
                null,
                FontApplyMode.OFF,
                null,
                null);

        TemplateConfigSummaryFormatter.Result result = newFormatter(id -> null).format(value);

        assertEquals("Interface No value · System", result.summary());
        assertEquals(1, result.summaryParts.size());
    }

    @Test
    public void autoViewportApplyModeDoesNotShowAsCustomSummary() {
        TemplateConfigValue value = TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.relativeScale(100000),
                ViewportApplyMode.AUTO,
                null,
                FontApplyMode.OFF,
                null,
                null);

        TemplateConfigSummaryFormatter.Result result = newFormatter(id -> null).format(value);

        assertEquals("Interface 100%", result.summary());
        assertEquals(1, result.summaryParts.size());
    }

    @Test
    public void emptyTemplateUsesEmptySummary() {
        TemplateConfigSummaryFormatter.Result result = newFormatter(id -> null)
                .format(TemplateConfigValue.EMPTY);

        assertEquals("No custom values.", result.summary());
        assertTrue(result.summaryParts.isEmpty());
        assertFalse(result.typefaceStatus.missing);
    }

    @Test
    public void customSemanticsDropsDefaultModesBeforeSummary() {
        TemplateConfigValue rawValue = TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.off(),
                ViewportTargetType.RELATIVE_SCALE,
                ViewportApplyMode.AUTO,
                null,
                FontApplyMode.SYSTEM_EMULATION,
                null,
                null);

        TemplateConfigValue customValue = TemplateCustomSemantics.customValue(rawValue);
        TemplateConfigSummaryFormatter.Result result = newFormatter(id -> null)
                .format(rawValue);

        assertFalse(customValue.hasAnyValue());
        assertEquals("No custom values.", result.summary());
        assertTrue(result.summaryParts.isEmpty());
    }

    @Test
    public void missingTypefaceOnlyDoesNotCreateConfiguredSummaryPart() {
        TemplateConfigValue value = TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.off(),
                ViewportApplyMode.OFF,
                null,
                FontApplyMode.OFF,
                "font_missing",
                null);

        TemplateConfigSummaryFormatter.Result result = newFormatter(id ->
                TemplateConfigSummaryFormatter.TypefaceStatus.missing(id)).format(value);

        assertEquals("No custom values.", result.summary());
        assertTrue(result.summaryParts.isEmpty());
        assertTrue(result.typefaceStatus.missing);
        assertEquals("font_missing", result.typefaceStatus.typefaceId);
    }

    private static TemplateConfigSummaryFormatter newFormatter(
            TemplateConfigSummaryFormatter.TypefaceResolver resolver) {
        return new TemplateConfigSummaryFormatter(new TestText(), resolver);
    }

    private static final class TestText implements TemplateConfigSummaryFormatter.Text {
        @Override
        public String emptySummary() {
            return "No custom values.";
        }

        @Override
        public String viewportSummary(String detail) {
            return "Interface " + detail;
        }

        @Override
        public String viewportTargetTypeScale() {
            return "Scale";
        }

        @Override
        public String viewportTargetTypeWidth() {
            return "Width";
        }

        @Override
        public String fontSummary(String detail) {
            return "Font " + detail;
        }

        @Override
        public String noValue() {
            return "No value";
        }

        @Override
        public String typeface(String displayName) {
            return "Font style: " + displayName;
        }

        @Override
        public String hookDomains() {
            return "Custom hook chain";
        }

        @Override
        public String modeAuto() {
            return "Auto";
        }

        @Override
        public String modeSystem() {
            return "System";
        }

        @Override
        public String modeCompat() {
            return "Compat";
        }
    }
}

