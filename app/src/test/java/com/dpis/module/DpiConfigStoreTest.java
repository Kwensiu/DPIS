package com.dpis.module;

import android.content.SharedPreferences;

import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
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
    public void returnsNullEffectiveDensityWhenStoredViewportWidthIsNonPositive() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit().putInt("viewport.bin.mt.plus.canary.width_dp", 0).commit();

        DpiConfigStore store = new DpiConfigStore(prefs);

        assertNull(store.getTargetViewportWidthDp("bin.mt.plus.canary"));
    }

    @Test
    public void defaultsViewportModeToOffWhenLegacyWidthIsInvalid() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit().putInt("viewport.bin.mt.plus.canary.width_dp", 0).commit();

        DpiConfigStore store = new DpiConfigStore(prefs);

        assertEquals(ViewportApplyMode.OFF,
                store.getTargetViewportApplyMode("bin.mt.plus.canary"));
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
    public void returnsNullFontScaleWhenStoredValueOutOfRange() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit().putInt("font.bin.mt.plus.canary.scale_percent", 301).commit();

        DpiConfigStore store = new DpiConfigStore(prefs);

        assertNull(store.getTargetFontScalePercent("bin.mt.plus.canary"));
    }

    @Test
    public void defaultsFontModeToOffWhenLegacyScaleIsInvalid() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit().putInt("font.bin.mt.plus.canary.scale_percent", 301).commit();

        DpiConfigStore store = new DpiConfigStore(prefs);

        assertEquals(FontApplyMode.OFF,
                store.getTargetFontApplyMode("bin.mt.plus.canary"));
    }

    @Test
    public void fallsBackToDefaultsWhenIntReadFails() {
        String viewportKey = "viewport.bin.mt.plus.canary.width_dp";
        String fontKey = "font.bin.mt.plus.canary.scale_percent";
        ThrowingIntReadPrefs prefs = new ThrowingIntReadPrefs(Set.of(viewportKey, fontKey));
        prefs.edit()
                .putString(viewportKey, "not_an_int")
                .putString(fontKey, "not_an_int")
                .commit();

        DpiConfigStore store = new DpiConfigStore(prefs);

        assertNull(store.getTargetViewportWidthDp("bin.mt.plus.canary"));
        assertNull(store.getTargetFontScalePercent("bin.mt.plus.canary"));
    }

    @Test
    public void defaultsFontModeToSystemEmulationWhenLegacyScaleExists() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit().putInt("font.bin.mt.plus.canary.scale_percent", 115).commit();
        DpiConfigStore store = new DpiConfigStore(prefs);

        assertEquals(FontApplyMode.SYSTEM_EMULATION,
                store.getTargetFontApplyMode("bin.mt.plus.canary"));
    }

    @Test
    public void updatesAndClearsFontMode() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        assertTrue(store.setTargetFontScalePercent("bin.mt.plus.canary", 115));

        assertTrue(store.setTargetFontApplyMode("bin.mt.plus.canary", FontApplyMode.FIELD_REWRITE));
        assertEquals(FontApplyMode.FIELD_REWRITE,
                store.getTargetFontApplyMode("bin.mt.plus.canary"));

        assertTrue(store.setTargetFontApplyMode("bin.mt.plus.canary", FontApplyMode.OFF));
        assertEquals(FontApplyMode.SYSTEM_EMULATION,
                store.getTargetFontApplyMode("bin.mt.plus.canary"));
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

    @Test
    public void launcherIconIsVisibleByDefault() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);

        assertFalse(store.isLauncherIconHidden());
    }

    @Test
    public void updatesLauncherIconVisibilityToggle() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);

        assertTrue(store.setLauncherIconHidden(true));
        assertTrue(store.isLauncherIconHidden());
        assertTrue(store.setLauncherIconHidden(false));
        assertFalse(store.isLauncherIconHidden());
    }

    @Test
    public void startupDisclaimerRequiresExplicitAcceptance() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);

        assertFalse(store.isStartupDisclaimerAccepted());
        assertTrue(store.setStartupDisclaimerAccepted(true));
        assertTrue(store.isStartupDisclaimerAccepted());
    }

    @Test
    public void mirrorsWritesToBackupPreferencesWhenConfigured() {
        FakePrefs remotePrefs = new FakePrefs();
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(remotePrefs, localPrefs);

        assertTrue(store.setTargetFontScalePercent("com.max.xiaoheihe", 150));
        assertTrue(store.setTargetViewportWidthDp("com.max.xiaoheihe", 360));

        DpiConfigStore localView = new DpiConfigStore(localPrefs);
        assertEquals(Integer.valueOf(150),
                localView.getTargetFontScalePercent("com.max.xiaoheihe"));
        assertEquals(Integer.valueOf(360),
                localView.getTargetViewportWidthDp("com.max.xiaoheihe"));
    }

    @Test
    public void readsFromBackupWhenPrimaryPreferencesMissingValues() {
        FakePrefs remotePrefs = new FakePrefs();
        FakePrefs localPrefs = new FakePrefs();
        localPrefs.edit()
                .putStringSet(DpiConfigStore.KEY_TARGET_PACKAGES,
                        new LinkedHashSet<>(Set.of("com.max.xiaoheihe")))
                .putInt("font.com.max.xiaoheihe.scale_percent", 165)
                .commit();
        DpiConfigStore store = new DpiConfigStore(remotePrefs, localPrefs);

        assertTrue(store.getConfiguredPackages().contains("com.max.xiaoheihe"));
        assertEquals(Integer.valueOf(165), store.getTargetFontScalePercent("com.max.xiaoheihe"));
    }

    @Test
    public void doesNotFallbackToBackupPackageSetWhenPrimaryExplicitlyEmpty() {
        FakePrefs remotePrefs = new FakePrefs();
        remotePrefs.edit()
                .putStringSet(DpiConfigStore.KEY_TARGET_PACKAGES, new LinkedHashSet<>())
                .commit();
        FakePrefs localPrefs = new FakePrefs();
        localPrefs.edit()
                .putStringSet(DpiConfigStore.KEY_TARGET_PACKAGES,
                        new LinkedHashSet<>(Set.of("com.max.xiaoheihe")))
                .commit();
        DpiConfigStore store = new DpiConfigStore(remotePrefs, localPrefs);

        assertTrue(store.getConfiguredPackages().isEmpty());
    }

    @Test
    public void ensureSeedConfigUsesPrimaryExistenceInsteadOfBackup() {
        FakePrefs remotePrefs = new FakePrefs();
        FakePrefs localPrefs = new FakePrefs();
        localPrefs.edit()
                .putInt("viewport.com.max.xiaoheihe.width_dp", 300)
                .commit();
        DpiConfigStore store = new DpiConfigStore(remotePrefs, localPrefs);
        LinkedHashMap<String, Integer> seed = new LinkedHashMap<>();
        seed.put("com.max.xiaoheihe", DpiConfig.SEED_TARGET_VIEWPORT_WIDTH_DP);

        assertTrue(store.ensureSeedConfig(seed));

        DpiConfigStore remoteOnly = new DpiConfigStore(remotePrefs);
        assertEquals(Integer.valueOf(DpiConfig.SEED_TARGET_VIEWPORT_WIDTH_DP),
                remoteOnly.getTargetViewportWidthDp("com.max.xiaoheihe"));
    }

    @Test
    public void snapshotAllMergesPrimaryAndBackupValues() {
        FakePrefs remotePrefs = new FakePrefs();
        remotePrefs.edit()
                .putBoolean(DpiConfigStore.KEY_GLOBAL_LOG_ENABLED, true)
                .putInt("viewport.com.max.xiaoheihe.width_dp", 420)
                .commit();
        FakePrefs localPrefs = new FakePrefs();
        localPrefs.edit()
                .putBoolean(DpiConfigStore.KEY_GLOBAL_LOG_ENABLED, false)
                .putInt("font.com.max.xiaoheihe.scale_percent", 135)
                .commit();
        DpiConfigStore store = new DpiConfigStore(remotePrefs, localPrefs);

        Map<String, Object> snapshot = store.snapshotAll();

        assertEquals(true, snapshot.get(DpiConfigStore.KEY_GLOBAL_LOG_ENABLED));
        assertEquals(420, snapshot.get("viewport.com.max.xiaoheihe.width_dp"));
        assertEquals(135, snapshot.get("font.com.max.xiaoheihe.scale_percent"));
    }

    @Test
    public void replaceAllOverwritesPrimaryAndBackupValues() {
        FakePrefs remotePrefs = new FakePrefs();
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(remotePrefs, localPrefs);
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put(DpiConfigStore.KEY_GLOBAL_LOG_ENABLED, true);
        values.put("viewport.com.max.xiaoheihe.width_dp", 360);
        values.put("font.com.max.xiaoheihe.scale_percent", 120);
        values.put(DpiConfigStore.KEY_TARGET_PACKAGES,
                new LinkedHashSet<>(Set.of("com.max.xiaoheihe")));

        assertTrue(store.replaceAll(values));

        DpiConfigStore remoteOnly = new DpiConfigStore(remotePrefs);
        DpiConfigStore localOnly = new DpiConfigStore(localPrefs);
        assertTrue(remoteOnly.isGlobalLogEnabled());
        assertEquals(Integer.valueOf(360),
                remoteOnly.getTargetViewportWidthDp("com.max.xiaoheihe"));
        assertEquals(Integer.valueOf(120),
                localOnly.getTargetFontScalePercent("com.max.xiaoheihe"));
        assertTrue(localOnly.getConfiguredPackages().contains("com.max.xiaoheihe"));
    }

    private static final class ThrowingIntReadPrefs implements SharedPreferences {
        private final FakePrefs delegate = new FakePrefs();
        private final Set<String> intReadFailureKeys;

        private ThrowingIntReadPrefs(Set<String> intReadFailureKeys) {
            this.intReadFailureKeys = intReadFailureKeys;
        }

        @Override
        public Map<String, ?> getAll() {
            return delegate.getAll();
        }

        @Override
        public String getString(String key, String defValue) {
            return delegate.getString(key, defValue);
        }

        @Override
        public Set<String> getStringSet(String key, Set<String> defValues) {
            return delegate.getStringSet(key, defValues);
        }

        @Override
        public int getInt(String key, int defValue) {
            if (intReadFailureKeys.contains(key)) {
                throw new ClassCastException("forced int read failure for test");
            }
            return delegate.getInt(key, defValue);
        }

        @Override
        public long getLong(String key, long defValue) {
            return delegate.getLong(key, defValue);
        }

        @Override
        public float getFloat(String key, float defValue) {
            return delegate.getFloat(key, defValue);
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            return delegate.getBoolean(key, defValue);
        }

        @Override
        public boolean contains(String key) {
            return delegate.contains(key);
        }

        @Override
        public Editor edit() {
            return delegate.edit();
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(
                OnSharedPreferenceChangeListener listener) {
            delegate.registerOnSharedPreferenceChangeListener(listener);
        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(
                OnSharedPreferenceChangeListener listener) {
            delegate.unregisterOnSharedPreferenceChangeListener(listener);
        }
    }
}

