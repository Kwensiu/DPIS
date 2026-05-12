package com.dpis.module;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FontRuntimePropertySyncerTest {
    @Test
    public void fieldRewritePublishesForceFontWithoutSystemEmulation() {
        assertEquals("setprop 'debug.dpis.compatfont.a55b5fe1' '0'; "
                        + "setprop 'debug.dpis.fontmode.a55b5fe1' 'field_rewrite'; "
                        + "setprop 'debug.dpis.forcefont.a55b5fe1' '200'",
                FontRuntimePropertySyncer.buildTargetCommandForTest(
                        "com.miui.gallery", 200, FontApplyMode.FIELD_REWRITE, false));
    }

    @Test
    public void systemEmulationPublishesCompatFontAndClearsForceFontWhenNativeHookIsOff() {
        assertEquals("setprop 'debug.dpis.compatfont.a55b5fe1' '200'; "
                        + "setprop 'debug.dpis.fontmode.a55b5fe1' 'system_emulation'; "
                        + "setprop 'debug.dpis.forcefont.a55b5fe1' '0'",
                FontRuntimePropertySyncer.buildTargetCommandForTest(
                        "com.miui.gallery", 200, FontApplyMode.SYSTEM_EMULATION, false));
    }

    @Test
    public void nativeHookPublishesForceFontForEnabledFontModes() {
        assertEquals("setprop 'debug.dpis.compatfont.a55b5fe1' '200'; "
                        + "setprop 'debug.dpis.fontmode.a55b5fe1' 'system_emulation'; "
                        + "setprop 'debug.dpis.forcefont.a55b5fe1' '200'",
                FontRuntimePropertySyncer.buildTargetCommandForTest(
                        "com.miui.gallery", 200, FontApplyMode.SYSTEM_EMULATION, true));
    }

    @Test
    public void clearTargetClearsAllFontRuntimeMirrors() {
        assertEquals("setprop 'debug.dpis.font.a55b5fe1' '0'; "
                        + "setprop 'debug.dpis.rustbin.a55b5fe1' '0'; "
                        + "setprop 'debug.dpis.compatfont.a55b5fe1' '0'; "
                        + "setprop 'debug.dpis.fontmode.a55b5fe1' 'off'; "
                        + "setprop 'debug.dpis.forcefont.a55b5fe1' '0'",
                FontRuntimePropertySyncer.buildClearTargetCommandForTest("com.miui.gallery"));
    }
}
