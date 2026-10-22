package com.dpis.module;

import com.dpis.module.runtime.font.HyperOsFlutterFontBridge;


import com.dpis.module.fonts.FontApplyMode;


import com.dpis.module.runtime.font.FontRuntimePropertySyncer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FontRuntimePropertySyncerTest {
    @Test
    public void fieldRewritePublishesForceFontWithoutSystemEmulation() {
        assertEquals(expectedFontCommand("0", "field_rewrite", "200"),
                FontRuntimePropertySyncer.buildTargetCommandForTest(
                        "com.miui.gallery", 200, FontApplyMode.FIELD_REWRITE, false, 0));
    }

    @Test
    public void systemEmulationPublishesCompatFontAndClearsForceFontWhenNativeHookIsOff() {
        assertEquals(expectedFontCommand("200", "system_emulation", "0"),
                FontRuntimePropertySyncer.buildTargetCommandForTest(
                        "com.miui.gallery", 200, FontApplyMode.SYSTEM_EMULATION, false, 0));
    }

    @Test
    public void nativeHookPublishesForceFontForEnabledFontModes() {
        assertEquals(expectedFontCommand("200", "system_emulation", "200"),
                FontRuntimePropertySyncer.buildTargetCommandForTest(
                        "com.miui.gallery", 200, FontApplyMode.SYSTEM_EMULATION, true, 0));
    }

    @Test
    public void clearTargetClearsAllFontRuntimeMirrors() {
        assertEquals(expectedClearFontScaleCommand() + "; "
                        + expectedTypefaceCommand("0"),
                FontRuntimePropertySyncer.buildClearTargetCommandForTest("com.miui.gallery", 0));
    }

    @Test
    public void clearFontScaleTargetDoesNotClearTypefaceMirror() {
        assertEquals(expectedClearFontScaleCommand(),
                FontRuntimePropertySyncer.buildClearFontScaleCommandForTest("com.miui.gallery", 0));
    }

    @Test
    public void typefacePublishesRuntimeMirror() {
        assertEquals(expectedTypefaceCommand("font_abcd1234"),
                FontRuntimePropertySyncer.buildTypefaceCommandForTest(
                        "com.miui.gallery", "font_abcd1234", 0));
    }

    @Test
    public void clearTargetClearsPersistentCompatFontMirrors() {
        String[] assignments = HyperOsFlutterFontBridge.clearTargetAssignmentsForTest(
                "com.miui.gallery", 0);

        assertEquals("debug.dpis.font.0.a55b5fe1", assignments[0]);
        assertEquals("0", assignments[1]);
        assertEquals("debug.dpis.forcefont.0.a55b5fe1", assignments[2]);
        assertEquals("0", assignments[3]);
        assertEquals("debug.dpis.compatfont.0.a55b5fe1", assignments[4]);
        assertEquals("0", assignments[5]);
        assertEquals("debug.dpis.fontmode.0.a55b5fe1", assignments[6]);
        assertEquals(FontApplyMode.OFF, assignments[7]);
        assertEquals("persist.debug.dpis.forcefont.0.a55b5fe1", assignments[8]);
        assertEquals("0", assignments[9]);
        assertEquals("persist.debug.dpis.compatfont.0.a55b5fe1", assignments[10]);
        assertEquals("0", assignments[11]);
        assertEquals("persist.debug.dpis.fontmode.0.a55b5fe1", assignments[12]);
        assertEquals(FontApplyMode.OFF, assignments[13]);
        assertEquals("debug.dpis.typeface.0.a55b5fe1", assignments[14]);
        assertEquals("0", assignments[15]);
        assertEquals("persist.debug.dpis.typeface.0.a55b5fe1", assignments[16]);
        assertEquals("0", assignments[17]);
        assertEquals("debug.dpis.rustbin.0.a55b5fe1", assignments[18]);
        assertEquals("0", assignments[19]);
    }

    private static String expectedFontCommand(String compatFont, String mode, String forceFont) {
        return "setprop 'debug.dpis.compatfont.0.a55b5fe1' '" + compatFont + "'; "
                + "setprop 'persist.debug.dpis.compatfont.0.a55b5fe1' '" + compatFont + "'; "
                + "setprop 'debug.dpis.fontmode.0.a55b5fe1' '" + mode + "'; "
                + "setprop 'persist.debug.dpis.fontmode.0.a55b5fe1' '" + mode + "'; "
                + "setprop 'debug.dpis.forcefont.0.a55b5fe1' '" + forceFont + "'; "
                + "setprop 'persist.debug.dpis.forcefont.0.a55b5fe1' '" + forceFont + "'";
    }

    private static String expectedClearFontScaleCommand() {
        return "setprop 'debug.dpis.font.0.a55b5fe1' '0'; "
                + "setprop 'debug.dpis.rustbin.0.a55b5fe1' '0'; "
                + expectedFontCommand("0", "off", "0");
    }

    private static String expectedTypefaceCommand(String typefaceId) {
        return "setprop 'debug.dpis.typeface.0.a55b5fe1' '" + typefaceId + "'; "
                + "setprop 'persist.debug.dpis.typeface.0.a55b5fe1' '" + typefaceId + "'";
    }
}
