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

        assertFalse(store.isFlutterFontHookEnabled());
        assertFalse(store.isFlutterSettingsFontHookEnabled());
        assertFalse(store.isHyperOsFlutterFontHookEnabled());

        assertTrue(store.setFlutterFontHookEnabled(true));
        assertTrue(store.setFlutterSettingsFontHookEnabled(true));
        assertTrue(store.setHyperOsFlutterFontHookEnabled(true));
        assertTrue(store.isFlutterFontHookEnabled());
        assertTrue(store.isFlutterSettingsFontHookEnabled());
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
    public void bridgeTypefacePropertyNameUsesStablePackageHash() {
        assertEquals("debug.dpis.typeface.a55b5fe1",
                HyperOsFlutterFontBridge.typefacePropertyNameForPackage("com.miui.gallery"));
        assertEquals("persist.debug.dpis.typeface.a55b5fe1",
                HyperOsFlutterFontBridge.persistentTypefacePropertyNameForPackage("com.miui.gallery"));
    }

    @Test
    public void compat100FactoryUsesPackageSystemPropertiesWithoutExportedProvider() throws Exception {
        String factory = readSource("src/main/java/com/dpis/module/ConfigStoreFactory.java");
        String prefs = readSource("src/main/java/com/dpis/module/RuntimePropertyConfigPreferences.java");
        String app = readSource("src/main/java/com/dpis/module/DpisApplication.java");

        assertTrue(factory.contains("createForCompat100Host(String packageName)"));
        assertTrue(factory.contains("new RuntimePropertyConfigPreferences(packageName, autoViewportRuntimeRoute)"));
        assertTrue(factory.contains("AutoViewportRuntimeRoute.ABSOLUTE_TARGETS_ONLY"));
        assertFalse(factory.contains("CompatConfigProviderPreferences"));
        assertTrue(prefs.contains("ViewportPropertyBridge.readTargetSpec(packageName)"));
        assertTrue(prefs.contains("HyperOsFlutterFontBridge.readForceFontScalePercent(packageName)"));
        assertTrue(prefs.contains("HyperOsFlutterFontBridge.readCompatFontScalePercent(packageName)"));
        assertTrue(prefs.contains("viewportTargetSpec.isAbsoluteDp()"));
        assertTrue(prefs.contains("HyperOsFlutterFontBridge.readCompatFontMode(packageName)"));
        assertTrue(prefs.contains("FontHookDomainPropertyBridge.readOverride(packageName)"));
        assertTrue(prefs.contains("HyperOsFlutterFontBridge.readTypefaceId(packageName)"));
        assertTrue(app.contains("RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(configStore)"));
        assertTrue(app.contains("DpiConfigStore localStore = ConfigStoreFactory.createForModuleApp(this);"));
        assertTrue(app.contains("RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(remoteStore)"));
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
    public void rustProcessEnvironmentRequiresNativeDomain() {
        PerAppDisplayConfigSource source = new PerAppDisplayConfigSource(
                ConfigSnapshot::empty,
                packageName -> new PackageConfigSnapshot(
                        packageName,
                        true,
                        (Integer) null,
                        ViewportApplyMode.OFF,
                        300,
                        FontApplyMode.FIELD_REWRITE,
                        null,
                        false,
                        false,
                        false,
                        new HookDomainOverride(
                                true,
                                java.util.Set.of(FontHookDomainRegistry.ID_RESOURCES_FONT),
                                java.util.Set.of())));

        Object[] result = HyperOsRustProcessHookInstaller.applyEnvironmentArgsForLegacy(
                source,
                Arrays.asList("ignored",
                        "com.miui.gallery",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "/data/app/MIUIGallery/lib/arm64/libapp_gallery.so",
                        ""));

        assertEquals(null, result);
    }

    @Test
    public void nativeHookInstallerRequiresEnabledFontMode() throws Exception {
        String source = readSource("src/main/java/com/dpis/module/HyperOsFlutterFontHookInstaller.java");

        assertTrue(source.contains("store.getTargetFontApplyMode(packageName)"));
        assertTrue(source.contains("FontApplyMode.isEnabled("));
        assertFalse(source.contains("store == null || !store.isHyperOsFlutterFontHookEnabled()"));
    }

    @Test
    public void nativeProxyRefreshRequiresFlutterMasterSwitch() throws Exception {
        String source = readSource("src/main/java/com/dpis/module/HyperOsNativeProxyRefreshCoordinator.java");

        assertTrue(source.contains("!store.isFlutterFontHookEnabled()"));
        assertTrue(source.contains("!store.isHyperOsFlutterFontHookEnabled()"));
    }

    @Test
    public void nativeHookInstallerProbesGenericFlutterLibraryLoads() throws Exception {
        String source = readSource("src/main/java/com/dpis/module/HyperOsFlutterFontHookInstaller.java");
        String nativeSource = readSource("src/main/cpp/dpis_native.cpp");
        String moduleMain = readSource("src/modern101/java/com/dpis/module/ModuleMain.java");
        String appProcessInstaller = readSource("src/main/java/com/dpis/module/AppProcessHookInstaller.java");

        assertFalse(moduleMain.contains("HyperOsFlutterFontHookInstaller.install("));
        assertTrue(moduleMain.contains("packagePlan.hyperOsNativeFlutterFontEnabled"));
        assertTrue(appProcessInstaller.contains("fontDomainPlan.hyperOsNativeFlutterEnabled"));
        assertTrue(appProcessInstaller.contains("HyperOsFlutterFontHookInstaller.install(xposed, packageName, store)"));
        assertTrue(source.contains("installRuntimeLibraryProbe(xposed, packageName)"));
        assertTrue(source.contains("installFlutterViewAttachProbe(xposed, packageName)"));
        assertTrue(source.contains("private static final boolean DEBUG_PROBES = BuildConfig.DEBUG"));
        assertTrue(source.contains("installDebugOnlyProbes(xposed, packageName)"));
        assertTrue(source.contains("if (!DEBUG_PROBES)"));
        assertTrue(source.contains("installActivityResumeProbe(xposed, packageName)"));
        assertTrue(source.contains("installFrameProbe(xposed, packageName)"));
        assertTrue(source.contains("installViewRootTraversalProbe(xposed, packageName)"));
        assertTrue(source.contains("installHandlerDispatchProbe(xposed, packageName)"));
        assertTrue(source.contains("\"handler-\" + remaining"));
        assertTrue(source.contains("Handler dispatch Flutter probe ready"));
        assertTrue(source.contains("\"view-root-\" + remaining"));
        assertTrue(source.contains("ViewRoot traversal Flutter probe ready"));
        assertTrue(source.contains("\"frame-\" + remaining"));
        assertTrue(source.contains("Choreographer frame Flutter probe ready"));
        assertTrue(source.contains("\"activity-resume\""));
        assertTrue(source.contains("\"loadLibrary0\""));
        assertTrue(source.contains("\"load0\""));
        assertTrue(source.contains("onRuntimeLibraryLoaded(packageName, loadedName)"));
        assertTrue(source.contains("genericFlutterProbeStatus(packageName, source)"));
        assertTrue(source.contains("logGenericFlutterProbe(packageName, \"post-configure\")"));
        assertTrue(source.contains("scheduleDelayedGenericFlutterProbe(packageName)"));
        assertTrue(source.contains("if (DEBUG_PROBES)"));
        assertTrue(source.contains("scheduleMainThreadGenericFlutterProbe(packageName)"));
        assertTrue(source.contains("scheduleOneShotThreadGenericFlutterProbe(packageName)"));
        assertTrue(source.contains("scheduleLateMapsProbe(packageName)"));
        assertTrue(source.contains("logGenericFlutterProbe(packageName, \"post-install\")"));
        assertTrue(source.contains("\"delayed-\" + delay + \"ms\""));
        assertTrue(source.contains("\"main-delayed-\" + delay + \"ms\""));
        assertTrue(source.contains("\"thread-delayed-8000ms\""));
        assertTrue(source.contains("DPIS_FONT Flutter late maps probe"));
        assertTrue(source.contains("findMappedLibraryBaseForTest(\"libapp.so\")"));
        assertTrue(source.contains("\"flutter-view-attached \" + view.getClass().getName()"));
        assertTrue(source.contains("findMappedLibraryBaseForTest(\"libflutter.so\")"));
        assertTrue(source.contains("javaMapsBase="));
        assertTrue(source.contains("isFlutterLibraryNameForTest"));
        assertTrue(nativeSource.contains("kGenericFlutterLibrary = \"libflutter.so\""));
        assertTrue(nativeSource.contains("kGenericFlutterAppLibrary = \"libapp.so\""));
        assertTrue(nativeSource.contains("Generic Flutter font probe: process="));
        assertTrue(nativeSource.contains("Generic Flutter poll thread start result="));
        assertTrue(nativeSource.contains("Generic Flutter status tick: process="));
        assertTrue(nativeSource.contains("bool is_debug_build()"));
        assertTrue(nativeSource.contains("if (!is_debug_build())"));
        assertTrue(nativeSource.contains("is_generic_flutter_font_hook_experiment_enabled()"));
        assertTrue(nativeSource.contains("DPIS_GENERIC_FLUTTER_FONT_HOOK"));
        assertTrue(nativeSource.contains("debug.dpis.generic_flutter_font_hook"));
        assertTrue(nativeSource.contains("schedule_generic_flutter_status();"));
        assertTrue(nativeSource.contains("!is_debug_build() && index >= 7"));
        assertTrue(nativeSource.contains("\"native-poll-%d\""));
        assertTrue(nativeSource.contains("Generic Flutter mapped: process="));
        assertTrue(nativeSource.contains("+ \" route=\""));
        assertFalse(nativeSource.contains("Last-resort generic Flutter route"));
        assertTrue(nativeSource.contains("GENERIC_PUSH_STYLE_D11"));
        assertFalse(nativeSource.contains("matches_verified_push_style_d11_window"));
        assertFalse(nativeSource.contains("VERIFIED_PUSH_STYLE_D11"));
        assertTrue(nativeSource.contains("g_generic_push_style_hooked.load(std::memory_order_acquire)"));
        assertFalse(nativeSource.contains("&& g_generic_create_hooked.load(std::memory_order_acquire)"));
        assertTrue(nativeSource.contains("inline_hook_arm64(push_target"));
        assertTrue(nativeSource.contains("kGenericParagraphBuilderPushStyleOffset = 0x82d470"));
        assertTrue(nativeSource.contains("Generic Flutter ParagraphBuilder::pushStyle hook result="));
        assertTrue(nativeSource.contains("Generic Flutter ParagraphBuilder::pushStyle fontSize override: process="));
        assertTrue(nativeSource.contains("\"status-probe \" + source"));
        assertTrue(nativeSource.contains("overrideCalls="));
        assertTrue(nativeSource.contains("createCalls="));
        assertTrue(nativeSource.contains("pushStyleCalls="));
        assertTrue(nativeSource.contains("lastInputMilli="));
        assertTrue(nativeSource.contains("g_generic_get_scaled_font_size_hooked"));
        assertTrue(nativeSource.contains("lastPollBase="));
        assertTrue(nativeSource.contains("Generic Flutter native poll: process="));
        assertTrue(nativeSource.contains("Flutter text string probe: process="));
        assertTrue(nativeSource.contains("library="));
        assertTrue(nativeSource.contains("bridge_log_info(\"DPIS_FONT \" + message)"));
        assertTrue(nativeSource.contains("GetStaticMethodID("));
        assertTrue(nativeSource.contains("Java_com_dpis_module_HyperOsFlutterFontHookInstaller_genericFlutterProbeStatus"));
        assertTrue(nativeSource.contains("std::fopen(\"/proc/self/maps\", \"r\")"));
        assertTrue(nativeSource.contains("detected-not-hooked"));
        assertTrue(nativeSource.contains("push-style-d11-hooked"));
        assertFalse(nativeSource.contains("Generic Flutter GetScaledFontSize hook result="));
        assertFalse(nativeSource.contains("\"Generic Flutter ParagraphBuilder::Create hook result=\""));
    }

    @Test
    public void genericFlutterNameDetectionCoversRuntimeAndPathForms() {
        assertTrue(HyperOsFlutterFontHookInstaller.isFlutterLibraryNameForTest("flutter"));
        assertTrue(HyperOsFlutterFontHookInstaller.isFlutterLibraryNameForTest("libflutter.so"));
        assertTrue(HyperOsFlutterFontHookInstaller.isFlutterLibraryNameForTest(
                "/data/app/example/lib/arm64/libflutter.so"));
        assertTrue(HyperOsFlutterFontHookInstaller.isFlutterLibraryNameForTest(
                "libhyper_os_flutter.so"));
        assertFalse(HyperOsFlutterFontHookInstaller.isFlutterLibraryNameForTest("webviewchromium"));
    }

    @Test
    public void flutterViewClassNameDetectionCoversFlutterEmbeddingAndPlugins() {
        assertTrue(HyperOsFlutterFontHookInstaller.isFlutterViewClassNameForTest(
                "io.flutter.embedding.android.FlutterView"));
        assertTrue(HyperOsFlutterFontHookInstaller.isFlutterViewClassNameForTest(
                "com.pichillilorenzo.flutter_inappwebview_android.webview.InAppWebView"));
        assertFalse(HyperOsFlutterFontHookInstaller.isFlutterViewClassNameForTest(
                "android.widget.TextView"));
    }

    @Test
    public void genericFlutterProbeStatusParserReadsBaseAddress() {
        String status = "Generic Flutter font probe: process=p package=p source=s"
                + " handle=0 base=123456 configured=1 enabled=1"
                + " targetFontScalePercent=300 status=detected-not-hooked";

        assertEquals(123456L, HyperOsFlutterFontHookInstaller.parseFlutterBaseForTest(status));
        assertEquals(0L, HyperOsFlutterFontHookInstaller.parseFlutterBaseForTest("base=bad"));
        assertEquals(0L, HyperOsFlutterFontHookInstaller.parseFlutterBaseForTest(null));
    }

    @Test
    public void mapsStartAddressParserReadsExecutableMappingBase() {
        String line = "6f60e60000-6f6189f000 r-xp 00000000 fe:4f 2284291"
                + " /data/app/example/lib/arm64/libflutter.so";

        assertEquals(0x6f60e60000L,
                HyperOsFlutterFontHookInstaller.parseMapsStartAddressForTest(line));
        assertEquals(0L, HyperOsFlutterFontHookInstaller.parseMapsStartAddressForTest("bad"));
    }

    @Test
    public void nativeLoaderCanResolveExtractedModuleLibraryFromLsposedClassLoaderText() {
        String text = "LspModuleClassLoader[module=/data/app/~~id==/"
                + "io.github.kwensiu.dpis-abcd==/base.apk, nativeLibraryDirectories=[]]";

        assertEquals("/data/app/~~id==/io.github.kwensiu.dpis-abcd==/base.apk",
                HyperOsFlutterFontHookInstaller.parseModuleApkPathForTest(text));
        assertEquals("arm64",
                HyperOsFlutterFontHookInstaller.nativeDirectoryNamesForAbi("arm64-v8a")[0]);
        assertEquals("arm",
                HyperOsFlutterFontHookInstaller.nativeDirectoryNamesForAbi("armeabi-v7a")[0]);
        assertEquals("x86",
                HyperOsFlutterFontHookInstaller.nativeDirectoryNamesForAbi("x86")[0]);
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
        return SourceSmokeTestPaths.read(relativePath);
    }
}
