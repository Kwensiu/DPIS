package com.dpis.module;

import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DpiConfigStoreTest {
    @Test
    public void parsesConfiguredPackageSetFromStoredStrings() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit().putStringSet(DpiConfigStore.KEY_TARGET_PACKAGES, new LinkedHashSet<>(Arrays.asList(
                "com.max.xiaoheihe", "bin.mt.plus.canary"))).commit();

        DpiConfigStore store = new DpiConfigStore(prefs);

        assertTrue(store.getConfiguredPackages().contains("com.max.xiaoheihe"));
        assertTrue(store.getConfiguredPackages().contains("bin.mt.plus.canary"));
        assertFalse(store.getConfiguredPackages().contains("com.example.other"));
    }

    @Test
    public void resolvesEffectiveDensityFromTargetValue() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit().putInt("viewport.bin.mt.plus.canary.width_dp", 360).commit();

        DpiConfigStore store = new DpiConfigStore(prefs);

        assertEquals(Integer.valueOf(360), store.getTargetViewportWidthDp("bin.mt.plus.canary"));
    }

    @Test
    public void returnsNullEffectiveDensityWhenTargetMissing() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);

        assertNull(store.getTargetViewportWidthDp("bin.mt.plus.canary"));
        assertNull(store.getTargetViewportWidthDp("bin.mt.plus.canary"));
    }

    @Test
    public void seedsMissingPackageListAndTargetValuesWithoutOverwritingExistingValues() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putStringSet(DpiConfigStore.KEY_TARGET_PACKAGES, new LinkedHashSet<>(Set.of("bin.mt.plus.canary")))
                .putInt("viewport.bin.mt.plus.canary.width_dp", 420)
                .commit();

        DpiConfigStore store = new DpiConfigStore(prefs);
        store.ensureSeedConfig(DpiConfig.getSeedViewportWidthDps());

        assertEquals(Integer.valueOf(420), store.getTargetViewportWidthDp("bin.mt.plus.canary"));
        assertEquals(Integer.valueOf(DpiConfig.SEED_TARGET_VIEWPORT_WIDTH_DP),
                store.getTargetViewportWidthDp("com.max.xiaoheihe"));
        assertTrue(store.getConfiguredPackages().contains("bin.mt.plus.canary"));
        assertTrue(store.getConfiguredPackages().contains("com.max.xiaoheihe"));
    }

    @Test
    public void updatesViewportWidthForConfiguredPackage() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        assertTrue(store.ensureSeedConfig(DpiConfig.getSeedViewportWidthDps()));

        assertTrue(store.setTargetViewportWidthDp("bin.mt.plus.canary", 360));

        assertEquals(Integer.valueOf(360), store.getTargetViewportWidthDp("bin.mt.plus.canary"));
        assertEquals(Integer.valueOf(360), store.getTargetViewportWidthDp("bin.mt.plus.canary"));
    }

    @Test
    public void clearsViewportWidthWhenDisabled() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        assertTrue(store.ensureSeedConfig(DpiConfig.getSeedViewportWidthDps()));

        assertTrue(store.clearTargetViewportWidthDp("bin.mt.plus.canary"));

        assertNull(store.getTargetViewportWidthDp("bin.mt.plus.canary"));
        assertNull(store.getTargetViewportWidthDp("bin.mt.plus.canary"));
        assertFalse(store.getConfiguredPackages().contains("bin.mt.plus.canary"));
    }

    @Test
    public void updatesFontScaleForConfiguredPackage() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);

        assertTrue(store.setTargetFontScalePercent("bin.mt.plus.canary", 115));

        assertEquals(Integer.valueOf(115), store.getTargetFontScalePercent("bin.mt.plus.canary"));
        assertTrue(store.getConfiguredPackages().contains("bin.mt.plus.canary"));
    }

    @Test
    public void clearsFontScaleWhenDisabled() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        assertTrue(store.setTargetFontScalePercent("bin.mt.plus.canary", 115));

        assertTrue(store.clearTargetFontScalePercent("bin.mt.plus.canary"));

        assertNull(store.getTargetFontScalePercent("bin.mt.plus.canary"));
        assertFalse(store.getConfiguredPackages().contains("bin.mt.plus.canary"));
    }

    @Test
    public void keepsPackageConfiguredWhenClearingViewportButFontScaleExists() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        assertTrue(store.setTargetViewportWidthDp("bin.mt.plus.canary", 360));
        assertTrue(store.setTargetFontScalePercent("bin.mt.plus.canary", 115));

        assertTrue(store.clearTargetViewportWidthDp("bin.mt.plus.canary"));

        assertNull(store.getTargetViewportWidthDp("bin.mt.plus.canary"));
        assertEquals(Integer.valueOf(115), store.getTargetFontScalePercent("bin.mt.plus.canary"));
        assertTrue(store.getConfiguredPackages().contains("bin.mt.plus.canary"));
    }

    @Test
    public void keepsPackageConfiguredWhenClearingFontScaleButViewportExists() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        assertTrue(store.setTargetViewportWidthDp("bin.mt.plus.canary", 360));
        assertTrue(store.setTargetFontScalePercent("bin.mt.plus.canary", 115));

        assertTrue(store.clearTargetFontScalePercent("bin.mt.plus.canary"));

        assertEquals(Integer.valueOf(360), store.getTargetViewportWidthDp("bin.mt.plus.canary"));
        assertNull(store.getTargetFontScalePercent("bin.mt.plus.canary"));
        assertTrue(store.getConfiguredPackages().contains("bin.mt.plus.canary"));
    }

    @Test
    public void reportsFailureWhenViewportWidthCommitFails() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        assertTrue(store.ensureSeedConfig(DpiConfig.getSeedViewportWidthDps()));
        prefs.setCommitResult(false);

        assertFalse(store.setTargetViewportWidthDp("bin.mt.plus.canary", 320));
        assertEquals(Integer.valueOf(DpiConfig.SEED_TARGET_VIEWPORT_WIDTH_DP),
                store.getTargetViewportWidthDp("bin.mt.plus.canary"));
    }

    @Test
    public void reportsFailureWhenViewportWidthClearCommitFails() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        assertTrue(store.ensureSeedConfig(DpiConfig.getSeedViewportWidthDps()));
        prefs.setCommitResult(false);

        assertFalse(store.clearTargetViewportWidthDp("bin.mt.plus.canary"));
        assertEquals(Integer.valueOf(DpiConfig.SEED_TARGET_VIEWPORT_WIDTH_DP),
                store.getTargetViewportWidthDp("bin.mt.plus.canary"));
    }

    @Test
    public void disablesSystemServerHooksByDefault() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);

        assertTrue(store.isSystemServerHooksEnabled());
    }

    @Test
    public void enablesSystemServerSafeModeByDefault() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);

        assertTrue(store.isSystemServerSafeModeEnabled());
    }

    @Test
    public void updatesSystemServerGlobalToggles() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);

        assertTrue(store.setSystemServerHooksEnabled(false));
        assertTrue(store.setSystemServerSafeModeEnabled(false));
        assertFalse(store.isSystemServerHooksEnabled());
        assertFalse(store.isSystemServerSafeModeEnabled());
    }

    @Test
    public void disablesGlobalLogsByDefault() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);

        assertFalse(store.isGlobalLogEnabled());
    }

    @Test
    public void updatesGlobalLogToggle() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);

        assertTrue(store.setGlobalLogEnabled(false));
        assertFalse(store.isGlobalLogEnabled());
    }
}

