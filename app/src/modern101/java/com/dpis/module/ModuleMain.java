package com.dpis.module;

import de.robv.android.xposed.XposedBridge;
import io.github.libxposed.api.XposedModule;

public final class ModuleMain extends XposedModule {
    private static final String BRIDGE_LOG_PREFIX = "DPIS ";
    private volatile DpiConfigStore configStore;
    private volatile boolean moduleLoadedObserved;
    private volatile boolean systemServerInstallAttempted;
    private volatile boolean firstPackageReadyLogged;
    private volatile boolean appProcessInstallAttempted;
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
        bridgeLog(message);
        try {
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
    public void onPackageReady(PackageReadyParam param) {
        bridgeLog("onPackageReady enter: process=" + currentProcessName
                + ", package=" + param.getPackageName());
        XposedSelfActivation.markIfSelfPackage(
                param.getPackageName(),
                param.getClassLoader(),
                "libxposed-package-ready");
        DpiConfigStore store = getOrCreateConfigStore();
        ConfigSnapshot snapshot = ConfigSnapshotLoader.fromStore(store);
        HookRuntimePolicy policy = HookRuntimePolicy.fromSnapshot(snapshot);
        DpisLog.setLoggingEnabled(policy.globalLogEnabled);
        bridgeLog("package ready: process=" + currentProcessName
                + ", package=" + param.getPackageName());
        SystemServerDisplayDiagnostics.flushPending();
        maybeInstallSystemServerFromPackageReady(store, policy, param.getPackageName());
        maybeLogFirstPackageReady(param.getPackageName());
        if (Modern101AppSpecificRouteInstaller.handlePackageReady(this, param, currentProcessName)) {
            return;
        }
        installAppProcessHooksIfConfigured(store, policy, snapshot, param.getPackageName(),
                "package-ready");
        retryTypefaceHooksWithPackageReady(store, snapshot, param.getPackageName());
        retryFlutterHooksWithAppClassLoader(store, snapshot, param.getClassLoader(),
                param.getPackageName());
    }

    private void maybeInstallAppProcessFromModuleLoaded(DpiConfigStore store, String processName) {
        rawBridgeLog("module-loaded app hook install enter: process=" + processName);
        DpisLog.i("module-loaded app hook install enter: process=" + processName);
        if (SystemServerProcess.isSystemServer(processName, "")) {
            rawBridgeLog("module-loaded app hook install skipped system process: process="
                    + processName);
            DpisLog.i("module-loaded app hook install skipped system process: process=" + processName);
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
                DpisLog.i("module-loaded app config unavailable: process=" + processName
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
            DpisLog.i("module-loaded app config fallback: process=" + processName
                    + ", package=" + packageName
                    + ", packages=" + snapshot.getConfiguredPackages());
        }
        HookRuntimePolicy policy = HookRuntimePolicy.fromSnapshot(snapshot);
        DpisLog.setLoggingEnabled(policy.globalLogEnabled);
        if (Modern101AppSpecificRouteInstaller.shouldSuppressModuleLoadedGenericHooks(
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
        appProcessInstallAttempted = true;
        try {
            AppProcessHookInstaller.install(this, store, policy, packagePlan);
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
        bridgeLog("flutter-retry proceeding: package=" + packageName
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

    private void maybeInstallSystemServerFromPackageReady(DpiConfigStore store,
            HookRuntimePolicy policy,
            String packageName) {
        if (systemServerInstallAttempted) {
            return;
        }
        if (!SystemServerMutationPolicy.shouldInstallSystemServerHooks(
                currentProcessName,
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
                SystemServerDisplayEnvironmentInstaller.install(this, store);
                String message = "system_server installer ready: process=" + currentProcessName
                        + ", package=" + packageName;
                DpisLog.i(message);
                bridgeLog(message);
            } catch (Throwable throwable) {
                DpisLog.e("system_server installer failed", throwable);
                bridgeLog("system_server installer failed: " + throwable.getClass().getName()
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

    private static void bridgeLog(String message) {
        if (!DpisLog.isLoggingEnabled()) {
            return;
        }
        rawBridgeLog(message);
    }

    private static void rawBridgeLog(String message) {
        try {
            XposedBridge.log(BRIDGE_LOG_PREFIX + message);
        } catch (Throwable ignored) {
            // Keep module behavior unchanged even if XposedBridge logging is unavailable.
        }
    }
}
