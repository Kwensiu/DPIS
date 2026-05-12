package com.dpis.module;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FontRuntimePropertySyncerTest {
    @Test
    public void fieldRewritePublishesForceFontWithoutSystemEmulation() {
        assertEquals(expectedFontCommand("0", "field_rewrite", "200"),
                FontRuntimePropertySyncer.buildTargetCommandForTest(
                        "com.miui.gallery", 200, FontApplyMode.FIELD_REWRITE, false));
    }

    @Test
    public void systemEmulationPublishesCompatFontAndClearsForceFontWhenNativeHookIsOff() {
        assertEquals(expectedFontCommand("200", "system_emulation", "0"),
                FontRuntimePropertySyncer.buildTargetCommandForTest(
                        "com.miui.gallery", 200, FontApplyMode.SYSTEM_EMULATION, false));
    }

    @Test
    public void nativeHookPublishesForceFontForEnabledFontModes() {
        assertEquals(expectedFontCommand("200", "system_emulation", "200"),
                FontRuntimePropertySyncer.buildTargetCommandForTest(
                        "com.miui.gallery", 200, FontApplyMode.SYSTEM_EMULATION, true));
    }

    @Test
    public void clearTargetClearsAllFontRuntimeMirrors() {
        assertEquals("setprop 'debug.dpis.font.a55b5fe1' '0'; "
                        + "setprop 'debug.dpis.rustbin.a55b5fe1' '0'; "
                        + expectedFontCommand("0", "off", "0"),
                FontRuntimePropertySyncer.buildClearTargetCommandForTest("com.miui.gallery"));
    }

    @Test
    public void clearTargetClearsPersistentCompatFontMirrors() {
        String[] assignments = HyperOsFlutterFontBridge.clearTargetAssignmentsForTest(
                "com.miui.gallery");

        assertEquals("debug.dpis.font.a55b5fe1", assignments[0]);
        assertEquals("0", assignments[1]);
        assertEquals("debug.dpis.forcefont.a55b5fe1", assignments[2]);
        assertEquals("0", assignments[3]);
        assertEquals("debug.dpis.compatfont.a55b5fe1", assignments[4]);
        assertEquals("0", assignments[5]);
        assertEquals("debug.dpis.fontmode.a55b5fe1", assignments[6]);
        assertEquals(FontApplyMode.OFF, assignments[7]);
        assertEquals("persist.debug.dpis.forcefont.a55b5fe1", assignments[8]);
        assertEquals("0", assignments[9]);
        assertEquals("persist.debug.dpis.compatfont.a55b5fe1", assignments[10]);
        assertEquals("0", assignments[11]);
        assertEquals("persist.debug.dpis.fontmode.a55b5fe1", assignments[12]);
        assertEquals(FontApplyMode.OFF, assignments[13]);
        assertEquals("debug.dpis.rustbin.a55b5fe1", assignments[14]);
        assertEquals("0", assignments[15]);
    }

    private static String expectedFontCommand(String compatFont, String mode, String forceFont) {
        return "setprop 'debug.dpis.compatfont.a55b5fe1' '" + compatFont + "'; "
                + "setprop 'persist.debug.dpis.compatfont.a55b5fe1' '" + compatFont + "'; "
                + "setprop 'debug.dpis.fontmode.a55b5fe1' '" + mode + "'; "
                + "setprop 'persist.debug.dpis.fontmode.a55b5fe1' '" + mode + "'; "
                + "setprop 'debug.dpis.forcefont.a55b5fe1' '" + forceFont + "'; "
                + "setprop 'persist.debug.dpis.forcefont.a55b5fe1' '" + forceFont + "'";
    }
}
