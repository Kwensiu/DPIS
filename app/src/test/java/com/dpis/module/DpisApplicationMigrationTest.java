package com.dpis.module;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DpisApplicationMigrationTest {
    @Test
    public void doesNotOverwriteRemoteTogglesWhenLocalMissing() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);

        FakePrefs remotePrefs = new FakePrefs();
        DpiConfigStore remote = new DpiConfigStore(remotePrefs);
        assertTrue(remote.setSystemServerHooksEnabled(false));
        assertTrue(remote.setSystemServerSafeModeEnabled(false));
        assertTrue(remote.setGlobalLogEnabled(true));

        invokeMigrate(local, remote);

        assertFalse(remote.isSystemServerHooksEnabled());
        assertFalse(remote.isSystemServerSafeModeEnabled());
        assertTrue(remote.isGlobalLogEnabled());
    }

    @Test
    public void seedsRemoteTogglesWhenRemoteMissing() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);
        assertTrue(local.setSystemServerHooksEnabled(false));
        assertTrue(local.setSystemServerSafeModeEnabled(false));
        assertTrue(local.setGlobalLogEnabled(true));

        FakePrefs remotePrefs = new FakePrefs();
        DpiConfigStore remote = new DpiConfigStore(remotePrefs);

        invokeMigrate(local, remote);

        assertFalse(remote.isSystemServerHooksEnabled());
        assertFalse(remote.isSystemServerSafeModeEnabled());
        assertTrue(remote.isGlobalLogEnabled());
    }

    @Test
    public void seedsRemoteTogglesWhenOnlyBackupContainsValue() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);
        assertTrue(local.setSystemServerHooksEnabled(false));
        assertTrue(local.setSystemServerSafeModeEnabled(false));
        assertTrue(local.setGlobalLogEnabled(true));

        FakePrefs remotePrefs = new FakePrefs();
        FakePrefs backupPrefs = new FakePrefs();
        backupPrefs.edit()
                .putBoolean(DpiConfigStore.KEY_SYSTEM_SERVER_HOOKS_ENABLED, true)
                .putBoolean(DpiConfigStore.KEY_SYSTEM_SERVER_SAFE_MODE_ENABLED, true)
                .putBoolean(DpiConfigStore.KEY_GLOBAL_LOG_ENABLED, false)
                .commit();
        DpiConfigStore remote = new DpiConfigStore(remotePrefs, backupPrefs);

        invokeMigrate(local, remote);

        DpiConfigStore remoteOnly = new DpiConfigStore(remotePrefs);
        assertFalse(remoteOnly.isSystemServerHooksEnabled());
        assertFalse(remoteOnly.isSystemServerSafeModeEnabled());
        assertTrue(remoteOnly.isGlobalLogEnabled());
    }

    @Test
    public void seedsRemoteFontScaleWhenRemoteMissing() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);
        assertTrue(local.setTargetFontScalePercent("com.max.xiaoheihe", 150));

        FakePrefs remotePrefs = new FakePrefs();
        DpiConfigStore remote = new DpiConfigStore(remotePrefs);

        invokeMigrate(local, remote);

        assertTrue(remote.getConfiguredPackages().contains("com.max.xiaoheihe"));
        assertTrue(remote.getTargetViewportWidthDp("com.max.xiaoheihe") == null);
        assertTrue(remote.getTargetFontScalePercent("com.max.xiaoheihe") != null);
        assertTrue(remote.getTargetFontScalePercent("com.max.xiaoheihe") == 150);
    }

    @Test
    public void doesNotOverwriteRemoteFontScaleWhenAlreadyPresent() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);
        assertTrue(local.setTargetFontScalePercent("com.max.xiaoheihe", 150));

        FakePrefs remotePrefs = new FakePrefs();
        DpiConfigStore remote = new DpiConfigStore(remotePrefs);
        assertTrue(remote.setTargetFontScalePercent("com.max.xiaoheihe", 130));

        invokeMigrate(local, remote);

        assertTrue(remote.getTargetFontScalePercent("com.max.xiaoheihe") != null);
        assertTrue(remote.getTargetFontScalePercent("com.max.xiaoheihe") == 130);
    }

    @Test
    public void seedsRemoteFontModeWhenRemoteMissing() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);
        assertTrue(local.setTargetFontScalePercent("com.max.xiaoheihe", 150));
        assertTrue(local.setTargetFontApplyMode("com.max.xiaoheihe", FontApplyMode.FIELD_REWRITE));

        FakePrefs remotePrefs = new FakePrefs();
        DpiConfigStore remote = new DpiConfigStore(remotePrefs);

        invokeMigrate(local, remote);

        assertTrue(FontApplyMode.FIELD_REWRITE.equals(
                remote.getTargetFontApplyMode("com.max.xiaoheihe")));
    }

    @Test
    public void seedsRemoteViewportWhenOnlyBackupContainsValue() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);
        assertTrue(local.setTargetViewportWidthDp("com.max.xiaoheihe", 360));

        FakePrefs remotePrefs = new FakePrefs();
        FakePrefs backupPrefs = new FakePrefs();
        backupPrefs.edit().putInt("viewport.com.max.xiaoheihe.width_dp", 300).commit();
        DpiConfigStore remote = new DpiConfigStore(remotePrefs, backupPrefs);

        invokeMigrate(local, remote);

        DpiConfigStore remoteOnly = new DpiConfigStore(remotePrefs);
        assertTrue(remoteOnly.getTargetViewportWidthDp("com.max.xiaoheihe") != null);
        assertTrue(remoteOnly.getTargetViewportWidthDp("com.max.xiaoheihe") == 360);
    }

    @Test
    public void seedsRemoteFontScaleWhenOnlyBackupContainsValue() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);
        assertTrue(local.setTargetFontScalePercent("com.max.xiaoheihe", 150));

        FakePrefs remotePrefs = new FakePrefs();
        FakePrefs backupPrefs = new FakePrefs();
        backupPrefs.edit().putInt("font.com.max.xiaoheihe.scale_percent", 120).commit();
        DpiConfigStore remote = new DpiConfigStore(remotePrefs, backupPrefs);

        invokeMigrate(local, remote);

        DpiConfigStore remoteOnly = new DpiConfigStore(remotePrefs);
        assertTrue(remoteOnly.getTargetFontScalePercent("com.max.xiaoheihe") != null);
        assertTrue(remoteOnly.getTargetFontScalePercent("com.max.xiaoheihe") == 150);
    }

    @Test
    public void preservesRemoteOnlyPackageConfigDuringMigration() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);
        assertTrue(local.setTargetViewportWidthDp("com.miui.weather2", 500));

        FakePrefs remotePrefs = new FakePrefs();
        DpiConfigStore remote = new DpiConfigStore(remotePrefs);
        assertTrue(remote.setTargetViewportWidthDp("com.miui.gallery", 500));
        assertTrue(remote.setTargetViewportApplyMode(
                "com.miui.gallery", ViewportApplyMode.FIELD_REWRITE));
        assertTrue(remote.setTargetFontScalePercent("com.miui.gallery", 300));
        assertTrue(remote.setTargetFontApplyMode(
                "com.miui.gallery", FontApplyMode.SYSTEM_EMULATION));

        invokeMigrate(local, remote);

        DpiConfigStore remoteOnly = new DpiConfigStore(remotePrefs);
        assertTrue(remoteOnly.getConfiguredPackages().contains("com.miui.gallery"));
        assertTrue(remoteOnly.getTargetViewportWidthDp("com.miui.gallery") == 500);
        assertTrue(remoteOnly.getTargetFontScalePercent("com.miui.gallery") == 300);
        assertTrue(FontApplyMode.SYSTEM_EMULATION.equals(
                remoteOnly.getTargetFontApplyMode("com.miui.gallery")));
    }

    @Test
    public void nativeProxyAssetResolverUsesFirstSupportedAvailableAbi() {
        LinkedHashSet<String> available = new LinkedHashSet<>();
        available.add("native/armeabi-v7a/libdpis_native.so");
        available.add("native/arm64-v8a/libdpis_native.so");

        String path = HyperOsNativeProxyAssetExporter.resolveNativeProxyAssetPath(available,
                new String[]{"x86_64", "arm64-v8a", "armeabi-v7a"});

        assertEquals("native/arm64-v8a/libdpis_native.so", path);
    }

    @Test
    public void nativeProxyAssetResolverFallsBackToArm64ThenFirstAvailable() {
        LinkedHashSet<String> available = new LinkedHashSet<>();
        available.add("native/armeabi-v7a/libdpis_native.so");
        available.add("native/arm64-v8a/libdpis_native.so");

        String arm64Path = HyperOsNativeProxyAssetExporter.resolveNativeProxyAssetPath(available,
                new String[]{"x86_64"});
        String firstPath = HyperOsNativeProxyAssetExporter.resolveNativeProxyAssetPath(available,
                new String[]{"x86_64", "x86"});

        assertEquals("native/arm64-v8a/libdpis_native.so", arm64Path);
        assertEquals("native/arm64-v8a/libdpis_native.so", firstPath);
    }

    @Test
    public void nativeProxyAssetResolverReturnsNullWhenNoAssetsAvailable() {
        assertNull(HyperOsNativeProxyAssetExporter.resolveNativeProxyAssetPath(new LinkedHashSet<>(),
                new String[]{"arm64-v8a"}));
        assertNull(HyperOsNativeProxyAssetExporter.resolveNativeProxyAssetPath(null,
                new String[]{"arm64-v8a"}));
    }

    @Test
    public void doesNotOverwriteRemoteLauncherIconHiddenWhenLocalMissing() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);

        FakePrefs remotePrefs = new FakePrefs();
        DpiConfigStore remote = new DpiConfigStore(remotePrefs);
        assertTrue(remote.setLauncherIconHidden(true));

        invokeMigrate(local, remote);

        assertTrue(remote.isLauncherIconHidden());
    }

    @Test
    public void seedsRemoteLauncherIconHiddenWhenRemoteMissing() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);
        assertTrue(local.setLauncherIconHidden(true));

        FakePrefs remotePrefs = new FakePrefs();
        DpiConfigStore remote = new DpiConfigStore(remotePrefs);

        invokeMigrate(local, remote);

        assertTrue(remote.isLauncherIconHidden());
    }

    @Test
    public void migrationDoesNotCopyLegacyGlobalFontHookFlags() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);
        assertTrue(local.setFlutterFontHookEnabled(true));
        assertTrue(local.setFlutterSettingsFontHookEnabled(true));
        assertTrue(local.setHyperOsFlutterFontHookEnabled(true));

        FakePrefs remotePrefs = new FakePrefs();
        DpiConfigStore remote = new DpiConfigStore(remotePrefs);

        invokeMigrate(local, remote);

        assertFalse(remote.isFlutterFontHookEnabled());
        assertFalse(remote.isFlutterSettingsFontHookEnabled());
        assertFalse(remote.isHyperOsFlutterFontHookEnabled());
    }

    @Test
    public void publishRuntimeConfigKeepsTemplatesAndGlobalPrefillLocalOnly() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);
        TemplateConfigValue globalPrefill = new TemplateConfigValue(
                ViewportTargetSpec.absoluteDp(411),
                ViewportApplyMode.AUTO,
                120,
                FontApplyMode.SYSTEM_EMULATION,
                "font_default",
                "resources_font");
        assertTrue(new GlobalPrefillStore(localPrefs).write(globalPrefill));
        QuickTemplateStore localTemplates = new QuickTemplateStore(localPrefs);
        assertTrue(localTemplates.save(new QuickTemplateStore.QuickTemplate(
                "template_a",
                "Compact",
                1000L,
                Set.of("com.example.one"),
                new TemplateConfigValue(
                        ViewportTargetSpec.relativeScale(1100),
                        ViewportApplyMode.COMPAT,
                        115,
                        FontApplyMode.FIELD_REWRITE,
                        "font_template",
                        "textview_sp"))));
        assertTrue(localTemplates.reorder(List.of("template_a")));

        FakePrefs remotePrefs = new FakePrefs();
        DpiConfigStore remote = new DpiConfigStore(remotePrefs);
        assertTrue(remote.setTargetFontScalePercent("com.miui.weather2", 200));

        invokePublish(local, remote);

        assertEquals(TemplateConfigValue.EMPTY, new GlobalPrefillStore(remotePrefs).read());
        assertNull(new QuickTemplateStore(remotePrefs).read("template_a"));
        assertEquals(globalPrefill, new GlobalPrefillStore(localPrefs).read());
        QuickTemplateStore.QuickTemplate mirroredTemplate =
                new QuickTemplateStore(localPrefs).read("template_a");
        assertTrue(mirroredTemplate != null);
        assertEquals("Compact", mirroredTemplate.name);
        assertEquals(Set.of("com.example.one"), mirroredTemplate.selectedPackages);
        assertEquals("font_template", mirroredTemplate.configValue.typefaceId);
        assertNull(remote.getTargetFontScalePercent("com.miui.weather2"));
    }

    @Test
    public void publishRuntimeConfigClearsRemoteTemplateAndPrefillConfigAfterLocalDelete() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);

        FakePrefs remotePrefs = new FakePrefs();
        DpiConfigStore remote = new DpiConfigStore(remotePrefs);
        assertTrue(new GlobalPrefillStore(remotePrefs).write(new TemplateConfigValue(
                ViewportTargetSpec.absoluteDp(512),
                ViewportApplyMode.SYSTEM,
                null,
                FontApplyMode.OFF,
                "remote_font",
                null)));
        assertTrue(new QuickTemplateStore(remotePrefs).save(new QuickTemplateStore.QuickTemplate(
                "template_a",
                "Remote",
                2000L,
                Set.of("com.example.remote"),
                new TemplateConfigValue(
                        ViewportTargetSpec.off(),
                        ViewportApplyMode.OFF,
                        null,
                        FontApplyMode.OFF,
                        "remote_template_font",
                        null))));
        assertTrue(remote.setTargetFontScalePercent("com.miui.weather2", 200));

        invokePublish(local, remote);

        assertEquals(TemplateConfigValue.EMPTY, new GlobalPrefillStore(localPrefs).read());
        assertNull(new QuickTemplateStore(localPrefs).read("template_a"));
        assertEquals(TemplateConfigValue.EMPTY, new GlobalPrefillStore(remotePrefs).read());
        assertNull(new QuickTemplateStore(remotePrefs).read("template_a"));
        assertNull(remote.getTargetFontScalePercent("com.miui.weather2"));
    }

    @Test
    public void publishRuntimeConfigOverwritesRemoteFromLocalAfterServiceBind() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);
        assertTrue(local.setHyperOsFlutterFontHookEnabled(false));

        FakePrefs remotePrefs = new FakePrefs();
        DpiConfigStore remote = new DpiConfigStore(remotePrefs);
        assertTrue(remote.setHyperOsFlutterFontHookEnabled(true));
        assertTrue(remote.setTargetFontScalePercent("com.miui.weather2", 200));
        assertTrue(remote.setTargetFontApplyMode("com.miui.weather2", FontApplyMode.FIELD_REWRITE));
        assertTrue(remote.setStartupDisclaimerAccepted(true));

        invokePublish(local, remote);

        assertFalse(local.isHyperOsFlutterFontHookEnabled());
        assertFalse(local.getConfiguredPackages().contains("com.miui.weather2"));
        assertFalse(remote.isHyperOsFlutterFontHookEnabled());
        assertFalse(remote.getConfiguredPackages().contains("com.miui.weather2"));
        assertNull(remote.getTargetFontScalePercent("com.miui.weather2"));
        assertFalse(local.isStartupDisclaimerAccepted());
        assertFalse(remote.isStartupDisclaimerAccepted());
    }

    @Test
    public void publishesWechatDpiToRemoteConfigOnServiceBind() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);
        assertTrue(local.setWechatDpi("com.tencent.mm", 600));

        FakePrefs remotePrefs = new FakePrefs();
        DpiConfigStore remote = new DpiConfigStore(remotePrefs);

        invokePublish(local, remote);

        assertEquals(Integer.valueOf(600), remote.getWechatDpi("com.tencent.mm"));
        assertTrue(remote.getConfiguredPackages().contains("com.tencent.mm"));
    }

    @Test
    public void publishRuntimeConfigDoesNotSendLocalStartupDisclaimerConsent() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);
        assertTrue(local.setStartupDisclaimerAccepted(true));

        FakePrefs remotePrefs = new FakePrefs();
        DpiConfigStore remote = new DpiConfigStore(remotePrefs);
        assertTrue(remote.setTargetFontScalePercent("com.miui.weather2", 200));

        invokePublish(local, remote);

        assertTrue(local.isStartupDisclaimerAccepted());
        assertFalse(remote.isStartupDisclaimerAccepted());
        assertNull(remote.getTargetFontScalePercent("com.miui.weather2"));
    }

    @Test
    public void publishRuntimeConfigDoesNotSendLocalOnlyUiState() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);
        assertTrue(local.setStartupDisclaimerAccepted(true));
        assertTrue(local.setInterfaceScalePercent(73));
        assertTrue(local.setLauncherIconHidden(true));
        assertTrue(local.setTargetFontScalePercent("com.miui.weather2", 200));

        FakePrefs remotePrefs = new FakePrefs();
        DpiConfigStore remote = new DpiConfigStore(remotePrefs);

        invokePublish(local, remote);

        assertTrue(local.isStartupDisclaimerAccepted());
        assertEquals(73, local.getInterfaceScalePercent());
        assertTrue(local.isLauncherIconHidden());
        assertFalse(remote.isStartupDisclaimerAccepted());
        assertEquals(AppUiScaleManager.DEFAULT_SCALE_PERCENT, remote.getInterfaceScalePercent());
        assertFalse(remote.isLauncherIconHidden());
        assertEquals(Integer.valueOf(200), remote.getTargetFontScalePercent("com.miui.weather2"));
    }

    @Test
    public void publishRuntimeConfigClearsRemoteOnlyUiState() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);

        FakePrefs remotePrefs = new FakePrefs();
        remotePrefs.edit()
                .putBoolean(DpiConfigStore.KEY_STARTUP_DISCLAIMER_ACCEPTED, true)
                .putInt(DpiConfigStore.KEY_INTERFACE_SCALE_PERCENT, 73)
                .putBoolean(DpiConfigStore.KEY_HIDE_LAUNCHER_ICON, true)
                .commit();
        DpiConfigStore remote = new DpiConfigStore(remotePrefs);

        invokePublish(local, remote);

        assertFalse(local.isStartupDisclaimerAccepted());
        assertEquals(AppUiScaleManager.DEFAULT_SCALE_PERCENT, local.getInterfaceScalePercent());
        assertFalse(local.isLauncherIconHidden());
        assertFalse(remote.isStartupDisclaimerAccepted());
        assertEquals(AppUiScaleManager.DEFAULT_SCALE_PERCENT, remote.getInterfaceScalePercent());
        assertFalse(remote.isLauncherIconHidden());
    }

    private static void invokeMigrate(DpiConfigStore from, DpiConfigStore to) throws Exception {
        Method method = DpisApplication.class.getDeclaredMethod(
                "migrateConfig", DpiConfigStore.class, DpiConfigStore.class);
        method.setAccessible(true);
        method.invoke(null, from, to);
    }

    private static void invokePublish(DpiConfigStore from, DpiConfigStore to) throws Exception {
        Method method = DpisApplication.class.getDeclaredMethod(
                "publishRuntimeConfig", DpiConfigStore.class, DpiConfigStore.class);
        method.setAccessible(true);
        method.invoke(null, from, to);
    }
}
