package com.dpis.module;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;

public class MainActivityConfiguredCountTest {
    @Test
    public void configuredCountIncludesSavedConfigAndKnownScopeOnly() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putInt("viewport.com.example.saved.width_dp", 360)
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertEquals(2, MainActivity.countUserVisibleConfiguredPackages(
                store,
                new MainActivity.ScopeState(Set.of(
                        "com.example.injected",
                        "com.example.saved"), true)));
    }

    @Test
    public void configuredCountDoesNotInferUnknownLegacyScope() {
        assertEquals(0, MainActivity.countUserVisibleConfiguredPackages(
                null,
                new MainActivity.ScopeState(Set.of("com.example.legacy"), false)));
    }

    @Test
    public void configuredCountExcludesSystemFrameworkScopeAliases() {
        assertEquals(1, MainActivity.countUserVisibleConfiguredPackages(
                null,
                new MainActivity.ScopeState(Set.of(
                        "system",
                        "android",
                        "com.example.injected"), true)));
    }

    @Test
    public void configuredCountMatchesKnownScopeOnlyPackage() {
        assertEquals(1, MainActivity.countUserVisibleConfiguredPackages(
                null,
                new MainActivity.ScopeState(Set.of("com.example.injected"), true)));
    }
}
