package com.dpis.module;

import com.dpis.module.quirks.WechatDpiMethodLocator;
import com.dpis.module.quirks.WechatDpiRoutes;
import com.dpis.module.quirks.WechatDpiRuntime;

import com.dpis.module.appconfig.WechatDpiConfig;

import com.dpis.module.fonts.PublishedFontFileResolver;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;


import org.junit.Test;

public class LegacyModuleHookSourceTest {
    @Test
    public void legacyEntryInstallsReplacementHooks() throws Exception {
        String source = read("src/legacy/java/com/dpis/module/LegacyModuleHook.java");
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
        assertTrue(source.contains("installFontFieldRewriteHooks(packageName, store)"));
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
        assertTrue(source.contains("\"android.content.res.ResourcesKey\".equals"));
        assertTrue(source.contains("installTypefaceOverrideHook(packageName, plan.targetTypefaceId, store)"));
        assertTrue(source.contains("LegacyTypefaceOverrideHookInstaller.install("));
        assertTrue(source.contains("plan.typefaceEnabled"));
        assertTrue(source.contains("boolean resourceHooksNeeded = plan.viewportEnabled"));
        assertTrue(source.contains("if (resourceHooksNeeded)"));
        assertTrue(source.contains("int activityHookCount = 0;"));
        assertTrue(source.contains("int createHookCount = 0;"));
        assertTrue(source.contains("int keyHookCount = 0;"));
        assertTrue(source.indexOf("if (resourceHooksNeeded)")
                < source.indexOf("installResourcesImplHook(packageName, store)"));
        assertTrue(source.indexOf("installResourcesReadHooks(packageName, store)")
                < source.indexOf("if (plan.typefaceEnabled)"));
        assertTrue(source.contains("ResourcesReadHookInstaller.applyConfigurationOverride"));
        assertTrue(source.contains("ResourcesReadHookInstaller.applyMetricsOverride"));
        assertTrue(source.contains("resolveActivePackageName(packageName)"));
        assertTrue(source.contains("resolveStoreForPackage(activePackage, store)"));
        assertTrue(source.contains("ConfigStoreFactory.createForLegacyHost(packageName)"));
        assertTrue(source.contains("FONT_FIELD_REWRITE_HOOKED.set(false);"));
        assertTrue(source.contains("if (Boolean.TRUE.equals(FONT_TEXTVIEW_UPDATE.get()))"));
        assertTrue(source.contains("Android's one-argument TextView#setTextSize delegates"));
        assertTrue(source.contains("getDeclaredMethod(\"getConfiguration\")"));
        assertTrue(source.contains("getDeclaredMethod(\"getDisplayMetrics\")"));
        assertTrue(source.contains("getDeclaredMethod(\"getSystem\")"));
        assertTrue(source.contains("SystemServerProcess.isSystemServer"));
        assertTrue(source.contains("installSystemServerHooksForLegacy()"));
        assertTrue(source.contains("LegacySystemServerHookInstaller.install"));
        assertTrue(source.indexOf("DisplayHookInstaller.setTargetPackageNameForLegacy(packageName)")
                < source.indexOf("DISPLAY_HOOKED.compareAndSet(false, true)"));
        assertTrue(source.indexOf("DisplayHookInstaller.setTargetStoreForLegacy(store)")
                < source.indexOf("DISPLAY_HOOKED.compareAndSet(false, true)"));
        assertTrue(source.contains("implements IXposedHookLoadPackage, IXposedHookZygoteInit"));
        assertTrue(source.contains("public void initZygote(StartupParam startupParam)"));
        assertTrue(source.contains("installSystemServerHooksForLegacy();"));
        assertTrue(source.contains("createLegacyStore(packageName, lpparam.processName)"));
        assertTrue(source.contains("createForLegacyMainProcessHost(packageName)"));
        assertTrue(source.contains("LegacyAppSpecificRouteInstaller.handleLoadPackage(lpparam)"));
        assertFalse(source.contains("com.tencent.mm"));
        assertFalse(source.contains("WECHAT_PACKAGE"));
        assertFalse(source.contains("screenResolution_target_field"));
        assertFalse(source.contains("WechatDpiRoutes.forVersionCode"));
        assertTrue(source.contains("shouldSuppressSecondaryProcessViewport(lpparam.processName, plan)"));
        assertTrue(source.contains("legacy secondary process viewport route suppressed"));
        assertTrue(source.contains("!processName.startsWith(plan.packageName + \":\")"));
        assertTrue(source.contains("plan.withoutViewportRoute()"));
        assertTrue(source.contains("package skipped after secondary process"));

        String typefaceSource = read(
                "src/legacy/java/com/dpis/module/LegacyTypefaceOverrideHookInstaller.java");
        assertTrue(typefaceSource.contains("XposedBridge.hookMethod(setTypeface"));
        assertTrue(typefaceSource.contains("XposedBridge.hookMethod(setTypefaceWithStyle"));
        assertTrue(typefaceSource.contains("XposedBridge.hookMethod(paintSetTypeface"));
        assertTrue(typefaceSource.contains("DPIS_FONT_STYLE "));
        assertTrue(typefaceSource.contains("FontLibraryStore fontLibraryStore"));
        assertTrue(typefaceSource.contains("PublishedFontFileResolver.resolve(typefaceId)"));

        String systemServerSource = read("src/legacy/java/com/dpis/module/LegacySystemServerHookInstaller.java");
        assertTrue(systemServerSource.contains("android.app.servertransaction.LaunchActivityItem"));
        assertTrue(systemServerSource.contains("applyLaunchActivityItemArgs"));
        assertTrue(systemServerSource.contains(
                "PerAppDisplayConfigSource.withLegacyRuntimePropertyFallback"));
        assertTrue(systemServerSource.contains("createForLegacySystemServerHost"));
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
        String rustSource = read("src/main/java/com/dpis/module/HyperOsRustProcessHookInstaller.java");
        assertTrue(rustSource.contains("applyEnvironmentArgsForLegacy"));
        assertTrue(rustSource.indexOf("return null;")
                < rustSource.indexOf("Object existingValue = args.get(ARG_ENVIRONMENTS);"));
        assertTrue(!rustSource.contains("HyperOsFlutterFontBridge.clearTarget(packageName);"));

        String resourcesReadSource = read("src/main/java/com/dpis/module/ResourcesReadHookInstaller.java");
        assertTrue(resourcesReadSource.contains("DPIS_VIEWPORT legacy auto fallback success: package="));
        assertTrue(resourcesReadSource.contains("sourceTag.startsWith(\"LegacyResourcesRead(\")"));
        assertTrue(resourcesReadSource.contains("ViewportApplyMode.AUTO.equals("));
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
