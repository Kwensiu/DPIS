package com.dpis.module;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RuntimeDebugPropertySyncerTest {
    @Test
    public void publishesGlobalLogAndFontDebugOverlayMirrors() {
        assertEquals("setprop 'debug.dpis.global_log_enabled' '1'; "
                        + "setprop 'persist.debug.dpis.global_log_enabled' '1'; "
                        + "setprop 'debug.dpis.font_debug_overlay_enabled' '0'; "
                        + "setprop 'persist.debug.dpis.font_debug_overlay_enabled' '0'",
                RuntimeDebugPropertySyncer.buildPublishCommandForTest(true, false));
    }

    @Test
    public void canDisableGlobalLogWhileKeepingOverlayPreferenceMirrored() {
        assertEquals("setprop 'debug.dpis.global_log_enabled' '0'; "
                        + "setprop 'persist.debug.dpis.global_log_enabled' '0'; "
                        + "setprop 'debug.dpis.font_debug_overlay_enabled' '1'; "
                        + "setprop 'persist.debug.dpis.font_debug_overlay_enabled' '1'",
                RuntimeDebugPropertySyncer.buildPublishCommandForTest(false, true));
    }
}
