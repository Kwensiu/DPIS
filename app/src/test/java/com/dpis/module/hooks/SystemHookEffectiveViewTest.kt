package com.dpis.module.hooks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemHookEffectiveViewTest {
    @Test
    fun desiredOnAndScopeMissingResolvesEffectiveOff() {
        val view = SystemHookEffectiveView.resolve(true, true, false)

        assertTrue(view.desiredEnabled)
        assertFalse(view.effectiveEnabled)
        assertEquals(SystemHookState.Reason.SCOPE_MISSING, view.reason)
    }

    @Test
    fun desiredOnWithScopeResolvesEffectiveOn() {
        val view = SystemHookEffectiveView.resolve(true, true, true)

        assertTrue(view.desiredEnabled)
        assertTrue(view.effectiveEnabled)
        assertEquals(SystemHookState.Reason.NONE, view.reason)
    }
}
