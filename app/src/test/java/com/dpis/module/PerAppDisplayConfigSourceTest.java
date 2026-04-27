package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PerAppDisplayConfigSourceTest {
    @Test
    public void returnsNullWhenViewportAndFontAreBothMissing() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());

        PerAppDisplayConfig config = new PerAppDisplayConfigSource(store)
                .get("com.example.target");

        assertNull(config);
    }

    @Test
    public void returnsFontOnlyConfigWhenViewportMissing() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        assertTrue(store.setTargetFontScalePercent("com.example.target", 125));

        PerAppDisplayConfig config = new PerAppDisplayConfigSource(store)
                .get("com.example.target");

        assertNotNull(config);
        assertFalse(config.hasViewportOverride());
        assertEquals(0, config.targetViewportWidthDp);
        assertEquals(Integer.valueOf(125), config.targetFontScalePercent);
    }

    @Test
    public void returnsNullWhenTargetDpisDisabled() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        assertTrue(store.setTargetViewportWidthDp("com.example.target", 500));
        assertTrue(store.setTargetFontScalePercent("com.example.target", 300));
        assertTrue(store.setTargetFontApplyMode(
                "com.example.target", FontApplyMode.SYSTEM_EMULATION));
        assertTrue(store.setTargetDpisEnabled("com.example.target", false));

        PerAppDisplayConfig config = new PerAppDisplayConfigSource(store)
                .get("com.example.target");

        assertNull(config);
    }

    @Test
    public void providerReflectsUpdatedStoreOnEachRead() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        assertTrue(store.setTargetFontScalePercent("com.example.target", 300));
        PerAppDisplayConfigSource source = new PerAppDisplayConfigSource(() -> store);
        assertNotNull(source.get("com.example.target"));

        assertTrue(store.clearTargetPackageConfig("com.example.target"));

        assertNull(source.get("com.example.target"));
    }

    @Test
    public void keepsViewportConfigWhenViewportExists() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        assertTrue(store.setTargetViewportWidthDp("com.example.target", 360));

        PerAppDisplayConfig config = new PerAppDisplayConfigSource(store)
                .get("com.example.target");

        assertNotNull(config);
        assertTrue(config.hasViewportOverride());
        assertEquals(360, config.targetViewportWidthDp);
    }

    @Test
    public void reportsSystemServerHooksEnabledByDefault() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());

        boolean enabled = new PerAppDisplayConfigSource(store).isSystemServerHooksEnabled();

        assertTrue(enabled);
    }

    @Test
    public void reportsSystemServerHooksDisabledWhenStoreFlagOff() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        assertTrue(store.setSystemServerHooksEnabled(false));

        boolean enabled = new PerAppDisplayConfigSource(store).isSystemServerHooksEnabled();

        assertFalse(enabled);
    }
}
