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
import static org.junit.Assert.assertNotNull;
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
    public void ttcImportExperimentDefaultsOff() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());

        assertFalse(store.isTtcFontImportEnabled());
    }

    @Test
    public void ttcImportExperimentCanBeEnabledAndDisabled() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());

        assertTrue(store.setTtcFontImportEnabled(true));
        assertTrue(store.isTtcFontImportEnabled());
        assertTrue(store.setTtcFontImportEnabled(false));
        assertFalse(store.isTtcFontImportEnabled());
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
    public void updatesTypefaceIdForConfiguredPackage() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);

        assertTrue(store.setTargetTypefaceId("bin.mt.plus.canary", "font_abcd1234"));

        assertEquals("font_abcd1234", store.getTargetTypefaceId("bin.mt.plus.canary"));
        assertTrue(store.hasPrimaryTargetTypefaceId("bin.mt.plus.canary"));
        assertTrue(store.getConfiguredPackages().contains("bin.mt.plus.canary"));
    }

    @Test
    public void clearsTypefaceIdAndRemovesPackageWhenItIsOnlyConfig() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        assertTrue(store.setTargetTypefaceId("bin.mt.plus.canary", "font_abcd1234"));

        assertTrue(store.clearTargetTypefaceId("bin.mt.plus.canary"));

        assertNull(store.getTargetTypefaceId("bin.mt.plus.canary"));
        assertFalse(store.hasPrimaryTargetTypefaceId("bin.mt.plus.canary"));
        assertFalse(store.getConfiguredPackages().contains("bin.mt.plus.canary"));
    }

    @Test
    public void keepsPackageConfiguredWhenClearingTypefaceButViewportExists() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        assertTrue(store.setTargetViewportWidthDp("bin.mt.plus.canary", 360));
        assertTrue(store.setTargetTypefaceId("bin.mt.plus.canary", "font_abcd1234"));

        assertTrue(store.clearTargetTypefaceId("bin.mt.plus.canary"));

        assertEquals(Integer.valueOf(360), store.getTargetViewportWidthDp("bin.mt.plus.canary"));
        assertTrue(store.getConfiguredPackages().contains("bin.mt.plus.canary"));
    }

    @Test
    public void keepsPackageConfiguredWhenClearingViewportButTypefaceExists() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        assertTrue(store.setTargetViewportWidthDp("bin.mt.plus.canary", 360));
        assertTrue(store.setTargetTypefaceId("bin.mt.plus.canary", "font_abcd1234"));

        assertTrue(store.clearTargetViewportWidthDp("bin.mt.plus.canary"));

        assertNull(store.getTargetViewportWidthDp("bin.mt.plus.canary"));
        assertEquals("font_abcd1234", store.getTargetTypefaceId("bin.mt.plus.canary"));
        assertTrue(store.getConfiguredPackages().contains("bin.mt.plus.canary"));
    }

    @Test
    public void clearTargetPackageConfigRemovesTypefaceId() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        assertTrue(store.setTargetTypefaceId("bin.mt.plus.canary", "font_abcd1234"));

        assertTrue(store.clearTargetPackageConfig("bin.mt.plus.canary"));

        assertNull(store.getTargetTypefaceId("bin.mt.plus.canary"));
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
    public void clearsViewportRemovesPackageWhenOnlyInvalidFontScaleKeyExists() {
        String packageName = "bin.mt.plus.canary";
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putStringSet(DpiConfigStore.KEY_TARGET_PACKAGES,
                        new LinkedHashSet<>(Set.of(packageName)))
                .putInt("viewport." + packageName + ".width_dp", 360)
                .putInt("font." + packageName + ".scale_percent", 301)
                .commit();
        DpiConfigStore store = new DpiConfigStore(prefs);

        assertNull(store.getTargetFontScalePercent(packageName));
        assertTrue(store.clearTargetViewportWidthDp(packageName));
        assertFalse(store.getConfiguredPackages().contains(packageName));
    }

    @Test
    public void enablingDpisRemovesPackageWhenOnlyInvalidNumericKeysRemain() {
        String packageName = "bin.mt.plus.canary";
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putStringSet(DpiConfigStore.KEY_TARGET_PACKAGES,
                        new LinkedHashSet<>(Set.of(packageName)))
                .putInt("viewport." + packageName + ".width_dp", 0)
                .putInt("font." + packageName + ".scale_percent", 301)
                .putBoolean("dpis." + packageName + ".enabled", false)
                .commit();
        DpiConfigStore store = new DpiConfigStore(prefs);

        assertNull(store.getTargetViewportWidthDp(packageName));
        assertNull(store.getTargetFontScalePercent(packageName));
        assertTrue(store.setTargetDpisEnabled(packageName, true));
        assertFalse(store.getConfiguredPackages().contains(packageName));
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
    public void primaryPackageSetShadowsBackupWhenExplicitlyEmpty() {
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

        assertFalse(store.getConfiguredPackages().contains("com.max.xiaoheihe"));
    }

    @Test
    public void primaryPackageSetDoesNotUnionStaleBackupPackages() {
        FakePrefs remotePrefs = new FakePrefs();
        remotePrefs.edit()
                .putStringSet(DpiConfigStore.KEY_TARGET_PACKAGES,
                        new LinkedHashSet<>(Set.of("com.example.current")))
                .commit();
        FakePrefs localPrefs = new FakePrefs();
        localPrefs.edit()
                .putStringSet(DpiConfigStore.KEY_TARGET_PACKAGES,
                        new LinkedHashSet<>(Set.of("com.example.stale")))
                .commit();
        DpiConfigStore store = new DpiConfigStore(remotePrefs, localPrefs);

        assertTrue(store.getConfiguredPackages().contains("com.example.current"));
        assertFalse(store.getConfiguredPackages().contains("com.example.stale"));
    }

    @Test
    public void startupDisclaimerAcceptedFallsBackToBackupTrue() {
        FakePrefs remotePrefs = new FakePrefs();
        remotePrefs.edit()
                .putBoolean(DpiConfigStore.KEY_STARTUP_DISCLAIMER_ACCEPTED, false)
                .commit();
        FakePrefs localPrefs = new FakePrefs();
        localPrefs.edit()
                .putBoolean(DpiConfigStore.KEY_STARTUP_DISCLAIMER_ACCEPTED, true)
                .commit();
        DpiConfigStore store = new DpiConfigStore(remotePrefs, localPrefs);

        assertTrue(store.isStartupDisclaimerAccepted());
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
                .putString("font.library.entries", "[{\"id\":\"font_abcd1234\"}]")
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
        assertEquals("[{\"id\":\"font_abcd1234\"}]", snapshot.get("font.library.entries"));
    }

    @Test
    public void snapshotBackupExcludesFontLibraryMetadataButKeepsTypefaceSelection() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putStringSet(DpiConfigStore.KEY_TARGET_PACKAGES,
                        new LinkedHashSet<>(Set.of("com.max.xiaoheihe")))
                .putString("font.com.max.xiaoheihe.typeface_id", "font_abcd1234")
                .putString("font.library.entries", "[{\"id\":\"font_abcd1234\"}]")
                .putBoolean("font.debug.overlay_enabled", true)
                .putString("runtime.log.ring", "debug log")
                .putString("runtime.log.token", "token")
                .commit();
        DpiConfigStore store = new DpiConfigStore(prefs);

        Map<String, Object> snapshot = store.snapshotBackup();

        assertEquals("font_abcd1234", snapshot.get("font.com.max.xiaoheihe.typeface_id"));
        assertFalse(snapshot.containsKey("font.library.entries"));
        assertFalse(snapshot.containsKey("font.debug.overlay_enabled"));
        assertFalse(snapshot.containsKey("runtime.log.ring"));
        assertFalse(snapshot.containsKey("runtime.log.token"));
    }

    @Test
    public void snapshotBackupIncludesPrefillAndTemplateKeysWithoutFontLibraryMetadata() {
        FakePrefs prefs = new FakePrefs();
        assertTrue(new GlobalPrefillStore(prefs).write(new TemplateConfigValue(
                ViewportTargetSpec.absoluteDp(411),
                ViewportApplyMode.AUTO,
                120,
                FontApplyMode.FIELD_REWRITE,
                "missing_font_id",
                "resources_font")));
        assertTrue(new QuickTemplateStore(prefs).save(new QuickTemplateStore.QuickTemplate(
                "template_a",
                "Compact",
                1000L,
                Set.of("com.example.app"),
                new TemplateConfigValue(
                        ViewportTargetSpec.relativeScale(1100),
                        ViewportApplyMode.COMPAT,
                        115,
                        FontApplyMode.SYSTEM_EMULATION,
                        "missing_template_font_id",
                        "textview_sp"))));
        prefs.edit()
                .putString("font.library.entries", "[{\"id\":\"missing_font_id\"}]")
                .putString("font.library.migration_state", "done")
                .putBoolean("font.debug.overlay_enabled", true)
                .putString("runtime.log.ring", "debug log")
                .commit();
        DpiConfigStore store = new DpiConfigStore(prefs);

        Map<String, Object> all = store.snapshotAll();
        Map<String, Object> backup = store.snapshotBackup();

        assertEquals("missing_font_id", all.get("default_config.font.typeface_id"));
        assertEquals("Compact", all.get("template.template_a.name"));
        assertEquals("missing_font_id", backup.get("default_config.font.typeface_id"));
        assertEquals(411, backup.get("default_config.viewport.width_dp"));
        assertEquals(ViewportApplyMode.AUTO, backup.get("default_config.viewport.mode"));
        assertEquals(120, backup.get("default_config.font.scale_percent"));
        assertEquals("resources_font", backup.get("default_config.font.hook_domains"));
        assertEquals(new LinkedHashSet<>(Set.of("template_a")),
                backup.get(QuickTemplateStore.KEY_TEMPLATE_IDS));
        assertEquals("Compact", backup.get("template.template_a.name"));
        assertEquals(1000L, backup.get("template.template_a.updated_at"));
        assertEquals(new LinkedHashSet<>(Set.of("com.example.app")),
                backup.get("template.template_a.selected_packages"));
        assertEquals(ViewportTargetType.RELATIVE_SCALE,
                backup.get("template.template_a.config.viewport.target_type"));
        assertEquals(1100, backup.get("template.template_a.config.viewport.scale_permille"));
        assertEquals(ViewportApplyMode.COMPAT,
                backup.get("template.template_a.config.viewport.mode"));
        assertEquals(115, backup.get("template.template_a.config.font.scale_percent"));
        assertEquals(FontApplyMode.SYSTEM_EMULATION,
                backup.get("template.template_a.config.font.mode"));
        assertEquals("missing_template_font_id",
                backup.get("template.template_a.config.font.typeface_id"));
        assertEquals("textview_sp", backup.get("template.template_a.config.font.hook_domains"));
        assertFalse(backup.containsKey("font.library.entries"));
        assertFalse(backup.containsKey("font.library.migration_state"));
        assertFalse(backup.containsKey("font.debug.overlay_enabled"));
        assertFalse(backup.containsKey("runtime.log.ring"));
    }

    @Test
    public void replaceBackupIgnoresFontLibraryMetadataButRestoresTypefaceSelection() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put(DpiConfigStore.KEY_TARGET_PACKAGES,
                new LinkedHashSet<>(Set.of("com.max.xiaoheihe")));
        values.put("font.com.max.xiaoheihe.typeface_id", "font_abcd1234");
        values.put("font.library.entries", "[{\"id\":\"font_abcd1234\"}]");
        values.put("font.debug.overlay_enabled", true);
        values.put("runtime.log.ring", "debug log");
        values.put("runtime.log.token", "token");

        assertTrue(store.replaceBackup(values));

        assertEquals("font_abcd1234", store.getTargetTypefaceId("com.max.xiaoheihe"));
        assertFalse(prefs.contains("font.library.entries"));
        assertFalse(prefs.contains("font.debug.overlay_enabled"));
        assertFalse(prefs.contains("runtime.log.ring"));
        assertFalse(prefs.contains("runtime.log.token"));
    }

    @Test
    public void replaceBackupRestoresPrefillAndTemplateKeysWithMissingTypefaceIds() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put(DpiConfigStore.KEY_TARGET_PACKAGES,
                new LinkedHashSet<>(Set.of("com.max.xiaoheihe")));
        values.put("font.com.max.xiaoheihe.typeface_id", "font_abcd1234");
        values.put("default_config.font.typeface_id", "missing_font_id");
        values.put("default_config.viewport.width_dp", 411);
        values.put("default_config.viewport.target_type", ViewportTargetType.ABSOLUTE_DP);
        values.put("default_config.viewport.mode", ViewportApplyMode.AUTO);
        values.put("default_config.font.scale_percent", 120);
        values.put("default_config.font.mode", FontApplyMode.FIELD_REWRITE);
        values.put("default_config.font.hook_domains", "resources_font");
        values.put(QuickTemplateStore.KEY_TEMPLATE_IDS,
                new LinkedHashSet<>(Set.of("template_a")));
        values.put("template.template_a.name", "Compact");
        values.put("template.template_a.updated_at", 1000L);
        values.put("template.template_a.selected_packages",
                new LinkedHashSet<>(Set.of("com.example.app")));
        values.put("template.template_a.config.viewport.target_type",
                ViewportTargetType.RELATIVE_SCALE);
        values.put("template.template_a.config.viewport.scale_permille", 1100);
        values.put("template.template_a.config.viewport.mode", ViewportApplyMode.COMPAT);
        values.put("template.template_a.config.font.scale_percent", 115);
        values.put("template.template_a.config.font.mode", FontApplyMode.SYSTEM_EMULATION);
        values.put("template.template_a.config.font.typeface_id", "missing_template_font_id");
        values.put("template.template_a.config.font.hook_domains", "textview_sp");
        values.put("font.library.entries",
                "[{\"id\":\"missing_font_id\"},{\"id\":\"missing_template_font_id\"}]");
        values.put("font.library.migration_state", "done");

        assertTrue(store.replaceBackup(values));

        assertEquals("font_abcd1234", store.getTargetTypefaceId("com.max.xiaoheihe"));
        TemplateConfigValue prefill = new GlobalPrefillStore(prefs).read();
        assertEquals(ViewportTargetSpec.absoluteDp(411), prefill.viewportTargetSpec);
        assertEquals(ViewportApplyMode.AUTO, prefill.viewportApplyMode);
        assertEquals(Integer.valueOf(120), prefill.fontScalePercent);
        assertEquals(FontApplyMode.FIELD_REWRITE, prefill.fontApplyMode);
        assertEquals("missing_font_id", prefill.typefaceId);
        assertEquals("resources_font", prefill.fontHookDomainsRaw);

        QuickTemplateStore.QuickTemplate template = new QuickTemplateStore(prefs).read("template_a");
        assertNotNull(template);
        assertEquals("Compact", template.name);
        assertEquals(1000L, template.updatedAt);
        assertEquals(new LinkedHashSet<>(Set.of("com.example.app")),
                template.selectedPackages);
        assertEquals(ViewportTargetSpec.relativeScale(1100),
                template.configValue.viewportTargetSpec);
        assertEquals(ViewportApplyMode.COMPAT, template.configValue.viewportApplyMode);
        assertEquals(Integer.valueOf(115), template.configValue.fontScalePercent);
        assertEquals(FontApplyMode.SYSTEM_EMULATION, template.configValue.fontApplyMode);
        assertEquals("missing_template_font_id", template.configValue.typefaceId);
        assertEquals("textview_sp", template.configValue.fontHookDomainsRaw);
        assertFalse(prefs.contains("font.library.entries"));
        assertFalse(prefs.contains("font.library.migration_state"));
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

    @Test
    public void viewportScaleDraftPersistsWithoutChangingAbsoluteActiveType() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        assertTrue(store.setTargetViewportSpec(
                "com.example.app", ViewportTargetSpec.absoluteDp(480)));

        assertTrue(store.setTargetViewportScalePermilleDraft("com.example.app", 1250));

        assertTrue(store.getTargetViewportSpec("com.example.app").isAbsoluteDp());
        assertEquals(Integer.valueOf(480), store.getTargetViewportWidthDp("com.example.app"));
        assertEquals(Integer.valueOf(1250), store.getTargetViewportScalePermille("com.example.app"));
    }

    @Test
    public void viewportWidthDraftPersistsWithoutChangingRelativeActiveType() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        assertTrue(store.setTargetViewportSpec(
                "com.example.app", ViewportTargetSpec.relativeScale(1250)));

        assertTrue(store.setTargetViewportWidthDraft("com.example.app", 480));

        assertTrue(store.getTargetViewportSpec("com.example.app").isRelativeScale());
        assertEquals(Integer.valueOf(1250), store.getTargetViewportScalePermille("com.example.app"));
        assertEquals(Integer.valueOf(480), store.getTargetViewportWidthDp("com.example.app"));
    }

    @Test
    public void wechatTargetFieldAddsAndClearsConfiguredPackage() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);

        assertTrue(store.setWechatTargetField("com.tencent.mm", 600));
        assertEquals(Integer.valueOf(600), store.getWechatTargetField("com.tencent.mm"));
        assertTrue(store.hasTargetAppSpecificConfig("com.tencent.mm"));
        assertTrue(store.getConfiguredPackages().contains("com.tencent.mm"));

        assertTrue(store.clearWechatTargetField("com.tencent.mm"));
        assertNull(store.getWechatTargetField("com.tencent.mm"));
        assertFalse(store.hasTargetAppSpecificConfig("com.tencent.mm"));
        assertFalse(store.getConfiguredPackages().contains("com.tencent.mm"));
    }

    @Test
    public void wechatTargetFieldIgnoresUnsupportedPackageAndOutOfRangeValues() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());

        assertTrue(store.setWechatTargetField("com.example.app", 600));
        assertNull(store.getWechatTargetField("com.example.app"));
        assertFalse(store.getConfiguredPackages().contains("com.example.app"));

        assertTrue(store.setWechatTargetField("com.tencent.mm", 199));
        assertNull(store.getWechatTargetField("com.tencent.mm"));
        assertFalse(store.getConfiguredPackages().contains("com.tencent.mm"));

        assertTrue(store.setWechatTargetField("com.tencent.mm", 200));
        assertEquals(Integer.valueOf(200), store.getWechatTargetField("com.tencent.mm"));
        assertTrue(store.getConfiguredPackages().contains("com.tencent.mm"));
    }

    @Test
    public void migratesLegacyWechatViewportWidthToTargetField() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        assertTrue(store.setTargetViewportWidthDp("com.tencent.mm", 300));
        assertTrue(store.setTargetViewportApplyMode("com.tencent.mm", ViewportApplyMode.SYSTEM));

        assertTrue(store.migrateWechatViewportToTargetFieldIfNeeded());

        assertEquals(Integer.valueOf(300), store.getWechatTargetField("com.tencent.mm"));
        assertNull(store.getTargetViewportWidthDp("com.tencent.mm"));
        assertEquals(ViewportApplyMode.OFF,
                store.getTargetViewportApplyMode("com.tencent.mm"));
        assertTrue(store.getConfiguredPackages().contains("com.tencent.mm"));
    }

    @Test
    public void hasRealPackageConfigTreatsMissingTypefaceIdAsConfig() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());

        assertTrue(store.setTargetTypefaceId("com.example.app", "missing_font_id"));

        assertTrue(store.hasRealPackageConfig("com.example.app"));
        assertEquals("missing_font_id", store.getTargetTypefaceId("com.example.app"));
    }

    @Test
    public void packageTemplateConfigValueRoundTripsCopyableFieldsOnly() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        TemplateConfigValue value = new TemplateConfigValue(
                ViewportTargetSpec.relativeScale(1100),
                ViewportApplyMode.AUTO,
                140,
                FontApplyMode.FIELD_REWRITE,
                "missing_font_id",
                "resources_font,textview_sp");

        assertTrue(store.writePackageTemplateConfigValue("com.example.app", value));

        assertEquals(value, store.readPackageTemplateConfigValue("com.example.app"));
        assertTrue(store.getConfiguredPackages().contains("com.example.app"));
        assertTrue(store.isTargetDpisEnabled("com.example.app"));
        assertFalse(prefs.contains("target.com.example.app.dpis_enabled"));
    }

    @Test
    public void emptyPackageTemplateConfigValuePreservesDisabledStateAndMembership() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        assertTrue(store.setTargetDpisEnabled("com.example.app", false));
        assertTrue(store.setTargetViewportWidthDp("com.example.app", 411));
        assertTrue(store.setTargetTypefaceId("com.example.app", "missing_font_id"));

        assertTrue(store.writePackageTemplateConfigValue(
                "com.example.app", TemplateConfigValue.EMPTY));

        assertFalse(store.isTargetDpisEnabled("com.example.app"));
        assertTrue(store.getConfiguredPackages().contains("com.example.app"));
        assertNull(store.getTargetViewportWidthDp("com.example.app"));
        assertNull(store.getTargetTypefaceId("com.example.app"));
        assertTrue(prefs.contains("target.com.example.app.dpis_enabled"));
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

