package com.dpis.module

import com.dpis.module.settings.SystemFrameworkScope
import com.dpis.module.settings.SystemScopeCoordinator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemScopeCoordinatorBehaviorTest {
    @Test
    fun systemScopeResolverAcceptsModernAndLegacyAliases() {
        assertTrue(SystemFrameworkScope.containsSystemScope(setOf("system")))
        assertTrue(SystemFrameworkScope.containsSystemScope(setOf("android")))
        assertFalse(SystemFrameworkScope.containsSystemScope(setOf("com.example.app")))
    }

    @Test
    fun systemFrameworkScopeAliasesAreNotUserAppTargets() {
        assertTrue(SystemFrameworkScope.isFrameworkScopePackage("system"))
        assertTrue(SystemFrameworkScope.isFrameworkScopePackage("android"))
        assertFalse(SystemFrameworkScope.isFrameworkScopePackage("com.example.app"))
    }

    @Test
    fun legacyFallsBackToDesiredWhenServiceUnavailable() = assertTrue(
        SystemScopeCoordinator.resolveSystemHookEffectiveEnabled(true, false, false, true),
    )

    @Test
    fun legacyStillRequiresScopeWhenServiceAvailable() = assertFalse(
        SystemScopeCoordinator.resolveSystemHookEffectiveEnabled(true, true, false, true),
    )

    @Test
    fun modernRequiresScopeWhenServiceAvailable() = assertFalse(
        SystemScopeCoordinator.resolveSystemHookEffectiveEnabled(true, true, false, false),
    )
}
