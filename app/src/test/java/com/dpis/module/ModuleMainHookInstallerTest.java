package com.dpis.module;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ModuleMainHookInstallerTest {
    @Test
    public void moduleMainUsesExplicitSystemServerPolicyGuard() throws IOException {
        String source = read("src/modern/java/com/dpis/module/ModuleMain.java");

        assertTrue(source.contains("SystemServerMutationPolicy.shouldInstallSystemServerHooks("));
        assertTrue(source.contains("public void onSystemServerStarting(SystemServerStartingParam param)"));
        assertTrue(source.contains("system_server starting hook install enter"));
        assertTrue(source.contains("\"system-server-starting\""));
        assertTrue(source.contains("maybeInstallSystemServerHooks(store, policy, currentProcessName"));
        assertTrue(source.contains("system_server installer ready: source="));
        assertTrue(source.contains("ModulePackagePlan.resolve("));
        assertFalse(SourceSmokeTestPaths.exists("src", "main", "java", "com", "dpis", "module", "ModuleMain.java"));
    }

    @Test
    public void moduleMainDelegatesAppSpecificRoutes() throws IOException {
        String moduleMain = read("src/modern/java/com/dpis/module/ModuleMain.java");

        assertTrue(moduleMain.contains("ModernAppSpecificRouteInstaller.handlePackageReady("));
        assertTrue(moduleMain.contains(
                "ModernAppSpecificRouteInstaller.shouldSuppressModuleLoadedGenericHooks("));
        assertFalse(moduleMain.contains("WECHAT_PACKAGE"));
        assertFalse(moduleMain.contains("com.tencent.mm"));
        assertFalse(moduleMain.contains("WechatDpiModernHookInstaller.install("));
    }

    @Test
    public void modernModuleMainMarksSelfProcessForHomeActivation() throws IOException {
        String moduleMain = read("src/modern/java/com/dpis/module/ModuleMain.java");

        assertTrue(moduleMain.contains("XposedSelfActivation.markIfSelfPackage("));
        assertTrue(moduleMain.contains("param.getPackageName()"));
        assertTrue(moduleMain.contains("param.getClassLoader()"));
        assertTrue(moduleMain.contains("libxposed-package-ready"));
        assertFalse(moduleMain.contains("DpisApplication.markXposedSelfLoaded();"));
    }

    @Test
    public void modernAppSpecificRouteInstallerRoutesWechatDpi() throws IOException {
        String router = read(
                "src/modern/java/com/dpis/module/ModernAppSpecificRouteInstaller.java");
        String installer = read(
                "src/modern/java/com/dpis/module/WechatDpiModernHookInstaller.java");

        assertFalse(router.contains("handlePackageLoaded("));
        assertTrue(router.contains("handleModuleLoaded("));
        assertFalse(router.contains("WechatDpiRouteMode.useV1123CompatRoute()"));
        assertFalse(router.contains("ClassLoader.class.getDeclaredMethod("));
        assertFalse(router.contains("\"loadClass\", String.class, boolean.class"));
        assertTrue(router.contains("Application.class.getDeclaredMethod(\"attach\", Context.class)"));
        assertTrue(router.contains("application-attach route enter"));
        assertTrue(router.contains("application-attach hook ready"));
        assertTrue(router.contains("\"application_attach\""));
        assertFalse(router.contains("WechatDpiRoutes.matchesClassName(loadedClass.getName())"));
        assertFalse(router.contains("WechatDpiModernHookInstaller.installFromLoadedClass("));
        assertFalse(router.contains("param.getDefaultClassLoader()"));
        assertTrue(router.contains("WechatDpiConfig.appliesTo(param.getPackageName())"));
        assertTrue(router.contains("WechatDpiConfig.appliesTo(processName)"));
        assertTrue(router.contains("WechatDpiModernHookInstaller.install("));
        assertTrue(router.contains("param.getClassLoader()"));
        assertTrue(router.contains("param.getApplicationInfo()"));
        assertTrue(router.contains("describeClassLoaderForLog("));
        assertTrue(router.contains("alongside generic hooks"));
        assertTrue(installer.contains("ApplicationInfo applicationInfo"));
        assertFalse(installer.contains("installFromLoadedClass("));
        assertFalse(installer.contains("WechatDpiRouteMode.useV1123CompatRoute()"));
        assertFalse(installer.contains("WechatDpiMethodLocator.Source.LOADED_CLASS"));
        assertFalse(installer.contains("WechatDpiMethodLocator.densityManagerMethods("));
        assertTrue(installer.contains("installBottomTabIconScaleHook("));
        assertTrue(installer.contains("WECHAT_BOTTOM_TAB_ICON_VIEW_CLASS"));
        assertTrue(installer.contains("\"bottom_tab_icon\""));
        assertTrue(installer.contains("findBottomTabIconInitMethod("));
        assertTrue(installer.contains("findBottomTabIconScaleField("));
        assertTrue(installer.contains("WechatDpiRuntime.bottomTabIconScale("));
        assertTrue(installer.contains("bottom tab icon hook skipped: class not found"));
        assertTrue(installer.contains("resolveWechatVersionCode"));
        assertTrue(installer.contains("WechatDpiMethodLocator.locate("));
        assertTrue(installer.contains("WechatDpiRuntime.apply(metrics, dpi)"));
        assertTrue(installer.contains("configuredDpi="));
        assertTrue(installer.contains("describeClassLoaderForLog("));
        assertTrue(installer.contains("locatorResult.source.logName"));
        assertTrue(installer.contains("isDisplayMetricsMutator(hookMethod)"));
        assertTrue(installer.contains("displayMetricsArgument(chain.getArgs())"));
        assertTrue(installer.contains("isTargetFieldGetter(hookMethod)"));
        assertTrue(installer.contains("isTargetFieldSetter(hookMethod)"));
        assertTrue(installer.contains("Object result = chain.proceed();"));
        assertTrue(installer.contains("return result;"));
        assertFalse(installer.contains("findDisplayMetricsMethods(densityManagerClass)"));
        assertTrue(installer.contains("applyWechatDpi(metrics"));
        assertFalse(installer.contains("resourcesClassName"));
        assertFalse(installer.contains("installDpiGetterHook("));
        assertFalse(installer.contains("installDpiSetterHook("));
        assertFalse(installer.contains("chain.getArgs().set(0"));
    }

    @Test
    public void moduleMainConfiguresHyperOsFlutterNativeFontHook() throws IOException {
        String moduleMain = read("src/modern/java/com/dpis/module/ModuleMain.java");
        String build = read("build.gradle.kts");
        String flutterInstaller = read("src/main/java/com/dpis/module/HyperOsFlutterFontHookInstaller.java");
        String appProcessInstaller = read("src/main/java/com/dpis/module/AppProcessHookInstaller.java");

        assertFalse(moduleMain.contains("HyperOsFlutterFontHookInstaller.install("));
        assertTrue(moduleMain.contains("packagePlan.hyperOsNativeFlutterFontEnabled"));
        assertTrue(appProcessInstaller.contains("fontDomainPlan.hyperOsNativeFlutterEnabled"));
        assertTrue(appProcessInstaller.contains("HyperOsFlutterFontHookInstaller.install(xposed, packageName, store)"));
        assertTrue(moduleMain.contains("maybeInstallAppProcessFromModuleLoaded("));
        assertTrue(moduleMain.contains("installAppProcessHooksIfConfigured("));
        assertTrue(moduleMain.contains(
                "RuntimePropertyConfigPreferences.AutoViewportRuntimeRoute.ANY_ENABLED_TARGET"));
        assertTrue(moduleMain.contains("packageNameFromProcessName(processName)"));
        assertTrue(moduleMain.contains("installAppProcessHooksIfConfigured(runtimeStore, policy, snapshot, packageName,"));
        assertTrue(moduleMain.contains("module-loaded app hook install enter"));
        assertTrue(moduleMain.contains("module-loaded app hook install failed"));
        assertTrue(moduleMain.contains("ModernAppSpecificRouteInstaller.handleModuleLoaded(this, param.getProcessName())"));
        assertTrue(moduleMain.contains("rawBridgeLog("));
        assertTrue(moduleMain.contains("module-loaded app config fallback"));
        assertTrue(moduleMain.contains("module-loaded app config unavailable"));
        assertTrue(moduleMain.contains("appProcessInstallAttempted"));
        assertTrue(moduleMain.contains("\"module-loaded\""));
        assertTrue(moduleMain.contains("\"module-loaded-fallback\""));
        assertTrue(moduleMain.contains("\"package-ready\""));
        assertTrue(read("src/main/java/com/dpis/module/SystemServerDisplayEnvironmentInstaller.java")
                .contains("HyperOsRustProcessHookInstaller.install("));
        assertFalse(SourceSmokeTestPaths.exists("src", "modern", "resources", "META-INF", "xposed", "native_init.list"));
        assertFalse(SourceSmokeTestPaths.exists("src", "main", "assets", "native_init"));
        assertTrue(flutterInstaller.contains("System.loadLibrary(\"dpis_native\")"));
        assertTrue(build.contains("externalNativeBuild"));
    }

    @Test
    public void moduleMainRetriesFlutterHooksWithAppClassLoaderFromPackageReady() throws IOException {
        String source = read("src/modern/java/com/dpis/module/ModuleMain.java");

        assertTrue(source.contains("retryFlutterHooksWithAppClassLoader("));
        assertTrue(source.contains("param.getClassLoader()"));
        assertTrue(source.contains("FlutterSettingsFontHookInstaller.retryWithAppClassLoader("));
        assertTrue(source.contains("resolveDebugFontOverrideForPackage(packageName)"));
        assertTrue(source.contains("packagePlan.buildExecutionPlan("));
        assertTrue(source.contains("HookExecutionPlan executionPlan"));
        assertTrue(source.contains("!packagePlan.targetDpisEnabled || !packagePlan.fontScaleActive"));
        assertTrue(source.contains("packagePlan.flutterSettingsFontEnabled"));
        assertTrue(source.contains("packagePlan.hyperOsNativeFlutterFontEnabled"));
    }

    @Test
    public void hyperOsNativeFlutterDoesNotBypassAppProcessInstaller() throws IOException {
        String moduleMain = read("src/modern/java/com/dpis/module/ModuleMain.java");
        String appProcessInstaller = read("src/main/java/com/dpis/module/AppProcessHookInstaller.java");

        assertFalse(moduleMain.contains("HyperOsFlutterFontHookInstaller.install("));
        assertTrue(moduleMain.contains(
                "AppProcessHookInstaller.install(this, store, policy, packagePlan)"));
        assertTrue(appProcessInstaller.contains("HyperOsFlutterFontHookInstaller.install("));
        assertTrue(appProcessInstaller.contains("resolveFontDomainPlan("));
        assertTrue(appProcessInstaller.contains("hyperOsNativeFlutterEnabled"));
    }

    @Test
    public void modernHotReloadKeepsStableIdsAndDropsLegacyOnlyHooks() throws IOException {
        String moduleMain = read("src/modern/java/com/dpis/module/ModuleMain.java");
        String resourcesRead = read("src/main/java/com/dpis/module/ResourcesReadHookInstaller.java");
        String resourcesImpl = read("src/main/java/com/dpis/module/ResourcesImplHookInstaller.java");
        String resourcesManager = read("src/main/java/com/dpis/module/ResourcesManagerHookInstaller.java");

        assertTrue(moduleMain.contains("onHotReloaded(XposedModuleInterface.HotReloadedParam param)"));
        assertTrue(moduleMain.contains("ResourcesManagerHookInstaller.resetForHotReload();"));
        assertTrue(moduleMain.contains("ResourcesImplHookInstaller.resetForHotReload();"));
        assertTrue(moduleMain.contains("ResourcesReadHookInstaller.resetForHotReload();"));
        assertFalse(moduleMain.contains("ModernHookRegistry"));
        assertFalse(resourcesRead.contains("hookRegistry.register("));
        assertFalse(resourcesImpl.contains("hookRegistry.register("));
        assertFalse(resourcesManager.contains("hookRegistry.register("));
        assertTrue(resourcesRead.contains(".setId(\"resources_read_get_configuration\")"));
        assertTrue(resourcesRead.contains(".setId(\"resources_read_get_display_metrics\")"));
        assertTrue(resourcesRead.contains(".setId(\"resources_read_get_system\")"));
        assertTrue(resourcesImpl.contains(".setId(\"resources_impl_update_configuration\")"));
        assertTrue(resourcesManager.contains(".setId(HOOK_ID_APPLY_CONFIGURATION)"));
        assertTrue(resourcesManager.contains(".setId(HOOK_ID_UPDATE_RESOURCES_FOR_ACTIVITY)"));
        assertTrue(resourcesManager.contains(".setId(HOOK_ID_RESOURCE_CREATION_PREFIX + \"#\""));
        assertTrue(resourcesManager.contains(".setId(HOOK_ID_RESOURCES_KEY_PREFIX + \"#\""));
        assertFalse(moduleMain.contains("maybeInstallSystemServerHooks(store, policy, currentProcessName, \"android\",\n                    \"hot-reload\")"));
        assertTrue(moduleMain.contains("system_server hot reload skipped: replay not supported"));
    }

    @Test
    public void systemServerHookDoesNotRetryOriginalAfterProceedThrows() throws IOException {
        String installer = read("src/main/java/com/dpis/module/SystemServerDisplayEnvironmentInstaller.java");

        assertTrue(installer.contains("boolean proceedAttempted = false;"));
        assertTrue(installer.contains("proceedAttempted = true;"));
        assertTrue(installer.contains("if (proceedAttempted)"));
        assertTrue(installer.contains("throw throwable;"));
        assertTrue(installer.contains("return chain.proceed();"));
    }

    @Test
    public void issueSpecificDiagnosticsDoNotRemainInRuntimeSources() throws IOException {
        assertFalse(read("src/modern/java/com/dpis/module/ModuleMain.java")
                .contains("DPIS_DIAG"));
        assertFalse(read("src/main/java/com/dpis/module/ConfigStoreFactory.java")
                .contains("DPIS_DIAG"));
        assertFalse(read("src/main/java/com/dpis/module/SystemServerDisplayEnvironmentInstaller.java")
                .contains("DPIS_DIAG"));
    }

    @Test
    public void temporaryProbesAndPackerReferencesDoNotRemainInRuntimeSources() throws IOException {
        String moduleMain = read("src/modern/java/com/dpis/module/ModuleMain.java");
        String flutterInstaller = read("src/main/java/com/dpis/module/FlutterSettingsFontHookInstaller.java");

        // Temporary selftest/probe prefixes must not remain
        assertFalse("SELFTEST probe must be removed", moduleMain.contains("DPIS_HOOK_SELFTEST"));
        assertFalse("RUNTIME_PROBE must be removed", moduleMain.contains("DPIS_RUNTIME_PROBE"));
        assertFalse("APPCLASS probe must be removed", moduleMain.contains("DPIS_APPCLASS"));
        assertFalse("SHELL probe must be removed", moduleMain.contains("DPIS_SHELL"));
        assertFalse("CL_CAPABILITY probe must be removed", moduleMain.contains("DPIS_CL_CAPABILITY"));
        assertFalse("module-loaded install path must not keep temporary probe wording",
                moduleMain.contains("module-loaded app hook probe"));

        // Packer-specific class names must not appear in production sources
        assertFalse("shell packer class must not be in ModuleMain",
                moduleMain.contains("s.h.e.l.l"));
        assertFalse("target app package must not be hardcoded in ModuleMain",
                moduleMain.contains("com.mfcloudcalculate.networkdisk"));
        assertFalse("shell packer class must not be in FlutterSettingsFontHookInstaller",
                flutterInstaller.contains("s.h.e.l.l"));
        assertFalse("target app package must not be in FlutterSettingsFontHookInstaller",
                flutterInstaller.contains("com.mfcloudcalculate.networkdisk"));
    }

    @Test
    public void debugBuildKeepsRuntimeHookLogsVisible() throws IOException {
        String source = read("src/main/java/com/dpis/module/DpisLog.java");

        assertTrue(source.contains("BuildConfig.DEBUG || isLoggingEnabled()"));
    }

    @Test
    public void moduleMainUsesPackagePlanHookEligibilityGate() throws IOException {
        String moduleMain = read("src/modern/java/com/dpis/module/ModuleMain.java");

        assertTrue(moduleMain.contains("if (!packagePlan.shouldInstallHooks())"));
        assertFalse(moduleMain.contains("packagePlan.targetViewportWidthDp == null"
                + System.lineSeparator()
                + "                && !packagePlan.fontScaleActive"));
    }

    @Test
    public void modernRuntimeHotReloadDocsAndScriptMatchCurrentBoundary() throws IOException {
        String docs = readRepositoryRoot("docs/modern-runtime-resync.md");
        String script = readRepositoryRoot("scripts/pull-lsposed-logs.ps1");

        assertTrue(docs.contains("System-server replay"));
        assertTrue(docs.contains("hot-reload surface"));
        assertTrue(docs.contains("dynamic resource-creation / `createResourcesImpl` overload hooks derive ids"));
        assertTrue(script.contains("[string] $Device"));
        assertTrue(script.contains("ls -t /data/adb/lspd/log/modules_*.log"));
        assertTrue(script.contains("verbose_*.log"));
        assertFalse(script.contains("192.168.5.130:5555"));
    }

    @Test
    public void moduleMainAllowsPackageOwnedSecondaryProcessesForViewportHooks() throws IOException {
        String moduleMain = read("src/modern/java/com/dpis/module/ModuleMain.java");

        assertTrue(moduleMain.contains("!processName.startsWith(packagePlan.packageName + \":\")"));
    }

    @Test
    public void nativeFontHookUsesRustEnvironmentAsRuntimeFontSource() throws IOException {
        String nativeSource = read("src/main/cpp/dpis_native.cpp");

        assertTrue(nativeSource.contains("DPIS_FONT_SCALE_PERCENT"));
        assertTrue(nativeSource.contains("std::getenv"));
        assertTrue(nativeSource.contains("read_proc_cmdline_value"));
        assertTrue(nativeSource.contains("value == \"false\" || value == \"disabled\""));
        assertTrue(nativeSource.contains("return g_enabled.load(std::memory_order_relaxed)"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }

    private static String readRepositoryRoot(String relativePath) throws IOException {
        return SourceSmokeTestPaths.readRepositoryRoot(relativePath);
    }
}
