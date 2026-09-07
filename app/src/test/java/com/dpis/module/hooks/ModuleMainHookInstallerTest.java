package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dpis.module.runtime.appprocess.WebApkCarrierResolver;

import org.junit.Test;

import java.io.IOException;

public class ModuleMainHookInstallerTest {
    @Test
    public void moduleMainUsesExplicitSystemServerPolicyGuard() throws IOException {
        String source = read("src/modern/java/com/dpis/module/ModuleMain.java");

        assertTrue(source.contains("SystemServerMutationPolicy.shouldInstallSystemServerHooks("));
        assertTrue(source.contains("public void onSystemServerStarting(SystemServerStartingParam param)"));
        assertTrue(source.contains("public void onPackageLoaded(PackageLoadedParam param)"));
        assertTrue(source.contains("onPackageLoaded enter: process="));
        assertTrue(source.contains("system_server starting hook install enter"));
        assertTrue(source.contains("bridgeRuntimeLog(\"system_server starting hook install enter"));
        assertTrue(source.contains("\"system-server-starting\""));
        assertTrue(source.contains("\"module-loaded\""));
        assertTrue(source.contains("\"package-loaded\""));
        assertTrue(source.contains("resolveSystemServerRuntimePolicy("));
        assertTrue(source.contains("resolveHookedRuntimePolicy("));
        assertTrue(source.contains("HookRuntimePolicy.fromStore(store)"));
        assertTrue(source.contains("HookRuntimePolicy appProcessPolicy = resolveHookedRuntimePolicy(store);"));
        assertTrue(source.contains("installAppProcessHooksIfConfigured(store, appProcessPolicy, snapshot"));
        assertTrue(source.contains("HookRuntimePolicy policy = resolveHookedRuntimePolicy(store);"));
        assertTrue(source.contains("installAppProcessHooksIfConfigured(runtimeStore, policy, snapshot"));
        assertTrue(source.contains("String processName = resolveCurrentProcessName();"));
        assertTrue(source.contains("maybeInstallAppProcessFromPackageLoaded(store, processName, param.getPackageName())"));
        assertTrue(source.contains("Application.getProcessName()"));
        assertTrue(source.contains("new File(\"/proc/self/cmdline\").toPath()"));
        assertTrue(source.contains("package-loaded app hook install enter"));
        assertTrue(source.contains("package-loaded app hook install skipped system process"));
        assertTrue(source.contains("package-loaded app hook install failed"));
        assertTrue(source.contains("Do not downgrade route planning just because"));
        assertTrue(source.contains("maybeInstallSystemServerHooks(store, systemPolicy, currentProcessName"));
        assertTrue(source.contains("param != null ? param.getClassLoader() : null"));
        assertTrue(source.contains("getModernApiCapabilities(), systemServerClassLoader"));
        assertTrue(source.contains("maybeInstallSystemServerHooks(configStore, policy, param.getProcessName()"));
        assertTrue(source.contains("system_server installer "));
        assertTrue(source.contains("bridgeRuntimeLog(message);"));
        assertTrue(source.contains("result.hasInstalledHooks() ? \"ready\" : \"no-hooks\""));
        assertTrue(source.contains("ModulePackagePlan.resolve("));
        assertFalse(SourceSmokeTestPaths.exists("src", "main", "java", "com", "dpis", "module", "ModuleMain.java"));
    }

    @Test
    public void moduleMainDoesNotAliasChromeToWebApkOwnerInAppProcess() throws IOException {
        String source = read("src/modern/java/com/dpis/module/ModuleMain.java");

        assertTrue(source.contains("packageNameFromProcessName(processName)"));
        assertFalse(source.contains(WebApkCarrierResolver.WEBAPK_PACKAGE_EXTRA));
        assertFalse(source.contains("WebApkCarrierResolver"));
    }

    @Test
    public void moduleMainInstallsChromeChromiumViewportProbeOnlyWhenDebugPropertyMatches()
            throws IOException {
        String source = read("src/modern/java/com/dpis/module/ModuleMain.java");

        assertTrue(source.contains("installChromiumViewportProbe(param.getPackageName(), param.getClassLoader())"));
        assertTrue(source.contains("private void installChromiumViewportProbe(String packageName, ClassLoader classLoader)"));
        assertTrue(source.contains("ChromiumViewportProbeHookInstaller.install(this, classLoader)"));
        assertTrue(source.contains("WebApkRuntimeOwnerBridge.CHROME_PACKAGE.equals(packageName)"));
        assertTrue(source.contains("debug.dpis.webapk.chromium_probe_package"));
        assertTrue(source.contains("DebugPackageOverride.matches(PROP_CHROMIUM_VIEWPORT_PROBE_PACKAGE"));
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
                "src/modern/java/com/dpis/module/ModernAppSpecificRouteInstaller.kt");
        String installer = read(
                "src/modern/java/com/dpis/module/wechat/WechatDpiModernHookInstaller.java");
        String wechatRoute = read(
                "src/modern/java/com/dpis/module/wechat/WechatDpiRouteCoordinator.kt");

        assertFalse(router.contains("handlePackageLoaded("));
        assertTrue(router.contains("handleModuleLoaded("));
        assertFalse(router.contains("WechatDpiRouteMode.useV1123CompatRoute()"));
        assertFalse(router.contains("ClassLoader.class.getDeclaredMethod("));
        assertFalse(router.contains("\"loadClass\", String.class, boolean.class"));
        assertTrue(router.contains("WechatDpiRouteCoordinator"));
        assertTrue(wechatRoute.contains("Application::class.java.getDeclaredMethod(\"attach\", Context::class.java)"));
        assertTrue(wechatRoute.contains("application-attach retry result"));
        assertTrue(wechatRoute.contains("application-attach hook ready"));
        assertTrue(wechatRoute.contains("\"application_attach\""));
        assertFalse(router.contains("WechatDpiRoutes.matchesClassName(loadedClass.getName())"));
        assertFalse(router.contains("WechatDpiModernHookInstaller.installFromLoadedClass("));
        assertFalse(router.contains("param.getDefaultClassLoader()"));
        assertTrue(wechatRoute.contains("WechatDpiConfig.appliesTo(param.packageName)"));
        assertTrue(wechatRoute.contains("WechatDpiConfig.appliesTo(processName)"));
        assertTrue(wechatRoute.contains("WechatDpiModernHookInstaller.install("));
        assertTrue(wechatRoute.contains("param.classLoader"));
        assertTrue(wechatRoute.contains("param.applicationInfo"));
        assertTrue(wechatRoute.contains("describeClassLoaderForLog("));
        assertTrue(wechatRoute.contains("alongside generic hooks"));
        assertTrue(installer.contains("ApplicationInfo applicationInfo"));
        assertFalse(installer.contains("installFromLoadedClass("));
        assertFalse(installer.contains("WechatDpiRouteMode.useV1123CompatRoute()"));
        assertFalse(installer.contains("WechatDpiMethodLocator.Source.LOADED_CLASS"));
        assertFalse(installer.contains("WechatDpiMethodLocator.densityManagerMethods("));
        assertTrue(installer.contains("installBottomTabIconHook("));
        assertTrue(installer.contains("WECHAT_BOTTOM_TAB_ICON_VIEW_CLASS"));
        assertTrue(installer.contains("\"bottom_tab_icon\""));
        assertTrue(installer.contains("findBottomTabIconInitMethod("));
        assertTrue(installer.contains("findBottomTabIconScaleField("));
        assertTrue(installer.contains("WechatDpiRuntime.bottomTabIconScale("));
        assertTrue(installer.contains("Bitmap.createScaledBitmap("));
        assertTrue(installer.contains("postBottomTabBitmapNormalization("));
        assertTrue(installer.contains("postOnAnimation("));
        assertTrue(installer.contains("BOTTOM_TAB_BITMAP_NORMALIZE_MAX_ATTEMPTS"));
        assertTrue(installer.contains("scaleField != null"));
        assertTrue(installer.contains("originalBitmaps"));
        assertTrue(installer.contains("tabIconView.invalidate()"));
        assertTrue(installer.contains("bottom tab icon hook skipped: class not found"));
        assertTrue(installer.contains("resolveWechatVersionCode"));
        assertTrue(installer.contains("WechatDpiMethodLocator.locate("));
        assertTrue(installer.contains("phase.getAllowsDexKit()"));
        assertTrue(wechatRoute.contains("WechatDpiInstallPhase.PACKAGE_READY"));
        assertTrue(wechatRoute.contains("WechatDpiInstallPhase.APPLICATION_ATTACH"));
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
        String flutterInstaller = read("src/main/java/com/dpis/module/runtime/font/HyperOsFlutterFontHookInstaller.java");
        String appProcessInstaller = read("src/main/java/com/dpis/module/runtime/appprocess/AppProcessHookInstaller.java");

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
        assertTrue(moduleMain.contains("\"package-loaded\""));
        assertTrue(moduleMain.contains("\"module-loaded-fallback\""));
        assertTrue(moduleMain.contains("\"package-ready\""));
        assertTrue(read("src/main/java/com/dpis/module/runtime/systemserver/SystemServerDisplayEnvironmentInstaller.java")
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
        String appProcessInstaller = read("src/main/java/com/dpis/module/runtime/appprocess/AppProcessHookInstaller.java");

        assertFalse(moduleMain.contains("HyperOsFlutterFontHookInstaller.install("));
        assertTrue(moduleMain.contains("AppProcessHookInstaller.install("));
        assertTrue(moduleMain.contains("getModernApiCapabilities()"));
        assertTrue(appProcessInstaller.contains("HyperOsFlutterFontHookInstaller.install("));
        assertTrue(appProcessInstaller.contains("resolveFontDomainPlan("));
        assertTrue(appProcessInstaller.contains("hyperOsNativeFlutterEnabled"));
    }

    @Test
    public void modernUsesApi101BaselineWithApi102HotReloadAndHookIds() throws IOException {
        String moduleMain = read("src/modern/java/com/dpis/module/ModuleMain.java");
        String resourcesRead = read("src/main/java/com/dpis/module/runtime/appprocess/ResourcesReadHookInstaller.kt");
        String resourcesImpl = read("src/main/java/com/dpis/module/runtime/appprocess/ResourcesImplHookInstaller.kt");
        String resourcesManager = read("src/main/java/com/dpis/module/runtime/appprocess/ResourcesManagerHookInstaller.kt");
        String capabilities = read("src/main/java/com/dpis/module/runtime/hookapi/ModernApiCapabilities.java");
        String api101 = read("src/main/java/com/dpis/module/runtime/hookapi/ModernApi101Capabilities.java");
        String api102 = read("src/main/java/com/dpis/module/runtime/hookapi/ModernApi102Capabilities.java");
        String resolver = read("src/main/java/com/dpis/module/runtime/hookapi/ModernApiCapabilitiesResolver.java");

        assertTrue(moduleMain.contains("onHotReloading(XposedModuleInterface.HotReloadingParam param)"));
        assertTrue(moduleMain.contains("onHotReloaded(XposedModuleInterface.HotReloadedParam param)"));
        assertTrue(moduleMain.contains("restoreHotReloadState(savedState)"));
        assertTrue(moduleMain.contains("replayPackageReadySupplementsAfterHotReload("));
        assertTrue(moduleMain.contains("AppProcessHotReloadResetter.resetAll();"));
        assertFalse(moduleMain.contains("ModernHookRegistry"));
        assertTrue(moduleMain.contains("private volatile ModernApiCapabilities modernApiCapabilities;"));
        assertTrue(moduleMain.contains("private ModernApiCapabilities getModernApiCapabilities()"));
        assertTrue(moduleMain.contains("ModernApiCapabilitiesResolver.fromXposed(this)"));
        assertTrue(capabilities.contains("interface ModernApiCapabilities"));
        assertTrue(capabilities.contains("supportsStableHookIds()"));
        assertTrue(capabilities.contains("supportsHotReloadCallbacks()"));
        assertTrue(capabilities.contains("applyStableHookId"));
        assertTrue(api101.contains("final class ModernApi101Capabilities"));
        assertTrue(api101.contains("return false;"));
        assertTrue(api102.contains("final class ModernApi102Capabilities"));
        assertTrue(api102.contains("XposedInterface.HookBuilder.class.getMethod(\"setId\", String.class)"));
        assertTrue(resolver.contains("static final int API_101 = 101;"));
        assertTrue(resolver.contains("static final int API_102 = 102;"));
        assertTrue(resolver.contains("keeps API 101 as the loading baseline and targets API 102"));
        assertTrue(resolver.contains("API 102 hosts may exercise hot reload and stable hook ids"));
        assertTrue(resolver.contains("API 101 keeps"));
        assertTrue(resourcesRead.contains("apiCapabilities.applyStableHookId"));
        assertTrue(resourcesImpl.contains("apiCapabilities.applyStableHookId"));
        assertTrue(resourcesManager.contains("apiCapabilities.applyStableHookId<HookBuilder?>("));
        assertTrue(read("src/main/java/com/dpis/module/runtime/font/ActivityThreadFontHookInstaller.java")
                .contains("HOOK_ID_HANDLE_BIND_APPLICATION"));
        assertTrue(read("src/main/java/com/dpis/module/runtime/font/WebViewFontHookInstaller.kt")
                .contains("HOOK_ID_WEBVIEW_GET_SETTINGS"));
        assertTrue(read("src/main/java/com/dpis/module/runtime/font/WebViewFontHookInstaller.kt")
                .contains("HOOK_ID_WEBSETTINGS_SET_TEXT_ZOOM"));
        assertTrue(read("src/main/java/com/dpis/module/runtime/font/ForceTextSizeHookInstaller.java")
                .contains("HOOK_ID_TEXTVIEW_SET_TEXT_SIZE_WITH_UNIT"));
        assertTrue(read("src/main/java/com/dpis/module/runtime/font/ForceTextSizeHookInstaller.java")
                .contains("HOOK_ID_PAINT_SET_TEXT_SIZE"));
        assertTrue(read("src/main/java/com/dpis/module/runtime/font/ForceTextSizeHookInstaller.java")
                .contains("bridgeMutationAppliedIfChanged("));
        String typefaceInstaller = read("src/main/java/com/dpis/module/runtime/font/TypefaceOverrideHookInstaller.kt");
        assertTrue(typefaceInstaller.contains("HOOK_ID_TEXTVIEW_SET_TYPEFACE"));
        assertTrue(typefaceInstaller.contains("HOOK_ID_PAINT_SET_TYPEFACE"));
        assertTrue(typefaceInstaller.contains("apiCapabilities.applyStableHookId<HookBuilder?>("));
        assertTrue(typefaceInstaller.contains("bridgeOverrideAppliedIfChanged("));
        String appProcessInstaller = read("src/main/java/com/dpis/module/runtime/appprocess/AppProcessHookInstaller.java");
        assertTrue(appProcessInstaller.contains("ForceTextSizeHookInstaller.install("));
        assertTrue(appProcessInstaller.contains("plan.fontDomainPlan,"));
        assertTrue(appProcessInstaller.contains("apiCapabilities);"));
        assertTrue(read("src/main/java/com/dpis/module/runtime/appprocess/DisplayHookInstaller.kt")
                .contains("HOOK_ID_DISPLAY_GET_DISPLAY_INFO"));
        assertTrue(read("src/main/java/com/dpis/module/runtime/appprocess/WindowMetricsHookInstaller.java")
                .contains("HOOK_ID_WINDOW_METRICS_GET_BOUNDS"));
        assertTrue(read("src/modern/java/com/dpis/module/ModernAppSpecificRouteInstaller.kt")
                .contains("handlePackageReadyReplay("));
        assertTrue(read("src/modern/java/com/dpis/module/wechat/WechatDpiRouteCoordinator.kt")
                .contains("WechatDpiInstallPhase.HOT_RELOAD_PACKAGE_READY"));
        assertTrue(moduleMain.contains("replaySystemServerAfterHotReload(store, currentProcessName);"));
        assertTrue(moduleMain.contains("system_server hot reload replay enter"));
        assertTrue(moduleMain.contains("SystemServerDisplayEnvironmentInstaller.resetForHotReload();"));
        assertTrue(moduleMain.contains("\"hot-reload\""));
        assertTrue(read("src/main/java/com/dpis/module/runtime/systemserver/SystemServerDisplayEnvironmentInstaller.java")
                .contains("apiCapabilities.applyStableHookId("));
        assertTrue(read("src/main/java/com/dpis/module/runtime/systemserver/SystemServerDisplayEnvironmentInstaller.java")
                .contains("static void resetForHotReload()"));
        assertTrue(read("src/main/java/com/dpis/module/runtime/appprocess/AppProcessHotReloadResetter.java")
                .contains("static void resetAll()"));
        assertTrue(read("src/main/java/com/dpis/module/runtime/systemserver/SystemServerHookCatalog.java")
                .contains("system_server_launch_activity_item"));
    }

    @Test
    public void systemServerHookDoesNotRetryOriginalAfterProceedThrows() throws IOException {
        String installer = read("src/main/java/com/dpis/module/runtime/systemserver/SystemServerDisplayEnvironmentInstaller.java");

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
        assertFalse(read("src/main/java/com/dpis/module/runtime/systemserver/SystemServerDisplayEnvironmentInstaller.java")
                .contains("DPIS_DIAG"));
    }

    @Test
    public void temporaryProbesAndPackerReferencesDoNotRemainInRuntimeSources() throws IOException {
        String moduleMain = read("src/modern/java/com/dpis/module/ModuleMain.java");
        String flutterInstaller = read("src/main/java/com/dpis/module/runtime/font/FlutterSettingsFontHookInstaller.java");

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
    public void modernRuntimeApiCapabilityDocsAndScriptMatchCurrentBoundary() throws IOException {
        String docs = readRepositoryRoot("docs/modern-runtime-resync.md");
        String script = readRepositoryRoot("scripts/pull-lsposed-logs.ps1");

        assertTrue(docs.contains("declares `minApiVersion=101`"));
        assertTrue(docs.contains("`targetApiVersion=102`"));
        assertTrue(docs.contains("declare `autoHotReload=true`"));
        assertTrue(docs.contains("API 102 hosts can use the hot-reload lifecycle"));
        assertTrue(docs.contains("API 101 hosts keep"));
        assertTrue(docs.contains("install-and-restart path"));
        assertTrue(docs.contains("stable hook ids"));
        assertTrue(docs.contains("framework exposes"));
        assertTrue(docs.contains("degrades to the 101 capability set"));
        assertTrue(docs.contains("ForceTextSize hook ready"));
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
