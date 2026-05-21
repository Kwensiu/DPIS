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
        PerAppDisplayConfigSource source = new PerAppDisplayConfigSource(
                () -> ConfigSnapshotLoader.fromStore(store));
        assertNotNull(source.get("com.example.target"));

        assertTrue(store.clearTargetPackageConfig("com.example.target"));

        assertNull(source.get("com.example.target"));
    }

    @Test
    public void usesPackageFallbackWhenSnapshotHasNoPackage() {
        PerAppDisplayConfigSource source = new PerAppDisplayConfigSource(
                ConfigSnapshot::empty,
                packageName -> new PackageConfigSnapshot(
                        packageName,
                        true,
                        500,
                        ViewportApplyMode.FIELD_REWRITE,
                        200,
                        FontApplyMode.FIELD_REWRITE,
                        null,
                        false,
                        false,
                        false));

        PerAppDisplayConfig config = source.get("com.example.target");

        assertNotNull(config);
        assertEquals(500, config.targetViewportWidthDp);
        assertEquals(Integer.valueOf(200), config.targetFontScalePercent);
        assertEquals(FontApplyMode.FIELD_REWRITE, config.targetFontMode);
    }

    @Test
    public void doesNotUsePackageFallbackWhenSnapshotDisablesPackage() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        assertTrue(store.setTargetFontScalePercent("com.example.target", 200));
        assertTrue(store.setTargetDpisEnabled("com.example.target", false));
        PerAppDisplayConfigSource source = new PerAppDisplayConfigSource(
                () -> ConfigSnapshotLoader.fromStore(store),
                packageName -> new PackageConfigSnapshot(
                        packageName,
                        true,
                        null,
                        ViewportApplyMode.OFF,
                        200,
                        FontApplyMode.FIELD_REWRITE,
                        null,
                        false,
                        false,
                        false));

        assertNull(source.get("com.example.target"));
    }

    @Test
    public void usesPackageFallbackWhenSnapshotHasStaleEnabledPackage() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        assertTrue(store.setTargetFontScalePercent("com.example.target", 100));
        PerAppDisplayConfigSource source = new PerAppDisplayConfigSource(
                () -> ConfigSnapshotLoader.fromStore(store),
                packageName -> new PackageConfigSnapshot(
                        packageName,
                        true,
                        600,
                        ViewportApplyMode.FIELD_REWRITE,
                        200,
                        FontApplyMode.FIELD_REWRITE,
                        null,
                        false,
                        false,
                        false));

        PerAppDisplayConfig config = source.get("com.example.target");

        assertNotNull(config);
        assertEquals(600, config.targetViewportWidthDp);
        assertEquals(Integer.valueOf(200), config.targetFontScalePercent);
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
    public void nativeFlutterFlagReflectsFinalDomainPlan() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        assertTrue(store.setTargetFontScalePercent("com.miui.gallery", 200));
        assertTrue(store.setTargetFontApplyMode("com.miui.gallery", FontApplyMode.FIELD_REWRITE));

        PerAppDisplayConfig automatic = new PerAppDisplayConfigSource(store)
                .get("com.miui.gallery");

        assertNotNull(automatic);
        assertTrue(automatic.hyperOsFlutterFontHookEnabled);

        assertTrue(new HookDomainOverrideStore(store).save(
                "com.miui.gallery",
                java.util.Set.of(FontHookDomainRegistry.ID_RESOURCES_FONT),
                java.util.Set.of()));

        PerAppDisplayConfig customWithoutNative = new PerAppDisplayConfigSource(store)
                .get("com.miui.gallery");

        assertNotNull(customWithoutNative);
        assertFalse(customWithoutNative.hyperOsFlutterFontHookEnabled);
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
