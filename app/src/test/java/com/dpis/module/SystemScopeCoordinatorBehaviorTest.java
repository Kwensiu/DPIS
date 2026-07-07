package com.dpis.module;

import com.dpis.module.settings.SystemScopeCoordinator;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SystemScopeCoordinatorBehaviorTest {
    @Test
    public void legacyFallsBackToDesiredWhenServiceUnavailable() {
        assertTrue(SystemScopeCoordinator.resolveSystemHookEffectiveEnabled(
                true,
                false,
                false,
                true));
    }

    @Test
    public void legacyStillRequiresScopeWhenServiceAvailable() {
        assertFalse(SystemScopeCoordinator.resolveSystemHookEffectiveEnabled(
                true,
                true,
                false,
                true));
    }

    @Test
    public void modernRequiresScopeWhenServiceAvailable() {
        assertFalse(SystemScopeCoordinator.resolveSystemHookEffectiveEnabled(
                true,
                true,
                false,
                false));
    }
}
