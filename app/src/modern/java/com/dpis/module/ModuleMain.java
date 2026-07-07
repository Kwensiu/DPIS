package com.dpis.module;

import com.dpis.module.settings.SystemScopeCoordinator;

import com.dpis.module.config.ConfigSnapshot;
import com.dpis.module.config.ConfigSnapshotLoader;
import com.dpis.module.config.ModulePackagePlan;
import com.dpis.module.config.RuntimePropertyConfigPreferences;


import com.dpis.module.diagnostics.FeedbackDiagnosticRuntimeHotPathEvents;

import com.dpis.module.diagnostics.FeedbackDiagnosticRuntimeEvents;

import com.dpis.module.runtime.ModuleRuntimeStateReporter;
import com.dpis.module.runtime.XposedSelfActivation;
import com.dpis.module.runtime.systemserver.SystemServerDisplayDiagnostics;
import com.dpis.module.runtime.systemserver.SystemServerDisplayEnvironmentInstaller;
import com.dpis.module.runtime.systemserver.SystemServerMutationPolicy;
import com.dpis.module.runtime.systemserver.SystemServerProcess;

import com.dpis.module.runtime.appprocess.WebApkRuntimeOwnerBridge;

import com.dpis.module.runtime.appprocess.AppProcessHotReloadResetter;

import com.dpis.module.runtime.appprocess.AppProcessHookInstaller;

import com.dpis.module.runtime.font.DebugFontOverride;
import com.dpis.module.runtime.font.FlutterSettingsFontHookInstaller;

import com.dpis.module.runtime.appprocess.ChromiumViewportProbeHookInstaller;

import com.dpis.module.hooks.HookExecutionPlan;
import com.dpis.module.hooks.HookRuntimePolicy;
import com.dpis.module.runtime.hookapi.ModernApiCapabilities;
import com.dpis.module.runtime.hookapi.ModernApiCapabilitiesResolver;

import com.dpis.module.fonts.FontDebugStatsTransport;

import com.dpis.module.runtime.DebugPackageOverride;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.os.Build;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public final class ModuleMain extends XposedModule {
    private static final String BRIDGE_LOG_PREFIX = "DPIS ";
    private static final String PROP_CHROMIUM_VIEWPORT_PROBE_PACKAGE =
            "debug.dpis.webapk.chromium_probe_package";
    private volatile DpisConfigStore configStore;
    private volatile ModernApiCapabilities modernApiCapabilities;
    private volatile boolean moduleLoadedObserved;
    private volatile boolean systemServerInstallAttempted;
    private volatile boolean firstPackageReadyLogged;
    private volatile boolean appProcessInstallAttempted;
    private volatile String currentProcessName = "unknown";
    private volatile String lastPackageReadyPackageName;
    private volatile ClassLoader lastPackageReadyClassLoader;
    private volatile ApplicationInfo lastPackageReadyApplicationInfo;

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        moduleLoadedObserved = true;
        currentProcessName = param.getProcessName();
        configStore = ConfigStoreFactory.createForXposedHost(this);
        FontDebugStatsTransport.initialize(this);
        DpisLog.setLoggingEnabled(configStore.isGlobalLogEnabled());
        String message = "module loaded: process=" + param.getProcessName()
                + ", marker=" + SystemServerDisplayDiagnostics.BUILD_MARKER;
        if (SystemServerProcess.isSystemServer(param.getProcessName(), "")) {
            ModuleRuntimeStateReporter.reportSystemServerLoaded();
        }
        SystemServerDisplayDiagnostics.recordPending(
                message);
        DpisLog.i(message);
        log(android.util.Log.INFO, "DPIS", BRIDGE_LOG_PREFIX + message);
        try {
            ModernAppSpecificRouteInstaller.handleModuleLoaded(this, param.getProcessName());
            if (SystemServerProcess.isSystemServer(param.getProcessName(), "")) {
                HookRuntimePolicy policy = resolveSystemServerRuntimePolicy(
                        configStore, param.getProcessName());
                maybeInstallSystemServerHooks(configStore, policy, param.getProcessName(), "android",
                        null,
                        "module-loaded");
            }
            maybeInstallAppProcessFromModuleLoaded(configStore, param.getProcessName());
        } catch (Throwable throwable) {
            rawBridgeLog("module-loaded app hook install failed: process=" + param.getProcessName()
                    + ", error=" + throwable.getClass().getName()
                    + ": " + throwable.getMessage());
            DpisLog.e("module-loaded app hook install failed: process=" + param.getProcessName(),
                    throwable);
        }
    }

    @Override
    public void onSystemServerStarting(SystemServerStartingParam param) {
        DpisConfigStore store = getOrCreateConfigStore();
        String processName = currentProcessName;
        if (!SystemServerProcess.isSystemServer(processName, "android")) {
            processName = "system";
        }
        HookRuntimePolicy policy = resolveSystemServerRuntimePolicy(store, processName);
        bridgeRuntimeLog("system_server starting hook install enter: process=" + processName
                + ", classLoader=" + describeClassLoader(param));
        maybeInstallSystemServerHooks(store, policy, processName, "android",
                param != null ? param.getClassLoader() : null,
                "system-server-starting");
    }

    @Override
    public void onPackageLoaded(PackageLoadedParam param) {
        String processName = resolveCurrentProcessName();
        log(android.util.Log.INFO, "DPIS", BRIDGE_LOG_PREFIX + "onPackageLoaded enter: process="
                + processName + ", package=" + param.getPackageName());
        DpisConfigStore store = getOrCreateConfigStore();
        try {
            maybeInstallAppProcessFromPackageLoaded(store, processName, param.getPackageName());
        } catch (Throwable throwable) {
            rawBridgeLog("package-loaded app hook install failed: process=" + processName
                    + ", package=" + param.getPackageName()
                    + ", error=" + throwable.getClass().getName()
                    + ": " + throwable.getMessage());
            DpisLog.e("package-loaded app hook install failed: process=" + processName
                    + ", package=" + param.getPackageName(), throwable);
        }
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        log(android.util.Log.INFO, "DPIS", BRIDGE_LOG_PREFIX + "onPackageReady enter: process=" + currentProcessName
                + ", package=" + param.getPackageName());
        XposedSelfActivation.markIfSelfPackage(
                param.getPackageName(),
                param.getClassLoader(),
                "libxposed-package-ready");
        rememberPackageReady(param.getPackageName(), param.getClassLoader(),
                param.getApplicationInfo());
        DpisConfigStore store = getOrCreateConfigStore();
        ConfigSnapshot snapshot = ConfigSnapshotLoader.fromStore(store);
        HookRuntimePolicy systemPolicy = resolveSystemServerRuntimePolicy(store, currentProcessName);
        HookRuntimePolicy appProcessPolicy = resolveHookedRuntimePolicy(store);
        DpisLog.setLoggingEnabled(appProcessPolicy.globalLogEnabled);
        log(android.util.Log.INFO, "DPIS", BRIDGE_LOG_PREFIX + "package ready: process=" + currentProcessName
                + ", package=" + param.getPackageName());
        SystemServerDisplayDiagnostics.flushPending();
        maybeInstallSystemServerHooks(store, systemPolicy, currentProcessName, param.getPackageName(),
                null,
                "package-ready");
        maybeLogFirstPackageReady(param.getPackageName());
        if (ModernAppSpecificRouteInstaller.handlePackageReady(this, param, currentProcessName)) {
            return;
        }
        installAppProcessHooksIfConfigured(store, appProcessPolicy, snapshot, param.getPackageName(),
                "package-ready");
        installChromiumViewportProbe(param.getPackageName(), param.getClassLoader());
        retryTypefaceHooksWithPackageReady(store, snapshot, param.getPackageName());
        retryFlutterHooksWithAppClassLoader(store, snapshot, param.getClassLoader(),
                param.getPackageName());
    }

    @Override
    public boolean onHotReloading(XposedModuleInterface.HotReloadingParam param) {
        log(android.util.Log.INFO, "DPIS", BRIDGE_LOG_PREFIX
                + "hot reload begin: process=" + currentProcessName);
        FeedbackDiagnosticRuntimeEvents.recordHotReload(
                currentProcessName,
                "runtime",
                "begin",
                "hot reload begin: process=" + currentProcessName);
        // API 102 does not replay package-ready for us. Carry only framework/app
        // objects that are not owned by the old module classloader so the new
        // generation can retry classloader-dependent supplement hooks.
        param.setSavedInstanceState(new Object[] {
                currentProcessName,
                lastPackageReadyPackageName,
                lastPackageReadyClassLoader,
                lastPackageReadyApplicationInfo
        });
        return true;
    }

    @Override
    public void onHotReloaded(XposedModuleInterface.HotReloadedParam param) {
        Object savedState = param.getSavedInstanceState();
        PackageReadyReplayState replayState = restoreHotReloadState(savedState);
        currentProcessName = replayState.processName != null
                ? replayState.processName
                : currentProcessName;
        rememberPackageReady(replayState.packageName, replayState.classLoader,
                replayState.applicationInfo);
        firstPackageReadyLogged = false;
        appProcessInstallAttempted = false;
        AppProcessHotReloadResetter.resetAll();
        DpisConfigStore store = getOrCreateConfigStore();
        try {
            log(android.util.Log.INFO, "DPIS", BRIDGE_LOG_PREFIX
                    + "hot reload replay: process=" + currentProcessName
                    + ", systemServerAttempted=" + systemServerInstallAttempted
                    + ", appAttempted=" + appProcessInstallAttempted);
            FeedbackDiagnosticRuntimeEvents.recordHotReload(
                    currentProcessName,
                    "runtime",
                    "replay",
                    "hot reload replay: process=" + currentProcessName
                            + ", systemServerAttempted=" + systemServerInstallAttempted
                            + ", appAttempted=" + appProcessInstallAttempted);
            replaySystemServerAfterHotReload(store, currentProcessName);
            maybeInstallAppProcessFromModuleLoaded(store, currentProcessName);
            replayPackageReadySupplementsAfterHotReload(
                    store,
                    replayState.packageName,
                    replayState.classLoader,
                    replayState.applicationInfo);
        } finally {
            log(android.util.Log.INFO, "DPIS", BRIDGE_LOG_PREFIX
                    + "hot reload end: process=" + currentProcessName);
            FeedbackDiagnosticRuntimeEvents.recordHotReload(
                    currentProcessName,
                    "runtime",
                    "end",
                    "hot reload end: process=" + currentProcessName);
        }
    }

    private void replaySystemServerAfterHotReload(DpisConfigStore store, String processName) {
        if (!SystemServerProcess.isSystemServer(processName, "")) {
            DpisLog.i("system_server hot reload skipped: process=" + processName);
            return;
        }
        bridgeHotReloadLog("system_server hot reload replay enter: process=" + processName);
        SystemServerDisplayEnvironmentInstaller.resetForHotReload();
        systemServerInstallAttempted = false;
        HookRuntimePolicy policy = resolveSystemServerRuntimePolicy(store, processName);
        maybeInstallSystemServerHooks(
                store,
                policy,
                processName,
                "android",
                null,
                "hot-reload");
    }

    private void rememberPackageReady(String packageName, ClassLoader classLoader,
            ApplicationInfo applicationInfo) {
        if (packageName == null || classLoader == null) {
            return;
        }
        lastPackageReadyPackageName = packageName;
        lastPackageReadyClassLoader = classLoader;
        lastPackageReadyApplicationInfo = applicationInfo;
    }

    private static PackageReadyReplayState restoreHotReloadState(Object savedState) {
        if (savedState instanceof Object[] values) {
            String processName = values.length > 0 && values[0] instanceof String value
                    ? value
                    : null;
            String packageName = values.length > 1 && values[1] instanceof String value
                    ? value
                    : null;
            ClassLoader classLoader = values.length > 2 && values[2] instanceof ClassLoader value
                    ? value
                    : null;
            ApplicationInfo applicationInfo =
                    values.length > 3 && values[3] instanceof ApplicationInfo value
                            ? value
                            : null;
            return new PackageReadyReplayState(
                    processName, packageName, classLoader, applicationInfo);
        }
        return new PackageReadyReplayState(
                savedState instanceof String value ? value : null,
                null,
                null,
                null);
    }

    private void replayPackageReadySupplementsAfterHotReload(DpisConfigStore store,
            String packageName,
            ClassLoader classLoader,
            ApplicationInfo applicationInfo) {
        if (packageName == null || classLoader == null) {
            bridgeHotReloadLog("package-ready hot reload replay skipped: process=" + currentProcessName
                    + ", package=" + packageName + ", classLoaderMissing="
                    + (classLoader == null));
            return;
        }
        if (SystemServerProcess.isSystemServer(currentProcessName, packageName)) {
            bridgeHotReloadLog("package-ready hot reload replay skipped system process: process="
                    + currentProcessName + ", package=" + packageName);
            return;
        }
        bridgeHotReloadLog("package-ready hot reload replay enter: process=" + currentProcessName
                + ", package=" + packageName
                + ", classLoader=" + classLoader.getClass().getName());
        ConfigSnapshot snapshot = ConfigSnapshotLoader.fromStore(store);
        ModernAppSpecificRouteInstaller.handlePackageReadyReplay(
                this, packageName, classLoader, applicationInfo, currentProcessName);
        retryTypefaceHooksWithPackageReady(store, snapshot, packageName);
        installChromiumViewportProbe(packageName, classLoader);
        retryFlutterHooksWithAppClassLoader(store, snapshot, classLoader, packageName);
    }

    private void installChromiumViewportProbe(String packageName, ClassLoader classLoader) {
        if (!WebApkRuntimeOwnerBridge.CHROME_PACKAGE.equals(packageName)
                || classLoader == null
                || !DebugPackageOverride.matches(PROP_CHROMIUM_VIEWPORT_PROBE_PACKAGE,
                        packageName)) {
            return;
        }
        try {
            ChromiumViewportProbeHookInstaller.install(this, classLoader);
        } catch (Throwable throwable) {
            DpisLog.e("DPIS_WEBAPK Chromium viewport probe failed", throwable);
        }
    }

    private void maybeInstallAppProcessFromModuleLoaded(DpisConfigStore store, String processName) {
        rawBridgeLog("module-loaded app hook install enter: process=" + processName);
        if (SystemServerProcess.isSystemServer(processName, "")) {
            rawBridgeLog("module-loaded app hook install skipped system process: process="
                    + processName);
            return;
        }
        String packageName = packageNameFromProcessName(processName);
        DpisConfigStore runtimeStore = createModuleLoadedAppProcessStore(store, packageName);
        ConfigSnapshot snapshot = ConfigSnapshotLoader.fromStore(runtimeStore);
        String source = "module-loaded";
        if (!snapshot.isConfigured(packageName)) {
            ConfigSnapshot fallbackSnapshot = ConfigSnapshotLoader.fromStore(store);
            if (!fallbackSnapshot.isConfigured(packageName)) {
                rawBridgeLog("module-loaded app config unavailable: process=" + processName
                        + ", package=" + packageName
                        + ", propertyPackages=" + snapshot.getConfiguredPackages()
                        + ", fallbackPackages=" + fallbackSnapshot.getConfiguredPackages());
                return;
            }
            runtimeStore = store;
            snapshot = fallbackSnapshot;
            source = "module-loaded-fallback";
            rawBridgeLog("module-loaded app config fallback: process=" + processName
                    + ", package=" + packageName
                    + ", packages=" + snapshot.getConfiguredPackages());
        }
        HookRuntimePolicy policy = resolveHookedRuntimePolicy(store);
        DpisLog.setLoggingEnabled(policy.globalLogEnabled);
        if (ModernAppSpecificRouteInstaller.shouldSuppressModuleLoadedGenericHooks(
                packageName, processName)) {
            return;
        }
        installAppProcessHooksIfConfigured(runtimeStore, policy, snapshot, packageName,
                source);
    }

    private void maybeInstallAppProcessFromPackageLoaded(DpisConfigStore store,
            String processName,
            String packageName) {
        rawBridgeLog("package-loaded app hook install enter: process=" + processName
                + ", package=" + packageName);
        if (SystemServerProcess.isSystemServer(processName, packageName)) {
            rawBridgeLog("package-loaded app hook install skipped system process: process="
                    + processName + ", package=" + packageName);
            return;
        }
        ConfigSnapshot snapshot = ConfigSnapshotLoader.fromStore(store);
        HookRuntimePolicy policy = resolveHookedRuntimePolicy(store);
        DpisLog.setLoggingEnabled(policy.globalLogEnabled);
        if (ModernAppSpecificRouteInstaller.shouldSuppressModuleLoadedGenericHooks(
                packageName, processName)) {
            return;
        }
        installAppProcessHooksIfConfigured(store, policy, snapshot, packageName,
                "package-loaded");
    }

    private String resolveCurrentProcessName() {
        String processName = currentProcessName;
        if (isKnownProcessName(processName)) {
            return processName;
        }
        String runtimeProcessName = runtimeProcessName();
        if (isKnownProcessName(runtimeProcessName)) {
            currentProcessName = runtimeProcessName;
            return runtimeProcessName;
        }
        return processName;
    }

    private static boolean isKnownProcessName(String processName) {
        return processName != null && !processName.isBlank() && !"unknown".equals(processName);
    }

    private static String runtimeProcessName() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            String processName = Application.getProcessName();
            if (isKnownProcessName(processName)) {
                return processName;
            }
        }
        // onPackageLoaded has no process-name accessor in libxposed API 101.
        // Keep Android 8.x supported by reading the kernel-provided cmdline.
        return readProcSelfCmdline();
    }

    private static String readProcSelfCmdline() {
        try {
            byte[] bytes = Files.readAllBytes(new File("/proc/self/cmdline").toPath());
            int length = 0;
            while (length < bytes.length && bytes[length] != 0) {
                length++;
            }
            return new String(bytes, 0, length, StandardCharsets.UTF_8).trim();
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private static DpisConfigStore createModuleLoadedAppProcessStore(DpisConfigStore fallbackStore,
            String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return fallbackStore;
        }
        // module-loaded runs before package-ready and only has the process name.
        // Secondary app processes are configured by their owning package, not by
        // the full process name suffix.
        // For per-app runtime mirrors, treat auto viewport as the app-process
        // projection route. Relative scale intentionally avoids system_server
        // viewport mutation, while absolute targets may still use system_server.
        return new DpisConfigStore(
                new RuntimePropertyConfigPreferences(packageName,
                        RuntimePropertyConfigPreferences.AutoViewportRuntimeRoute.ANY_ENABLED_TARGET));
    }

    private static String packageNameFromProcessName(String processName) {
        if (processName == null) {
            return null;
        }
        int separator = processName.indexOf(':');
        return separator > 0 ? processName.substring(0, separator) : processName;
    }

    private void installAppProcessHooksIfConfigured(DpisConfigStore store,
            HookRuntimePolicy policy,
            ConfigSnapshot snapshot,
            String requestedPackageName,
            String source) {
        if (appProcessInstallAttempted) {
            return;
        }
        ModulePackagePlan packagePlan = ModulePackagePlan.resolve(snapshot, requestedPackageName);
        String packageName = packagePlan.packageName;
        if (!snapshot.isConfigured(packageName)) {
            DpisLog.i("package not configured: package=" + packageName);
            return;
        }
        if (!packagePlan.targetDpisEnabled) {
            DpisLog.i("target app disabled by dpis toggle: package=" + packageName);
            return;
        }
        if (shouldSuppressSecondaryProcessViewport(currentProcessName, packagePlan)) {
            DpisLog.i("secondary process viewport route suppressed: process="
                    + currentProcessName + ", package=" + packageName
                    + ", viewportMode=" + packagePlan.targetViewportMode);
            packagePlan = packagePlan.withoutViewportRoute();
            if (!packagePlan.hasSecondaryProcessSafeRoute()) {
                DpisLog.i("target app disabled after secondary process viewport suppression: process="
                        + currentProcessName + ", package=" + packageName);
                return;
            }
        }
        if (!packagePlan.shouldInstallHooks()) {
            DpisLog.i("target app disabled: package=" + packageName);
            return;
        }
        DpisLog.i("target app matched: package=" + packageName
                + ", source=" + source
                + ", targetViewportSpec=" + packagePlan.targetViewportSpec
                + ", targetViewportMode=" + packagePlan.targetViewportMode
                + ", targetFontScalePercent=" + packagePlan.targetFontScalePercent
                + ", targetFontMode=" + packagePlan.targetFontMode
                + ", typefaceActive=" + packagePlan.typefaceActive
                + ", targetTypefaceId=" + packagePlan.targetTypefaceId
                + ", flutterSettingsFont=" + packagePlan.flutterSettingsFontEnabled
                + ", hyperOsNativeFlutterFont=" + packagePlan.hyperOsNativeFlutterFontEnabled);
        FeedbackDiagnosticRuntimeHotPathEvents.probe(
                packageName,
                "process_entry",
                "process-entry source=" + source
                        + ", process=" + currentProcessName
        );
        appProcessInstallAttempted = true;
        try {
            AppProcessHookInstaller.install(
                    this, store, policy, packagePlan, getModernApiCapabilities());
        } catch (Throwable throwable) {
            appProcessInstallAttempted = false;
            DpisLog.e("failed to install app process hooks", throwable);
        }
    }

    private static boolean shouldSuppressSecondaryProcessViewport(String processName,
                                                                  ModulePackagePlan packagePlan) {
        if (processName == null || processName.isBlank() || packagePlan == null
                || packagePlan.packageName == null || packagePlan.packageName.isBlank()) {
            return false;
        }
        return !processName.equals(packagePlan.packageName)
                && !processName.startsWith(packagePlan.packageName + ":")
                && packagePlan.viewportEnabled;
    }

    private void retryFlutterHooksWithAppClassLoader(DpisConfigStore store,
            ConfigSnapshot snapshot,
            ClassLoader classLoader,
            String packageName) {
        if (classLoader == null || packageName == null) {
            return;
        }
        if (SystemServerProcess.isSystemServer(currentProcessName, packageName)) {
            return;
        }
        ModulePackagePlan packagePlan = ModulePackagePlan.resolve(snapshot, packageName);
        if (!packagePlan.targetDpisEnabled || !packagePlan.fontScaleActive) {
            return;
        }
        HookRuntimePolicy policy = HookRuntimePolicy.fromSnapshot(snapshot);
        DebugFontOverride debugOverride = AppProcessHookInstaller
                .resolveDebugFontOverrideForPackage(packageName);
        HookExecutionPlan executionPlan = packagePlan.buildExecutionPlan(policy, debugOverride);
        if (!executionPlan.flutterSettingsEnabled) {
            return;
        }
        log(android.util.Log.INFO, "DPIS", BRIDGE_LOG_PREFIX + "flutter-retry proceeding: package=" + packageName
                + ", classLoader=" + classLoader.getClass().getName()
                + ", fontPercent=" + packagePlan.targetFontScalePercent
                + ", fontMode=" + packagePlan.targetFontMode);
        FlutterSettingsFontHookInstaller.retryWithAppClassLoader(
                this, packageName, store, executionPlan.fontDomainPlan, classLoader);
    }

    private void retryTypefaceHooksWithPackageReady(DpisConfigStore store,
            ConfigSnapshot snapshot,
            String packageName) {
        if (SystemServerProcess.isSystemServer(currentProcessName, packageName)) {
            return;
        }
        ModulePackagePlan packagePlan = ModulePackagePlan.resolve(snapshot, packageName);
        if (!packagePlan.targetDpisEnabled || !packagePlan.typefaceActive) {
            return;
        }
        AppProcessHookInstaller.installTypefaceHooks(
                this,
                packagePlan.packageName,
                store,
                packagePlan.targetTypefaceId);
    }

    private DpisConfigStore getOrCreateConfigStore() {
        DpisConfigStore local = configStore;
        if (local == null) {
            local = ConfigStoreFactory.createForXposedHost(this);
            FontDebugStatsTransport.initialize(this);
            configStore = local;
        }
        return local;
    }

    private ModernApiCapabilities getModernApiCapabilities() {
        ModernApiCapabilities local = modernApiCapabilities;
        if (local == null) {
            local = ModernApiCapabilitiesResolver.fromXposed(this);
            modernApiCapabilities = local;
        }
        return local;
    }

    private HookRuntimePolicy resolveSystemServerRuntimePolicy(DpisConfigStore store,
            String processName) {
        if (SystemServerProcess.isSystemServer(processName, "")) {
            // Reaching the system process is the runtime scope proof. XposedService
            // is a UI-side capability and may be unavailable inside hooked runtime
            // processes, so keep this path tied to the stored user switch.
            return HookRuntimePolicy.fromStore(store);
        }
        return HookRuntimePolicy.fromEffectiveSystemHookState(
                store,
                SystemScopeCoordinator.resolveSystemHookEffectiveEnabled(store));
    }

    private HookRuntimePolicy resolveHookedRuntimePolicy(DpisConfigStore store) {
        // If this code is running inside a hooked process, that process already
        // proves the module scope. Do not downgrade route planning just because
        // XposedService is unavailable inside the runtime process.
        return HookRuntimePolicy.fromStore(store);
    }

    private static String describeClassLoader(SystemServerStartingParam param) {
        if (param == null || param.getClassLoader() == null) {
            return "null";
        }
        return param.getClassLoader().getClass().getName();
    }

    private void maybeInstallSystemServerHooks(DpisConfigStore store,
            HookRuntimePolicy policy,
            String processName,
            String packageName,
            ClassLoader systemServerClassLoader,
            String source) {
        if (systemServerInstallAttempted) {
            return;
        }
        if (!SystemServerMutationPolicy.shouldInstallSystemServerHooks(
                processName,
                packageName,
                policy)) {
            return;
        }
        synchronized (this) {
            if (systemServerInstallAttempted) {
                return;
            }
            systemServerInstallAttempted = true;
            try {
                SystemServerDisplayEnvironmentInstaller.InstallResult result =
                        SystemServerDisplayEnvironmentInstaller.install(
                        this, store, getModernApiCapabilities(), systemServerClassLoader);
                systemServerInstallAttempted = result.isComplete();
                String message = "system_server installer "
                        + (result.hasInstalledHooks() ? "ready" : "no-hooks")
                        + ": source=" + source
                        + ", process=" + processName
                        + ", package=" + packageName
                        + ", installed=" + result.installedCount
                        + ", missing=" + result.missingCount
                        + ", alreadyInstalled=" + result.alreadyInstalled
                        + ", installedEntries=" + result.installedEntries
                        + ", missingEntries=" + result.missingEntries;
                bridgeRuntimeLog(message);
                if ("hot-reload".equals(source)) {
                    bridgeHotReloadLog("system_server hot reload replay "
                            + (result.hasInstalledHooks() ? "ready" : "no-hooks")
                            + ": process=" + processName + ", package=" + packageName
                            + ", installed=" + result.installedCount
                            + ", missing=" + result.missingCount
                            + ", alreadyInstalled=" + result.alreadyInstalled
                            + ", installedEntries=" + result.installedEntries
                            + ", missingEntries=" + result.missingEntries);
                }
            } catch (Throwable throwable) {
                systemServerInstallAttempted = false;
                DpisLog.e("system_server installer failed", throwable);
                rawBridgeLog("system_server installer failed: source=" + source
                        + ", error=" + throwable.getClass().getName()
                        + ": " + throwable.getMessage());
                if ("hot-reload".equals(source)) {
                    bridgeHotReloadLog("system_server hot reload replay failed: process="
                            + processName + ", error=" + throwable.getClass().getName()
                            + ": " + throwable.getMessage());
                }
            }
        }
    }

    private void maybeLogFirstPackageReady(String packageName) {
        if (firstPackageReadyLogged) {
            return;
        }
        synchronized (this) {
            if (firstPackageReadyLogged) {
                return;
            }
            DpisLog.i(SystemServerDisplayDiagnostics.buildPackageReadyStateLog(
                    currentProcessName,
                    packageName,
                    moduleLoadedObserved,
                    systemServerInstallAttempted));
            firstPackageReadyLogged = true;
        }
    }

    private static void rawBridgeLog(String message) {
        android.util.Log.i("DPIS", BRIDGE_LOG_PREFIX + message);
    }

    private void bridgeRuntimeLog(String message) {
        log(android.util.Log.INFO, "DPIS", BRIDGE_LOG_PREFIX + message);
        rawBridgeLog(message);
    }

    private void bridgeHotReloadLog(String message) {
        log(android.util.Log.INFO, "DPIS", BRIDGE_LOG_PREFIX + message);
        rawBridgeLog(message);
    }

    private static final class PackageReadyReplayState {
        final String processName;
        final String packageName;
        final ClassLoader classLoader;
        final ApplicationInfo applicationInfo;

        PackageReadyReplayState(String processName,
                String packageName,
                ClassLoader classLoader,
                ApplicationInfo applicationInfo) {
            this.processName = processName;
            this.packageName = packageName;
            this.classLoader = classLoader;
            this.applicationInfo = applicationInfo;
        }
    }
}
