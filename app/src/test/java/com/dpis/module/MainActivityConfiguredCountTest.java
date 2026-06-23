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
        DpiConfigStore store = new DpiConfigStore(prefs);

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
}
