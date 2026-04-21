package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SystemHookEffectiveViewTest {
    @Test
    public void desiredOnAndScopeMissing_resolvesEffectiveOff() {
        SystemHookEffectiveView view = SystemHookEffectiveView.resolve(
                true,
                true,
                false);

        assertTrue(view.desiredEnabled);
        assertFalse(view.effectiveEnabled);
        assertEquals(SystemHookState.Reason.SCOPE_MISSING, view.reason);
    }

    @Test
    public void desiredOnWithScope_resolvesEffectiveOn() {
        SystemHookEffectiveView view = SystemHookEffectiveView.resolve(
                true,
                true,
                true);

        assertTrue(view.desiredEnabled);
        assertTrue(view.effectiveEnabled);
        assertEquals(SystemHookState.Reason.NONE, view.reason);
    }
}
