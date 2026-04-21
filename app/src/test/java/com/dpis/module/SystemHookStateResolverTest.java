package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SystemHookStateResolverTest {
    @Test
    public void desiredOff_resolvesToDisabledByUser() {
        SystemHookState state = SystemHookStateResolver.resolve(false, false, true, true);

        assertFalse(state.switchChecked);
        assertTrue(state.switchEnabled);
        assertFalse(state.desiredEnabled);
        assertFalse(state.effectiveEnabled);
        assertEquals(SystemHookState.Reason.DISABLED_BY_USER, state.reason);
    }

    @Test
    public void pendingRequest_resolvesToCheckedButDisabledUi() {
        SystemHookState state = SystemHookStateResolver.resolve(true, true, true, false);

        assertTrue(state.switchChecked);
        assertFalse(state.switchEnabled);
        assertTrue(state.desiredEnabled);
        assertFalse(state.effectiveEnabled);
        assertEquals(SystemHookState.Reason.REQUEST_PENDING, state.reason);
    }

    @Test
    public void missingScope_resolvesToDesiredOnButNotEffective() {
        SystemHookState state = SystemHookStateResolver.resolve(true, false, true, false);

        assertTrue(state.switchChecked);
        assertTrue(state.switchEnabled);
        assertTrue(state.desiredEnabled);
        assertFalse(state.effectiveEnabled);
        assertEquals(SystemHookState.Reason.SCOPE_MISSING, state.reason);
    }

    @Test
    public void scopeReady_resolvesToEffectiveOn() {
        SystemHookState state = SystemHookStateResolver.resolve(true, false, true, true);

        assertTrue(state.switchChecked);
        assertTrue(state.switchEnabled);
        assertTrue(state.desiredEnabled);
        assertTrue(state.effectiveEnabled);
        assertEquals(SystemHookState.Reason.NONE, state.reason);
    }
}
