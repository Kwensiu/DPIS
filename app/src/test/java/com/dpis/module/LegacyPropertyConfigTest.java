package com.dpis.module;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LegacyPropertyConfigTest {
    @Test
    public void viewportSyncPublishesCompatConfigWithoutEnablingSystemEmulation() {
        String command = ViewportPropertySyncer.buildCompatConfigCommandForTest(
                "com.max.xiaoheihe", 300, ViewportApplyMode.FIELD_REWRITE);

        assertEquals(expectedViewportCommand("0", "absolute_dp", "0", "300", "compat"), command);
    }

    @Test
    public void viewportSystemEmulationPublishesBothRuntimeAndCompatConfig() {
        String command = ViewportPropertySyncer.buildCompatConfigCommandForTest(
                "com.max.xiaoheihe", 300, ViewportApplyMode.SYSTEM_EMULATION);

        assertEquals(expectedViewportCommand("300", "absolute_dp", "0", "300", "system"), command);
    }

    @Test
    public void viewportRelativeScalePublishesScaleWithoutWidthOnlyValues() {
        String command = ViewportPropertySyncer.buildCompatConfigCommandForTest(
                "com.max.xiaoheihe", ViewportTargetSpec.relativeScale(1250), ViewportApplyMode.SYSTEM);

        assertEquals(expectedViewportCommand("0", "relative_scale", "1250", "0", "system"), command);
    }

    @Test
    public void viewportOffOrInvalidWidthClearsRuntimeAndCompatConfig() {
        assertEquals(expectedViewportCommand("0", "off", "0", "0", "off"),
                ViewportPropertySyncer.buildCompatConfigCommandForTest(
                        "com.max.xiaoheihe", 300, ViewportApplyMode.OFF));
        assertEquals(expectedViewportCommand("0", "off", "0", "0", "off"),
                ViewportPropertySyncer.buildCompatConfigCommandForTest(
                        "com.max.xiaoheihe", 0, ViewportApplyMode.FIELD_REWRITE));
    }

    @Test
    public void viewportBoundaryWidthsArePreserved() {
        assertEquals(expectedViewportCommand("0", "absolute_dp", "0", "1", "compat"),
                ViewportPropertySyncer.buildCompatConfigCommandForTest(
                        "com.max.xiaoheihe", 1, ViewportApplyMode.FIELD_REWRITE));
        assertEquals(expectedViewportCommand("0", "absolute_dp", "0", "9999", "compat"),
                ViewportPropertySyncer.buildCompatConfigCommandForTest(
                        "com.max.xiaoheihe", 9999, ViewportApplyMode.FIELD_REWRITE));
    }

    @Test
    public void fontSyncPublishesCompatModeWithoutEnablingSystemEmulation() {
        String command = CompatFontPropertySyncer.buildCompatConfigCommandForTest(
                "com.max.xiaoheihe", 200, FontApplyMode.FIELD_REWRITE);

        assertEquals(expectedFontCommand("0", "field_rewrite", "200"), command);
    }

    @Test
    public void fontSystemEmulationPublishesRuntimeValueAndMode() {
        String command = CompatFontPropertySyncer.buildCompatConfigCommandForTest(
                "com.max.xiaoheihe", 200, FontApplyMode.SYSTEM_EMULATION);

        assertEquals(expectedFontCommand("200", "system_emulation", "0"), command);
    }

    @Test
    public void fontOffOrInvalidPercentClearsRuntimeAndMode() {
        assertEquals(expectedFontCommand("0", "off", "0"),
                CompatFontPropertySyncer.buildCompatConfigCommandForTest(
                        "com.max.xiaoheihe", 200, FontApplyMode.OFF));
        assertEquals(expectedFontCommand("0", "off", "0"),
                CompatFontPropertySyncer.buildCompatConfigCommandForTest(
                        "com.max.xiaoheihe", 0, FontApplyMode.FIELD_REWRITE));
    }

    @Test
    public void fontBoundaryPercentsArePreserved() {
        assertEquals(expectedFontCommand("0", "field_rewrite", "50"),
                CompatFontPropertySyncer.buildCompatConfigCommandForTest(
                        "com.max.xiaoheihe", 50, FontApplyMode.FIELD_REWRITE));
        assertEquals(expectedFontCommand("0", "field_rewrite", "100"),
                CompatFontPropertySyncer.buildCompatConfigCommandForTest(
                        "com.max.xiaoheihe", 100, FontApplyMode.FIELD_REWRITE));
        assertEquals(expectedFontCommand("0", "field_rewrite", "300"),
                CompatFontPropertySyncer.buildCompatConfigCommandForTest(
                        "com.max.xiaoheihe", 300, FontApplyMode.FIELD_REWRITE));
    }

    @Test
    public void legacyCompatFontPropertyDefaultsToSystemEmulationWithoutMode() {
        assertEquals(FontApplyMode.SYSTEM_EMULATION,
                RuntimePropertyConfigPreferences.resolveRuntimeFontModeForTest(
                        200, FontApplyMode.OFF, null));
    }

    @Test
    public void forceFontPropertyDefaultsToFieldRewriteWithoutMode() {
        assertEquals(FontApplyMode.FIELD_REWRITE,
                RuntimePropertyConfigPreferences.resolveRuntimeFontModeForTest(
                        200, FontApplyMode.OFF, 200));
    }

    @Test
    public void explicitCompatFontModeOverridesPropertyOrigin() {
        assertEquals(FontApplyMode.SYSTEM_EMULATION,
                RuntimePropertyConfigPreferences.resolveRuntimeFontModeForTest(
                        200, FontApplyMode.SYSTEM_EMULATION, 200));
        assertEquals(FontApplyMode.FIELD_REWRITE,
                RuntimePropertyConfigPreferences.resolveRuntimeFontModeForTest(
                        200, FontApplyMode.FIELD_REWRITE, null));
    }

    @Test
    public void legacyMainProcessKeepsAutoRelativeScaleSystemFirst() {
        assertEquals(ViewportApplyMode.COMPAT,
                RuntimePropertyConfigPreferences.resolveRuntimeViewportModeForTest(
                        ViewportApplyMode.AUTO,
                        ViewportTargetSpec.absoluteDp(500),
                        RuntimePropertyConfigPreferences.AutoViewportRuntimeRoute.ABSOLUTE_TARGETS_ONLY));
        assertEquals(ViewportApplyMode.AUTO,
                RuntimePropertyConfigPreferences.resolveRuntimeViewportModeForTest(
                        ViewportApplyMode.AUTO,
                        ViewportTargetSpec.relativeScale(1500),
                        RuntimePropertyConfigPreferences.AutoViewportRuntimeRoute.ABSOLUTE_TARGETS_ONLY));
        assertEquals(ViewportApplyMode.AUTO,
                RuntimePropertyConfigPreferences.resolveRuntimeViewportModeForTest(
                        ViewportApplyMode.AUTO,
                        ViewportTargetSpec.off(),
                        RuntimePropertyConfigPreferences.AutoViewportRuntimeRoute.ABSOLUTE_TARGETS_ONLY));
    }

    @Test
    public void modernRuntimeMirrorCanResolveAutoRelativeScaleAsAppProcessRoute() {
        assertEquals(ViewportApplyMode.COMPAT,
                RuntimePropertyConfigPreferences.resolveRuntimeViewportModeForTest(
                        ViewportApplyMode.AUTO,
                        ViewportTargetSpec.relativeScale(1500),
                        RuntimePropertyConfigPreferences.AutoViewportRuntimeRoute.ANY_ENABLED_TARGET));
    }

    private static String expectedViewportCommand(String viewport,
                                                  String targetType,
                                                  String scalePermille,
                                                  String compatConfig,
                                                  String mode) {
        return "setprop 'debug.dpis.vp.eab4efd3' '" + viewport + "'; "
                + "setprop 'persist.debug.dpis.vp.eab4efd3' '" + viewport + "'; "
                + "setprop 'debug.dpis.vptype.eab4efd3' '" + targetType + "'; "
                + "setprop 'persist.debug.dpis.vptype.eab4efd3' '" + targetType + "'; "
                + "setprop 'debug.dpis.vpscale.eab4efd3' '" + scalePermille + "'; "
                + "setprop 'persist.debug.dpis.vpscale.eab4efd3' '" + scalePermille + "'; "
                + "setprop 'debug.dpis.vpcfg.eab4efd3' '" + compatConfig + "'; "
                + "setprop 'persist.debug.dpis.vpcfg.eab4efd3' '" + compatConfig + "'; "
                + "setprop 'debug.dpis.vpmode.eab4efd3' '" + mode + "'; "
                + "setprop 'persist.debug.dpis.vpmode.eab4efd3' '" + mode + "'";
    }

    private static String expectedFontCommand(String compatFont, String mode, String forceFont) {
        return "setprop 'debug.dpis.compatfont.eab4efd3' '" + compatFont + "'; "
                + "setprop 'persist.debug.dpis.compatfont.eab4efd3' '" + compatFont + "'; "
                + "setprop 'debug.dpis.fontmode.eab4efd3' '" + mode + "'; "
                + "setprop 'persist.debug.dpis.fontmode.eab4efd3' '" + mode + "'; "
                + "setprop 'debug.dpis.forcefont.eab4efd3' '" + forceFont + "'; "
                + "setprop 'persist.debug.dpis.forcefont.eab4efd3' '" + forceFont + "'";
    }
}
