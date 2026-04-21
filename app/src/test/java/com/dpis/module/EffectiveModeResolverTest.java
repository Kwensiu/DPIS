package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EffectiveModeResolverTest {
    @Test
    public void viewportEmulationTurnsOffWhenSystemHooksDisabled() {
        String mode = EffectiveModeResolver.resolveViewportMode(
                ViewportApplyMode.SYSTEM_EMULATION, false);

        assertEquals(ViewportApplyMode.OFF, mode);
    }

    @Test
    public void fontEmulationTurnsOffWhenSystemHooksDisabled() {
        String mode = EffectiveModeResolver.resolveFontMode(
                FontApplyMode.SYSTEM_EMULATION, false);

        assertEquals(FontApplyMode.OFF, mode);
    }

    @Test
    public void replaceModesRemainUnchanged() {
        String viewportMode = EffectiveModeResolver.resolveViewportMode(
                ViewportApplyMode.FIELD_REWRITE, false);
        String fontMode = EffectiveModeResolver.resolveFontMode(
                FontApplyMode.FIELD_REWRITE, false);

        assertEquals(ViewportApplyMode.FIELD_REWRITE, viewportMode);
        assertEquals(FontApplyMode.FIELD_REWRITE, fontMode);
    }
}
