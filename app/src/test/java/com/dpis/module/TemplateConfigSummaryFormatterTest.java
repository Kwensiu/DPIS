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
                "Interface 112.5% · Font 120% · Font route: Compat"
                        + " · Font style: Demo Font · Custom hook chain",
                result.summary());
        assertEquals(5, result.summaryParts.size());
        assertEquals("Interface 112.5%", result.summaryParts.get(0));
        assertEquals("Font 120%", result.summaryParts.get(1));
        assertEquals("Font route: Compat", result.summaryParts.get(2));
        assertEquals("Font style: Demo Font", result.summaryParts.get(3));
        assertEquals("Custom hook chain", result.summaryParts.get(4));
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

        assertEquals("Interface 411dp · Interface route: Compat", result.summary());
        assertEquals(2, result.summaryParts.size());
        assertEquals("Interface 411dp", result.summaryParts.get(0));
        assertEquals("Interface route: Compat", result.summaryParts.get(1));
        assertTrue(result.typefaceStatus.missing);
        assertEquals("font_missing", result.typefaceStatus.typefaceId);
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
        public String viewportScale(int wholePercent, int decimalPercent) {
            return "Interface " + wholePercent + "." + decimalPercent + "%";
        }

        @Override
        public String viewportWidth(int widthDp) {
            return "Interface " + widthDp + "dp";
        }

        @Override
        public String viewportMode(String modeLabel) {
            return "Interface route: " + modeLabel;
        }

        @Override
        public String fontScale(int percent) {
            return "Font " + percent + "%";
        }

        @Override
        public String fontMode(String modeLabel) {
            return "Font route: " + modeLabel;
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
