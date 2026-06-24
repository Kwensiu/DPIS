package com.dpis.module;

import android.content.pm.ApplicationInfo;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public final class ModuleMain extends XposedModule {
    private static final String BRIDGE_LOG_PREFIX = "DPIS ";
    private volatile DpiConfigStore configStore;
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
        rememberPackageReady(param.getPackageName(), param.getClassLoader(),
                param.getApplicationInfo());
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
        ResourcesManagerHookInstaller.resetForHotReload();
        ResourcesImplHookInstaller.resetForHotReload();
        ResourcesReadHookInstaller.resetForHotReload();
        DpiConfigStore store = getOrCreateConfigStore();
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
            DpisLog.i("system_server hot reload skipped: replay not supported");
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

    private void replayPackageReadySupplementsAfterHotReload(DpiConfigStore store,
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
        retryFlutterHooksWithAppClassLoader(store, snapshot, classLoader, packageName);
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

    private DpiConfigStore getOrCreateConfigStore() {
        DpiConfigStore local = configStore;
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
                SystemServerDisplayEnvironmentInstaller.install(
                        this, store, getModernApiCapabilities());
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
