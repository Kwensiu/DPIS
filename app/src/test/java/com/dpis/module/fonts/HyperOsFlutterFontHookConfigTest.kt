package com.dpis.module

import com.dpis.module.config.ConfigSnapshot
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.config.PackageConfigSnapshot
import com.dpis.module.config.PerAppDisplayConfigSource
import com.dpis.module.config.PerAppDisplayConfigSource.PackageFallbackProvider
import com.dpis.module.config.PerAppDisplayConfigSource.SnapshotProvider
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import com.dpis.module.hooks.HookDomainOverride
import com.dpis.module.runtime.font.HyperOsFlutterFontBridge
import com.dpis.module.runtime.font.HyperOsFlutterFontHookInstaller
import com.dpis.module.runtime.systemserver.HyperOsRustProcessHookInstaller
import com.dpis.module.runtime.systemserver.PerAppDisplayConfig
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportPropertyBridge.parseOverrideValueForTest
import com.dpis.module.viewport.ViewportPropertyBridge.propertyNameForPackage
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.math.max


class HyperOsFlutterFontHookConfigTest {
    @Test
    fun experimentalHookDefaultsOffAndPersists() {
        val store = DpisConfigStore(FakePrefs())

        Assert.assertFalse(store.isFlutterFontHookEnabled)
        Assert.assertFalse(store.isFlutterSettingsFontHookEnabled)
        Assert.assertFalse(store.isHyperOsFlutterFontHookEnabled)

        Assert.assertTrue(store.setFlutterFontHookEnabled(true))
        Assert.assertTrue(store.setFlutterSettingsFontHookEnabled(true))
        Assert.assertTrue(store.setHyperOsFlutterFontHookEnabled(true))
        Assert.assertTrue(store.isFlutterFontHookEnabled)
        Assert.assertTrue(store.isFlutterSettingsFontHookEnabled)
        Assert.assertTrue(store.isHyperOsFlutterFontHookEnabled)
    }

    @Test
    fun bridgePropertyNameUsesStablePackageHash() {
        Assert.assertEquals(
            "debug.dpis.font.a55b5fe1",
            HyperOsFlutterFontBridge.propertyNameForPackage("com.miui.gallery")
        )
    }

    @Test
    fun bridgeForcePropertyNameUsesStablePackageHash() {
        Assert.assertEquals(
            "debug.dpis.forcefont.a55b5fe1",
            HyperOsFlutterFontBridge.forcePropertyNameForPackage("com.miui.gallery")
        )
    }

    @Test
    fun bridgeCompatFontPropertyNameUsesStablePackageHash() {
        Assert.assertEquals(
            "debug.dpis.compatfont.a55b5fe1",
            HyperOsFlutterFontBridge.compatFontPropertyNameForPackage("com.miui.gallery")
        )
    }

    @Test
    fun bridgeTypefacePropertyNameUsesStablePackageHash() {
        Assert.assertEquals(
            "debug.dpis.typeface.a55b5fe1",
            HyperOsFlutterFontBridge.typefacePropertyNameForPackage("com.miui.gallery")
        )
        Assert.assertEquals(
            "persist.debug.dpis.typeface.a55b5fe1",
            HyperOsFlutterFontBridge.persistentTypefacePropertyNameForPackage("com.miui.gallery")
        )
    }

    @Test
    @Throws(Exception::class)
    fun legacyFactoryUsesPackageSystemPropertiesWithoutExportedProvider() {
        val factory: String = readSource(
            "src/legacy/java/com/dpis/module/LegacyConfigStoreFactory.kt"
        )
        val prefs: String =
            readSource("src/main/java/com/dpis/module/config/RuntimePropertyConfigPreferences.kt")
        val app: String = readSource("src/main/java/com/dpis/module/DpisApplication.kt")

        Assert.assertTrue(factory.contains("fun create(packageName: String?): DpisConfigStore"))
        Assert.assertTrue(factory.contains("RuntimePropertyConfigPreferences(packageName, route)"))
        Assert.assertTrue(factory.contains("AutoViewportRuntimeRoute.ANY_ENABLED_TARGET"))
        Assert.assertFalse(factory.contains("CompatConfigProviderPreferences"))
        Assert.assertTrue(prefs.contains("ViewportPropertyBridge.readTargetSpec(packageName)"))
        Assert.assertTrue(prefs.contains("HyperOsFlutterFontBridge.readForceFontScalePercent(packageName)"))
        Assert.assertTrue(prefs.contains("HyperOsFlutterFontBridge.readCompatFontScalePercent(packageName)"))
        Assert.assertTrue(prefs.contains("targetSpec.absoluteWidthDp()"))
        Assert.assertTrue(prefs.contains("HyperOsFlutterFontBridge.readCompatFontMode(packageName)"))
        Assert.assertTrue(prefs.contains("FontHookDomainPropertyBridge.readOverride(packageName)"))
        Assert.assertTrue(prefs.contains("HyperOsFlutterFontBridge.readTypefaceId(packageName)"))
        Assert.assertTrue(app.contains("RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(configStore)"))
        Assert.assertTrue(app.contains("val localStore = ConfigStoreFactory.createLocalModuleConfigStore(this)"))
        Assert.assertTrue(app.contains("fun getActiveHookConfigStore(context: Context?): DpisConfigStore?"))
        Assert.assertTrue(app.contains("if (context == null)"))
        Assert.assertTrue(app.contains("return null"))
        Assert.assertTrue(app.contains("RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(refreshedStore)"))
    }


    @Test
    fun viewportBridgePropertyNameUsesStablePackageHash() {
        Assert.assertEquals(
            "debug.dpis.vp.a55b5fe1",
            propertyNameForPackage("com.miui.gallery")
        )
    }

    @Test
    fun viewportBridgeParsesPositiveOverrideAndExplicitClear() {
        Assert.assertEquals(300, parseOverrideValueForTest("300"))
        Assert.assertEquals(0, parseOverrideValueForTest("0"))
        Assert.assertNull(parseOverrideValueForTest(""))
        Assert.assertNull(parseOverrideValueForTest("abc"))
    }

    @Test
    fun rustProcessProxyFallsBackToSiblingPath() {
        val proxyPath = HyperOsRustProcessHookInstaller.resolveProxyLibraryPathForTest(
            "/missing/MIUIGallery/lib/arm64/libapp_gallery.so"
        )

        Assert.assertNull(proxyPath)
    }

    @Test
    @Throws(Exception::class)
    fun bridgeDoesNotClearRustTargetWhenUiHookSwitchIsOff() {
        val method = HyperOsFlutterFontBridge::class.java.getDeclaredMethod(
            "shouldClearOnPublishTargetSkipForTest",
            String::class.java,
            PerAppDisplayConfig::class.java
        )
        method.isAccessible = true
        val config = PerAppDisplayConfig(
            "com.miui.gallery", null,
            300, FontApplyMode.SYSTEM_EMULATION, false
        )

        val shouldClear = method.invoke(null, "com.miui.gallery", config) as Boolean

        Assert.assertFalse(shouldClear)
    }

    @Test
    fun rustProcessEnvironmentRequiresNativeDomain() {
        val source = PerAppDisplayConfigSource(
            SnapshotProvider { ConfigSnapshot.empty() },
            PackageFallbackProvider { packageName ->
                PackageConfigSnapshot(
                    packageName,
                    true,
                    null,
                    ViewportApplyMode.OFF,
                    300,
                    FontApplyMode.FIELD_REWRITE,
                    null,
                    false,
                    false,
                    false,
                    HookDomainOverride(
                        true,
                        setOf(FontHookDomainRegistry.ID_RESOURCES_FONT),
                        emptySet(),
                    ),
                )
            },
        )

        val result = HyperOsRustProcessHookInstaller.applyEnvironmentArgsForLegacy(
            source,
            mutableListOf<Any?>(
                "ignored",
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
                ""
            )
        )

        Assert.assertNull(result)
    }

    @Test
    @Throws(Exception::class)
    fun typefaceOnlyProbeEntryDoesNotRequireFontScale() {
        val source: String =
            readSource("src/main/java/com/dpis/module/runtime/font/HyperOsFlutterFontHookInstaller.java")

        Assert.assertTrue(source.contains("installTypefaceProbe("))
        Assert.assertTrue(source.contains("store.getTargetTypefaceId(packageName)"))
        Assert.assertTrue(source.contains("installConfigured(xposed, packageName, 100, false, \"typeface\")"))
        Assert.assertTrue(source.contains("reason=\" + reason"))
    }

    @Test
    @Throws(Exception::class)
    fun appProcessInstallsTypefaceOnlyProbeAlongsideTypefaceHook() {
        val source: String = readSource(
            "src/main/java/com/dpis/module/runtime/appprocess/AppProcessHookInstaller.kt"
        )

        Assert.assertTrue(source.contains("if (packagePlan.typefaceEnabled)"))
        Assert.assertTrue(
            source.contains(
                "HyperOsFlutterFontHookInstaller.installTypefaceProbe(xposed, packageName, store)"
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun nativeHookInstallerRequiresEnabledFontMode() {
        val source: String =
            readSource("src/main/java/com/dpis/module/runtime/font/HyperOsFlutterFontHookInstaller.java")

        Assert.assertTrue(source.contains("store.getTargetFontApplyMode(packageName)"))
        Assert.assertTrue(source.contains("FontApplyMode.isEnabled("))
        Assert.assertFalse(source.contains("store == null || !store.isHyperOsFlutterFontHookEnabled()"))
    }

    @Test
    @Throws(Exception::class)
    fun nativeProxyRefreshRequiresFlutterMasterSwitch() {
        val source: String =
            readSource("src/main/java/com/dpis/module/runtime/hyperos/HyperOsNativeProxyRefreshCoordinator.java")

        Assert.assertTrue(source.contains("!store.isFlutterFontHookEnabled()"))
        Assert.assertTrue(source.contains("!store.isHyperOsFlutterFontHookEnabled()"))
    }

    @Test
    @Throws(Exception::class)
    fun nativeHookInstallerProbesGenericFlutterLibraryLoads() {
        val source: String =
            readSource("src/main/java/com/dpis/module/runtime/font/HyperOsFlutterFontHookInstaller.java")
        val nativeSource: String = readSource("src/main/cpp/dpis_native.cpp")
        val moduleMain: String = readSource("src/modern/java/com/dpis/module/ModuleMain.java")
        val appProcessInstaller: String =
            readSource("src/main/java/com/dpis/module/runtime/appprocess/AppProcessHookInstaller.kt")

        Assert.assertFalse(moduleMain.contains("HyperOsFlutterFontHookInstaller.install("))
        Assert.assertTrue(moduleMain.contains("packagePlan.hyperOsNativeFlutterFontEnabled"))
        Assert.assertTrue(appProcessInstaller.contains("fontDomainPlan.hyperOsNativeFlutterEnabled"))
        Assert.assertTrue(appProcessInstaller.contains("HyperOsFlutterFontHookInstaller.install(xposed, packageName, store)"))
        Assert.assertTrue(source.contains("installRuntimeLibraryProbe(xposed, packageName)"))
        Assert.assertTrue(source.contains("installFlutterViewAttachProbe(xposed, packageName)"))
        Assert.assertTrue(source.contains("private static boolean probesEnabled()"))
        Assert.assertTrue(source.contains("BuildConfig.DEBUG || DpisLog.isLoggingEnabled()"))
        Assert.assertTrue(source.contains("installDebugOnlyProbes(xposed, packageName)"))
        Assert.assertTrue(source.contains("if (!probesEnabled())"))
        Assert.assertTrue(source.contains("installActivityResumeProbe(xposed, packageName)"))
        Assert.assertTrue(source.contains("installFrameProbe(xposed, packageName)"))
        Assert.assertTrue(source.contains("installViewRootTraversalProbe(xposed, packageName)"))
        Assert.assertTrue(source.contains("installHandlerDispatchProbe(xposed, packageName)"))
        Assert.assertTrue(source.contains("installAssetManagerProbe(xposed, packageName)"))
        Assert.assertTrue(source.contains("Flutter Java asset open observed"))
        Assert.assertTrue(source.contains("hookAssetManagerMethod"))
        Assert.assertTrue(source.contains("\"openFd\""))
        Assert.assertTrue(source.contains("\"openNonAsset\""))
        Assert.assertTrue(source.contains("\"openNonAssetFd\""))
        Assert.assertTrue(source.contains("AssetManager.class.getDeclaredMethod"))
        Assert.assertTrue(source.contains("\"handler-\" + remaining"))
        Assert.assertTrue(source.contains("Handler dispatch Flutter probe ready"))
        Assert.assertTrue(source.contains("\"view-root-\" + remaining"))
        Assert.assertTrue(source.contains("ViewRoot traversal Flutter probe ready"))
        Assert.assertTrue(source.contains("\"frame-\" + remaining"))
        Assert.assertTrue(source.contains("Choreographer frame Flutter probe ready"))
        Assert.assertTrue(source.contains("\"activity-resume\""))
        Assert.assertTrue(source.contains("\"loadLibrary0\""))
        Assert.assertTrue(source.contains("\"load0\""))
        Assert.assertTrue(source.contains("onRuntimeLibraryLoaded(packageName, loadedName)"))
        Assert.assertTrue(source.contains("genericFlutterProbeStatus(packageName, source)"))
        Assert.assertTrue(source.contains("PublishedFontFileResolver.resolve(typefaceId)"))
        Assert.assertTrue(source.contains("loadNativeLibrary();"))
        Assert.assertTrue(source.contains("configureTypeface("))
        Assert.assertTrue(source.contains("publishedTypeface.getAbsolutePath()"))
        Assert.assertTrue(source.contains("TYPEFACE_ASSET_REPLACEMENT_READY"))
        Assert.assertTrue(source.contains("logGenericFlutterProbe(packageName, \"post-configure-\" + reason)"))
        Assert.assertTrue(source.contains("scheduleDelayedGenericFlutterProbe(packageName)"))
        Assert.assertTrue(source.contains("if (probesEnabled())"))
        Assert.assertTrue(source.contains("scheduleMainThreadGenericFlutterProbe(packageName)"))
        Assert.assertTrue(source.contains("scheduleOneShotThreadGenericFlutterProbe(packageName)"))
        Assert.assertTrue(source.contains("scheduleLateMapsProbe(packageName)"))
        Assert.assertTrue(source.contains("logGenericFlutterProbe(packageName, \"post-install-\" + reason)"))
        Assert.assertTrue(source.contains("\"delayed-\" + delay + \"ms\""))
        Assert.assertTrue(source.contains("\"main-delayed-\" + delay + \"ms\""))
        Assert.assertTrue(source.contains("\"thread-delayed-8000ms\""))
        Assert.assertTrue(source.contains("DPIS_FONT Flutter late maps probe"))
        Assert.assertTrue(source.contains("findMappedLibraryBaseForTest(\"libapp.so\")"))
        Assert.assertTrue(source.contains("\"flutter-view-attached \" + view.getClass().getName()"))
        Assert.assertTrue(source.contains("findMappedLibraryBaseForTest(\"libflutter.so\")"))
        Assert.assertTrue(source.contains("javaMapsBase="))
        Assert.assertTrue(source.contains("isFlutterLibraryNameForTest"))
        Assert.assertTrue(nativeSource.contains("kGenericFlutterLibrary = \"libflutter.so\""))
        Assert.assertTrue(nativeSource.contains("kGenericFlutterAppLibrary = \"libapp.so\""))
        Assert.assertTrue(nativeSource.contains("Generic Flutter font probe: process="))
        Assert.assertTrue(nativeSource.contains("Generic Flutter poll thread start result="))
        Assert.assertTrue(nativeSource.contains("Generic Flutter status tick: process="))
        Assert.assertTrue(nativeSource.contains("bool is_debug_build()"))
        Assert.assertTrue(nativeSource.contains("if (!is_debug_build())"))
        Assert.assertTrue(nativeSource.contains("is_generic_flutter_font_hook_experiment_enabled()"))
        Assert.assertTrue(nativeSource.contains("DPIS_GENERIC_FLUTTER_FONT_HOOK"))
        Assert.assertTrue(nativeSource.contains("debug.dpis.generic_flutter_font_hook"))
        Assert.assertTrue(nativeSource.contains("schedule_generic_flutter_status();"))
        Assert.assertTrue(nativeSource.contains("!is_debug_build() && index >= 7"))
        Assert.assertTrue(nativeSource.contains("\"native-poll-%d\""))
        Assert.assertTrue(nativeSource.contains("Generic Flutter mapped: process="))
        Assert.assertTrue(nativeSource.contains("+ \" route=\""))
        Assert.assertFalse(nativeSource.contains("Last-resort generic Flutter route"))
        Assert.assertTrue(nativeSource.contains("GENERIC_PUSH_STYLE_D11"))
        Assert.assertFalse(nativeSource.contains("matches_verified_push_style_d11_window"))
        Assert.assertFalse(nativeSource.contains("VERIFIED_PUSH_STYLE_D11"))
        Assert.assertTrue(nativeSource.contains("g_generic_push_style_hooked.load(std::memory_order_acquire)"))
        Assert.assertFalse(nativeSource.contains("&& g_generic_create_hooked.load(std::memory_order_acquire)"))
        Assert.assertTrue(nativeSource.contains("inline_hook_arm64(push_target"))
        Assert.assertTrue(nativeSource.contains("kGenericParagraphBuilderPushStyleOffset = 0x82d470"))
        Assert.assertTrue(nativeSource.contains("Generic Flutter ParagraphBuilder::pushStyle hook result="))
        Assert.assertTrue(nativeSource.contains("Generic Flutter ParagraphBuilder::pushStyle fontSize override: process="))
        Assert.assertTrue(nativeSource.contains("\"status-probe \" + source"))
        Assert.assertTrue(nativeSource.contains("overrideCalls="))
        Assert.assertTrue(nativeSource.contains("createCalls="))
        Assert.assertTrue(nativeSource.contains("pushStyleCalls="))
        Assert.assertTrue(nativeSource.contains("lastInputMilli="))
        Assert.assertTrue(nativeSource.contains("g_generic_get_scaled_font_size_hooked"))
        Assert.assertTrue(nativeSource.contains("lastPollBase="))
        Assert.assertTrue(nativeSource.contains("Generic Flutter native poll: process="))
        Assert.assertTrue(nativeSource.contains("Flutter text string probe: process="))
        Assert.assertTrue(nativeSource.contains("library="))
        Assert.assertTrue(nativeSource.contains("bridge_log_info(\"DPIS_FONT \" + message)"))
        Assert.assertTrue(nativeSource.contains("GetStaticMethodID("))
        Assert.assertTrue(
            nativeSource.contains(
                "Java_com_dpis_module_runtime_font_HyperOsFlutterFontHookInstaller_genericFlutterProbeStatus"
            )
        )
        Assert.assertTrue(
            nativeSource.contains(
                "Java_com_dpis_module_runtime_font_HyperOsFlutterFontHookInstaller_configureTypeface"
            )
        )
        Assert.assertTrue(nativeSource.contains("AAssetManager_open"))
        Assert.assertTrue(nativeSource.contains("dlopen(\"libandroid.so\""))
        Assert.assertTrue(nativeSource.contains("Flutter typeface asset replacement hit"))
        Assert.assertTrue(nativeSource.contains("g_asset_hooks_ready"))
        Assert.assertTrue(nativeSource.contains("ready="))
        Assert.assertTrue(nativeSource.contains("std::fopen(\"/proc/self/maps\", \"r\")"))
        Assert.assertTrue(nativeSource.contains("detected-not-hooked"))
        Assert.assertTrue(nativeSource.contains("push-style-d11-hooked"))
        Assert.assertFalse(nativeSource.contains("Generic Flutter GetScaledFontSize hook result="))
        Assert.assertFalse(nativeSource.contains("\"Generic Flutter ParagraphBuilder::Create hook result=\""))
    }

    @Test
    fun genericFlutterNameDetectionCoversRuntimeAndPathForms() {
        Assert.assertTrue(HyperOsFlutterFontHookInstaller.isFlutterLibraryNameForTest("flutter"))
        Assert.assertTrue(HyperOsFlutterFontHookInstaller.isFlutterLibraryNameForTest("libflutter.so"))
        Assert.assertTrue(
            HyperOsFlutterFontHookInstaller.isFlutterLibraryNameForTest(
                "/data/app/example/lib/arm64/libflutter.so"
            )
        )
        Assert.assertTrue(
            HyperOsFlutterFontHookInstaller.isFlutterLibraryNameForTest(
                "libhyper_os_flutter.so"
            )
        )
        Assert.assertFalse(HyperOsFlutterFontHookInstaller.isFlutterLibraryNameForTest("webviewchromium"))
    }

    @Test
    fun flutterViewClassNameDetectionCoversFlutterEmbeddingAndPlugins() {
        Assert.assertTrue(
            HyperOsFlutterFontHookInstaller.isFlutterViewClassNameForTest(
                "io.flutter.embedding.android.FlutterView"
            )
        )
        Assert.assertTrue(
            HyperOsFlutterFontHookInstaller.isFlutterViewClassNameForTest(
                "com.pichillilorenzo.flutter_inappwebview_android.webview.InAppWebView"
            )
        )
        Assert.assertFalse(
            HyperOsFlutterFontHookInstaller.isFlutterViewClassNameForTest(
                "android.widget.TextView"
            )
        )
    }

    @Test
    fun genericFlutterProbeStatusParserReadsBaseAddress() {
        val status = ("Generic Flutter font probe: process=p package=p source=s"
                + " handle=0 base=123456 configured=1 enabled=1"
                + " targetFontScalePercent=300 status=detected-not-hooked")

        Assert.assertEquals(
            123456L,
            HyperOsFlutterFontHookInstaller.parseFlutterBaseForTest(status)
        )
        Assert.assertEquals(0L, HyperOsFlutterFontHookInstaller.parseFlutterBaseForTest("base=bad"))
        Assert.assertEquals(0L, HyperOsFlutterFontHookInstaller.parseFlutterBaseForTest(null))
    }

    @Test
    fun mapsStartAddressParserReadsExecutableMappingBase() {
        val line = ("6f60e60000-6f6189f000 r-xp 00000000 fe:4f 2284291"
                + " /data/app/example/lib/arm64/libflutter.so")

        Assert.assertEquals(
            0x6f60e60000L,
            HyperOsFlutterFontHookInstaller.parseMapsStartAddressForTest(line)
        )
        Assert.assertEquals(0L, HyperOsFlutterFontHookInstaller.parseMapsStartAddressForTest("bad"))
    }

    @Test
    fun nativeLoaderCanResolveExtractedModuleLibraryFromLsposedClassLoaderText() {
        val text = ("LspModuleClassLoader[module=/data/app/~~id==/"
                + "io.github.kwensiu.dpis-abcd==/base.apk, nativeLibraryDirectories=[]]")

        Assert.assertEquals(
            "/data/app/~~id==/io.github.kwensiu.dpis-abcd==/base.apk",
            HyperOsFlutterFontHookInstaller.parseModuleApkPathForTest(text)
        )
        Assert.assertEquals(
            "arm64",
            HyperOsFlutterFontHookInstaller.nativeDirectoryNamesForAbi("arm64-v8a")[0]
        )
        Assert.assertEquals(
            "arm",
            HyperOsFlutterFontHookInstaller.nativeDirectoryNamesForAbi("armeabi-v7a")[0]
        )
        Assert.assertEquals(
            "x86",
            HyperOsFlutterFontHookInstaller.nativeDirectoryNamesForAbi("x86")[0]
        )
    }

    @Test
    @Throws(Exception::class)
    fun nativeForceFontPropertyCanOverrideJniConfiguration() {
        val source: String = readSource("src/main/cpp/dpis_native.cpp")

        Assert.assertFalse(source.contains("if (g_configured_from_jni.load(std::memory_order_relaxed)) {\n        return;"))
        Assert.assertTrue(
            source.indexOf("debug.dpis.forcefont.%08x")
                    < source.indexOf("DPIS_FONT_SCALE_PERCENT")
        )
        Assert.assertTrue(
            source.indexOf("debug.dpis.forcefont")
                    < source.indexOf("debug.dpis.font.%08x")
        )
        Assert.assertTrue(source.contains("HyperOS font config source: process="))
    }

    @Test
    @Throws(Exception::class)
    fun nativeCreateHookScalesWeatherCreateFontRegisters() {
        val source: String = readSource("src/main/cpp/dpis_native.cpp")

        val createScale = source.indexOf("bl dpis_create_scaled_d0")
        val createTrampoline = source.substring(max(0, createScale - 300), createScale + 180)
        Assert.assertTrue(source.contains("double create_observed_scale(double observed_scale)"))
        Assert.assertTrue(source.contains("return 1.0;"))
        Assert.assertTrue(createTrampoline.contains("ldr d0, [sp, #80]"))
        Assert.assertTrue(createTrampoline.contains("bl dpis_create_scaled_d0"))
        Assert.assertTrue(createTrampoline.contains("fmul d0, d0, d1"))
    }

    @Test
    @Throws(Exception::class)
    fun nativeProxyLoadsOriginalRustBinaryFromEnvironmentBeforeProperty() {
        val source: String = readSource("src/main/cpp/dpis_native.cpp")

        val envRead = source.indexOf("read_environment(\"DPIS_RUST_BINARY\")")
        val propertyRead = source.indexOf("debug.dpis.rustbin.%08x")
        Assert.assertTrue(envRead >= 0)
        Assert.assertTrue(propertyRead >= 0)
        Assert.assertTrue(envRead < propertyRead)
    }

    @Test
    @Throws(Exception::class)
    fun weatherNativeProxyFallsBackToSiblingOriginalLibrary() {
        val source: String = readSource("src/main/cpp/dpis_native.cpp")

        Assert.assertTrue(source.contains("sibling_original_rust_binary_path()"))
        Assert.assertTrue(source.contains("uses_configuration_got_route()"))
        Assert.assertTrue(source.contains("DPIS_NATIVE_ROUTE"))
        Assert.assertTrue(source.contains("CONFIGURATION_GOT"))
        Assert.assertTrue(source.contains("libweather_app.so"))
        Assert.assertTrue(source.contains("path == \"0\""))
    }

    @Test
    @Throws(Exception::class)
    fun weatherGotHookValidatesOriginalSlotBeforePatching() {
        val source: String = readSource("src/main/cpp/dpis_native.cpp")

        Assert.assertTrue(source.contains("is_weather_configuration_font_scale_slot"))
        Assert.assertTrue(source.contains("GOT hook skipped: unexpected slot"))
        Assert.assertTrue(source.contains("describe_symbol(*slot)"))
        Assert.assertTrue(source.contains("kHyperOsAppPublicLibrary"))
        Assert.assertTrue(source.contains("ends_with(info.dli_fname, kHyperOsAppPublicLibrary)"))
        Assert.assertTrue(
            source.indexOf("is_weather_configuration_font_scale_slot(*slot)")
                    < source.indexOf("*slot = reinterpret_cast<void *>(Configuration_get_font_scale)")
        )
    }

    @Test
    @Throws(Exception::class)
    fun rustProcessProxyRejectsEmptySiblingPlaceholder() {
        val dir = Files.createTempDirectory("dpis-rust-proxy").toFile()
        val original = File(dir, "libweather_app.so")
        val proxy = File(dir, "libdpis_native.so")
        Assert.assertTrue(original.createNewFile())
        Assert.assertTrue(proxy.createNewFile())

        val proxyPath = HyperOsRustProcessHookInstaller.resolveProxyLibraryPathForTest(
            original.absolutePath
        )

        Assert.assertNull(proxyPath)
    }

    companion object {
        @Throws(Exception::class)
        private fun readSource(relativePath: String?): String {
            return SourceSmokeTestPaths.read(relativePath)
        }
    }
}
