package com.dpis.module;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class Compat100PropertyConfigTest {
    @Test
    public void viewportSyncPublishesCompatConfigWithoutEnablingSystemEmulation() {
        String command = ViewportPropertySyncer.buildCompatConfigCommandForTest(
                "com.max.xiaoheihe", 300, ViewportApplyMode.FIELD_REWRITE);

        assertEquals("setprop 'debug.dpis.vp.eab4efd3' '0'; "
                        + "setprop 'debug.dpis.vpcfg.eab4efd3' '300'; "
                        + "setprop 'debug.dpis.vpmode.eab4efd3' 'field_rewrite'",
                command);
    }

    @Test
    public void viewportSystemEmulationPublishesBothRuntimeAndCompatConfig() {
        String command = ViewportPropertySyncer.buildCompatConfigCommandForTest(
                "com.max.xiaoheihe", 300, ViewportApplyMode.SYSTEM_EMULATION);

        assertEquals("setprop 'debug.dpis.vp.eab4efd3' '300'; "
                        + "setprop 'debug.dpis.vpcfg.eab4efd3' '300'; "
                        + "setprop 'debug.dpis.vpmode.eab4efd3' 'system_emulation'",
                command);
    }

    @Test
    public void viewportOffOrInvalidWidthClearsRuntimeAndCompatConfig() {
        assertEquals("setprop 'debug.dpis.vp.eab4efd3' '0'; "
                        + "setprop 'debug.dpis.vpcfg.eab4efd3' '0'; "
                        + "setprop 'debug.dpis.vpmode.eab4efd3' 'off'",
                ViewportPropertySyncer.buildCompatConfigCommandForTest(
                        "com.max.xiaoheihe", 300, ViewportApplyMode.OFF));
        assertEquals("setprop 'debug.dpis.vp.eab4efd3' '0'; "
                        + "setprop 'debug.dpis.vpcfg.eab4efd3' '0'; "
                        + "setprop 'debug.dpis.vpmode.eab4efd3' 'off'",
                ViewportPropertySyncer.buildCompatConfigCommandForTest(
                        "com.max.xiaoheihe", 0, ViewportApplyMode.FIELD_REWRITE));
    }

    @Test
    public void viewportBoundaryWidthsArePreserved() {
        assertEquals("setprop 'debug.dpis.vp.eab4efd3' '0'; "
                        + "setprop 'debug.dpis.vpcfg.eab4efd3' '1'; "
                        + "setprop 'debug.dpis.vpmode.eab4efd3' 'field_rewrite'",
                ViewportPropertySyncer.buildCompatConfigCommandForTest(
                        "com.max.xiaoheihe", 1, ViewportApplyMode.FIELD_REWRITE));
        assertEquals("setprop 'debug.dpis.vp.eab4efd3' '0'; "
                        + "setprop 'debug.dpis.vpcfg.eab4efd3' '9999'; "
                        + "setprop 'debug.dpis.vpmode.eab4efd3' 'field_rewrite'",
                ViewportPropertySyncer.buildCompatConfigCommandForTest(
                        "com.max.xiaoheihe", 9999, ViewportApplyMode.FIELD_REWRITE));
    }

    @Test
    public void fontSyncPublishesCompatModeWithoutEnablingSystemEmulation() {
        String command = CompatFontPropertySyncer.buildCompatConfigCommandForTest(
                "com.max.xiaoheihe", 200, FontApplyMode.FIELD_REWRITE);

        assertEquals("setprop 'debug.dpis.compatfont.eab4efd3' '0'; "
                        + "setprop 'debug.dpis.fontmode.eab4efd3' 'field_rewrite'; "
                        + "setprop 'debug.dpis.forcefont.eab4efd3' '200'",
                command);
    }

    @Test
    public void fontSystemEmulationPublishesRuntimeValueAndMode() {
        String command = CompatFontPropertySyncer.buildCompatConfigCommandForTest(
                "com.max.xiaoheihe", 200, FontApplyMode.SYSTEM_EMULATION);

        assertEquals("setprop 'debug.dpis.compatfont.eab4efd3' '200'; "
                        + "setprop 'debug.dpis.fontmode.eab4efd3' 'system_emulation'; "
                        + "setprop 'debug.dpis.forcefont.eab4efd3' '0'",
                command);
    }

    @Test
    public void fontOffOrInvalidPercentClearsRuntimeAndMode() {
        assertEquals("setprop 'debug.dpis.compatfont.eab4efd3' '0'; "
                        + "setprop 'debug.dpis.fontmode.eab4efd3' 'off'; "
                        + "setprop 'debug.dpis.forcefont.eab4efd3' '0'",
                CompatFontPropertySyncer.buildCompatConfigCommandForTest(
                        "com.max.xiaoheihe", 200, FontApplyMode.OFF));
        assertEquals("setprop 'debug.dpis.compatfont.eab4efd3' '0'; "
                        + "setprop 'debug.dpis.fontmode.eab4efd3' 'off'; "
                        + "setprop 'debug.dpis.forcefont.eab4efd3' '0'",
                CompatFontPropertySyncer.buildCompatConfigCommandForTest(
                        "com.max.xiaoheihe", 0, FontApplyMode.FIELD_REWRITE));
    }

    @Test
    public void fontBoundaryPercentsArePreserved() {
        assertEquals("setprop 'debug.dpis.compatfont.eab4efd3' '0'; "
                        + "setprop 'debug.dpis.fontmode.eab4efd3' 'field_rewrite'; "
                        + "setprop 'debug.dpis.forcefont.eab4efd3' '50'",
                CompatFontPropertySyncer.buildCompatConfigCommandForTest(
                        "com.max.xiaoheihe", 50, FontApplyMode.FIELD_REWRITE));
        assertEquals("setprop 'debug.dpis.compatfont.eab4efd3' '0'; "
                        + "setprop 'debug.dpis.fontmode.eab4efd3' 'field_rewrite'; "
                        + "setprop 'debug.dpis.forcefont.eab4efd3' '100'",
                CompatFontPropertySyncer.buildCompatConfigCommandForTest(
                        "com.max.xiaoheihe", 100, FontApplyMode.FIELD_REWRITE));
        assertEquals("setprop 'debug.dpis.compatfont.eab4efd3' '0'; "
                        + "setprop 'debug.dpis.fontmode.eab4efd3' 'field_rewrite'; "
                        + "setprop 'debug.dpis.forcefont.eab4efd3' '300'",
                CompatFontPropertySyncer.buildCompatConfigCommandForTest(
                        "com.max.xiaoheihe", 300, FontApplyMode.FIELD_REWRITE));
    }

    @Test
    public void legacyCompatFontPropertyDefaultsToSystemEmulationWithoutMode() {
        assertEquals(FontApplyMode.SYSTEM_EMULATION,
                SystemPropertyConfigPreferences.resolveCompatFontModeForTest(
                        200, FontApplyMode.OFF, null));
    }

    @Test
    public void forceFontPropertyDefaultsToFieldRewriteWithoutMode() {
        assertEquals(FontApplyMode.FIELD_REWRITE,
                SystemPropertyConfigPreferences.resolveCompatFontModeForTest(
                        200, FontApplyMode.OFF, 200));
    }

    @Test
    public void explicitCompatFontModeOverridesPropertyOrigin() {
        assertEquals(FontApplyMode.SYSTEM_EMULATION,
                SystemPropertyConfigPreferences.resolveCompatFontModeForTest(
                        200, FontApplyMode.SYSTEM_EMULATION, 200));
        assertEquals(FontApplyMode.FIELD_REWRITE,
                SystemPropertyConfigPreferences.resolveCompatFontModeForTest(
                        200, FontApplyMode.FIELD_REWRITE, null));
    }
}
