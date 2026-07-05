package com.dpis.module;

import com.dpis.module.fonts.FontApplyMode;

import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;
import com.dpis.module.viewport.ViewportTargetType;

import com.dpis.module.templates.TemplateConfigValueAdapters;

import com.dpis.module.templates.GlobalPrefillStore;

import com.dpis.module.templates.QuickTemplateStore;

import com.dpis.module.templates.TemplateConfigValue;

import com.dpis.module.viewport.DpiConfig;

import android.content.SharedPreferences;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

public class DpisConfigStoreTest {
    @Test
    public void parsesConfiguredPackageSetFromStoredStrings() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit().putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES, new LinkedHashSet<>(Arrays.asList(
                "com.max.xiaoheihe", "bin.mt.plus.canary")))
                .putInt("viewport.com.max.xiaoheihe.width_dp", 360)
                .putInt("viewport.bin.mt.plus.canary.width_dp", 420)
                .commit();

        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.getConfiguredPackages().contains("com.max.xiaoheihe"));
        assertTrue(store.getConfiguredPackages().contains("bin.mt.plus.canary"));
        assertFalse(store.getConfiguredPackages().contains("com.example.other"));
    }

    @Test
    public void userVisibleConfigIncludesModeOnlySavedPreference() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES,
                        new LinkedHashSet<>(Set.of("org.mozilla.firefox")))
                .putString("viewport.org.mozilla.firefox.target_type",
                        ViewportTargetType.ABSOLUTE_DP)
                .putString("font.org.mozilla.firefox.mode",
                        FontApplyMode.FIELD_REWRITE)
                .commit();

        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.getConfiguredPackages().contains("org.mozilla.firefox"));
        assertTrue(store.hasUserVisiblePackageConfig("org.mozilla.firefox"));
    }

    @Test
    public void clearingLastValueLeavesUserVisibleConfigWhenModePreferenceRemains() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES,
                        new LinkedHashSet<>(Set.of("org.mozilla.firefox")))
                .putInt("viewport.org.mozilla.firefox.scale_milli_percent", 120000)
                .putString("viewport.org.mozilla.firefox.target_type",
                        ViewportTargetType.RELATIVE_SCALE)
                .putString("font.org.mozilla.firefox.mode",
                        FontApplyMode.FIELD_REWRITE)
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.clearTargetViewportValue("org.mozilla.firefox"));

        assertTrue(store.getConfiguredPackages().contains("org.mozilla.firefox"));
        assertTrue(store.hasUserVisiblePackageConfig("org.mozilla.firefox"));
    }

    @Test
    public void configuredPackagesDerivesCandidatesFromSavedPackageState() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putString("font.com.example.modeonly.mode", FontApplyMode.FIELD_REWRITE)
                .putBoolean("target.com.example.disabled.dpis_enabled", false)
                .putString("font.com.example.domains.hook_domains", "textview_sp")
                .commit();

        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.getConfiguredPackages().contains("com.example.modeonly"));
        assertTrue(store.getConfiguredPackages().contains("com.example.disabled"));
        assertTrue(store.getConfiguredPackages().contains("com.example.domains"));
        assertTrue(store.hasUserVisiblePackageConfig("com.example.modeonly"));
        assertTrue(store.hasUserVisiblePackageConfig("com.example.disabled"));
        assertTrue(store.hasUserVisiblePackageConfig("com.example.domains"));
    }

    @Test
    public void defaultDpisEnabledValueDoesNotCreateConfiguredPackage() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putBoolean("target.com.example.default.dpis_enabled", true)
                .commit();

        DpisConfigStore store = new DpisConfigStore(prefs);

        assertFalse(store.getConfiguredPackages().contains("com.example.default"));
        assertFalse(store.hasUserVisiblePackageConfig("com.example.default"));
    }

    @Test
    public void defaultPackageDraftValuesDoNotCreateConfiguredPackage() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES,
                        new LinkedHashSet<>(Set.of("com.example.default")))
                .putString("viewport.com.example.default.target_type",
                        ViewportTargetType.RELATIVE_SCALE)
                .putString("viewport.com.example.default.mode",
                        ViewportApplyMode.AUTO)
                .putString("font.com.example.default.mode",
                        FontApplyMode.SYSTEM_EMULATION)
                .commit();

        DpisConfigStore store = new DpisConfigStore(prefs);

        assertFalse(store.hasRealPackageConfig("com.example.default"));
        assertFalse(store.hasUserVisiblePackageConfig("com.example.default"));
    }

    @Test
    public void resolvesEffectiveDensityFromTargetValue() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit().putInt("viewport.bin.mt.plus.canary.width_dp", 360).commit();

        DpisConfigStore store = new DpisConfigStore(prefs);

        assertEquals(Integer.valueOf(360), store.getTargetViewportWidthDp("bin.mt.plus.canary"));
    }

    @Test
    public void returnsNullEffectiveDensityWhenTargetMissing() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertNull(store.getTargetViewportWidthDp("bin.mt.plus.canary"));
        assertNull(store.getTargetViewportWidthDp("bin.mt.plus.canary"));
    }

    @Test
    public void returnsNullEffectiveDensityWhenStoredViewportWidthIsNonPositive() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit().putInt("viewport.bin.mt.plus.canary.width_dp", 0).commit();

        DpisConfigStore store = new DpisConfigStore(prefs);

        assertNull(store.getTargetViewportWidthDp("bin.mt.plus.canary"));
    }

    @Test
    public void defaultsViewportModeToOffWhenLegacyWidthIsInvalid() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit().putInt("viewport.bin.mt.plus.canary.width_dp", 0).commit();

        DpisConfigStore store = new DpisConfigStore(prefs);

        assertEquals(ViewportApplyMode.OFF,
                store.getTargetViewportApplyMode("bin.mt.plus.canary"));
    }

    @Test
    public void seedsMissingPackageListAndTargetValuesWithoutOverwritingExistingValues() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES, new LinkedHashSet<>(Set.of("bin.mt.plus.canary")))
                .putInt("viewport.bin.mt.plus.canary.width_dp", 420)
                .commit();

        DpisConfigStore store = new DpisConfigStore(prefs);
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
        DpisConfigStore store = new DpisConfigStore(prefs);
        assertTrue(store.ensureSeedConfig(DpiConfig.getSeedViewportWidthDps()));

        assertTrue(store.setTargetViewportWidthDp("bin.mt.plus.canary", 360));

        assertEquals(Integer.valueOf(360), store.getTargetViewportWidthDp("bin.mt.plus.canary"));
        assertEquals(Integer.valueOf(360), store.getTargetViewportWidthDp("bin.mt.plus.canary"));
    }

    @Test
    public void clearsViewportWidthWhenDisabled() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        assertTrue(store.ensureSeedConfig(DpiConfig.getSeedViewportWidthDps()));

        assertTrue(store.clearTargetViewportWidthDp("bin.mt.plus.canary"));

        assertNull(store.getTargetViewportWidthDp("bin.mt.plus.canary"));
        assertNull(store.getTargetViewportWidthDp("bin.mt.plus.canary"));
        assertFalse(store.getConfiguredPackages().contains("bin.mt.plus.canary"));
    }

    @Test
    public void viewportGettersReadAggregatedViewportKeys() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putString("package_config.com.example.app.viewport.target_type",
                        ViewportTargetType.RELATIVE_SCALE)
                .putInt("package_config.com.example.app.viewport.scale_milli_percent", 125000)
                .putString("package_config.com.example.app.viewport.mode",
                        ViewportApplyMode.SYSTEM)
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertEquals(ViewportTargetType.RELATIVE_SCALE,
                store.getTargetViewportType("com.example.app"));
        assertEquals(Integer.valueOf(125000),
                store.getTargetViewportScaleMilliPercent("com.example.app"));
        assertEquals(ViewportTargetSpec.relativeScale(125000),
                store.getTargetViewportSpec("com.example.app"));
        assertEquals(ViewportApplyMode.SYSTEM,
                store.getTargetViewportApplyMode("com.example.app"));
    }

    @Test
    public void viewportSetterWritesLegacyAndAggregatedKeys() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.setTargetViewportSpec(
                "com.example.app", ViewportTargetSpec.relativeScale(125000)));

        assertEquals(ViewportTargetType.RELATIVE_SCALE,
                prefs.getString("viewport.com.example.app.target_type", null));
        assertEquals(ViewportTargetType.RELATIVE_SCALE,
                prefs.getString("package_config.com.example.app.viewport.target_type", null));
        assertEquals(Integer.valueOf(125000),
                Integer.valueOf(prefs.getInt(
                        "viewport.com.example.app.scale_milli_percent", 0)));
        assertEquals(Integer.valueOf(125000),
                Integer.valueOf(prefs.getInt(
                        "package_config.com.example.app.viewport.scale_milli_percent", 0)));
        // Legacy double-write
        assertEquals(Integer.valueOf(1250),
                Integer.valueOf(prefs.getInt(
                        "viewport.com.example.app.scale_permille", 0)));
        assertEquals(Integer.valueOf(1250),
                Integer.valueOf(prefs.getInt(
                        "package_config.com.example.app.viewport.scale_permille", 0)));
        assertTrue(store.getConfiguredPackages().contains("com.example.app"));
        assertTrue(store.hasUserVisiblePackageConfig("com.example.app"));
    }

    @Test
    public void viewportSpecSetterPreservesExistingApplyMode() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        assertTrue(store.setTargetViewportApplyMode("com.example.app", ViewportApplyMode.SYSTEM));

        assertTrue(store.setTargetViewportSpec(
                "com.example.app", ViewportTargetSpec.relativeScale(125000)));

        assertEquals(ViewportApplyMode.SYSTEM,
                prefs.getString("viewport.com.example.app.mode", null));
        assertEquals(ViewportApplyMode.SYSTEM,
                prefs.getString("package_config.com.example.app.viewport.mode", null));
        assertEquals(ViewportApplyMode.SYSTEM,
                store.getTargetViewportApplyMode("com.example.app"));
    }

    @Test
    public void viewportWidthSetterPreservesExistingApplyMode() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        assertTrue(store.setTargetViewportApplyMode("com.example.app", ViewportApplyMode.COMPAT));

        assertTrue(store.setTargetViewportWidthDp("com.example.app", 411));

        assertEquals(ViewportApplyMode.COMPAT,
                prefs.getString("viewport.com.example.app.mode", null));
        assertEquals(ViewportApplyMode.COMPAT,
                prefs.getString("package_config.com.example.app.viewport.mode", null));
        assertEquals(ViewportApplyMode.COMPAT,
                store.getTargetViewportApplyMode("com.example.app"));
    }

    @Test
    public void viewportClearRemovesLegacyAndAggregatedKeys() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        assertTrue(store.setTargetViewportSpec(
                "com.example.app", ViewportTargetSpec.relativeScale(125000)));
        assertTrue(store.setTargetViewportApplyMode("com.example.app", ViewportApplyMode.SYSTEM));

        assertTrue(store.clearTargetViewportWidthDp("com.example.app"));

        assertNull(store.getTargetViewportScaleMilliPercent("com.example.app"));
        assertEquals(ViewportTargetType.OFF, store.getTargetViewportType("com.example.app"));
        assertEquals(ViewportApplyMode.OFF, store.getTargetViewportApplyMode("com.example.app"));
        assertFalse(prefs.contains("viewport.com.example.app.target_type"));
        assertFalse(prefs.contains("viewport.com.example.app.scale_milli_percent"));
        assertFalse(prefs.contains("viewport.com.example.app.scale_permille"));
        assertFalse(prefs.contains("viewport.com.example.app.mode"));
        assertFalse(prefs.contains("package_config.com.example.app.viewport.target_type"));
        assertFalse(prefs.contains("package_config.com.example.app.viewport.scale_milli_percent"));
        assertFalse(prefs.contains("package_config.com.example.app.viewport.scale_permille"));
        assertFalse(prefs.contains("package_config.com.example.app.viewport.mode"));
        assertFalse(store.getConfiguredPackages().contains("com.example.app"));
    }

    @Test
    public void legacyOnlyViewportKeysRemainReadable() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putString("viewport.com.example.app.target_type",
                        ViewportTargetType.ABSOLUTE_DP)
                .putInt("viewport.com.example.app.width_dp", 411)
                .putString("viewport.com.example.app.mode", ViewportApplyMode.COMPAT)
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertEquals(ViewportTargetType.ABSOLUTE_DP,
                store.getTargetViewportType("com.example.app"));
        assertEquals(Integer.valueOf(411), store.getTargetViewportWidthDp("com.example.app"));
        assertEquals(ViewportTargetSpec.absoluteDp(411),
                store.getTargetViewportSpec("com.example.app"));
        assertEquals(ViewportApplyMode.COMPAT,
                store.getTargetViewportApplyMode("com.example.app"));
    }

    @Test
    public void configuredPackagesIncludeMixedLegacyAndAggregatedViewportState() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putInt("viewport.com.example.legacy.width_dp", 411)
                .putString("package_config.com.example.aggregated.viewport.mode",
                        ViewportApplyMode.SYSTEM)
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.getConfiguredPackages().contains("com.example.legacy"));
        assertTrue(store.getConfiguredPackages().contains("com.example.aggregated"));
        assertTrue(store.hasUserVisiblePackageConfig("com.example.legacy"));
        assertTrue(store.hasUserVisiblePackageConfig("com.example.aggregated"));
    }

    @Test
    public void updatesFontScaleForConfiguredPackage() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.setTargetFontScalePercent("bin.mt.plus.canary", 115));

        assertEquals(Integer.valueOf(115), store.getTargetFontScalePercent("bin.mt.plus.canary"));
        assertEquals(Integer.valueOf(115),
                Integer.valueOf(prefs.getInt("font.bin.mt.plus.canary.scale_percent", 0)));
        assertEquals(Integer.valueOf(115),
                Integer.valueOf(prefs.getInt(
                        "package_config.bin.mt.plus.canary.font.scale_percent", 0)));
        assertTrue(store.getConfiguredPackages().contains("bin.mt.plus.canary"));
    }

    @Test
    public void fontGettersReadAggregatedFontKeys() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putInt("package_config.com.example.app.font.scale_percent", 135)
                .putString("package_config.com.example.app.font.mode",
                        FontApplyMode.FIELD_REWRITE)
                .putString("package_config.com.example.app.font.typeface_id",
                        "font_abcd1234")
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertEquals(Integer.valueOf(135), store.getTargetFontScalePercent("com.example.app"));
        assertEquals(FontApplyMode.FIELD_REWRITE,
                store.getTargetFontApplyMode("com.example.app"));
        assertEquals("font_abcd1234", store.getTargetTypefaceId("com.example.app"));
    }

    @Test
    public void fontSetterWritesLegacyAndAggregatedKeys() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.setTargetFontScalePercent("com.example.app", 135));
        assertTrue(store.setTargetFontApplyMode("com.example.app", FontApplyMode.FIELD_REWRITE));
        assertTrue(store.setTargetTypefaceId("com.example.app", "font_abcd1234"));

        assertEquals(Integer.valueOf(135),
                Integer.valueOf(prefs.getInt("font.com.example.app.scale_percent", 0)));
        assertEquals(Integer.valueOf(135),
                Integer.valueOf(prefs.getInt(
                        "package_config.com.example.app.font.scale_percent", 0)));
        assertEquals(FontApplyMode.FIELD_REWRITE,
                prefs.getString("font.com.example.app.mode", null));
        assertEquals(FontApplyMode.FIELD_REWRITE,
                prefs.getString("package_config.com.example.app.font.mode", null));
        assertEquals("font_abcd1234",
                prefs.getString("font.com.example.app.typeface_id", null));
        assertEquals("font_abcd1234",
                prefs.getString("package_config.com.example.app.font.typeface_id", null));
    }

    @Test
    public void fontClearRemovesLegacyAndAggregatedKeys() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        assertTrue(store.setTargetFontScalePercent("com.example.app", 135));
        assertTrue(store.setTargetTypefaceId("com.example.app", "font_abcd1234"));

        assertTrue(store.clearTargetFontScalePercent("com.example.app"));
        assertTrue(store.clearTargetTypefaceId("com.example.app"));
        assertTrue(store.setTargetFontApplyMode("com.example.app", FontApplyMode.OFF));

        assertNull(store.getTargetFontScalePercent("com.example.app"));
        assertNull(store.getTargetTypefaceId("com.example.app"));
        assertEquals(FontApplyMode.OFF, store.getTargetFontApplyMode("com.example.app"));
        assertFalse(prefs.contains("font.com.example.app.scale_percent"));
        assertFalse(prefs.contains("package_config.com.example.app.font.scale_percent"));
        assertFalse(prefs.contains("font.com.example.app.typeface_id"));
        assertFalse(prefs.contains("package_config.com.example.app.font.typeface_id"));
        assertFalse(prefs.contains("font.com.example.app.mode"));
        assertFalse(prefs.contains("package_config.com.example.app.font.mode"));
        assertFalse(store.getConfiguredPackages().contains("com.example.app"));
    }

    @Test
    public void legacyOnlyFontKeysRemainReadable() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putInt("font.com.example.app.scale_percent", 135)
                .putString("font.com.example.app.mode", FontApplyMode.FIELD_REWRITE)
                .putString("font.com.example.app.typeface_id", "font_abcd1234")
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertEquals(Integer.valueOf(135), store.getTargetFontScalePercent("com.example.app"));
        assertEquals(FontApplyMode.FIELD_REWRITE,
                store.getTargetFontApplyMode("com.example.app"));
        assertEquals("font_abcd1234", store.getTargetTypefaceId("com.example.app"));
    }

    @Test
    public void configuredPackagesIncludeMixedLegacyAndAggregatedFontState() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putString("font.com.example.legacy.mode", FontApplyMode.FIELD_REWRITE)
                .putString("package_config.com.example.aggregated.font.mode",
                        FontApplyMode.FIELD_REWRITE)
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.getConfiguredPackages().contains("com.example.legacy"));
        assertTrue(store.getConfiguredPackages().contains("com.example.aggregated"));
        assertTrue(store.hasUserVisiblePackageConfig("com.example.legacy"));
        assertTrue(store.hasUserVisiblePackageConfig("com.example.aggregated"));
    }

    @Test
    public void aggregatedFontScaleDefaultsModeToSystemEmulationWhenModeMissing() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putInt("package_config.com.example.app.font.scale_percent", 135)
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertEquals(FontApplyMode.SYSTEM_EMULATION,
                store.getTargetFontApplyMode("com.example.app"));
    }

    @Test
    public void hookDomainGetterReadsAggregatedKey() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putString("package_config.com.example.app.font.hook_domains",
                        "resources_font,textview_sp")
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertEquals("resources_font,textview_sp",
                store.getPackageFontHookDomainsRaw("com.example.app"));
    }

    @Test
    public void hookDomainSetterWritesLegacyAndAggregatedKeys() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.setPackageFontHookDomainsRaw(
                "com.example.app", "resources_font,textview_sp"));

        assertEquals("resources_font,textview_sp",
                prefs.getString("font.com.example.app.hook_domains", null));
        assertEquals("resources_font,textview_sp",
                prefs.getString(
                        "package_config.com.example.app.font.hook_domains", null));
        assertTrue(store.getConfiguredPackages().contains("com.example.app"));
        assertTrue(store.hasUserVisiblePackageConfig("com.example.app"));
    }

    @Test
    public void hookDomainClearRemovesLegacyAndAggregatedKeys() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        assertTrue(store.setPackageFontHookDomainsRaw("com.example.app", "resources_font"));

        assertTrue(store.clearPackageFontHookDomainsRaw("com.example.app"));

        assertNull(store.getPackageFontHookDomainsRaw("com.example.app"));
        assertFalse(prefs.contains("font.com.example.app.hook_domains"));
        assertFalse(prefs.contains("package_config.com.example.app.font.hook_domains"));
        assertFalse(store.getConfiguredPackages().contains("com.example.app"));
    }

    @Test
    public void legacyOnlyHookDomainKeyRemainsReadable() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putString("font.com.example.app.hook_domains", "textview_sp")
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertEquals("textview_sp", store.getPackageFontHookDomainsRaw("com.example.app"));
    }

    @Test
    public void configuredPackagesIncludeMixedLegacyAndAggregatedHookDomainState() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putString("font.com.example.legacy.hook_domains", "textview_sp")
                .putString("package_config.com.example.aggregated.font.hook_domains",
                        "resources_font")
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.getConfiguredPackages().contains("com.example.legacy"));
        assertTrue(store.getConfiguredPackages().contains("com.example.aggregated"));
        assertTrue(store.hasUserVisiblePackageConfig("com.example.legacy"));
        assertTrue(store.hasUserVisiblePackageConfig("com.example.aggregated"));
    }

    @Test
    public void updatesWechatDpiForConfiguredPackage() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.setWechatDpi("com.tencent.mm", 360));

        assertEquals(Integer.valueOf(360), store.getWechatDpi("com.tencent.mm"));
        assertEquals(Integer.valueOf(360),
                Integer.valueOf(prefs.getInt("wechat.com.tencent.mm.dpi", 0)));
        assertEquals(Integer.valueOf(360),
                Integer.valueOf(prefs.getInt(
                        "package_config.com.tencent.mm.app.wechat_dpi", 0)));
        assertTrue(store.hasTargetAppSpecificConfig("com.tencent.mm"));
        assertTrue(store.getConfiguredPackages().contains("com.tencent.mm"));
    }

    @Test
    public void dpisEnabledReadsAggregatedDisabledOverride() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putBoolean("package_config.com.example.app.target.dpis_enabled", false)
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertFalse(store.isTargetDpisEnabled("com.example.app"));
        assertTrue(store.getConfiguredPackages().contains("com.example.app"));
        assertTrue(store.hasUserVisiblePackageConfig("com.example.app"));
    }

    @Test
    public void dpisEnabledSetterWritesAndClearsLegacyAndAggregatedKeys() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.setTargetDpisEnabled("com.example.app", false));

        assertFalse(store.isTargetDpisEnabled("com.example.app"));
        assertFalse(prefs.getBoolean("target.com.example.app.dpis_enabled", true));
        assertFalse(prefs.getBoolean(
                "package_config.com.example.app.target.dpis_enabled", true));
        assertTrue(store.getConfiguredPackages().contains("com.example.app"));

        assertTrue(store.setTargetDpisEnabled("com.example.app", true));

        assertTrue(store.isTargetDpisEnabled("com.example.app"));
        assertFalse(prefs.contains("target.com.example.app.dpis_enabled"));
        assertFalse(prefs.contains("package_config.com.example.app.target.dpis_enabled"));
        assertFalse(store.getConfiguredPackages().contains("com.example.app"));
    }

    @Test
    public void clearsWechatDpiWhenDisabled() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        assertTrue(store.setWechatDpi("com.tencent.mm", 360));

        assertTrue(store.clearWechatDpi("com.tencent.mm"));

        assertNull(store.getWechatDpi("com.tencent.mm"));
        assertFalse(prefs.contains("wechat.com.tencent.mm.dpi"));
        assertFalse(prefs.contains("package_config.com.tencent.mm.app.wechat_dpi"));
        assertFalse(store.getConfiguredPackages().contains("com.tencent.mm"));
    }

    @Test
    public void wechatDpiGetterReadsAggregatedKey() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putInt("package_config.com.tencent.mm.app.wechat_dpi", 600)
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertEquals(Integer.valueOf(600), store.getWechatDpi("com.tencent.mm"));
        assertTrue(store.hasTargetAppSpecificConfig("com.tencent.mm"));
        assertTrue(store.getConfiguredPackages().contains("com.tencent.mm"));
        assertTrue(store.hasUserVisiblePackageConfig("com.tencent.mm"));
    }

    @Test
    public void aggregatedWechatDpiIgnoredForUnsupportedPackage() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putInt("package_config.com.example.app.app.wechat_dpi", 600)
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertNull(store.getWechatDpi("com.example.app"));
        assertFalse(store.getConfiguredPackages().contains("com.example.app"));
        assertFalse(store.hasUserVisiblePackageConfig("com.example.app"));
    }

    @Test
    public void migratesLegacyWechatDpiToOfficialKey() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putInt("wechat.com.tencent.mm.wekit_dpi", 360)
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.migrateLegacyWechatDpi());

        assertEquals(Integer.valueOf(360), store.getWechatDpi("com.tencent.mm"));
        assertTrue(store.getConfiguredPackages().contains("com.tencent.mm"));
        assertFalse(prefs.contains("wechat.com.tencent.mm.wekit_dpi"));
        assertEquals(Integer.valueOf(360),
                Integer.valueOf(prefs.getInt(
                        "package_config.com.tencent.mm.app.wechat_dpi", 0)));
    }

    @Test
    public void legacyWechatDpiMigrationDoesNotOverwriteOfficialKey() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putInt("wechat.com.tencent.mm.wekit_dpi", 360)
                .putInt("wechat.com.tencent.mm.dpi", 600)
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.migrateLegacyWechatDpi());

        assertEquals(Integer.valueOf(600), store.getWechatDpi("com.tencent.mm"));
        assertFalse(prefs.contains("wechat.com.tencent.mm.wekit_dpi"));
    }

    @Test
    public void legacyWechatDpiMigrationDoesNotOverwriteLocalOfficialKey() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putInt("wechat.com.tencent.mm.wekit_dpi", 360)
                .putInt("wechat.com.tencent.mm.dpi", 600)
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.migrateLegacyWechatDpi());

        assertEquals(Integer.valueOf(600), store.getWechatDpi("com.tencent.mm"));
        assertFalse(prefs.contains("wechat.com.tencent.mm.wekit_dpi"));
    }

    @Test
    public void ttcImportExperimentDefaultsOff() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());

        assertFalse(store.isTtcFontImportEnabled());
    }

    @Test
    public void ttcImportExperimentCanBeEnabledAndDisabled() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());

        assertTrue(store.setTtcFontImportEnabled(true));
        assertTrue(store.isTtcFontImportEnabled());
        assertTrue(store.setTtcFontImportEnabled(false));
        assertFalse(store.isTtcFontImportEnabled());
    }

    @Test
    public void clearsFontScaleWhenDisabled() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        assertTrue(store.setTargetFontScalePercent("bin.mt.plus.canary", 115));

        assertTrue(store.clearTargetFontScalePercent("bin.mt.plus.canary"));

        assertNull(store.getTargetFontScalePercent("bin.mt.plus.canary"));
        assertFalse(prefs.contains("font.bin.mt.plus.canary.scale_percent"));
        assertFalse(prefs.contains("package_config.bin.mt.plus.canary.font.scale_percent"));
        assertFalse(store.getConfiguredPackages().contains("bin.mt.plus.canary"));
    }

    @Test
    public void updatesTypefaceIdForConfiguredPackage() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.setTargetTypefaceId("bin.mt.plus.canary", "font_abcd1234"));

        assertEquals("font_abcd1234", store.getTargetTypefaceId("bin.mt.plus.canary"));
        assertTrue(store.hasPrimaryTargetTypefaceId("bin.mt.plus.canary"));
        assertEquals("font_abcd1234",
                prefs.getString("package_config.bin.mt.plus.canary.font.typeface_id", null));
        assertTrue(store.getConfiguredPackages().contains("bin.mt.plus.canary"));
    }

    @Test
    public void clearsTypefaceIdAndRemovesPackageWhenItIsOnlyConfig() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        assertTrue(store.setTargetTypefaceId("bin.mt.plus.canary", "font_abcd1234"));

        assertTrue(store.clearTargetTypefaceId("bin.mt.plus.canary"));

        assertNull(store.getTargetTypefaceId("bin.mt.plus.canary"));
        assertFalse(store.hasPrimaryTargetTypefaceId("bin.mt.plus.canary"));
        assertFalse(prefs.contains("font.bin.mt.plus.canary.typeface_id"));
        assertFalse(prefs.contains("package_config.bin.mt.plus.canary.font.typeface_id"));
        assertFalse(store.getConfiguredPackages().contains("bin.mt.plus.canary"));
    }

    @Test
    public void keepsPackageConfiguredWhenClearingTypefaceButViewportExists() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        assertTrue(store.setTargetViewportWidthDp("bin.mt.plus.canary", 360));
        assertTrue(store.setTargetTypefaceId("bin.mt.plus.canary", "font_abcd1234"));

        assertTrue(store.clearTargetTypefaceId("bin.mt.plus.canary"));

        assertEquals(Integer.valueOf(360), store.getTargetViewportWidthDp("bin.mt.plus.canary"));
        assertTrue(store.getConfiguredPackages().contains("bin.mt.plus.canary"));
    }

    @Test
    public void keepsPackageConfiguredWhenClearingViewportButTypefaceExists() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
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
        DpisConfigStore store = new DpisConfigStore(prefs);
        assertTrue(store.setTargetTypefaceId("bin.mt.plus.canary", "font_abcd1234"));

        assertTrue(store.clearTargetPackageConfig("bin.mt.plus.canary"));

        assertNull(store.getTargetTypefaceId("bin.mt.plus.canary"));
        assertFalse(store.getConfiguredPackages().contains("bin.mt.plus.canary"));
    }

    @Test
    public void returnsNullFontScaleWhenStoredValueOutOfRange() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit().putInt("font.bin.mt.plus.canary.scale_percent", 301).commit();

        DpisConfigStore store = new DpisConfigStore(prefs);

        assertNull(store.getTargetFontScalePercent("bin.mt.plus.canary"));
    }

    @Test
    public void defaultsFontModeToOffWhenLegacyScaleIsInvalid() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit().putInt("font.bin.mt.plus.canary.scale_percent", 301).commit();

        DpisConfigStore store = new DpisConfigStore(prefs);

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

        DpisConfigStore store = new DpisConfigStore(prefs);

        assertNull(store.getTargetViewportWidthDp("bin.mt.plus.canary"));
        assertNull(store.getTargetFontScalePercent("bin.mt.plus.canary"));
    }

    @Test
    public void defaultsFontModeToSystemEmulationWhenLegacyScaleExists() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit().putInt("font.bin.mt.plus.canary.scale_percent", 115).commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertEquals(FontApplyMode.SYSTEM_EMULATION,
                store.getTargetFontApplyMode("bin.mt.plus.canary"));
    }

    @Test
    public void updatesAndClearsFontMode() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        assertTrue(store.setTargetFontScalePercent("bin.mt.plus.canary", 115));

        assertTrue(store.setTargetFontApplyMode("bin.mt.plus.canary", FontApplyMode.FIELD_REWRITE));
        assertEquals(FontApplyMode.FIELD_REWRITE,
                store.getTargetFontApplyMode("bin.mt.plus.canary"));
        assertEquals(FontApplyMode.FIELD_REWRITE,
                prefs.getString("package_config.bin.mt.plus.canary.font.mode", null));

        assertTrue(store.setTargetFontApplyMode("bin.mt.plus.canary", FontApplyMode.OFF));
        assertEquals(FontApplyMode.SYSTEM_EMULATION,
                store.getTargetFontApplyMode("bin.mt.plus.canary"));
        assertFalse(prefs.contains("font.bin.mt.plus.canary.mode"));
        assertFalse(prefs.contains("package_config.bin.mt.plus.canary.font.mode"));
    }

    @Test
    public void keepsPackageConfiguredWhenClearingViewportButFontScaleExists() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
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
                .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES,
                        new LinkedHashSet<>(Set.of(packageName)))
                .putInt("viewport." + packageName + ".width_dp", 360)
                .putInt("font." + packageName + ".scale_percent", 301)
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertNull(store.getTargetFontScalePercent(packageName));
        assertTrue(store.clearTargetViewportWidthDp(packageName));
        assertFalse(store.getConfiguredPackages().contains(packageName));
    }

    @Test
    public void enablingDpisRemovesPackageWhenOnlyInvalidNumericKeysRemain() {
        String packageName = "bin.mt.plus.canary";
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES,
                        new LinkedHashSet<>(Set.of(packageName)))
                .putInt("viewport." + packageName + ".width_dp", 0)
                .putInt("font." + packageName + ".scale_percent", 301)
                .putBoolean("dpis." + packageName + ".enabled", false)
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertNull(store.getTargetViewportWidthDp(packageName));
        assertNull(store.getTargetFontScalePercent(packageName));
        assertTrue(store.setTargetDpisEnabled(packageName, true));
        assertFalse(store.getConfiguredPackages().contains(packageName));
    }

    @Test
    public void keepsPackageConfiguredWhenClearingFontScaleButViewportExists() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
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
        DpisConfigStore store = new DpisConfigStore(prefs);
        assertTrue(store.ensureSeedConfig(DpiConfig.getSeedViewportWidthDps()));
        prefs.setCommitResult(false);

        assertFalse(store.setTargetViewportWidthDp("bin.mt.plus.canary", 320));
        assertEquals(Integer.valueOf(DpiConfig.SEED_TARGET_VIEWPORT_WIDTH_DP),
                store.getTargetViewportWidthDp("bin.mt.plus.canary"));
    }

    @Test
    public void reportsFailureWhenViewportWidthClearCommitFails() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        assertTrue(store.ensureSeedConfig(DpiConfig.getSeedViewportWidthDps()));
        prefs.setCommitResult(false);

        assertFalse(store.clearTargetViewportWidthDp("bin.mt.plus.canary"));
        assertEquals(Integer.valueOf(DpiConfig.SEED_TARGET_VIEWPORT_WIDTH_DP),
                store.getTargetViewportWidthDp("bin.mt.plus.canary"));
    }

    @Test
    public void disablesSystemServerHooksByDefault() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.isSystemServerHooksEnabled());
    }

    @Test
    public void enablesSystemServerSafeModeByDefault() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.isSystemServerSafeModeEnabled());
    }

    @Test
    public void updatesSystemServerGlobalToggles() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.setSystemServerHooksEnabled(false));
        assertTrue(store.setSystemServerSafeModeEnabled(false));
        assertFalse(store.isSystemServerHooksEnabled());
        assertFalse(store.isSystemServerSafeModeEnabled());
    }

    @Test
    public void disablesGlobalLogsByDefault() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertFalse(store.isGlobalLogEnabled());
    }

    @Test
    public void updatesGlobalLogToggle() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.setGlobalLogEnabled(false));
        assertFalse(store.isGlobalLogEnabled());
    }

    @Test
    public void startupDisclaimerRequiresExplicitAcceptance() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertFalse(store.isStartupDisclaimerAccepted());
        assertTrue(store.setStartupDisclaimerAccepted(true));
        assertTrue(store.isStartupDisclaimerAccepted());
    }

    @Test
    public void storeReadsOnlyItsOwnPreferences() {
        FakePrefs localPrefs = new FakePrefs();
        FakePrefs remotePrefs = new FakePrefs();
        remotePrefs.edit()
                .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES,
                        new LinkedHashSet<>(Set.of("com.max.xiaoheihe")))
                .putInt("font.com.max.xiaoheihe.scale_percent", 165)
                .putBoolean(DpisConfigStore.KEY_GLOBAL_LOG_ENABLED, true)
                .commit();
        DpisConfigStore store = new DpisConfigStore(localPrefs);

        assertFalse(store.getConfiguredPackages().contains("com.max.xiaoheihe"));
        assertNull(store.getTargetFontScalePercent("com.max.xiaoheihe"));
        assertFalse(store.isGlobalLogEnabled());
    }

    @Test
    public void storeWritesOnlyItsOwnPreferences() {
        FakePrefs localPrefs = new FakePrefs();
        FakePrefs remotePrefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(localPrefs);

        assertTrue(store.setTargetFontScalePercent("com.max.xiaoheihe", 150));
        assertTrue(store.setTargetViewportWidthDp("com.max.xiaoheihe", 360));
        assertTrue(store.setStartupDisclaimerAccepted(true));
        assertTrue(store.setInterfaceScalePercent(73));

        assertEquals(Integer.valueOf(150),
                store.getTargetFontScalePercent("com.max.xiaoheihe"));
        assertEquals(Integer.valueOf(360),
                store.getTargetViewportWidthDp("com.max.xiaoheihe"));
        assertTrue(store.isStartupDisclaimerAccepted());
        assertEquals(73, store.getInterfaceScalePercent());
        assertTrue(remotePrefs.getAll().isEmpty());
    }

    @Test
    public void localOnlyUiStateUsesExplicitLocalPreferences() {
        FakePrefs remotePrefs = new FakePrefs();
        FakePrefs localPrefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(remotePrefs, null, null, localPrefs);

        assertTrue(store.setTargetFontScalePercent("com.max.xiaoheihe", 150));
        assertTrue(store.setInterfaceScalePercent(73));
        assertTrue(store.setStartupDisclaimerAccepted(true));

        assertEquals(Integer.valueOf(150),
                store.getTargetFontScalePercent("com.max.xiaoheihe"));
        assertEquals(73, store.getInterfaceScalePercent());
        assertTrue(store.isStartupDisclaimerAccepted());
        assertTrue(remotePrefs.getAll().containsKey("font.com.max.xiaoheihe.scale_percent"));
        assertFalse(remotePrefs.getAll().containsKey(DpisConfigStore.KEY_INTERFACE_SCALE_PERCENT));
        assertFalse(remotePrefs.getAll().containsKey(DpisConfigStore.KEY_STARTUP_DISCLAIMER_ACCEPTED));
        assertEquals(73, localPrefs.getInt(DpisConfigStore.KEY_INTERFACE_SCALE_PERCENT, 0));
        assertTrue(localPrefs.getBoolean(DpisConfigStore.KEY_STARTUP_DISCLAIMER_ACCEPTED, false));
    }

    @Test
    public void ensureSeedConfigUsesOnlyCurrentStoreExistence() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putInt("viewport.com.max.xiaoheihe.width_dp", 300)
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);
        LinkedHashMap<String, Integer> seed = new LinkedHashMap<>();
        seed.put("com.max.xiaoheihe", DpiConfig.SEED_TARGET_VIEWPORT_WIDTH_DP);

        assertTrue(store.ensureSeedConfig(seed));

        assertEquals(Integer.valueOf(300),
                store.getTargetViewportWidthDp("com.max.xiaoheihe"));
    }

    @Test
    public void snapshotAllUsesOnlyCurrentStoreValues() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putBoolean(DpisConfigStore.KEY_GLOBAL_LOG_ENABLED, true)
                .putBoolean(DpisConfigStore.KEY_STARTUP_DISCLAIMER_ACCEPTED, true)
                .putInt("viewport.com.max.xiaoheihe.width_dp", 420)
                .putString("font.library.entries", "[{\"id\":\"font_abcd1234\"}]")
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        Map<String, Object> snapshot = store.snapshotAll();

        assertEquals(true, snapshot.get(DpisConfigStore.KEY_GLOBAL_LOG_ENABLED));
        assertEquals(420, snapshot.get("viewport.com.max.xiaoheihe.width_dp"));
        assertEquals("[{\"id\":\"font_abcd1234\"}]", snapshot.get("font.library.entries"));
        assertEquals(true, snapshot.get(DpisConfigStore.KEY_STARTUP_DISCLAIMER_ACCEPTED));
    }

    @Test
    public void snapshotRuntimeDeliveryExcludesLocalOnlyUiStateAndTemplates() {
        FakePrefs remotePrefs = new FakePrefs();
        remotePrefs.edit()
                .putBoolean(DpisConfigStore.KEY_STARTUP_DISCLAIMER_ACCEPTED, true)
                .putInt(DpisConfigStore.KEY_INTERFACE_SCALE_PERCENT, 73)
                .putString("default_config.font.typeface_id", "remote_default_font")
                .putStringSet(QuickTemplateStore.KEY_TEMPLATE_IDS,
                        new LinkedHashSet<>(Set.of("template_a")))
                .putString("template.template_a.name", "Remote")
                .putBoolean(DpisConfigStore.KEY_GLOBAL_LOG_ENABLED, true)
                .commit();
        DpisConfigStore store = new DpisConfigStore(remotePrefs);

        Map<String, Object> snapshot = store.snapshotRuntimeDelivery();

        assertFalse(snapshot.containsKey(DpisConfigStore.KEY_STARTUP_DISCLAIMER_ACCEPTED));
        assertFalse(snapshot.containsKey(DpisConfigStore.KEY_INTERFACE_SCALE_PERCENT));
        assertFalse(snapshot.containsKey("default_config.font.typeface_id"));
        assertFalse(snapshot.containsKey(QuickTemplateStore.KEY_TEMPLATE_IDS));
        assertFalse(snapshot.containsKey("template.template_a.name"));
        assertEquals(true, snapshot.get(DpisConfigStore.KEY_GLOBAL_LOG_ENABLED));
    }

    @Test
    public void snapshotBackupExcludesFontLibraryMetadataButKeepsTypefaceSelection() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES,
                        new LinkedHashSet<>(Set.of("com.max.xiaoheihe")))
                .putString("font.com.max.xiaoheihe.typeface_id", "font_abcd1234")
                .putString("package_config.com.max.xiaoheihe.font.typeface_id",
                        "font_abcd1234")
                .putString("font.library.entries", "[{\"id\":\"font_abcd1234\"}]")
                .putBoolean("font.debug.overlay_enabled", true)
                .putString("runtime.log.ring", "debug log")
                .putString("runtime.log.token", "token")
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        Map<String, Object> snapshot = store.snapshotBackup();

        assertFalse(snapshot.containsKey(DpisConfigStore.KEY_TARGET_PACKAGES));
        assertFalse(snapshot.containsKey("font.com.max.xiaoheihe.typeface_id"));
        assertEquals("font_abcd1234",
                snapshot.get("package_config.com.max.xiaoheihe.font.typeface_id"));
        assertFalse(snapshot.containsKey("font.library.entries"));
        assertFalse(snapshot.containsKey("font.debug.overlay_enabled"));
        assertFalse(snapshot.containsKey("runtime.log.ring"));
        assertFalse(snapshot.containsKey("runtime.log.token"));
    }

    @Test
    public void snapshotBackupIncludesPrefillAndTemplateKeysWithoutFontLibraryMetadata() {
        FakePrefs prefs = new FakePrefs();
        assertTrue(new GlobalPrefillStore(prefs).write(TemplateConfigValueAdapters.fromViewportTargetSpec(
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
                TemplateConfigValueAdapters.fromViewportTargetSpec(
                        ViewportTargetSpec.relativeScale(110000),
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
        DpisConfigStore store = new DpisConfigStore(prefs);

        Map<String, Object> all = store.snapshotAll();
        Map<String, Object> backup = store.snapshotBackup();

        assertEquals("missing_font_id", all.get("default_config.font.typeface_id"));
        assertEquals("Compact", all.get("template.template_a.name"));
        assertFalse(backup.containsKey(DpisConfigStore.KEY_TARGET_PACKAGES));
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
    public void replaceBackupIgnoresIncomingExcludedStateButPreservesLocalExcludedState() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putString("font.library.entries", "[{\"id\":\"local_font\"}]")
                .putString("font.library.migration_state", "local_done")
                .putBoolean("font.debug.overlay_enabled", true)
                .putString("runtime.log.ring", "local debug log")
                .putString("runtime.log.token", "local token")
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put(DpisConfigStore.KEY_TARGET_PACKAGES,
                new LinkedHashSet<>(Set.of("com.max.xiaoheihe")));
        values.put("font.com.max.xiaoheihe.typeface_id", "font_abcd1234");
        values.put("font.library.entries", "[{\"id\":\"incoming_font\"}]");
        values.put("font.library.migration_state", "incoming_done");
        values.put("font.debug.overlay_enabled", false);
        values.put("runtime.log.ring", "incoming debug log");
        values.put("runtime.log.token", "incoming token");

        assertTrue(store.replaceBackup(values));

        assertEquals("font_abcd1234", store.getTargetTypefaceId("com.max.xiaoheihe"));
        assertEquals("[{\"id\":\"local_font\"}]", prefs.getString("font.library.entries", null));
        assertEquals("local_done", prefs.getString("font.library.migration_state", null));
        assertTrue(prefs.getBoolean("font.debug.overlay_enabled", false));
        assertEquals("local debug log", prefs.getString("runtime.log.ring", null));
        assertEquals("local token", prefs.getString("runtime.log.token", null));
    }

    @Test
    public void replaceBackupRemovesStaleBackupManagedKeysWhilePreservingExcludedKeys() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES,
                        new LinkedHashSet<>(Set.of("com.old.app")))
                .putInt("viewport.com.old.app.width_dp", 360)
                .putString("font.com.old.app.typeface_id", "font_old")
                .putString("default_config.font.typeface_id", "font_old_default")
                .putStringSet(QuickTemplateStore.KEY_TEMPLATE_IDS,
                        new LinkedHashSet<>(Set.of("old_template")))
                .putString("template.old_template.name", "Old")
                .putString("template.old_template.config.font.typeface_id", "font_old_template")
                .putString("font.library.entries", "[{\"id\":\"font_local\"}]")
                .putString("font.library.migration_state", "local_done")
                .putBoolean("font.debug.overlay_enabled", true)
                .putString("runtime.log.ring", "local debug log")
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put(DpisConfigStore.KEY_TARGET_PACKAGES,
                new LinkedHashSet<>(Set.of("com.new.app")));
        values.put("font.com.new.app.typeface_id", "font_new");
        values.put("default_config.font.typeface_id", "font_new_default");
        values.put("font.library.entries", "[{\"id\":\"font_incoming\"}]");

        assertTrue(store.replaceBackup(values));

        assertFalse(store.getConfiguredPackages().contains("com.old.app"));
        assertTrue(store.getConfiguredPackages().contains("com.new.app"));
        assertNull(store.getTargetViewportWidthDp("com.old.app"));
        assertNull(store.getTargetTypefaceId("com.old.app"));
        assertEquals("font_new", store.getTargetTypefaceId("com.new.app"));
        assertEquals("font_new_default", new GlobalPrefillStore(prefs).read().typefaceId);
        assertFalse(prefs.contains(QuickTemplateStore.KEY_TEMPLATE_IDS));
        assertFalse(prefs.contains("template.old_template.name"));
        assertFalse(prefs.contains("template.old_template.config.font.typeface_id"));
        assertEquals("[{\"id\":\"font_local\"}]", prefs.getString("font.library.entries", null));
        assertEquals("local_done", prefs.getString("font.library.migration_state", null));
        assertTrue(prefs.getBoolean("font.debug.overlay_enabled", false));
        assertEquals("local debug log", prefs.getString("runtime.log.ring", null));
    }

    @Test
    public void replaceBackupMigratesLegacyPackageConfigAndDeletesLegacyKeys() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put(DpisConfigStore.KEY_TARGET_PACKAGES,
                new LinkedHashSet<>(Set.of("com.tencent.mm")));
        values.put("viewport.com.tencent.mm.target_type", ViewportTargetType.RELATIVE_SCALE);
        values.put("viewport.com.tencent.mm.scale_permille", 1250);
        values.put("font.com.tencent.mm.hook_domains", " resources_font,textview_sp ");
        values.put("target.com.tencent.mm.dpis_enabled", false);
        values.put("wechat.com.tencent.mm.dpi", 600);

        assertTrue(store.replaceBackup(values));

        assertEquals(ViewportTargetSpec.relativeScale(125000),
                store.readPackageConfig("com.tencent.mm").viewportTargetSpec);
        assertEquals("resources_font,textview_sp",
                store.readPackageConfig("com.tencent.mm").fontHookDomainsRaw);
        assertFalse(store.isTargetDpisEnabled("com.tencent.mm"));
        assertEquals(Integer.valueOf(600), store.getWechatDpi("com.tencent.mm"));
        assertEquals(ViewportTargetType.RELATIVE_SCALE,
                prefs.getString("package_config.com.tencent.mm.viewport.target_type", null));
        assertEquals("resources_font,textview_sp",
                prefs.getString("package_config.com.tencent.mm.font.hook_domains", null));
        assertFalse(prefs.contains("viewport.com.tencent.mm.target_type"));
        assertFalse(prefs.contains("viewport.com.tencent.mm.scale_permille"));
        assertFalse(prefs.contains("font.com.tencent.mm.hook_domains"));
        assertFalse(prefs.contains("target.com.tencent.mm.dpis_enabled"));
        assertFalse(prefs.contains("wechat.com.tencent.mm.dpi"));
    }

    @Test
    public void replaceBackupKeepsAggregatedPackageConfigWhenLegacyBackupConflicts() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put("viewport.com.example.app.target_type", ViewportTargetType.ABSOLUTE_DP);
        values.put("viewport.com.example.app.width_dp", 411);
        values.put("package_config.com.example.app.viewport.target_type",
                ViewportTargetType.RELATIVE_SCALE);
        values.put("package_config.com.example.app.viewport.scale_permille", 900);

        assertTrue(store.replaceBackup(values));

        assertEquals(ViewportTargetSpec.relativeScale(90000),
                store.readPackageConfig("com.example.app").viewportTargetSpec);
        assertFalse(prefs.contains("viewport.com.example.app.target_type"));
        assertFalse(prefs.contains("viewport.com.example.app.width_dp"));
        assertEquals(ViewportTargetType.RELATIVE_SCALE,
                prefs.getString("package_config.com.example.app.viewport.target_type", null));
        assertEquals(Integer.valueOf(900),
                Integer.valueOf(prefs.getInt(
                        "package_config.com.example.app.viewport.scale_permille", 0)));
    }

    @Test
    public void replaceBackupRestoresPrefillAndTemplateKeysWithMissingTypefaceIds() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put(DpisConfigStore.KEY_TARGET_PACKAGES,
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
        assertEquals(ViewportTargetSpec.absoluteDp(411), TemplateConfigValueAdapters.toViewportTargetSpec(prefill));
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
        assertEquals(ViewportTargetSpec.relativeScale(110000),
                TemplateConfigValueAdapters.toViewportTargetSpec(template.configValue));
        assertEquals(ViewportApplyMode.COMPAT, template.configValue.viewportApplyMode);
        assertEquals(Integer.valueOf(115), template.configValue.fontScalePercent);
        assertEquals(FontApplyMode.SYSTEM_EMULATION, template.configValue.fontApplyMode);
        assertEquals("missing_template_font_id", template.configValue.typefaceId);
        assertEquals("textview_sp", template.configValue.fontHookDomainsRaw);
        assertFalse(prefs.contains("font.library.entries"));
        assertFalse(prefs.contains("font.library.migration_state"));
    }

    @Test
    public void replaceAllOverwritesCurrentStoreValues() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putBoolean(DpisConfigStore.KEY_GLOBAL_LOG_ENABLED, false)
                .putInt("font.com.old.scale_percent", 120)
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put(DpisConfigStore.KEY_GLOBAL_LOG_ENABLED, true);
        values.put("viewport.com.max.xiaoheihe.width_dp", 360);
        values.put("font.com.max.xiaoheihe.scale_percent", 120);
        values.put(DpisConfigStore.KEY_TARGET_PACKAGES,
                new LinkedHashSet<>(Set.of("com.max.xiaoheihe")));

        assertTrue(store.replaceAll(values));

        assertTrue(store.isGlobalLogEnabled());
        assertNull(store.getTargetFontScalePercent("com.old"));
        assertEquals(Integer.valueOf(360),
                store.getTargetViewportWidthDp("com.max.xiaoheihe"));
        assertEquals(Integer.valueOf(120),
                store.getTargetFontScalePercent("com.max.xiaoheihe"));
        assertTrue(store.getConfiguredPackages().contains("com.max.xiaoheihe"));
    }

    @Test
    public void viewportScaleDraftPersistsWithoutChangingAbsoluteActiveType() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        assertTrue(store.setTargetViewportSpec(
                "com.example.app", ViewportTargetSpec.absoluteDp(480)));

        assertTrue(store.setTargetViewportScaleMilliPercentDraft("com.example.app", 125000));

        assertTrue(store.getTargetViewportSpec("com.example.app").isAbsoluteDp());
        assertEquals(Integer.valueOf(480), store.getTargetViewportWidthDp("com.example.app"));
        assertEquals(Integer.valueOf(125000),
                store.getTargetViewportScaleMilliPercent("com.example.app"));
    }

    @Test
    public void clearingViewportScaleDraftRemovesConfiguredPackageWhenLegacyMirrorWasOnlyValue() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.setTargetViewportScaleMilliPercentDraft("com.example.app", 125000));
        assertTrue(store.getConfiguredPackages().contains("com.example.app"));

        assertTrue(store.setTargetViewportScaleMilliPercentDraft("com.example.app", null));

        assertFalse(store.getConfiguredPackages().contains("com.example.app"));
        assertFalse(prefs.contains("viewport.com.example.app.scale_milli_percent"));
        assertFalse(prefs.contains("package_config.com.example.app.viewport.scale_milli_percent"));
        assertFalse(prefs.contains("viewport.com.example.app.scale_permille"));
        assertFalse(prefs.contains("package_config.com.example.app.viewport.scale_permille"));
    }

    @Test
    public void viewportWidthDraftPersistsWithoutChangingRelativeActiveType() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        assertTrue(store.setTargetViewportSpec(
                "com.example.app", ViewportTargetSpec.relativeScale(125000)));

        assertTrue(store.setTargetViewportWidthDraft("com.example.app", 480));

        assertTrue(store.getTargetViewportSpec("com.example.app").isRelativeScale());
        assertEquals(Integer.valueOf(125000),
                store.getTargetViewportScaleMilliPercent("com.example.app"));
        assertEquals(Integer.valueOf(480), store.getTargetViewportWidthDp("com.example.app"));
    }

    @Test
    public void wechatDpiAddsAndClearsConfiguredPackage() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.setWechatDpi("com.tencent.mm", 600));
        assertEquals(Integer.valueOf(600), store.getWechatDpi("com.tencent.mm"));
        assertTrue(store.hasTargetAppSpecificConfig("com.tencent.mm"));
        assertTrue(store.getConfiguredPackages().contains("com.tencent.mm"));

        assertTrue(store.clearWechatDpi("com.tencent.mm"));
        assertNull(store.getWechatDpi("com.tencent.mm"));
        assertFalse(store.hasTargetAppSpecificConfig("com.tencent.mm"));
        assertFalse(store.getConfiguredPackages().contains("com.tencent.mm"));
    }

    @Test
    public void wechatDpiIgnoresUnsupportedPackageAndOutOfRangeValues() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());

        assertTrue(store.setWechatDpi("com.example.app", 600));
        assertNull(store.getWechatDpi("com.example.app"));
        assertFalse(store.getConfiguredPackages().contains("com.example.app"));

        assertTrue(store.setWechatDpi("com.tencent.mm", 199));
        assertNull(store.getWechatDpi("com.tencent.mm"));
        assertFalse(store.getConfiguredPackages().contains("com.tencent.mm"));

        assertTrue(store.setWechatDpi("com.tencent.mm", 200));
        assertEquals(Integer.valueOf(200), store.getWechatDpi("com.tencent.mm"));
        assertTrue(store.getConfiguredPackages().contains("com.tencent.mm"));

        assertTrue(store.setWechatDpi("com.tencent.mm", 1000));
        assertEquals(Integer.valueOf(1000), store.getWechatDpi("com.tencent.mm"));
        assertTrue(store.getConfiguredPackages().contains("com.tencent.mm"));

        assertTrue(store.setWechatDpi("com.tencent.mm", 1001));
        assertNull(store.getWechatDpi("com.tencent.mm"));
        assertFalse(store.getConfiguredPackages().contains("com.tencent.mm"));
    }

    @Test
    public void savedWechatDpiKeyOnlyConfiguresSupportedPackage() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putInt("wechat.com.example.app.dpi", 600)
                .putInt("wechat.com.tencent.mm.dpi", 600)
                .commit();

        DpisConfigStore store = new DpisConfigStore(prefs);

        assertFalse(store.getConfiguredPackages().contains("com.example.app"));
        assertFalse(store.hasUserVisiblePackageConfig("com.example.app"));
        assertTrue(store.getConfiguredPackages().contains("com.tencent.mm"));
        assertTrue(store.hasUserVisiblePackageConfig("com.tencent.mm"));
    }

    @Test
    public void hasRealPackageConfigTreatsMissingTypefaceIdAsConfig() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());

        assertTrue(store.setTargetTypefaceId("com.example.app", "missing_font_id"));

        assertTrue(store.hasRealPackageConfig("com.example.app"));
        assertEquals("missing_font_id", store.getTargetTypefaceId("com.example.app"));
    }

    @Test
    public void packageTemplateConfigValueRoundTripsCopyableFieldsOnly() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        TemplateConfigValue value = TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.relativeScale(110000),
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
    public void packageTemplateConfigDoesNotCopyNonTemplateFields() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        assertTrue(store.writePackageConfig("com.tencent.mm", new PackageConfigValue(
                ViewportTargetSpec.relativeScale(120000),
                ViewportTargetType.RELATIVE_SCALE,
                ViewportApplyMode.SYSTEM,
                130,
                FontApplyMode.FIELD_REWRITE,
                "test_font",
                "resources_font",
                false,
                600)));

        TemplateConfigValue template = store.readPackageTemplateConfigValue("com.tencent.mm");
        assertTrue(store.writePackageTemplateConfigValue("com.example.target", template));

        assertEquals(Integer.valueOf(130), store.getTargetFontScalePercent("com.example.target"));
        assertEquals("test_font", store.getTargetTypefaceId("com.example.target"));
        assertTrue(store.isTargetDpisEnabled("com.example.target"));
        assertNull(store.getWechatDpi("com.example.target"));
        assertFalse(prefs.contains("target.com.example.target.dpis_enabled"));
        assertFalse(prefs.contains("package_config.com.example.target.target.dpis_enabled"));
        assertFalse(prefs.contains("wechat.com.example.target.dpi"));
        assertFalse(prefs.contains("package_config.com.example.target.app.wechat_dpi"));
    }

    @Test
    public void packageTemplateWriteDoesNotOverwriteExistingNonTemplateFields() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        assertTrue(store.writePackageConfig("com.tencent.mm", new PackageConfigValue(
                ViewportTargetSpec.absoluteDp(411),
                ViewportTargetType.ABSOLUTE_DP,
                ViewportApplyMode.SYSTEM,
                null,
                FontApplyMode.OFF,
                null,
                null,
                false,
                600)));

        assertTrue(store.writePackageTemplateConfigValue("com.tencent.mm", TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.relativeScale(115000),
                ViewportApplyMode.AUTO,
                125,
                FontApplyMode.FIELD_REWRITE,
                "new_font",
                "textview_sp")));

        assertFalse(store.isTargetDpisEnabled("com.tencent.mm"));
        assertEquals(Integer.valueOf(600), store.getWechatDpi("com.tencent.mm"));
        assertEquals(Integer.valueOf(125), store.getTargetFontScalePercent("com.tencent.mm"));
        assertEquals("new_font", store.getTargetTypefaceId("com.tencent.mm"));
    }

    @Test
    public void readPackageConfigAggregatesCurrentScatteredKeys() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES,
                        new LinkedHashSet<>(Set.of("com.tencent.mm")))
                .putString("viewport.com.tencent.mm.target_type",
                        ViewportTargetType.RELATIVE_SCALE)
                .putInt("viewport.com.tencent.mm.scale_permille", 1250)
                .putString("viewport.com.tencent.mm.mode",
                        ViewportApplyMode.SYSTEM)
                .putInt("font.com.tencent.mm.scale_percent", 140)
                .putString("font.com.tencent.mm.mode",
                        FontApplyMode.FIELD_REWRITE)
                .putString("font.com.tencent.mm.typeface_id", "test_font")
                .putString("font.com.tencent.mm.hook_domains", "resources_font,textview_sp")
                .putBoolean("target.com.tencent.mm.dpis_enabled", false)
                .putInt("wechat.com.tencent.mm.dpi", 600)
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        PackageConfigValue value = store.readPackageConfig("com.tencent.mm");

        assertEquals(new PackageConfigValue(
                ViewportTargetSpec.relativeScale(125000),
                ViewportTargetType.RELATIVE_SCALE,
                ViewportApplyMode.SYSTEM,
                140,
                FontApplyMode.FIELD_REWRITE,
                "test_font",
                "resources_font,textview_sp",
                false,
                600), value);
    }

    @Test
    public void migrateLegacyPackageConfigToAggregatedCopiesLegacyOnlyAndDeletesLegacyKeys() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES,
                        new LinkedHashSet<>(Set.of("com.tencent.mm")))
                .putString("viewport.com.tencent.mm.target_type",
                        ViewportTargetType.RELATIVE_SCALE)
                .putInt("viewport.com.tencent.mm.scale_permille", 1250)
                .putString("viewport.com.tencent.mm.mode",
                        ViewportApplyMode.SYSTEM)
                .putInt("font.com.tencent.mm.scale_percent", 140)
                .putString("font.com.tencent.mm.mode",
                        FontApplyMode.FIELD_REWRITE)
                .putString("font.com.tencent.mm.typeface_id", "test_font")
                .putString("font.com.tencent.mm.hook_domains", "resources_font")
                .putBoolean("target.com.tencent.mm.dpis_enabled", false)
                .putInt("wechat.com.tencent.mm.dpi", 600)
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.migrateLegacyPackageConfigToAggregated());

        assertEquals(new PackageConfigValue(
                ViewportTargetSpec.relativeScale(125000),
                ViewportTargetType.RELATIVE_SCALE,
                ViewportApplyMode.SYSTEM,
                140,
                FontApplyMode.FIELD_REWRITE,
                "test_font",
                "resources_font",
                false,
                600), store.readPackageConfig("com.tencent.mm"));
        assertEquals(ViewportTargetType.RELATIVE_SCALE,
                prefs.getString("package_config.com.tencent.mm.viewport.target_type", null));
        assertEquals(Integer.valueOf(1250),
                Integer.valueOf(prefs.getInt(
                        "package_config.com.tencent.mm.viewport.scale_permille", 0)));
        assertEquals(Integer.valueOf(140),
                Integer.valueOf(prefs.getInt(
                        "package_config.com.tencent.mm.font.scale_percent", 0)));
        assertFalse(prefs.getBoolean(
                "package_config.com.tencent.mm.target.dpis_enabled", true));
        assertEquals(Integer.valueOf(600),
                Integer.valueOf(prefs.getInt(
                        "package_config.com.tencent.mm.app.wechat_dpi", 0)));
        assertFalse(prefs.contains("viewport.com.tencent.mm.target_type"));
        assertFalse(prefs.contains("viewport.com.tencent.mm.scale_permille"));
        assertFalse(prefs.contains("font.com.tencent.mm.scale_percent"));
        assertFalse(prefs.contains("font.com.tencent.mm.typeface_id"));
        assertFalse(prefs.contains("target.com.tencent.mm.dpis_enabled"));
        assertFalse(prefs.contains("wechat.com.tencent.mm.dpi"));
    }

    @Test
    public void migrateLegacyPackageConfigToAggregatedKeepsAggregatedValueOnConflict() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putString("viewport.com.example.app.target_type",
                        ViewportTargetType.ABSOLUTE_DP)
                .putInt("viewport.com.example.app.width_dp", 411)
                .putInt("font.com.example.app.scale_percent", 130)
                .putString("package_config.com.example.app.viewport.target_type",
                        ViewportTargetType.RELATIVE_SCALE)
                .putInt("package_config.com.example.app.viewport.scale_permille", 900)
                .putInt("package_config.com.example.app.font.scale_percent", 150)
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.migrateLegacyPackageConfigToAggregated());

        assertEquals(ViewportTargetSpec.relativeScale(90000),
                store.readPackageConfig("com.example.app").viewportTargetSpec);
        assertEquals(Integer.valueOf(150),
                store.readPackageConfig("com.example.app").fontScalePercent);
        assertFalse(prefs.contains("viewport.com.example.app.target_type"));
        assertFalse(prefs.contains("viewport.com.example.app.width_dp"));
        assertFalse(prefs.contains("font.com.example.app.scale_percent"));
    }

    @Test
    public void migrateLegacyPackageConfigToAggregatedIgnoresInvalidWechatAndTargetPackagesOnly() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES,
                        new LinkedHashSet<>(Set.of("com.example.target_only")))
                .putInt("wechat.com.example.app.dpi", 600)
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.migrateLegacyPackageConfigToAggregated());

        assertFalse(prefs.contains("package_config.com.example.app.app.wechat_dpi"));
        assertFalse(store.hasUserVisiblePackageConfig("com.example.app"));
        assertFalse(store.hasUserVisiblePackageConfig("com.example.target_only"));
        assertEquals(PackageConfigValue.EMPTY, store.readPackageConfig("com.example.target_only"));
        assertFalse(prefs.contains("wechat.com.example.app.dpi"));
    }

    @Test
    public void readPackageConfigAggregatesNewPackageConfigKeys() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putString("package_config.com.tencent.mm.viewport.target_type",
                        ViewportTargetType.RELATIVE_SCALE)
                .putInt("package_config.com.tencent.mm.viewport.scale_permille", 1250)
                .putString("package_config.com.tencent.mm.viewport.mode",
                        ViewportApplyMode.SYSTEM)
                .putInt("package_config.com.tencent.mm.font.scale_percent", 140)
                .putString("package_config.com.tencent.mm.font.mode",
                        FontApplyMode.FIELD_REWRITE)
                .putString("package_config.com.tencent.mm.font.typeface_id", "test_font")
                .putString("package_config.com.tencent.mm.font.hook_domains",
                        "resources_font,textview_sp")
                .putBoolean("package_config.com.tencent.mm.target.dpis_enabled", false)
                .putInt("package_config.com.tencent.mm.app.wechat_dpi", 600)
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        PackageConfigValue value = store.readPackageConfig("com.tencent.mm");

        assertEquals(new PackageConfigValue(
                ViewportTargetSpec.relativeScale(125000),
                ViewportTargetType.RELATIVE_SCALE,
                ViewportApplyMode.SYSTEM,
                140,
                FontApplyMode.FIELD_REWRITE,
                "test_font",
                "resources_font,textview_sp",
                false,
                600), value);
        assertEquals(Integer.valueOf(140), store.getTargetFontScalePercent("com.tencent.mm"));
        assertFalse(store.isTargetDpisEnabled("com.tencent.mm"));
        assertTrue(store.getConfiguredPackages().contains("com.tencent.mm"));
        assertTrue(store.hasUserVisiblePackageConfig("com.tencent.mm"));
    }

    @Test
    public void legacyGettersDoNotResurrectClearedValuesFromAggregatedResidue() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        assertTrue(store.writePackageConfig("com.tencent.mm", new PackageConfigValue(
                ViewportTargetSpec.absoluteDp(411),
                ViewportTargetType.ABSOLUTE_DP,
                ViewportApplyMode.SYSTEM,
                135,
                FontApplyMode.FIELD_REWRITE,
                "test_font",
                "resources_font",
                false,
                700)));

        assertTrue(store.clearTargetViewportWidthDp("com.tencent.mm"));
        assertTrue(store.clearTargetFontScalePercent("com.tencent.mm"));
        assertTrue(store.clearTargetTypefaceId("com.tencent.mm"));
        assertTrue(store.clearPackageFontHookDomainsRaw("com.tencent.mm"));
        assertTrue(store.clearWechatDpi("com.tencent.mm"));
        assertTrue(store.setTargetDpisEnabled("com.tencent.mm", true));

        assertNull(store.getTargetViewportWidthDp("com.tencent.mm"));
        assertNull(store.getTargetFontScalePercent("com.tencent.mm"));
        assertNull(store.getTargetTypefaceId("com.tencent.mm"));
        assertNull(store.getPackageFontHookDomainsRaw("com.tencent.mm"));
        assertNull(store.getWechatDpi("com.tencent.mm"));
        assertTrue(store.isTargetDpisEnabled("com.tencent.mm"));
        assertTrue(store.readPackageConfig("com.tencent.mm").hasAnyValue());
        assertTrue(store.hasUserVisiblePackageConfig("com.tencent.mm"));
    }

    @Test
    public void writePackageConfigPersistsSparseLegacyAndAggregatedKeys() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.writePackageConfig("com.tencent.mm", new PackageConfigValue(
                ViewportTargetSpec.absoluteDp(411),
                ViewportTargetType.RELATIVE_SCALE,
                ViewportApplyMode.SYSTEM,
                135,
                FontApplyMode.FIELD_REWRITE,
                "test_font",
                "resources_font",
                false,
                700)));

        assertEquals(Integer.valueOf(411), store.getTargetViewportWidthDp("com.tencent.mm"));
        assertEquals(ViewportTargetType.ABSOLUTE_DP, store.getTargetViewportType("com.tencent.mm"));
        assertEquals(ViewportApplyMode.SYSTEM,
                prefs.getString("viewport.com.tencent.mm.mode", null));
        assertEquals(Integer.valueOf(135), store.getTargetFontScalePercent("com.tencent.mm"));
        assertEquals(FontApplyMode.FIELD_REWRITE,
                prefs.getString("font.com.tencent.mm.mode", null));
        assertEquals("test_font", store.getTargetTypefaceId("com.tencent.mm"));
        assertEquals("resources_font", store.getPackageFontHookDomainsRaw("com.tencent.mm"));
        assertFalse(store.isTargetDpisEnabled("com.tencent.mm"));
        assertEquals(Integer.valueOf(700), store.getWechatDpi("com.tencent.mm"));
        assertTrue(store.getConfiguredPackages().contains("com.tencent.mm"));
        assertEquals(Integer.valueOf(411),
                Integer.valueOf(prefs.getInt(
                        "package_config.com.tencent.mm.viewport.width_dp", 0)));
        assertEquals(Integer.valueOf(135),
                Integer.valueOf(prefs.getInt(
                        "package_config.com.tencent.mm.font.scale_percent", 0)));
        assertEquals("test_font",
                prefs.getString("package_config.com.tencent.mm.font.typeface_id", null));
        assertFalse(prefs.getBoolean(
                "package_config.com.tencent.mm.target.dpis_enabled", true));
        assertEquals(Integer.valueOf(700),
                Integer.valueOf(prefs.getInt(
                        "package_config.com.tencent.mm.app.wechat_dpi", 0)));
    }

    @Test
    public void writePackageConfigReplacesOldDataWithNewAndLegacyMirrors() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putString("viewport.com.tencent.mm.target_type",
                        ViewportTargetType.ABSOLUTE_DP)
                .putInt("viewport.com.tencent.mm.width_dp", 400)
                .putString("package_config.com.tencent.mm.viewport.target_type",
                        ViewportTargetType.RELATIVE_SCALE)
                .putInt("package_config.com.tencent.mm.viewport.scale_permille", 1200)
                .putString("package_config.com.tencent.mm.font.typeface_id", "old_font")
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.writePackageConfig("com.tencent.mm", new PackageConfigValue(
                ViewportTargetSpec.relativeScale(130000),
                ViewportTargetType.RELATIVE_SCALE,
                ViewportApplyMode.SYSTEM,
                145,
                FontApplyMode.FIELD_REWRITE,
                "new_font",
                "textview_sp",
                false,
                650)));

        assertFalse(prefs.contains("viewport.com.tencent.mm.width_dp"));
        assertEquals(ViewportTargetType.RELATIVE_SCALE,
                prefs.getString("viewport.com.tencent.mm.target_type", null));
        assertEquals(Integer.valueOf(1300),
                Integer.valueOf(prefs.getInt("viewport.com.tencent.mm.scale_permille", 0)));
        assertEquals(Integer.valueOf(1300),
                Integer.valueOf(prefs.getInt(
                        "package_config.com.tencent.mm.viewport.scale_permille", 0)));
        assertFalse(prefs.contains("package_config.com.tencent.mm.viewport.width_dp"));
        assertEquals("new_font", store.getTargetTypefaceId("com.tencent.mm"));
        assertFalse(store.isTargetDpisEnabled("com.tencent.mm"));
        assertEquals(Integer.valueOf(650), store.getWechatDpi("com.tencent.mm"));
    }

    @Test
    public void configuredPackagesAndVisibilityIncludeMixedLegacyAndAggregatedState() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putString("font.com.example.legacy.mode", FontApplyMode.FIELD_REWRITE)
                .putBoolean("package_config.com.example.disabled.target.dpis_enabled", false)
                .putString("package_config.com.example.domains.font.hook_domains",
                        "resources_font")
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.getConfiguredPackages().contains("com.example.legacy"));
        assertTrue(store.getConfiguredPackages().contains("com.example.disabled"));
        assertTrue(store.getConfiguredPackages().contains("com.example.domains"));
        assertTrue(store.hasUserVisiblePackageConfig("com.example.legacy"));
        assertTrue(store.hasUserVisiblePackageConfig("com.example.disabled"));
        assertTrue(store.hasUserVisiblePackageConfig("com.example.domains"));
    }

    @Test
    public void writeEmptyPackageConfigClearsKnownLegacyAndAggregatedKeysAndPrunesPackage() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES,
                        new LinkedHashSet<>(Set.of("com.tencent.mm")))
                .putString("viewport.com.tencent.mm.target_type",
                        ViewportTargetType.ABSOLUTE_DP)
                .putInt("viewport.com.tencent.mm.width_dp", 400)
                .putString("font.com.tencent.mm.typeface_id", "test_font")
                .putBoolean("target.com.tencent.mm.dpis_enabled", false)
                .putInt("wechat.com.tencent.mm.dpi", 600)
                .putString("package_config.com.tencent.mm.viewport.target_type",
                        ViewportTargetType.ABSOLUTE_DP)
                .putInt("package_config.com.tencent.mm.viewport.width_dp", 400)
                .putString("package_config.com.tencent.mm.font.typeface_id", "test_font")
                .putBoolean("package_config.com.tencent.mm.target.dpis_enabled", false)
                .putInt("package_config.com.tencent.mm.app.wechat_dpi", 600)
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.writePackageConfig("com.tencent.mm", PackageConfigValue.EMPTY));

        assertFalse(store.getConfiguredPackages().contains("com.tencent.mm"));
        assertFalse(prefs.contains("viewport.com.tencent.mm.target_type"));
        assertFalse(prefs.contains("viewport.com.tencent.mm.width_dp"));
        assertFalse(prefs.contains("font.com.tencent.mm.typeface_id"));
        assertFalse(prefs.contains("target.com.tencent.mm.dpis_enabled"));
        assertFalse(prefs.contains("wechat.com.tencent.mm.dpi"));
        assertFalse(prefs.contains("package_config.com.tencent.mm.viewport.target_type"));
        assertFalse(prefs.contains("package_config.com.tencent.mm.viewport.width_dp"));
        assertFalse(prefs.contains("package_config.com.tencent.mm.font.typeface_id"));
        assertFalse(prefs.contains("package_config.com.tencent.mm.target.dpis_enabled"));
        assertFalse(prefs.contains("package_config.com.tencent.mm.app.wechat_dpi"));
    }

    @Test
    public void emptyPackageTemplateConfigValuePreservesDisabledStateAndMembership() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
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
        assertTrue(prefs.contains("package_config.com.example.app.target.dpis_enabled"));
    }

    @Test
    public void emptyPackageTemplateConfigValuePreservesWechatDpiAndMembership() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        assertTrue(store.setWechatDpi("com.tencent.mm", 600));
        assertTrue(store.setTargetViewportWidthDp("com.tencent.mm", 411));
        assertTrue(store.setTargetTypefaceId("com.tencent.mm", "missing_font_id"));

        assertTrue(store.writePackageTemplateConfigValue(
                "com.tencent.mm", TemplateConfigValue.EMPTY));

        assertEquals(Integer.valueOf(600), store.getWechatDpi("com.tencent.mm"));
        assertTrue(store.getConfiguredPackages().contains("com.tencent.mm"));
        assertNull(store.getTargetViewportWidthDp("com.tencent.mm"));
        assertNull(store.getTargetTypefaceId("com.tencent.mm"));
        assertTrue(prefs.contains("wechat.com.tencent.mm.dpi"));
        assertTrue(prefs.contains("package_config.com.tencent.mm.app.wechat_dpi"));
    }

    @Test
    public void mirroredLegacySharedPrefsXmlContainsCommittedPackageConfig()
            throws Exception {
        File mirror = Files.createTempFile("dpis-mirror", ".xml").toFile();
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs, mirror);

        assertTrue(store.setTargetViewportSpec(
                "com.azure.authenticator",
                ViewportTargetSpec.relativeScale(150000)));
        assertTrue(store.setTargetFontScalePercent("com.azure.authenticator", 150));

        String xml = new String(Files.readAllBytes(mirror.toPath()), StandardCharsets.UTF_8);
        assertTrue(xml.contains("package_config.com.azure.authenticator.viewport.target_type"));
        assertTrue(xml.contains("package_config.com.azure.authenticator.viewport.scale_permille"));
        assertTrue(xml.contains("package_config.com.azure.authenticator.font.scale_percent"));
        assertTrue(xml.contains("<string>com.azure.authenticator</string>"));
    }

    @Test
    public void mirroredLegacySharedPrefsXmlEscapesStringsAndSets()
            throws Exception {
        File mirror = Files.createTempFile("dpis-mirror", ".xml").toFile();
        LinkedHashMap<String, Object> entries = new LinkedHashMap<>();
        entries.put("name\"key", "value<&>");
        entries.put("packages", new LinkedHashSet<>(Arrays.asList("a&b", "c<d")));

        DpisConfigStore.writeSharedPreferencesXmlForTest(entries, mirror);

        String xml = new String(Files.readAllBytes(mirror.toPath()), StandardCharsets.UTF_8);
        assertTrue(xml.contains("name=\"name&quot;key\""));
        assertTrue(xml.contains(">value&lt;&amp;&gt;</string>"));
        assertTrue(xml.contains("<string>a&amp;b</string>"));
        assertTrue(xml.contains("<string>c&lt;d</string>"));
    }

    @Test
    public void sharedPreferencesXmlRoundTripsForLegacyImport()
            throws Exception {
        File mirror = Files.createTempFile("dpis-mirror", ".xml").toFile();
        LinkedHashMap<String, Object> entries = new LinkedHashMap<>();
        entries.put(DpisConfigStore.KEY_TARGET_PACKAGES,
                new LinkedHashSet<>(Arrays.asList("com.example.one", "com.example.two")));
        entries.put("package_config.com.example.one.viewport.target_type", "relative_scale");
        entries.put("package_config.com.example.one.viewport.scale_permille", 1500);
        entries.put("package_config.com.example.two.font.scale_percent", 120);
        entries.put("package_config.com.example.two.target.dpis_enabled", false);

        DpisConfigStore.writeSharedPreferencesXmlForTest(entries, mirror);
        Map<String, Object> imported = DpisConfigStore.readSharedPreferencesXmlForTest(mirror);

        assertEquals(entries.get(DpisConfigStore.KEY_TARGET_PACKAGES),
                imported.get(DpisConfigStore.KEY_TARGET_PACKAGES));
        assertEquals("relative_scale",
                imported.get("package_config.com.example.one.viewport.target_type"));
        assertEquals(1500,
                imported.get("package_config.com.example.one.viewport.scale_permille"));
        assertEquals(120,
                imported.get("package_config.com.example.two.font.scale_percent"));
        assertEquals(false,
                imported.get("package_config.com.example.two.target.dpis_enabled"));
    }

    @Test
    public void importSharedPreferencesXmlReplacesPrimaryStore()
            throws Exception {
        File mirror = Files.createTempFile("dpis-mirror", ".xml").toFile();
        LinkedHashMap<String, Object> entries = new LinkedHashMap<>();
        entries.put(DpisConfigStore.KEY_TARGET_PACKAGES,
                new LinkedHashSet<>(Arrays.asList("com.example.one", "com.example.two")));
        entries.put("package_config.com.example.one.viewport.target_type", "relative_scale");
        entries.put("package_config.com.example.one.viewport.scale_permille", 1500);
        DpisConfigStore.writeSharedPreferencesXmlForTest(entries, mirror);
        DpisConfigStore store = new DpisConfigStore(new FakePrefs(), mirror);

        assertTrue(store.importSharedPreferencesXml(mirror));

        assertTrue(store.getConfiguredPackages().contains("com.example.one"));
        assertEquals(ViewportTargetSpec.relativeScale(150000),
                store.getTargetViewportSpec("com.example.one"));
    }

    @Test
    public void hasAnyUserVisiblePackageConfigIgnoresNonPackageResidualKeys() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putBoolean("global_log_enabled", true)
                .putString("runtime.last_route", "modern")
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertFalse(store.hasAnyUserVisiblePackageConfig());
    }

    @Test
    public void hasAnyUserVisiblePackageConfigDetectsRealPackageConfig() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES,
                        new LinkedHashSet<>(Arrays.asList("com.example.app")))
                .putString("package_config.com.example.app.viewport.target_type", "relative_scale")
                .putInt("package_config.com.example.app.viewport.scale_permille", 1500)
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        assertTrue(store.hasAnyUserVisiblePackageConfig());
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

