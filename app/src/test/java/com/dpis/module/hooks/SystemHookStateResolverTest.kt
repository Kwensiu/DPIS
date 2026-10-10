package com.dpis.module.hooks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemHookStateResolverTest {
    @Test
    fun desiredOffResolvesToDisabledByUser() {
        val state = SystemHookStateResolver.resolve(false, false, true, true)

        assertFalse(state.switchChecked)
        assertTrue(state.switchEnabled)
        assertFalse(state.desiredEnabled)
        assertFalse(state.effectiveEnabled)
        assertEquals(SystemHookState.Reason.DISABLED_BY_USER, state.reason)
    }

    @Test
    fun pendingRequestResolvesToCheckedButDisabledUi() {
        val state = SystemHookStateResolver.resolve(true, true, true, false)

        assertTrue(state.switchChecked)
        assertFalse(state.switchEnabled)
        assertTrue(state.desiredEnabled)
        assertFalse(state.effectiveEnabled)
        assertEquals(SystemHookState.Reason.REQUEST_PENDING, state.reason)
    }

    @Test
    fun missingScopeResolvesToDesiredOnButNotEffective() {
        val state = SystemHookStateResolver.resolve(true, false, true, false)

        assertTrue(state.switchChecked)
        assertTrue(state.switchEnabled)
        assertTrue(state.desiredEnabled)
        assertFalse(state.effectiveEnabled)
        assertEquals(SystemHookState.Reason.SCOPE_MISSING, state.reason)
    }

    @Test
    fun scopeReadyResolvesToEffectiveOn() {
        val state = SystemHookStateResolver.resolve(true, false, true, true)

        assertTrue(state.switchChecked)
        assertTrue(state.switchEnabled)
        assertTrue(state.desiredEnabled)
        assertTrue(state.effectiveEnabled)
        assertEquals(SystemHookState.Reason.NONE, state.reason)
    }
}
