package com.dpis.module;

import com.dpis.module.settings.SystemFrameworkScope;
import com.dpis.module.settings.SystemScopeCoordinator;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SystemScopeCoordinatorBehaviorTest {
    @Test
    public void systemScopeResolverAcceptsModernAndLegacyAliases() {
        assertTrue(SystemFrameworkScope.containsSystemScope(Set.of("system")));
        assertTrue(SystemFrameworkScope.containsSystemScope(Set.of("android")));
        assertFalse(SystemFrameworkScope.containsSystemScope(Set.of("com.example.app")));
    }

    @Test
    public void systemFrameworkScopeAliasesAreNotUserAppTargets() {
        assertTrue(SystemFrameworkScope.isFrameworkScopePackage("system"));
        assertTrue(SystemFrameworkScope.isFrameworkScopePackage("android"));
        assertFalse(SystemFrameworkScope.isFrameworkScopePackage("com.example.app"));
    }

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
