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
}
