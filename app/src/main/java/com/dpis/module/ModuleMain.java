package com.dpis.module;

import de.robv.android.xposed.XposedBridge;
import io.github.libxposed.api.XposedModule;

public final class ModuleMain extends XposedModule {
    private static final String BRIDGE_LOG_PREFIX = "DPIS ";
    private volatile DpiConfigStore configStore;
    private volatile boolean moduleLoadedObserved;
    private volatile boolean systemServerInstallAttempted;
    private volatile boolean firstPackageReadyLogged;
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
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        DpiConfigStore store = getOrCreateConfigStore();
        HookRuntimePolicy policy = HookRuntimePolicy.fromStore(store);
        DpisLog.setLoggingEnabled(policy.globalLogEnabled);
        bridgeLog("package ready: process=" + currentProcessName
                + ", package=" + param.getPackageName());
        SystemServerDisplayDiagnostics.flushPending();
        maybeInstallSystemServerFromPackageReady(store, policy, param.getPackageName());
        maybeLogFirstPackageReady(param.getPackageName());
        String packageName = param.getPackageName();
        if (!store.getConfiguredPackages().contains(packageName)) {
            DpisLog.i("package not configured: package=" + packageName);
            return;
        }
        Integer targetViewportWidthDp = store.getTargetViewportWidthDp(packageName);
        String targetViewportMode = store.getTargetViewportApplyMode(packageName);
        Integer targetFontScalePercent = store.getTargetFontScalePercent(packageName);
        String targetFontMode = store.getTargetFontApplyMode(packageName);
        boolean targetDpisEnabled = store.isTargetDpisEnabled(packageName);
        if (!targetDpisEnabled) {
            DpisLog.i("target app disabled by dpis toggle: package=" + packageName);
            return;
        }
        boolean fontScaleActive = targetFontScalePercent != null
                && targetFontScalePercent > 0
                && targetFontScalePercent != 100;
        if (targetViewportWidthDp == null
                && !fontScaleActive) {
            DpisLog.i("target app disabled: package=" + packageName);
            return;
        }
        DpisLog.i("target app matched: package=" + packageName
                + ", targetViewportWidthDp=" + targetViewportWidthDp
                + ", targetViewportMode=" + targetViewportMode
                + ", targetFontScalePercent=" + targetFontScalePercent
                + ", targetFontMode=" + targetFontMode);
        HyperOsFlutterFontHookInstaller.install(packageName, store);
        try {
            AppProcessHookInstaller.install(this, packageName, store, policy,
                    targetViewportWidthDp != null, targetViewportMode, targetFontMode,
                    fontScaleActive);
        } catch (Throwable throwable) {
            DpisLog.e("failed to install app process hooks", throwable);
        }
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
        try {
            XposedBridge.log(BRIDGE_LOG_PREFIX + message);
        } catch (Throwable ignored) {
            // Keep module behavior unchanged even if XposedBridge logging is unavailable.
        }
    }
}
