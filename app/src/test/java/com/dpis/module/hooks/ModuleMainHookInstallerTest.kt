package com.dpis.module

import com.dpis.module.runtime.appprocess.WebApkCarrierResolver
import org.junit.Assert
import org.junit.Test
import java.io.IOException

class ModuleMainHookInstallerTest {
    @Test
    @Throws(IOException::class)
    fun moduleMainUsesExplicitSystemServerPolicyGuard() {
        val source: String = read("src/modern/java/com/dpis/module/ModuleMain.java")

        Assert.assertTrue(source.contains("SystemServerMutationPolicy.shouldInstallSystemServerHooks("))
        Assert.assertTrue(source.contains("public void onSystemServerStarting(SystemServerStartingParam param)"))
        Assert.assertTrue(source.contains("public void onPackageLoaded(PackageLoadedParam param)"))
        Assert.assertTrue(source.contains("onPackageLoaded enter: process="))
        Assert.assertTrue(source.contains("system_server starting hook install enter"))
        Assert.assertTrue(source.contains("bridgeRuntimeLog(\"system_server starting hook install enter"))
        Assert.assertTrue(source.contains("\"system-server-starting\""))
        Assert.assertTrue(source.contains("\"module-loaded\""))
        Assert.assertTrue(source.contains("\"package-loaded\""))
        Assert.assertTrue(source.contains("resolveSystemServerRuntimePolicy("))
        Assert.assertTrue(source.contains("resolveHookedRuntimePolicy("))
        Assert.assertTrue(source.contains("HookRuntimePolicy.fromStore(store)"))
        Assert.assertTrue(source.contains("HookRuntimePolicy appProcessPolicy = resolveHookedRuntimePolicy(store);"))
        Assert.assertTrue(source.contains("installAppProcessHooksIfConfigured(store, appProcessPolicy, snapshot"))
        Assert.assertTrue(source.contains("HookRuntimePolicy policy = resolveHookedRuntimePolicy(store);"))
        Assert.assertTrue(source.contains("installAppProcessHooksIfConfigured(runtimeStore, policy, snapshot"))
        Assert.assertTrue(source.contains("String processName = resolveCurrentProcessName();"))
        Assert.assertTrue(source.contains("maybeInstallAppProcessFromPackageLoaded(store, processName, param.getPackageName())"))
        Assert.assertTrue(source.contains("Application.getProcessName()"))
        Assert.assertTrue(source.contains("new File(\"/proc/self/cmdline\").toPath()"))
        Assert.assertTrue(source.contains("package-loaded app hook install enter"))
        Assert.assertTrue(source.contains("package-loaded app hook install skipped system process"))
        Assert.assertTrue(source.contains("package-loaded app hook install failed"))
        Assert.assertTrue(source.contains("Do not downgrade route planning just because"))
        Assert.assertTrue(source.contains("maybeInstallSystemServerHooks(store, systemPolicy, currentProcessName"))
        Assert.assertTrue(source.contains("param != null ? param.getClassLoader() : null"))
        Assert.assertTrue(source.contains("getModernApiCapabilities(), systemServerClassLoader"))
        Assert.assertTrue(source.contains("maybeInstallSystemServerHooks(configStore, policy, param.getProcessName()"))
        Assert.assertTrue(source.contains("system_server installer "))
        Assert.assertTrue(source.contains("bridgeRuntimeLog(message);"))
        Assert.assertTrue(source.contains("result.hasInstalledHooks() ? \"ready\" : \"no-hooks\""))
        Assert.assertTrue(source.contains("ModulePackagePlan.resolve("))
        Assert.assertFalse(
            SourceSmokeTestPaths.exists(
                "src",
                "main",
                "java",
                "com",
                "dpis",
                "module",
                "ModuleMain.java"
            )
        )
    }

    @Test
    @Throws(IOException::class)
    fun moduleMainDoesNotAliasChromeToWebApkOwnerInAppProcess() {
        val source: String = read("src/modern/java/com/dpis/module/ModuleMain.java")

        Assert.assertTrue(source.contains("packageNameFromProcessName(processName)"))
        Assert.assertFalse(source.contains(WebApkCarrierResolver.WEBAPK_PACKAGE_EXTRA))
        Assert.assertFalse(source.contains("WebApkCarrierResolver"))
    }

    @Test
    @Throws(IOException::class)
    fun moduleMainInstallsChromeChromiumViewportProbeOnlyWhenDebugPropertyMatches() {
        val source: String = read("src/modern/java/com/dpis/module/ModuleMain.java")

        Assert.assertTrue(source.contains("installChromiumViewportProbe(param.getPackageName(), param.getClassLoader())"))
        Assert.assertTrue(source.contains("private void installChromiumViewportProbe(String packageName, ClassLoader classLoader)"))
        Assert.assertTrue(source.contains("ChromiumViewportProbeHookInstaller.install(this, classLoader)"))
        Assert.assertTrue(source.contains("WebApkRuntimeOwnerBridge.CHROME_PACKAGE.equals(packageName)"))
        Assert.assertTrue(source.contains("debug.dpis.webapk.chromium_probe_package"))
        Assert.assertTrue(source.contains("DebugPackageOverride.matches(PROP_CHROMIUM_VIEWPORT_PROBE_PACKAGE"))
    }

    @Test
    @Throws(IOException::class)
    fun moduleMainDelegatesAppSpecificRoutes() {
        val moduleMain: String = read("src/modern/java/com/dpis/module/ModuleMain.java")

        Assert.assertTrue(moduleMain.contains("ModernAppSpecificRouteInstaller.handlePackageReady("))
        Assert.assertTrue(
            moduleMain.contains(
                "ModernAppSpecificRouteInstaller.shouldSuppressModuleLoadedGenericHooks("
            )
        )
        Assert.assertFalse(moduleMain.contains("WECHAT_PACKAGE"))
        Assert.assertFalse(moduleMain.contains("com.tencent.mm"))
        Assert.assertFalse(moduleMain.contains("WechatDpiModernHookInstaller.install("))
    }

    @Test
    @Throws(IOException::class)
    fun modernModuleMainMarksSelfProcessForHomeActivation() {
        val moduleMain: String = read("src/modern/java/com/dpis/module/ModuleMain.java")

        Assert.assertTrue(moduleMain.contains("XposedSelfActivation.markIfSelfPackage("))
        Assert.assertTrue(moduleMain.contains("param.getPackageName()"))
        Assert.assertTrue(moduleMain.contains("param.getClassLoader()"))
        Assert.assertTrue(moduleMain.contains("libxposed-package-ready"))
        Assert.assertFalse(moduleMain.contains("DpisApplication.markXposedSelfLoaded();"))
    }

    @Test
    @Throws(IOException::class)
    fun modernAppSpecificRouteInstallerRoutesWechatDpi() {
        val router: String = read(
            "src/modern/java/com/dpis/module/ModernAppSpecificRouteInstaller.kt"
        )
        val installer: String = read(
            "src/modern/java/com/dpis/module/wechat/WechatDpiModernHookInstaller.kt"
        )
        val bottomTabInstaller: String = read(
            "src/modern/java/com/dpis/module/wechat/WechatDpiModernBottomTabHookInstaller.kt"
        )
        val wechatRoute: String = read(
            "src/modern/java/com/dpis/module/wechat/WechatDpiRouteCoordinator.kt"
        )

        Assert.assertFalse(router.contains("handlePackageLoaded("))
        Assert.assertTrue(router.contains("handleModuleLoaded("))
        Assert.assertFalse(router.contains("WechatDpiRouteMode.useV1123CompatRoute()"))
        Assert.assertFalse(router.contains("ClassLoader.class.getDeclaredMethod("))
        Assert.assertFalse(router.contains("\"loadClass\", String.class, boolean.class"))
        Assert.assertTrue(router.contains("WechatDpiRouteCoordinator"))
        Assert.assertTrue(wechatRoute.contains("Application::class.java.getDeclaredMethod(\"attach\", Context::class.java)"))
        Assert.assertTrue(wechatRoute.contains("application-attach retry result"))
        Assert.assertTrue(wechatRoute.contains("application-attach hook ready"))
        Assert.assertTrue(wechatRoute.contains("\"application_attach\""))
        Assert.assertFalse(router.contains("WechatDpiRoutes.matchesClassName(loadedClass.getName())"))
        Assert.assertFalse(router.contains("WechatDpiModernHookInstaller.installFromLoadedClass("))
        Assert.assertFalse(router.contains("param.getDefaultClassLoader()"))
        Assert.assertTrue(wechatRoute.contains("WechatDpiConfig.appliesTo(param.packageName)"))
        Assert.assertTrue(wechatRoute.contains("WechatDpiConfig.appliesTo(processName)"))
        Assert.assertTrue(wechatRoute.contains("WechatDpiModernHookInstaller.install("))
        Assert.assertTrue(wechatRoute.contains("param.classLoader"))
        Assert.assertTrue(wechatRoute.contains("param.applicationInfo"))
        Assert.assertTrue(wechatRoute.contains("describeClassLoaderForLog("))
        Assert.assertTrue(wechatRoute.contains("alongside generic hooks"))
        Assert.assertTrue(installer.contains("applicationInfo: ApplicationInfo?"))
        Assert.assertFalse(installer.contains("installFromLoadedClass("))
        Assert.assertFalse(installer.contains("WechatDpiRouteMode.useV1123CompatRoute()"))
        Assert.assertFalse(installer.contains("WechatDpiMethodLocator.Source.LOADED_CLASS"))
        Assert.assertFalse(installer.contains("WechatDpiMethodLocator.densityManagerMethods("))
        Assert.assertTrue(installer.contains("WechatDpiModernBottomTabHookInstaller.install("))
        Assert.assertTrue(bottomTabInstaller.contains("\"bottom_tab_icon\""))
        Assert.assertTrue(bottomTabInstaller.contains("WechatDpiRuntime.bottomTabIconScale("))
        Assert.assertTrue(bottomTabInstaller.contains("Bitmap.createScaledBitmap("))
        Assert.assertTrue(bottomTabInstaller.contains("postOnAnimation"))
        Assert.assertTrue(bottomTabInstaller.contains("MAX_NORMALIZE_ATTEMPTS"))
        Assert.assertTrue(bottomTabInstaller.contains("originalBitmaps"))
        Assert.assertTrue(bottomTabInstaller.contains("view.invalidate()"))
        Assert.assertTrue(installer.contains("resolveWechatVersionCode"))
        Assert.assertTrue(installer.contains("WechatDpiMethodLocator.locate("))
        Assert.assertTrue(installer.contains("phase.allowsDexKit"))
        Assert.assertTrue(wechatRoute.contains("WechatDpiInstallPhase.PACKAGE_READY"))
        Assert.assertTrue(wechatRoute.contains("WechatDpiInstallPhase.APPLICATION_ATTACH"))
        Assert.assertTrue(installer.contains("WechatDpiRuntime.detached(metrics, targetDpi)"))
        Assert.assertTrue(installer.contains("ModernApiCapabilitiesResolver.fromXposed(xposed)"))
        Assert.assertTrue(installer.contains("HOOK_ID_DENSITY_PREFIX"))
        Assert.assertTrue(bottomTabInstaller.contains("HOOK_ID"))
        Assert.assertFalse(installer.contains("WechatDpiResourceRecovery"))
        Assert.assertTrue(installer.contains("configuredDpi="))
        Assert.assertTrue(installer.contains("describeClassLoaderForLog("))
        Assert.assertTrue(installer.contains("locator.source.logName"))
        Assert.assertFalse(installer.contains("isDisplayMetricsMutator(hookMethod)"))
        Assert.assertFalse(installer.contains("displayMetricsArgument(chain.getArgs())"))
        Assert.assertTrue(installer.contains("val result = chain.proceed()"))
        Assert.assertTrue(installer.contains("applyWechatDpi(result, methodName(method), phase)"))
        Assert.assertFalse(installer.contains("findDisplayMetricsMethods(densityManagerClass)"))
        Assert.assertTrue(installer.contains("applyWechatDpi(result"))
        Assert.assertFalse(installer.contains("resourcesClassName"))
        Assert.assertFalse(installer.contains("installDpiGetterHook("))
        Assert.assertFalse(installer.contains("installDpiSetterHook("))
        Assert.assertFalse(installer.contains("chain.getArgs().set(0"))
    }

    @Test
    @Throws(IOException::class)
    fun moduleMainConfiguresHyperOsFlutterNativeFontHook() {
        val moduleMain: String = read("src/modern/java/com/dpis/module/ModuleMain.java")
        val build: String = read("build.gradle.kts")
        val flutterInstaller: String =
            read("src/main/java/com/dpis/module/runtime/font/HyperOsFlutterFontHookInstaller.java")
        val appProcessInstaller: String =
            read("src/main/java/com/dpis/module/runtime/appprocess/AppProcessHookInstaller.kt")

        Assert.assertFalse(moduleMain.contains("HyperOsFlutterFontHookInstaller.install("))
        Assert.assertTrue(moduleMain.contains("packagePlan.hyperOsNativeFlutterFontEnabled"))
        Assert.assertTrue(appProcessInstaller.contains("fontDomainPlan.hyperOsNativeFlutterEnabled"))
        Assert.assertTrue(appProcessInstaller.contains("HyperOsFlutterFontHookInstaller.install(xposed, packageName, store)"))
        Assert.assertTrue(moduleMain.contains("maybeInstallAppProcessFromModuleLoaded("))
        Assert.assertTrue(moduleMain.contains("installAppProcessHooksIfConfigured("))
        Assert.assertTrue(
            moduleMain.contains(
                "RuntimePropertyConfigPreferences.AutoViewportRuntimeRoute.ANY_ENABLED_TARGET"
            )
        )
        Assert.assertTrue(moduleMain.contains("packageNameFromProcessName(processName)"))
        Assert.assertTrue(moduleMain.contains("installAppProcessHooksIfConfigured(runtimeStore, policy, snapshot, packageName,"))
        Assert.assertTrue(moduleMain.contains("module-loaded app hook install enter"))
        Assert.assertTrue(moduleMain.contains("module-loaded app hook install failed"))
        Assert.assertTrue(moduleMain.contains("ModernAppSpecificRouteInstaller.handleModuleLoaded(this, param.getProcessName())"))
        Assert.assertTrue(moduleMain.contains("rawBridgeLog("))
        Assert.assertTrue(moduleMain.contains("module-loaded app config fallback"))
        Assert.assertTrue(moduleMain.contains("module-loaded app config unavailable"))
        Assert.assertTrue(moduleMain.contains("appProcessInstallAttempted"))
        Assert.assertTrue(moduleMain.contains("\"module-loaded\""))
        Assert.assertTrue(moduleMain.contains("\"package-loaded\""))
        Assert.assertTrue(moduleMain.contains("\"module-loaded-fallback\""))
        Assert.assertTrue(moduleMain.contains("\"package-ready\""))
        Assert.assertTrue(
            read("src/main/java/com/dpis/module/runtime/systemserver/SystemServerDisplayEnvironmentInstaller.java")
                .contains("HyperOsRustProcessHookInstaller.install(")
        )
        Assert.assertFalse(
            SourceSmokeTestPaths.exists(
                "src",
                "modern",
                "resources",
                "META-INF",
                "xposed",
                "native_init.list"
            )
        )
        Assert.assertFalse(SourceSmokeTestPaths.exists("src", "main", "assets", "native_init"))
        Assert.assertTrue(flutterInstaller.contains("System.loadLibrary(\"dpis_native\")"))
        Assert.assertTrue(build.contains("externalNativeBuild"))
    }

    @Test
    @Throws(IOException::class)
    fun moduleMainRetriesFlutterHooksWithAppClassLoaderFromPackageReady() {
        val source: String = read("src/modern/java/com/dpis/module/ModuleMain.java")

        Assert.assertTrue(source.contains("retryFlutterHooksWithAppClassLoader("))
        Assert.assertTrue(source.contains("param.getClassLoader()"))
        Assert.assertTrue(source.contains("FlutterSettingsFontHookInstaller.retryWithAppClassLoader("))
        Assert.assertTrue(source.contains("resolveDebugFontOverrideForPackage(packageName)"))
        Assert.assertTrue(source.contains("packagePlan.buildExecutionPlan("))
        Assert.assertTrue(source.contains("HookExecutionPlan executionPlan"))
        Assert.assertTrue(source.contains("!packagePlan.targetDpisEnabled || !packagePlan.fontScaleActive"))
        Assert.assertTrue(source.contains("packagePlan.flutterSettingsFontEnabled"))
        Assert.assertTrue(source.contains("packagePlan.hyperOsNativeFlutterFontEnabled"))
    }

    @Test
    @Throws(IOException::class)
    fun hyperOsNativeFlutterDoesNotBypassAppProcessInstaller() {
        val moduleMain: String = read("src/modern/java/com/dpis/module/ModuleMain.java")
        val appProcessInstaller: String =
            read("src/main/java/com/dpis/module/runtime/appprocess/AppProcessHookInstaller.kt")

        Assert.assertFalse(moduleMain.contains("HyperOsFlutterFontHookInstaller.install("))
        Assert.assertTrue(moduleMain.contains("AppProcessHookInstaller.install("))
        Assert.assertTrue(moduleMain.contains("getModernApiCapabilities()"))
        Assert.assertTrue(appProcessInstaller.contains("HyperOsFlutterFontHookInstaller.install("))
        Assert.assertTrue(appProcessInstaller.contains("resolveFontDomainPlan("))
        Assert.assertTrue(appProcessInstaller.contains("hyperOsNativeFlutterEnabled"))
    }

    @Test
    @Throws(IOException::class)
    fun modernUsesApi101BaselineWithApi102HotReloadAndHookIds() {
        val moduleMain: String = read("src/modern/java/com/dpis/module/ModuleMain.java")
        val resourcesRead: String =
            read("src/main/java/com/dpis/module/runtime/appprocess/ResourcesReadHookInstaller.kt")
        val resourcesImpl: String =
            read("src/main/java/com/dpis/module/runtime/appprocess/ResourcesImplHookInstaller.kt")
        val resourcesManager: String =
            read("src/main/java/com/dpis/module/runtime/appprocess/ResourcesManagerHookInstaller.kt")
        val capabilities: String =
            read("src/main/java/com/dpis/module/runtime/hookapi/ModernApiCapabilities.java")
        val api101: String =
            read("src/main/java/com/dpis/module/runtime/hookapi/ModernApi101Capabilities.java")
        val api102: String =
            read("src/main/java/com/dpis/module/runtime/hookapi/ModernApi102Capabilities.java")
        val resolver: String =
            read("src/main/java/com/dpis/module/runtime/hookapi/ModernApiCapabilitiesResolver.java")

        Assert.assertTrue(moduleMain.contains("onHotReloading(XposedModuleInterface.HotReloadingParam param)"))
        Assert.assertTrue(moduleMain.contains("onHotReloaded(XposedModuleInterface.HotReloadedParam param)"))
        Assert.assertTrue(moduleMain.contains("restoreHotReloadState(savedState)"))
        Assert.assertTrue(moduleMain.contains("replayPackageReadySupplementsAfterHotReload("))
        Assert.assertTrue(moduleMain.contains("AppProcessHotReloadResetter.resetAll();"))
        Assert.assertFalse(moduleMain.contains("ModernHookRegistry"))
        Assert.assertTrue(moduleMain.contains("private volatile ModernApiCapabilities modernApiCapabilities;"))
        Assert.assertTrue(moduleMain.contains("private ModernApiCapabilities getModernApiCapabilities()"))
        Assert.assertTrue(moduleMain.contains("ModernApiCapabilitiesResolver.fromXposed(this)"))
        Assert.assertTrue(capabilities.contains("interface ModernApiCapabilities"))
        Assert.assertTrue(capabilities.contains("supportsStableHookIds()"))
        Assert.assertTrue(capabilities.contains("supportsHotReloadCallbacks()"))
        Assert.assertTrue(capabilities.contains("applyStableHookId"))
        Assert.assertTrue(api101.contains("final class ModernApi101Capabilities"))
        Assert.assertTrue(api101.contains("return false;"))
        Assert.assertTrue(api102.contains("final class ModernApi102Capabilities"))
        Assert.assertTrue(api102.contains("XposedInterface.HookBuilder.class.getMethod(\"setId\", String.class)"))
        Assert.assertTrue(resolver.contains("static final int API_101 = 101;"))
        Assert.assertTrue(resolver.contains("static final int API_102 = 102;"))
        Assert.assertTrue(resolver.contains("keeps API 101 as the loading baseline and targets API 102"))
        Assert.assertTrue(resolver.contains("API 102 hosts may exercise hot reload and stable hook ids"))
        Assert.assertTrue(resolver.contains("API 101 keeps"))
        Assert.assertTrue(resourcesRead.contains("apiCapabilities.applyStableHookId"))
        Assert.assertTrue(resourcesImpl.contains("apiCapabilities.applyStableHookId"))
        Assert.assertTrue(resourcesManager.contains("apiCapabilities.applyStableHookId<HookBuilder>("))
        Assert.assertTrue(
            read("src/main/java/com/dpis/module/runtime/font/ActivityThreadFontHookInstaller.java")
                .contains("HOOK_ID_HANDLE_BIND_APPLICATION")
        )
        Assert.assertTrue(
            read("src/main/java/com/dpis/module/runtime/font/WebViewFontHookInstaller.kt")
                .contains("HOOK_ID_WEBVIEW_GET_SETTINGS")
        )
        Assert.assertTrue(
            read("src/main/java/com/dpis/module/runtime/font/WebViewFontHookInstaller.kt")
                .contains("HOOK_ID_WEBSETTINGS_SET_TEXT_ZOOM")
        )
        Assert.assertTrue(
            read("src/main/java/com/dpis/module/runtime/font/ForceTextSizeHookRuntime.kt")
                .contains("HOOK_ID_TEXTVIEW_SET_TEXT_SIZE_WITH_UNIT")
        )
        Assert.assertTrue(
            read("src/main/java/com/dpis/module/runtime/font/ForceTextSizeHookRuntime.kt")
                .contains("HOOK_ID_PAINT_SET_TEXT_SIZE")
        )
        Assert.assertTrue(
            read("src/main/java/com/dpis/module/runtime/font/ForceTextSizeHookRuntime.kt")
                .contains("bridgeMutationAppliedIfChanged(")
        )
        val typefaceInstaller: String =
            read("src/main/java/com/dpis/module/runtime/font/TypefaceOverrideHookInstaller.kt")
        Assert.assertTrue(typefaceInstaller.contains("HOOK_ID_TEXTVIEW_SET_TYPEFACE"))
        Assert.assertTrue(typefaceInstaller.contains("HOOK_ID_PAINT_SET_TYPEFACE"))
        Assert.assertTrue(typefaceInstaller.contains("apiCapabilities.applyStableHookId<HookBuilder>("))
        Assert.assertTrue(typefaceInstaller.contains("bridgeOverrideAppliedIfChanged("))
        val appProcessInstaller: String =
            read("src/main/java/com/dpis/module/runtime/appprocess/AppProcessHookInstaller.kt")
        Assert.assertTrue(appProcessInstaller.contains("ForceTextSizeHookInstaller.install("))
        Assert.assertTrue(appProcessInstaller.contains("plan.fontDomainPlan,"))
        Assert.assertTrue(appProcessInstaller.contains("apiCapabilities"))
        Assert.assertTrue(
            read("src/main/java/com/dpis/module/runtime/appprocess/DisplayHookInstaller.kt")
                .contains("HOOK_ID_DISPLAY_GET_DISPLAY_INFO")
        )
        Assert.assertTrue(
            read("src/main/java/com/dpis/module/runtime/appprocess/WindowMetricsHookInstaller.kt")
                .contains("HOOK_ID_WINDOW_METRICS_GET_BOUNDS")
        )
        Assert.assertTrue(
            read("src/modern/java/com/dpis/module/ModernAppSpecificRouteInstaller.kt")
                .contains("handlePackageReadyReplay(")
        )
        Assert.assertTrue(
            read("src/modern/java/com/dpis/module/wechat/WechatDpiRouteCoordinator.kt")
                .contains("WechatDpiInstallPhase.HOT_RELOAD_PACKAGE_READY")
        )
        Assert.assertTrue(
            read("src/modern/java/com/dpis/module/wechat/WechatDpiRouteCoordinator.kt")
                .contains("supportsHotReloadCallbacks()")
        )
        Assert.assertTrue(moduleMain.contains("replaySystemServerAfterHotReload(store, currentProcessName);"))
        Assert.assertTrue(moduleMain.contains("system_server hot reload replay enter"))
        Assert.assertTrue(moduleMain.contains("SystemServerDisplayEnvironmentInstaller.resetForHotReload();"))
        Assert.assertTrue(moduleMain.contains("\"hot-reload\""))
        Assert.assertTrue(
            read("src/main/java/com/dpis/module/runtime/systemserver/SystemServerDisplayEnvironmentInstaller.java")
                .contains("apiCapabilities.applyStableHookId(")
        )
        Assert.assertTrue(
            read("src/main/java/com/dpis/module/runtime/systemserver/SystemServerDisplayEnvironmentInstaller.java")
                .contains("static void resetForHotReload()")
        )
        Assert.assertTrue(
            read("src/main/java/com/dpis/module/runtime/appprocess/AppProcessHotReloadResetter.kt")
                .contains("fun resetAll()")
        )
        Assert.assertTrue(
            read("src/main/java/com/dpis/module/runtime/systemserver/SystemServerHookCatalog.java")
                .contains("system_server_launch_activity_item")
        )
    }

    @Test
    @Throws(IOException::class)
    fun systemServerHookDoesNotRetryOriginalAfterProceedThrows() {
        val installer: String =
            read("src/main/java/com/dpis/module/runtime/systemserver/SystemServerDisplayEnvironmentInstaller.java")

        Assert.assertTrue(installer.contains("boolean proceedAttempted = false;"))
        Assert.assertTrue(installer.contains("proceedAttempted = true;"))
        Assert.assertTrue(installer.contains("if (proceedAttempted)"))
        Assert.assertTrue(installer.contains("throw throwable;"))
        Assert.assertTrue(installer.contains("return chain.proceed();"))
    }

    @Test
    @Throws(IOException::class)
    fun issueSpecificDiagnosticsDoNotRemainInRuntimeSources() {
        Assert.assertFalse(
            read("src/modern/java/com/dpis/module/ModuleMain.java")
                .contains("DPIS_DIAG")
        )
        Assert.assertFalse(
            read("src/main/java/com/dpis/module/runtime/ConfigStoreFactory.java")
                .contains("DPIS_DIAG")
        )
        Assert.assertFalse(
            read("src/main/java/com/dpis/module/runtime/systemserver/SystemServerDisplayEnvironmentInstaller.java")
                .contains("DPIS_DIAG")
        )
    }

    @Test
    @Throws(IOException::class)
    fun temporaryProbesAndPackerReferencesDoNotRemainInRuntimeSources() {
        val moduleMain: String = read("src/modern/java/com/dpis/module/ModuleMain.java")
        val flutterInstaller: String =
            read("src/main/java/com/dpis/module/runtime/font/FlutterSettingsFontHookInstaller.java")

        // Temporary selftest/probe prefixes must not remain
        Assert.assertFalse(
            "SELFTEST probe must be removed",
            moduleMain.contains("DPIS_HOOK_SELFTEST")
        )
        Assert.assertFalse(
            "RUNTIME_PROBE must be removed",
            moduleMain.contains("DPIS_RUNTIME_PROBE")
        )
        Assert.assertFalse("APPCLASS probe must be removed", moduleMain.contains("DPIS_APPCLASS"))
        Assert.assertFalse("SHELL probe must be removed", moduleMain.contains("DPIS_SHELL"))
        Assert.assertFalse(
            "CL_CAPABILITY probe must be removed",
            moduleMain.contains("DPIS_CL_CAPABILITY")
        )
        Assert.assertFalse(
            "module-loaded install path must not keep temporary probe wording",
            moduleMain.contains("module-loaded app hook probe")
        )

        // Packer-specific class names must not appear in production sources
        Assert.assertFalse(
            "shell packer class must not be in ModuleMain",
            moduleMain.contains("s.h.e.l.l")
        )
        Assert.assertFalse(
            "target app package must not be hardcoded in ModuleMain",
            moduleMain.contains("com.mfcloudcalculate.networkdisk")
        )
        Assert.assertFalse(
            "shell packer class must not be in FlutterSettingsFontHookInstaller",
            flutterInstaller.contains("s.h.e.l.l")
        )
        Assert.assertFalse(
            "target app package must not be in FlutterSettingsFontHookInstaller",
            flutterInstaller.contains("com.mfcloudcalculate.networkdisk")
        )
    }

    @Test
    @Throws(IOException::class)
    fun debugBuildKeepsRuntimeHookLogsVisible() {
        val source: String = read("src/main/java/com/dpis/module/diagnostics/DpisLog.java")

        Assert.assertTrue(source.contains("BuildConfig.DEBUG || isLoggingEnabled()"))
    }

    @Test
    @Throws(IOException::class)
    fun moduleMainUsesPackagePlanHookEligibilityGate() {
        val moduleMain: String = read("src/modern/java/com/dpis/module/ModuleMain.java")

        Assert.assertTrue(moduleMain.contains("if (!packagePlan.shouldInstallHooks())"))
        Assert.assertFalse(
            moduleMain.contains(
                ("packagePlan.targetViewportWidthDp == null"
                        + System.lineSeparator()
                        + "                && !packagePlan.fontScaleActive")
            )
        )
    }

    @Test
    @Throws(IOException::class)
    fun modernRuntimeApiCapabilityDocsAndScriptMatchCurrentBoundary() {
        val docs: String = readRepositoryRoot("docs/modern-runtime-resync.md")
        val script: String = readRepositoryRoot("scripts/pull-lsposed-logs.ps1")

        Assert.assertTrue(docs.contains("declares `minApiVersion=101`"))
        Assert.assertTrue(docs.contains("`targetApiVersion=102`"))
        Assert.assertTrue(docs.contains("declare `autoHotReload=true`"))
        Assert.assertTrue(docs.contains("API 102 hosts can use the hot-reload lifecycle"))
        Assert.assertTrue(docs.contains("API 101 hosts keep"))
        Assert.assertTrue(docs.contains("install-and-restart path"))
        Assert.assertTrue(docs.contains("stable hook ids"))
        Assert.assertTrue(docs.contains("framework exposes"))
        Assert.assertTrue(docs.contains("degrades to the 101 capability set"))
        Assert.assertTrue(docs.contains("ForceTextSize hook ready"))
        Assert.assertTrue(docs.contains("dynamic resource-creation / `createResourcesImpl` overload hooks derive ids"))
        Assert.assertTrue(script.contains("[string] \$Device"))
        Assert.assertTrue(script.contains("ls -t /data/adb/lspd/log/modules_*.log"))
        Assert.assertTrue(script.contains("verbose_*.log"))
        Assert.assertFalse(script.contains("192.168.5.130:5555"))
    }

    @Test
    @Throws(IOException::class)
    fun moduleMainAllowsPackageOwnedSecondaryProcessesForViewportHooks() {
        val moduleMain: String = read("src/modern/java/com/dpis/module/ModuleMain.java")

        Assert.assertTrue(moduleMain.contains("!processName.startsWith(packagePlan.packageName + \":\")"))
    }

    @Test
    @Throws(IOException::class)
    fun nativeFontHookUsesRustEnvironmentAsRuntimeFontSource() {
        val nativeSource: String = read("src/main/cpp/dpis_native.cpp")

        Assert.assertTrue(nativeSource.contains("DPIS_FONT_SCALE_PERCENT"))
        Assert.assertTrue(nativeSource.contains("std::getenv"))
        Assert.assertTrue(nativeSource.contains("read_proc_cmdline_value"))
        Assert.assertTrue(nativeSource.contains("value == \"false\" || value == \"disabled\""))
        Assert.assertTrue(nativeSource.contains("return g_enabled.load(std::memory_order_relaxed)"))
    }

    companion object {
        @Throws(IOException::class)
        private fun read(relativePath: String?): String {
            return SourceSmokeTestPaths.read(relativePath)
        }

        @Throws(IOException::class)
        private fun readRepositoryRoot(relativePath: String?): String {
            return SourceSmokeTestPaths.readRepositoryRoot(relativePath)
        }
    }
}
