package com.dpis.module;

import java.util.List;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public final class ModuleMain extends XposedModule {
    private static final String BRIDGE_LOG_PREFIX = "DPIS ";
    private volatile DpiConfigStore configStore;
    private volatile boolean moduleLoadedObserved;
    private volatile boolean systemServerInstallAttempted;
    private volatile boolean firstPackageReadyLogged;
    private volatile boolean appProcessInstallAttempted;
    private volatile ModernHookRegistry hookRegistry;
    private volatile String currentProcessName = "unknown";

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
        DpiConfigStore store = getOrCreateConfigStore();
        HookRuntimePolicy policy = HookRuntimePolicy.fromNullableStore(store);
        String processName = currentProcessName;
        if (!SystemServerProcess.isSystemServer(processName, "android")) {
            processName = "system";
        }
        rawBridgeLog("system_server starting hook install enter: process=" + processName
                + ", classLoader=" + describeClassLoader(param));
        maybeInstallSystemServerHooks(store, policy, processName, "android",
                "system-server-starting");
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        log(android.util.Log.INFO, "DPIS", BRIDGE_LOG_PREFIX + "onPackageReady enter: process=" + currentProcessName
                + ", package=" + param.getPackageName());
        XposedSelfActivation.markIfSelfPackage(
                param.getPackageName(),
                param.getClassLoader(),
                "libxposed-package-ready");
        DpiConfigStore store = getOrCreateConfigStore();
        ConfigSnapshot snapshot = ConfigSnapshotLoader.fromStore(store);
        HookRuntimePolicy policy = HookRuntimePolicy.fromSnapshot(snapshot);
        DpisLog.setLoggingEnabled(policy.globalLogEnabled);
        log(android.util.Log.INFO, "DPIS", BRIDGE_LOG_PREFIX + "package ready: process=" + currentProcessName
                + ", package=" + param.getPackageName());
        SystemServerDisplayDiagnostics.flushPending();
        maybeInstallSystemServerHooks(store, policy, currentProcessName, param.getPackageName(),
                "package-ready");
        maybeLogFirstPackageReady(param.getPackageName());
        if (ModernAppSpecificRouteInstaller.handlePackageReady(this, param, currentProcessName)) {
            return;
        }
        installAppProcessHooksIfConfigured(store, policy, snapshot, param.getPackageName(),
                "package-ready");
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
        param.setSavedInstanceState(currentProcessName);
        return true;
    }

    @Override
    public void onHotReloaded(XposedModuleInterface.HotReloadedParam param) {
        Object savedState = param.getSavedInstanceState();
        currentProcessName = savedState instanceof String value ? value : currentProcessName;
        systemServerInstallAttempted = false;
        firstPackageReadyLogged = false;
        appProcessInstallAttempted = false;
        ModernHookRegistry registry = getOrCreateHookRegistry();
        registry.clear();
        ResourcesManagerHookInstaller.resetForHotReload();
        ResourcesImplHookInstaller.resetForHotReload();
        ResourcesReadHookInstaller.resetForHotReload();
        DpiConfigStore store = getOrCreateConfigStore();
        HookRuntimePolicy policy = HookRuntimePolicy.fromNullableStore(store);
        try {
            log(android.util.Log.INFO, "DPIS", BRIDGE_LOG_PREFIX
                    + "hot reload replay: process=" + currentProcessName
                    + ", systemServerAttempted=" + !systemServerInstallAttempted
                    + ", appAttempted=" + !appProcessInstallAttempted);
            FeedbackDiagnosticRuntimeEvents.recordHotReload(
                    currentProcessName,
                    "runtime",
                    "replay",
                    "hot reload replay: process=" + currentProcessName
                            + ", systemServerAttempted=" + !systemServerInstallAttempted
                            + ", appAttempted=" + !appProcessInstallAttempted);
            maybeInstallSystemServerHooks(store, policy, currentProcessName, "android",
                    "hot-reload");
            maybeInstallAppProcessFromModuleLoaded(store, currentProcessName);
        } finally {
            unhookLegacyHotReloadHandles(param.getOldHookHandles(), registry);
            log(android.util.Log.INFO, "DPIS", BRIDGE_LOG_PREFIX
                    + "hot reload end: process=" + currentProcessName);
            FeedbackDiagnosticRuntimeEvents.recordHotReload(
                    currentProcessName,
                    "runtime",
                    "end",
                    "hot reload end: process=" + currentProcessName);
        }
    }

    private void maybeInstallAppProcessFromModuleLoaded(DpiConfigStore store, String processName) {
        rawBridgeLog("module-loaded app hook install enter: process=" + processName);
        if (SystemServerProcess.isSystemServer(processName, "")) {
            rawBridgeLog("module-loaded app hook install skipped system process: process="
                    + processName);
            return;
        }
        String packageName = packageNameFromProcessName(processName);
        DpiConfigStore runtimeStore = createModuleLoadedAppProcessStore(store, packageName);
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
        HookRuntimePolicy policy = HookRuntimePolicy.fromSnapshot(snapshot);
        DpisLog.setLoggingEnabled(policy.globalLogEnabled);
        if (ModernAppSpecificRouteInstaller.shouldSuppressModuleLoadedGenericHooks(
                packageName, processName)) {
            return;
        }
        installAppProcessHooksIfConfigured(runtimeStore, policy, snapshot, packageName,
                source);
    }

    private static DpiConfigStore createModuleLoadedAppProcessStore(DpiConfigStore fallbackStore,
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
        return new DpiConfigStore(
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

    private void installAppProcessHooksIfConfigured(DpiConfigStore store,
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
            AppProcessHookInstaller.install(this, store, policy, packagePlan, getOrCreateHookRegistry());
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

    private void retryFlutterHooksWithAppClassLoader(DpiConfigStore store,
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

    private void retryTypefaceHooksWithPackageReady(DpiConfigStore store,
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

    ModernHookRegistry getOrCreateHookRegistry() {
        ModernHookRegistry registry = hookRegistry;
        if (registry == null) {
            synchronized (this) {
                registry = hookRegistry;
                if (registry == null) {
                    registry = new ModernHookRegistry();
                    hookRegistry = registry;
                }
            }
        }
        return registry;
    }

    private DpiConfigStore getOrCreateConfigStore() {
        DpiConfigStore local = configStore;
        if (local == null) {
            local = ConfigStoreFactory.createForXposedHost(this);
            FontDebugStatsTransport.initialize(this);
            configStore = local;
        }
        return local;
    }

    private static String describeClassLoader(SystemServerStartingParam param) {
        if (param == null || param.getClassLoader() == null) {
            return "null";
        }
        return param.getClassLoader().getClass().getName();
    }

    private void maybeInstallSystemServerHooks(DpiConfigStore store,
            HookRuntimePolicy policy,
            String processName,
            String packageName,
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
                SystemServerDisplayEnvironmentInstaller.install(this, store, getOrCreateHookRegistry());
                String message = "system_server installer ready: source=" + source
                        + ", process=" + processName
                        + ", package=" + packageName;
                rawBridgeLog(message);
            } catch (Throwable throwable) {
                DpisLog.e("system_server installer failed", throwable);
                rawBridgeLog("system_server installer failed: source=" + source
                        + ", error=" + throwable.getClass().getName()
                        + ": " + throwable.getMessage());
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

    private static void unhookLegacyHotReloadHandles(List<XposedInterface.HookHandle> oldHandles,
                                                     ModernHookRegistry registry) {
        // Hot reload currently only rebuilds the id-stable resource hooks.
        // Retaining the remaining old-generation handles avoids silently
        // dropping package-ready / classloader-dependent paths that the new
        // generation cannot reconstruct from hot reload state alone yet.
        // Those hooks remain in place until the process restarts.
        if (oldHandles == null) {
            return;
        }
    }
}
