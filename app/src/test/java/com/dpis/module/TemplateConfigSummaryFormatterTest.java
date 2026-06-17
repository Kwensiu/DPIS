package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TemplateConfigSummaryFormatterTest {
    @Test
    public void formatsConfiguredTemplateSummary() {
        TemplateConfigSummaryFormatter formatter = newFormatter(id ->
                TemplateConfigSummaryFormatter.TypefaceStatus.resolved(id, "Demo Font"));
        TemplateConfigValue value = new TemplateConfigValue(
                ViewportTargetSpec.relativeScale(1125),
                ViewportApplyMode.AUTO,
                120,
                FontApplyMode.FIELD_REWRITE,
                "font_demo",
                "resources_font");

        TemplateConfigSummaryFormatter.Result result = formatter.format(value);

        assertEquals(
                "Interface 112.5% · Auto · Font 120% · Compat · Demo Font · Custom hook chain",
                result.summary());
        assertEquals(2, result.summaryParts.size());
        assertEquals("Interface 112.5% · Auto", result.summaryParts.get(0));
        assertEquals("Font 120% · Compat · Demo Font · Custom hook chain",
                result.summaryParts.get(1));
        assertFalse(result.typefaceStatus.missing);
    }

    @Test
    public void surfacesMissingTypefaceWithoutDroppingOtherSummaryParts() {
        TemplateConfigSummaryFormatter formatter = newFormatter(id ->
                TemplateConfigSummaryFormatter.TypefaceStatus.missing(id));
        TemplateConfigValue value = new TemplateConfigValue(
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
    public void routeOnlyValuesDoNotCreateSummaryParts() {
        TemplateConfigValue value = new TemplateConfigValue(
                ViewportTargetSpec.off(),
                ViewportApplyMode.COMPAT,
                null,
                FontApplyMode.FIELD_REWRITE,
                null,
                null);

        TemplateConfigSummaryFormatter.Result result = newFormatter(id -> null).format(value);

        assertEquals("No values configured.", result.summary());
        assertTrue(result.summaryParts.isEmpty());
    }

    @Test
    public void emptyTemplateUsesEmptySummary() {
        TemplateConfigSummaryFormatter.Result result = newFormatter(id -> null)
                .format(TemplateConfigValue.EMPTY);

        assertEquals("No values configured.", result.summary());
        assertTrue(result.summaryParts.isEmpty());
        assertFalse(result.typefaceStatus.missing);
    }

    @Test
    public void missingTypefaceOnlyDoesNotCreateConfiguredSummaryPart() {
        TemplateConfigValue value = new TemplateConfigValue(
                ViewportTargetSpec.off(),
                ViewportApplyMode.OFF,
                null,
                FontApplyMode.OFF,
                "font_missing",
                null);

        TemplateConfigSummaryFormatter.Result result = newFormatter(id ->
                TemplateConfigSummaryFormatter.TypefaceStatus.missing(id)).format(value);

        assertEquals("No values configured.", result.summary());
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
            return "No values configured.";
        }

        @Override
        public String viewportSummary(String detail) {
            return "Interface " + detail;
        }

        @Override
        public String fontSummary(String detail) {
            return "Font " + detail;
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
