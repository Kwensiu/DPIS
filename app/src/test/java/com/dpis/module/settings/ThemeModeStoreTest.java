package com.dpis.module.settings;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ThemeModeStoreTest {
    @Test
    public void explicitModesOverrideSystemAppearance() {
        assertFalse(ThemeModeStore.resolveDarkTheme(ThemeModeStore.LIGHT, true));
        assertTrue(ThemeModeStore.resolveDarkTheme(ThemeModeStore.DARK, false));
    }

    @Test
    public void followSystemMirrorsCurrentSystemAppearance() {
        assertFalse(ThemeModeStore.resolveDarkTheme(ThemeModeStore.FOLLOW_SYSTEM, false));
        assertTrue(ThemeModeStore.resolveDarkTheme(ThemeModeStore.FOLLOW_SYSTEM, true));
    }

    @Test
    public void dynamicColorDefaultsOnAndHonorsExplicitChoice() {
        assertTrue(ThemeModeStore.resolveDynamicColorEnabled(null));
        assertTrue(ThemeModeStore.resolveDynamicColorEnabled(true));
        assertFalse(ThemeModeStore.resolveDynamicColorEnabled(false));
    }

    @Test
    public void defaultStaticThemeColorIsPurple() {
        assertEquals(ThemeModeStore.COLOR_PURPLE, ThemeModeStore.DEFAULT_STATIC_THEME_COLOR);
        assertTrue(ThemeModeStore.supportedThemeColors().contains(ThemeModeStore.DEFAULT_STATIC_THEME_COLOR));
    }
}
