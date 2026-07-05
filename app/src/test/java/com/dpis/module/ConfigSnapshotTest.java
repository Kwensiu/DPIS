package com.dpis.module;

import com.dpis.module.viewport.ViewportApplyMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ConfigSnapshotTest {
    @Test
    public void snapshotKeepsConfiguredDisabledPackageButSourceSkipsIt() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetViewportWidthDp("com.example.app", 360);
        store.setTargetDpisEnabled("com.example.app", false);

        ConfigSnapshot snapshot = ConfigSnapshotLoader.fromStore(store);

        assertTrue(snapshot.isConfigured("com.example.app"));
        assertNotNull(snapshot.getPackage("com.example.app"));
        assertFalse(snapshot.getPackage("com.example.app").dpisEnabled);
        assertNull(new PerAppDisplayConfigSource(snapshot).get("com.example.app"));
        assertFalse(ModulePackagePlan.resolve(snapshot, "com.example.app").shouldInstallHooks());
    }

    @Test
    public void snapshotPreservesLegacyDefaultModes() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES,
                        java.util.Set.of("com.example.app"))
                .putInt("viewport.com.example.app.width_dp", 360)
                .putInt("font.com.example.app.scale_percent", 125)
                .commit();

        ConfigSnapshot snapshot = ConfigSnapshotLoader.fromStore(new DpisConfigStore(prefs));
        PackageConfigSnapshot packageConfig = snapshot.getPackage("com.example.app");

        assertNotNull(packageConfig);
        assertEquals(ViewportApplyMode.SYSTEM_EMULATION, packageConfig.targetViewportMode);
        assertEquals(FontApplyMode.SYSTEM_EMULATION, packageConfig.targetFontMode);
    }

    @Test
    public void snapshotIncludesTypefaceId() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        store.setTargetTypefaceId("com.example.app", "font_abcd1234");

        ConfigSnapshot snapshot = ConfigSnapshotLoader.fromStore(store);
        PackageConfigSnapshot packageConfig = snapshot.getPackage("com.example.app");

        assertEquals("font_abcd1234", packageConfig.targetTypefaceId);
    }

    @Test
    public void configuredPackagesAreImmutable() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetViewportWidthDp("com.example.app", 360);

        ConfigSnapshot snapshot = ConfigSnapshotLoader.fromStore(store);

        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.getConfiguredPackages().add("com.example.other"));
    }

    @Test
    public void globalFlagsAreCapturedOnce() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setSystemServerHooksEnabled(false);
        ConfigSnapshot snapshot = ConfigSnapshotLoader.fromStore(store);

        store.setSystemServerHooksEnabled(true);

        assertFalse(snapshot.isSystemServerHooksEnabled());
        assertTrue(snapshot.hasSystemServerHooksEnabled());
    }

    @Test
    public void packageSnapshotIgnoresLegacyGlobalExperimentalFlags() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetFontScalePercent("com.example.app", 125);
        store.setTargetFontApplyMode("com.example.app", FontApplyMode.FIELD_REWRITE);
        store.setFlutterFontHookEnabled(true);
        store.setFlutterSettingsFontHookEnabled(true);
        store.setHyperOsFlutterFontHookEnabled(true);

        PackageConfigSnapshot packageConfig =
                ConfigSnapshotLoader.fromStore(store).getPackage("com.example.app");

        assertNotNull(packageConfig);
        assertFalse(packageConfig.flutterFontHookEnabled);
        assertFalse(packageConfig.flutterSettingsFontHookEnabled);
        assertFalse(packageConfig.hyperOsFlutterFontHookEnabled);
    }
}
