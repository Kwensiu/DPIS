package com.dpis.module;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;


import org.junit.Test;

public class Compat100LegacyModuleHookSourceTest {
    @Test
    public void legacyEntryInstallsReplacementHooks() throws Exception {
        String source = read("src/compat100/java/com/dpis/module/Compat100LegacyModuleHook.java");

        assertTrue(source.contains("installDisplayHooks(packageName, store)"));
        assertTrue(source.contains("installWindowMetricsHook()"));
        assertTrue(source.contains("installFontFieldRewriteHooks(packageName, store)"));
        assertTrue(source.contains("DisplayHookInstaller.applyDisplayMetrics"));
        assertTrue(source.contains("DisplayHookInstaller.applyPoint"));
        assertTrue(source.contains("DisplayHookInstaller.applyDisplayInfo"));
        assertTrue(source.contains("DisplayHookInstaller.setTargetPackageNameForCompat100(packageName)"));
        assertTrue(source.contains("DisplayHookInstaller.setTargetStoreForCompat100(store)"));
        assertTrue(source.contains("FONT_TEXTVIEW_UPDATE"));
        assertTrue(source.contains("installResourcesReadHooks(packageName, store)"));
        assertTrue(source.contains("installResourcesKeyHooks("));
        assertTrue(source.contains("ResourcesManagerHookInstaller.maybeApplyKeyOverride"));
        assertTrue(source.contains("\"createResourcesImpl\".equals(methodName)"));
        assertTrue(source.contains("\"android.content.res.ResourcesKey\".equals"));
        assertTrue(source.contains("installTypefaceOverrideHook(packageName, plan.targetTypefaceId, store)"));
        assertTrue(source.contains("Compat100TypefaceOverrideHookInstaller.install("));
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
        assertTrue(source.contains("ConfigStoreFactory.createForCompat100Host(packageName)"));
        assertTrue(source.contains("FONT_FIELD_REWRITE_HOOKED.set(false);"));
        assertTrue(source.contains("if (Boolean.TRUE.equals(FONT_TEXTVIEW_UPDATE.get()))"));
        assertTrue(source.contains("Android's one-argument TextView#setTextSize delegates"));
        assertTrue(source.contains("getDeclaredMethod(\"getConfiguration\")"));
        assertTrue(source.contains("getDeclaredMethod(\"getDisplayMetrics\")"));
        assertTrue(source.contains("getDeclaredMethod(\"getSystem\")"));
        assertTrue(source.contains("SystemServerProcess.isSystemServer"));
        assertTrue(source.contains("installSystemServerHooksForCompat100()"));
        assertTrue(source.contains("Compat100SystemServerHookInstaller.install"));
        assertTrue(source.indexOf("DisplayHookInstaller.setTargetPackageNameForCompat100(packageName)")
                < source.indexOf("DISPLAY_HOOKED.compareAndSet(false, true)"));
        assertTrue(source.indexOf("DisplayHookInstaller.setTargetStoreForCompat100(store)")
                < source.indexOf("DISPLAY_HOOKED.compareAndSet(false, true)"));
        assertTrue(source.contains("implements IXposedHookLoadPackage, IXposedHookZygoteInit"));
        assertTrue(source.contains("public void initZygote(StartupParam startupParam)"));
        assertTrue(source.contains("installSystemServerHooksForCompat100();"));
        assertTrue(source.contains("createCompat100Store(packageName, lpparam.processName)"));
        assertTrue(source.contains("createForCompat100MainProcessHost(packageName)"));
        assertTrue(source.contains("Compat100AppSpecificRouteInstaller.handleLoadPackage(lpparam)"));
        assertFalse(source.contains("com.tencent.mm"));
        assertFalse(source.contains("WECHAT_PACKAGE"));
        assertFalse(source.contains("screenResolution_target_field"));
        assertFalse(source.contains("WechatDpiRoutes.forVersionCode"));
        assertTrue(source.contains("shouldSuppressSecondaryProcessViewport(lpparam.processName, plan)"));
        assertTrue(source.contains("compat100 legacy secondary process viewport route suppressed"));
        assertTrue(source.contains("!processName.startsWith(plan.packageName + \":\")"));
        assertTrue(source.contains("plan.withoutViewportRoute()"));
        assertTrue(source.contains("package skipped after secondary process"));

        String typefaceSource = read(
                "src/compat100/java/com/dpis/module/Compat100TypefaceOverrideHookInstaller.java");
        assertTrue(typefaceSource.contains("XposedBridge.hookMethod(setTypeface"));
        assertTrue(typefaceSource.contains("XposedBridge.hookMethod(setTypefaceWithStyle"));
        assertTrue(typefaceSource.contains("XposedBridge.hookMethod(paintSetTypeface"));
        assertTrue(typefaceSource.contains("DPIS_FONT_STYLE "));
        assertTrue(typefaceSource.contains("FontLibraryStore fontLibraryStore"));
        assertTrue(typefaceSource.contains("PublishedFontFileResolver.resolve(typefaceId)"));

        String systemServerSource = read("src/compat100/java/com/dpis/module/Compat100SystemServerHookInstaller.java");
        assertTrue(systemServerSource.contains("android.app.servertransaction.LaunchActivityItem"));
        assertTrue(systemServerSource.contains("applyLaunchActivityItemArgs"));
        assertTrue(systemServerSource.contains(
                "PerAppDisplayConfigSource.withCompat100RuntimePropertyFallback"));
        assertTrue(systemServerSource.contains("createForCompat100SystemServerHost"));
        assertTrue(systemServerSource.contains("applyLaunchActivityItemArgs(source, param.args)"));
        assertTrue(systemServerSource.contains("PerAppDisplayOverrideCalculator.calculate"));
        assertTrue(systemServerSource.contains("config.targetViewportSpec"));
        assertFalse(systemServerSource.contains("config.targetViewportWidthDp()"));
        assertTrue(systemServerSource.contains("ViewportRuntimeMarkerBridge.publishSystemServerRecord"));
        assertTrue(systemServerSource.contains("ViewportRuntimeMarkerBridge.read"));
        assertTrue(systemServerSource.contains("matchesCurrentConfiguration"));
        assertTrue(systemServerSource.contains("ViewportOverride.apply"));
        assertTrue(systemServerSource.contains("FontApplyMode.SYSTEM_EMULATION"));
        assertTrue(systemServerSource.contains("Compat100RustProcessHookInstaller.install(source)"));
        int launchApplyIndex = systemServerSource.indexOf("static void applyLaunchActivityItemArgs");
        int afterLaunchApplyIndex = systemServerSource.indexOf(
                "private static String findActivityInfoPackage", launchApplyIndex);
        assertTrue(launchApplyIndex > 0);
        assertTrue(afterLaunchApplyIndex > launchApplyIndex);
        String launchApplyMethod = systemServerSource.substring(
                launchApplyIndex, afterLaunchApplyIndex);
        assertTrue(launchApplyMethod.contains(
                "resolveTargetEnvironment(packageName, baseConfiguration, config)"));
        assertFalse(launchApplyMethod.contains("applyConfiguration(configuration, environment)"));

        String compatRustSource = read("src/compat100/java/com/dpis/module/Compat100RustProcessHookInstaller.java");
        assertTrue(compatRustSource.contains("XposedBridge.hookMethod(method"));
        assertTrue(compatRustSource.contains("param.args = updatedArgs"));
        String rustSource = read("src/main/java/com/dpis/module/HyperOsRustProcessHookInstaller.java");
        assertTrue(rustSource.contains("applyEnvironmentArgsForLegacy"));
        assertTrue(rustSource.indexOf("return null;")
                < rustSource.indexOf("Object existingValue = args.get(ARG_ENVIRONMENTS);"));
        assertTrue(!rustSource.contains("HyperOsFlutterFontBridge.clearTarget(packageName);"));
    }

    @Test
    public void compat100AppSpecificRouteInstallerOwnsWechatDpiRoute() throws Exception {
        String router = read("src/compat100/java/com/dpis/module/Compat100AppSpecificRouteInstaller.java");
        String installer = read(
                "src/compat100/java/com/dpis/module/WechatDpiCompat100HookInstaller.java");

        assertTrue(router.contains("WechatDpiConfig.appliesTo(lpparam.packageName)"));
        assertTrue(router.contains("WechatDpiConfig.appliesTo(lpparam.processName)"));
        assertTrue(router.contains("WechatDpiCompat100HookInstaller.install(lpparam)"));
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
