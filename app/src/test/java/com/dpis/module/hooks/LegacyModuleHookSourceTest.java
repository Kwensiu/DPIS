package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LegacyModuleHookSourceTest {
    @Test
    public void legacyEntryInstallsReplacementHooks() throws Exception {
        String source = read("src/legacy/java/com/dpis/module/LegacyModuleHook.kt");
        String selfActivation = read(
                "src/legacy/java/com/dpis/module/LegacyXposedSelfActivation.java");
        String proguard = SourceSmokeTestPaths.readRepositoryRoot("app/proguard-rules.pro");

        assertTrue(source.contains("Legacy APK uses the traditional Xposed entrypoint"));
        assertTrue(source.contains("LegacyXposedSelfActivation.markIfSelfPackage("));
        assertTrue(source.contains("\"legacy-handle-load-package\""));
        assertTrue(selfActivation.contains("final class LegacyXposedSelfActivation"));
        assertTrue(selfActivation.contains("XposedSelfActivation.markIfSelfPackage("));
        assertTrue(selfActivation.contains("xposedSelfLoadedByLegacyConstructorHook"));
        assertTrue(proguard.contains("static void markXposedSelfLoaded();"));
        assertTrue(proguard.contains("boolean xposedSelfLoadedByLegacyConstructorHook;"));
        assertTrue(source.contains("installDisplayHooks(packageName, store)"));
        assertTrue(source.contains("installWindowMetricsHook()"));
        assertTrue(source.contains("installFontFieldRewriteHooks("));
        assertTrue(source.contains("DisplayHookInstaller.applyDisplayMetrics"));
        assertTrue(source.contains("DisplayHookInstaller.applyPoint"));
        assertTrue(source.contains("DisplayHookInstaller.applyDisplayInfo"));
        assertTrue(source.contains("DisplayHookInstaller.setTargetPackageNameForLegacy(packageName)"));
        assertTrue(source.contains("DisplayHookInstaller.setTargetStoreForLegacy(store)"));
        assertTrue(source.contains("FONT_TEXTVIEW_UPDATE"));
        assertTrue(source.contains("installResourcesReadHooks(packageName, store)"));
        assertTrue(source.contains("installResourcesKeyHooks("));
        assertTrue(source.contains("ResourcesManagerHookInstaller.maybeApplyKeyOverride"));
        assertTrue(source.contains("\"createResourcesImpl\".equals(methodName)"));
        assertTrue(source.contains("\"android.content.res.ResourcesKey\" == parameterTypes[0]?.name"));
        assertTrue(source.contains("installTypefaceOverrideHook("));
        assertTrue(source.contains("LegacyTypefaceOverrideHookInstaller.install("));
        assertTrue(source.contains("plan.typefaceEnabled"));
        assertTrue(source.contains("val resourceHooksNeeded = plan.viewportEnabled"));
        assertTrue(source.contains("if (resourceHooksNeeded)"));
        assertTrue(source.contains("val activityHookCount = 0"));
        assertTrue(source.contains("val createHookCount = 0"));
        assertTrue(source.contains("val keyHookCount = 0"));
        assertTrue(source.indexOf("if (resourceHooksNeeded)")
                < source.indexOf("installResourcesImplHook(packageName, store)"));
        assertTrue(source.indexOf("installResourcesReadHooks(packageName, store)")
                < source.indexOf("if (plan.typefaceEnabled)"));
        assertTrue(source.contains("ResourcesReadHookInstaller.applyConfigurationOverride"));
        assertTrue(source.contains("ResourcesReadHookInstaller.applyMetricsOverride"));
        assertTrue(source.contains("resolveActivePackageName("));
        assertTrue(source.contains("resolveStoreForPackage("));
        assertTrue(source.contains("LegacyConfigStoreFactory.create(packageName)"));
        assertTrue(source.contains("LegacyConfigStoreFactory::create"));
        assertTrue(source.contains("FONT_FIELD_REWRITE_HOOKED.set(false)"));
        assertTrue(source.contains("FONT_TEXTVIEW_UPDATE.get() == true"));
        assertTrue(source.contains("Android's one-argument TextView#setTextSize delegates"));
        assertTrue(source.contains("getDeclaredMethod(\"getConfiguration\")"));
        assertTrue(source.contains("getDeclaredMethod(\"getDisplayMetrics\")"));
        assertTrue(source.contains("getDeclaredMethod(\"getSystem\")"));
        assertTrue(source.contains("SystemServerProcess.isSystemServer"));
        assertTrue(source.contains("installSystemServerHooksForLegacy()"));
        assertTrue(source.contains("LegacySystemServerHookInstaller.install"));
        assertTrue(source.indexOf("DisplayHookInstaller.setTargetPackageNameForLegacy(")
                < source.indexOf("DISPLAY_HOOKED.compareAndSet("));
        assertTrue(source.indexOf("DisplayHookInstaller.setTargetStoreForLegacy(")
                < source.indexOf("DISPLAY_HOOKED.compareAndSet("));
        assertTrue(source.contains(": IXposedHookLoadPackage, IXposedHookZygoteInit"));
        assertTrue(source.contains("override fun initZygote("));
        assertTrue(source.contains("installSystemServerHooksForLegacy()"));
        assertTrue(source.contains("createLegacyStore("));
        assertTrue(source.contains("LegacyConfigStoreFactory.createMainProcess(packageName)"));
        assertTrue(source.contains("LegacyAppSpecificRouteInstaller.handleLoadPackage(lpparam)"));
        assertFalse(source.contains("com.tencent.mm"));
        assertFalse(source.contains("WECHAT_PACKAGE"));
        assertFalse(source.contains("screenResolution_target_field"));
        assertFalse(source.contains("WechatDpiRoutes.forVersionCode"));
        assertTrue(source.contains("shouldSuppressSecondaryProcessViewport("));
        assertTrue(source.contains("legacy secondary process viewport route suppressed"));
        assertTrue(source.contains("!processName.startsWith(plan.packageName + \":\")"));
        assertTrue(source.contains("plan.withoutViewportRoute()"));
        assertTrue(source.contains("package skipped after secondary process"));

        String typefaceSource = read(
                "src/legacy/java/com/dpis/module/LegacyTypefaceOverrideHookInstaller.kt");
        assertTrue(typefaceSource.contains("XposedBridge.hookMethod(setTypeface"));
        assertTrue(typefaceSource.contains("XposedBridge.hookMethod(setTypefaceWithStyle"));
        assertTrue(typefaceSource.contains("XposedBridge.hookMethod(paintSetTypeface"));
        assertTrue(typefaceSource.contains("DPIS_FONT_STYLE "));
        assertTrue(typefaceSource.contains("fontLibraryStore: FontLibraryStore?"));
        assertTrue(typefaceSource.contains("PublishedFontFileResolver.resolve(typefaceId)"));
        assertTrue(typefaceSource.contains("FontProviderTypefaceLoader.load(typefaceId, ttcIndex)"));
        assertTrue(typefaceSource.contains("FontTypefaceLoader.load(file, ttcIndex)"));
        assertTrue(typefaceSource.contains("RuntimeEvents.recordTypeface("));
        assertTrue(typefaceSource.contains("\"source_provider_loaded\""));
        assertTrue(typefaceSource.contains("\"source_fallback_loaded\""));
        assertTrue(typefaceSource.contains("\"hook_installed\""));
        assertTrue(typefaceSource.contains("\"replacement_hit\""));
        assertTrue(typefaceSource.contains("\"load_failed\""));

        String systemServerSource = read("src/legacy/java/com/dpis/module/LegacySystemServerHookInstaller.java");
        assertTrue(systemServerSource.contains("android.app.servertransaction.LaunchActivityItem"));
        assertTrue(systemServerSource.contains("applyLaunchActivityItemArgs"));
        assertTrue(systemServerSource.contains(
                "PerAppDisplayConfigSource.withLegacyRuntimePropertyFallback"));
        assertTrue(systemServerSource.contains("LegacyConfigStoreFactory.createSystemServer"));
        assertTrue(systemServerSource.contains(
                "legacy system_server install enter: hooksEnabled="));
        assertTrue(systemServerSource.contains(
                "legacy system_server launch-activity-item skipped: reason=hooks-disabled"));
        assertTrue(systemServerSource.contains(
                "legacy system_server launch-activity-item skipped: reason=package-unresolved"));
        assertTrue(systemServerSource.contains(
                "legacy system_server launch-activity-item skipped: package="));
        assertTrue(systemServerSource.contains("applyLaunchActivityItemArgs(source, param.args)"));
        assertTrue(systemServerSource.contains("PerAppDisplayOverrideCalculator.calculate"));
        assertTrue(systemServerSource.contains("config.targetViewportSpec"));
        assertFalse(systemServerSource.contains("config.targetViewportWidthDp()"));
        assertTrue(systemServerSource.contains("ViewportRuntimeMarkerBridge.publishSystemServerRecord"));
        assertTrue(systemServerSource.contains("ViewportRuntimeMarkerBridge.read"));
        assertTrue(systemServerSource.contains("matchesCurrentConfiguration"));
        assertTrue(systemServerSource.contains(
                "legacy system_server launch-activity-item marker state: package="));
        assertTrue(systemServerSource.contains(
                "legacy system_server launch-activity-item marker publish: package="));
        assertTrue(systemServerSource.contains(
                "legacy system_server launch-activity-item marker reuse: package="));
        assertTrue(systemServerSource.contains("ViewportOverride.apply"));
        assertTrue(systemServerSource.contains("applyLaunchActivityItemObject(source, param.thisObject)"));
        assertTrue(systemServerSource.contains("readLaunchActivityInfo"));
        assertTrue(systemServerSource.contains("readLaunchActivityConfiguration"));
        assertTrue(systemServerSource.contains("applyConfigurationField(launchActivityItem, \"mCurConfig\", environment)"));
        assertTrue(systemServerSource.contains("applyConfigurationField(launchActivityItem, \"mOverrideConfig\", environment)"));
        assertTrue(systemServerSource.contains(
                "legacy system_server launch-activity-item object apply: package="));
        assertTrue(systemServerSource.contains("FontApplyMode.SYSTEM_EMULATION"));
        assertTrue(systemServerSource.contains("LegacyRustProcessHookInstaller.install(source)"));
        assertTrue(systemServerSource.contains("findPackageNameRecursive(arg, 0)"));
        assertTrue(systemServerSource.contains("getPackageName"));
        assertTrue(systemServerSource.contains("getIntent"));
        assertTrue(systemServerSource.contains("extractPackageFromText(String.valueOf(target))"));
        int launchApplyIndex = systemServerSource.indexOf("static void applyLaunchActivityItemArgs");
        int afterLaunchApplyIndex = systemServerSource.indexOf(
                "private static String findActivityInfoPackage", launchApplyIndex);
        assertTrue(launchApplyIndex > 0);
        assertTrue(afterLaunchApplyIndex > launchApplyIndex);
        String launchApplyMethod = systemServerSource.substring(
                launchApplyIndex, afterLaunchApplyIndex);
        assertTrue(launchApplyMethod.contains(
                "resolveTargetEnvironment(packageName, baseConfiguration, config)"));
        assertTrue(launchApplyMethod.contains("applyConfiguration(configuration, environment)"));

        String compatRustSource = read("src/legacy/java/com/dpis/module/LegacyRustProcessHookInstaller.java");
        assertTrue(compatRustSource.contains("XposedBridge.hookMethod(method"));
        assertTrue(compatRustSource.contains("param.args = updatedArgs"));
        String rustSource = read("src/main/java/com/dpis/module/runtime/systemserver/HyperOsRustProcessHookInstaller.java");
        assertTrue(rustSource.contains("applyEnvironmentArgsForLegacy"));
        assertTrue(rustSource.indexOf("return null;")
                < rustSource.indexOf("Object existingValue = args.get(ARG_ENVIRONMENTS);"));
        assertFalse(rustSource.contains("HyperOsFlutterFontBridge.clearTarget(packageName);"));

        String resourcesReadSource = read("src/main/java/com/dpis/module/runtime/appprocess/ResourcesReadHookInstaller.kt");
        assertTrue(resourcesReadSource.contains("DPIS_VIEWPORT legacy auto fallback success: package="));
        assertTrue(resourcesReadSource.contains("sourceTag.startsWith(\"LegacyResourcesRead(\")"));
        assertTrue(resourcesReadSource.contains("ViewportApplyMode.AUTO !="));
    }

    @Test
    public void legacyAppSpecificRouteInstallerOwnsWechatDpiRoute() throws Exception {
        String router = read("src/legacy/java/com/dpis/module/LegacyAppSpecificRouteInstaller.java");
        String installer = read(
                "src/legacy/java/com/dpis/module/WechatDpiLegacyHookInstaller.java");

        assertTrue(router.contains("WechatDpiConfig.appliesTo(lpparam.packageName)"));
        assertTrue(router.contains("WechatDpiConfig.appliesTo(lpparam.processName)"));
        assertTrue(router.contains("WechatDpiLegacyHookInstaller.install(lpparam)"));
        assertTrue(router.contains("alongside generic hooks"));
        assertTrue(installer.contains("WechatDpiMethodLocator.locate("));
        assertTrue(installer.contains("WechatDpiRuntime.apply(metrics, dpi)"));
        assertTrue(installer.contains("locatorResult.source.logName"));
        assertFalse(installer.contains("WechatDpiRoutes.forVersionCode(versionCode)"));
        assertFalse(installer.contains("findDisplayMetricsMethods(densityManagerClass)"));
        assertTrue(installer.contains("XposedBridge.hookMethod(metricsMethod"));
        assertTrue(installer.contains("applyWechatDpi(metrics"));
        assertFalse(installer.contains("XposedBridge.hookMethod(targetGetter"));
        assertFalse(installer.contains("installSetterHook("));
        assertFalse(installer.contains("XposedBridge.hookAllConstructors"));
        assertTrue(installer.contains("afterHookedMethod"));
    }

    private static String read(String relativePath) throws Exception {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
