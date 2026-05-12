package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

public class HyperOsFlutterFontHookConfigTest {
    @Test
    public void experimentalHookDefaultsOffAndPersists() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());

        assertFalse(store.isHyperOsFlutterFontHookEnabled());

        assertTrue(store.setHyperOsFlutterFontHookEnabled(true));
        assertTrue(store.isHyperOsFlutterFontHookEnabled());
    }

    @Test
    public void bridgePropertyNameUsesStablePackageHash() {
        assertEquals("debug.dpis.font.a55b5fe1",
                HyperOsFlutterFontBridge.propertyNameForPackage("com.miui.gallery"));
    }

    @Test
    public void bridgeForcePropertyNameUsesStablePackageHash() {
        assertEquals("debug.dpis.forcefont.a55b5fe1",
                HyperOsFlutterFontBridge.forcePropertyNameForPackage("com.miui.gallery"));
    }

    @Test
    public void bridgeCompatFontPropertyNameUsesStablePackageHash() {
        assertEquals("debug.dpis.compatfont.a55b5fe1",
                HyperOsFlutterFontBridge.compatFontPropertyNameForPackage("com.miui.gallery"));
    }

    @Test
    public void compat100FactoryUsesPackageSystemPropertiesWithoutExportedProvider() throws Exception {
        String factory = readSource("src/main/java/com/dpis/module/ConfigStoreFactory.java");
        String prefs = readSource("src/main/java/com/dpis/module/SystemPropertyConfigPreferences.java");
        String app = readSource("src/main/java/com/dpis/module/DpisApplication.java");

        assertTrue(factory.contains("createForCompat100Host(String packageName)"));
        assertTrue(factory.contains("new SystemPropertyConfigPreferences(packageName)"));
        assertFalse(factory.contains("CompatConfigProviderPreferences"));
        assertTrue(prefs.contains("ViewportPropertyBridge.readTargetWidthDp(packageName)"));
        assertTrue(prefs.contains("HyperOsFlutterFontBridge.readForceFontScalePercent(packageName)"));
        assertTrue(prefs.contains("HyperOsFlutterFontBridge.readCompatFontScalePercent(packageName)"));
        assertTrue(prefs.contains("ViewportPropertyBridge.readCompatConfigWidthDp(packageName)"));
        assertTrue(prefs.contains("HyperOsFlutterFontBridge.readCompatFontMode(packageName)"));
        assertTrue(app.contains("ViewportPropertySyncer.syncConfiguredTargetsAsync(configStore)"));
        assertTrue(app.contains("CompatFontPropertySyncer.syncConfiguredTargetsAsync(configStore)"));
        assertTrue(app.contains("DpiConfigStore localStore = ConfigStoreFactory.createForModuleApp(this);"));
        assertTrue(app.contains("ViewportPropertySyncer.syncConfiguredTargetsAsync(remoteStore)"));
        assertTrue(app.contains("CompatFontPropertySyncer.syncConfiguredTargetsAsync(remoteStore)"));
    }


    @Test
    public void viewportBridgePropertyNameUsesStablePackageHash() {
        assertEquals("debug.dpis.vp.a55b5fe1",
                ViewportPropertyBridge.propertyNameForPackage("com.miui.gallery"));
    }

    @Test
    public void viewportBridgeParsesPositiveOverrideAndExplicitClear() {
        assertEquals(Integer.valueOf(300), ViewportPropertyBridge.parseOverrideValueForTest("300"));
        assertEquals(Integer.valueOf(0), ViewportPropertyBridge.parseOverrideValueForTest("0"));
        assertEquals(null, ViewportPropertyBridge.parseOverrideValueForTest(""));
        assertEquals(null, ViewportPropertyBridge.parseOverrideValueForTest("abc"));
    }

    @Test
    public void rustProcessEnvironmentIncludesFontTarget() {
        String envs = HyperOsRustProcessHookInstaller.appendEnvironmentForTest(
                "", "com.miui.gallery", 300,
                "/data/app/MIUIGallery/lib/arm64/libapp_gallery.so");

        assertEquals("DPIS_PACKAGE=com.miui.gallery --envs=DPIS_FONT_SCALE_PERCENT=300"
                        + " --envs=DPIS_RUST_BINARY=/data/app/MIUIGallery/lib/arm64/libapp_gallery.so"
                        + " --cold-boot-speed",
                envs);
    }

    @Test
    public void rustProcessProxyFallsBackToSiblingPath() {
        String proxyPath = HyperOsRustProcessHookInstaller.resolveProxyLibraryPathForTest(
                "/missing/MIUIGallery/lib/arm64/libapp_gallery.so");

        assertEquals(null, proxyPath);
    }

    @Test
    public void bridgeDoesNotClearRustTargetWhenUiHookSwitchIsOff() throws Exception {
        java.lang.reflect.Method method = HyperOsFlutterFontBridge.class.getDeclaredMethod(
                "shouldClearOnPublishTargetSkipForTest", String.class, PerAppDisplayConfig.class);
        method.setAccessible(true);
        PerAppDisplayConfig config = new PerAppDisplayConfig("com.miui.gallery", null,
                300, FontApplyMode.SYSTEM_EMULATION, false);

        boolean shouldClear = (boolean) method.invoke(null, "com.miui.gallery", config);

        assertFalse(shouldClear);
    }

    @Test
    public void nativeHookInstallerRequiresEnabledFontMode() throws Exception {
        String source = readSource("src/main/java/com/dpis/module/HyperOsFlutterFontHookInstaller.java");

        assertTrue(source.contains("store.getTargetFontApplyMode(packageName)"));
        assertTrue(source.contains("FontApplyMode.isEnabled("));
    }

    @Test
    public void nativeForceFontPropertyCanOverrideJniConfiguration() throws Exception {
        String source = readSource("src/main/cpp/dpis_native.cpp");

        assertFalse(source.contains("if (g_configured_from_jni.load(std::memory_order_relaxed)) {\n        return;"));
        assertTrue(source.indexOf("debug.dpis.forcefont.%08x")
                < source.indexOf("DPIS_FONT_SCALE_PERCENT"));
        assertTrue(source.indexOf("debug.dpis.forcefont")
                < source.indexOf("debug.dpis.font.%08x"));
        assertTrue(source.contains("HyperOS font config source: process="));
    }
    @Test
    public void nativeCreateHookScalesWeatherCreateFontRegisters() throws Exception {
        String source = readSource("src/main/cpp/dpis_native.cpp");

        int createScale = source.indexOf("bl dpis_create_scaled_d0");
        String createTrampoline = source.substring(Math.max(0, createScale - 300), createScale + 180);
        assertTrue(source.contains("double create_observed_scale(double observed_scale)"));
        assertTrue(source.contains("return 1.0;"));
        assertTrue(createTrampoline.contains("ldr d0, [sp, #80]"));
        assertTrue(createTrampoline.contains("bl dpis_create_scaled_d0"));
        assertTrue(createTrampoline.contains("fmul d0, d0, d1"));
    }

    @Test
    public void nativeProxyLoadsOriginalRustBinaryFromEnvironmentBeforeProperty() throws Exception {
        String source = readSource("src/main/cpp/dpis_native.cpp");

        int envRead = source.indexOf("read_environment(\"DPIS_RUST_BINARY\")");
        int propertyRead = source.indexOf("debug.dpis.rustbin.%08x");
        assertTrue(envRead >= 0);
        assertTrue(propertyRead >= 0);
        assertTrue(envRead < propertyRead);
    }

    @Test
    public void weatherNativeProxyFallsBackToSiblingOriginalLibrary() throws Exception {
        String source = readSource("src/main/cpp/dpis_native.cpp");

        assertTrue(source.contains("sibling_original_rust_binary_path()"));
        assertTrue(source.contains("current_process_name() != \"com.miui.weather2\""));
        assertTrue(source.contains("libweather_app.so"));
        assertTrue(source.contains("path == \"0\""));
    }

    @Test
    public void weatherGotHookValidatesOriginalSlotBeforePatching() throws Exception {
        String source = readSource("src/main/cpp/dpis_native.cpp");

        assertTrue(source.contains("is_weather_configuration_font_scale_slot"));
        assertTrue(source.contains("GOT hook skipped: unexpected slot"));
        assertTrue(source.contains("describe_symbol(*slot)"));
        assertTrue(source.contains("kHyperOsAppPublicLibrary"));
        assertTrue(source.contains("ends_with(info.dli_fname, kHyperOsAppPublicLibrary)"));
        assertTrue(source.indexOf("is_weather_configuration_font_scale_slot(*slot)")
                < source.indexOf("*slot = reinterpret_cast<void *>(Configuration_get_font_scale)"));
    }

    @Test
    public void rustProcessArgumentProbeSummarizesTargetStringArguments() {
        String summary = HyperOsRustProcessHookInstaller.buildArgumentProbeSummaryForTest(
                Arrays.asList("ignored",
                        "com.miui.weather2",
                        Integer.valueOf(1),
                        "/data/app/weather/lib/arm64/libweather_app.so",
                        "--envs=EXISTING=value"));

        assertTrue(summary.contains("size=5"));
        assertTrue(summary.contains("1=com.miui.weather2"));
        assertTrue(summary.contains("3=/data/app/weather/lib/arm64/libweather_app.so"));
        assertTrue(summary.contains("4=--envs=EXISTING=value"));
    }

    @Test
    public void rustProcessArgumentProbeSkipsUnrelatedPackages() {
        String summary = HyperOsRustProcessHookInstaller.buildArgumentProbeSummaryForTest(
                Arrays.asList("com.example.app", "/data/app/example/libfoo.so"));

        assertEquals(null, summary);
    }

    @Test
    public void rustProcessProxyRejectsEmptySiblingPlaceholder() throws Exception {
        java.io.File dir = java.nio.file.Files.createTempDirectory("dpis-rust-proxy").toFile();
        java.io.File original = new java.io.File(dir, "libweather_app.so");
        java.io.File proxy = new java.io.File(dir, "libdpis_native.so");
        assertTrue(original.createNewFile());
        assertTrue(proxy.createNewFile());

        String proxyPath = HyperOsRustProcessHookInstaller.resolveProxyLibraryPathForTest(
                original.getAbsolutePath());

        assertEquals(null, proxyPath);
    }

    private static String readSource(String relativePath) throws Exception {
        java.nio.file.Path path = java.nio.file.Paths.get(relativePath);
        if (!java.nio.file.Files.exists(path)) {
            path = java.nio.file.Paths.get("app", relativePath);
        }
        return new String(java.nio.file.Files.readAllBytes(path),
                java.nio.charset.StandardCharsets.UTF_8);
    }
}
