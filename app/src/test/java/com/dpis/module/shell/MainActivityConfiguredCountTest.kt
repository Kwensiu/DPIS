package com.dpis.module

import com.dpis.module.applist.ScopeState
import com.dpis.module.config.DpisConfigStore
import org.junit.Assert.assertEquals
import org.junit.Test

class MainActivityConfiguredCountTest {
    @Test
    fun configuredCountIncludesSavedConfigAndKnownScopeOnly() {
        val prefs = FakePrefs()
        prefs.edit()
            .putInt("viewport.com.example.saved.width_dp", 360)
            .commit()
        val store = DpisConfigStore(prefs)

        assertEquals(
            2,
            MainActivity.countUserVisibleConfiguredPackages(
                store,
                ScopeState(
                    setOf("com.example.injected", "com.example.saved"),
                    true,
                ),
            ),
        )
    }

    @Test
    fun configuredCountDoesNotInferUnknownLegacyScope() {
        assertEquals(
            0,
            MainActivity.countUserVisibleConfiguredPackages(
                null,
                ScopeState(setOf("com.example.legacy"), false),
            ),
        )
    }

    @Test
    fun configuredCountExcludesSystemFrameworkScopeAliases() {
        assertEquals(
            1,
            MainActivity.countUserVisibleConfiguredPackages(
                null,
                ScopeState(
                    setOf("system", "android", "com.example.injected"),
                    true,
                ),
            ),
        )
    }

    @Test
    fun configuredCountMatchesKnownScopeOnlyPackage() {
        assertEquals(
            1,
            MainActivity.countUserVisibleConfiguredPackages(
                null,
                ScopeState(setOf("com.example.injected"), true),
            ),
        )
    }
}
