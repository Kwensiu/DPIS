package com.dpis.module;

import android.app.Application;
import android.content.Context;
import com.google.android.material.color.DynamicColors;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public final class DpisApplication extends Application implements XposedServiceHelper.OnServiceListener {
    private static final long UPDATE_CACHE_STARTUP_MAX_AGE_MS = 24 * 60 * 60 * 1000L;

    interface ServiceStateListener {
        void onServiceStateChanged();
    }

    private static final Set<ServiceStateListener> SERVICE_STATE_LISTENERS =
            new CopyOnWriteArraySet<>();

    private static volatile DpisApplication instance;
    private static volatile DpiConfigStore configStore;
    private static volatile XposedService xposedService;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        DynamicColors.applyToActivitiesIfAvailable(this);
        HyperOsNativeProxyAssetExporter.exportBundledNativeProxyLibrary(this);
        configStore = ConfigStoreFactory.createLocalModuleConfigStore(this);
        configStore.migrateLegacyWechatDpi();
        DpisLog.setLoggingEnabled(configStore.isGlobalLogEnabled());
        RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(configStore);
        XposedServiceHelper.registerListener(this);
        UpdatePackageInstaller.clearStaleUpdateCache(this, UPDATE_CACHE_STARTUP_MAX_AGE_MS);
    }

    @Override
    public void onServiceBind(XposedService service) {
        DpiConfigStore localStore = ConfigStoreFactory.createLocalModuleConfigStore(this);
        DpiConfigStore remoteStore = ConfigStoreFactory.createRemoteModuleConfigStore(service);
        localStore.migrateLegacyWechatDpi();
        publishRuntimeConfig(localStore, remoteStore);
        configStore = ConfigStoreFactory.createActiveModuleConfigStore(this, service);
        DpisLog.setLoggingEnabled(configStore.isGlobalLogEnabled());
        RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(configStore);
        xposedService = service;
        notifyServiceStateChanged();
    }

    @Override
    public void onServiceDied(XposedService service) {
        configStore = ConfigStoreFactory.createLocalModuleConfigStore(this);
        DpisLog.setLoggingEnabled(configStore.isGlobalLogEnabled());
        RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(configStore);
        xposedService = null;
        notifyServiceStateChanged();
    }

    static DpiConfigStore getConfigStore() {
        return configStore;
    }

    static XposedService getXposedService() {
        return xposedService;
    }

    static DpiConfigStore getActiveHookConfigStore(Context context) {
        DpiConfigStore sharedStore = configStore;
        if (sharedStore != null) {
            return sharedStore;
        }
        if (context == null) {
            return null;
        }
        XposedService service = xposedService;
        return service != null
                ? ConfigStoreFactory.createActiveModuleConfigStore(context, service)
                : ConfigStoreFactory.createLocalModuleConfigStore(context);
    }

    static void reloadConfigStore() {
        DpisApplication application = instance;
        if (application == null) {
            return;
        }
        XposedService service = xposedService;
        DpiConfigStore localStore = ConfigStoreFactory.createLocalModuleConfigStore(application);
        publishRuntimeConfig(localStore, ConfigStoreFactory.createRemoteModuleConfigStore(service));
        DpiConfigStore refreshedStore = service != null
                ? ConfigStoreFactory.createActiveModuleConfigStore(application, service)
                : ConfigStoreFactory.createLocalModuleConfigStore(application);
        configStore = refreshedStore;
        DpisLog.setLoggingEnabled(refreshedStore.isGlobalLogEnabled());
        RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(refreshedStore);
        notifyServiceStateChanged();
    }

    static void addServiceStateListener(ServiceStateListener listener, boolean notifyImmediately) {
        SERVICE_STATE_LISTENERS.add(listener);
        if (notifyImmediately) {
            listener.onServiceStateChanged();
        }
    }

    static void removeServiceStateListener(ServiceStateListener listener) {
        SERVICE_STATE_LISTENERS.remove(listener);
    }

    private static void notifyServiceStateChanged() {
        for (ServiceStateListener listener : SERVICE_STATE_LISTENERS) {
            listener.onServiceStateChanged();
        }
    }

    private static void migrateConfig(DpiConfigStore from, DpiConfigStore to) {
        if (from == null || to == null || from == to) {
            return;
        }
        Set<String> localPackages = from.getConfiguredPackages();
        LinkedHashMap<String, Integer> seedViewportWidthDps = new LinkedHashMap<>();
        for (String packageName : localPackages) {
            ViewportTargetSpec viewportTargetSpec = from.getTargetViewportSpec(packageName);
            if (viewportTargetSpec.isAbsoluteDp()) {
                seedViewportWidthDps.put(packageName, viewportTargetSpec.absoluteWidthDp());
            }
        }
        if (!seedViewportWidthDps.isEmpty()) {
            to.ensureSeedConfig(seedViewportWidthDps);
        }
        for (String packageName : localPackages) {
            Integer wechatDpi = from.getWechatDpi(packageName);
            if (wechatDpi != null && to.getWechatDpi(packageName) == null) {
                to.setWechatDpi(packageName, wechatDpi);
            }
            Integer fontScalePercent = from.getTargetFontScalePercent(packageName);
            if (fontScalePercent != null && fontScalePercent > 0) {
                if (!to.hasPrimaryTargetFontScalePercent(packageName)) {
                    to.setTargetFontScalePercent(packageName, fontScalePercent);
                }
            }
            String viewportMode = from.getTargetViewportApplyMode(packageName);
            ViewportTargetSpec viewportTargetSpec = from.getTargetViewportSpec(packageName);
            if (viewportTargetSpec.isEnabled() && !to.getTargetViewportSpec(packageName).isEnabled()) {
                to.setTargetViewportSpec(packageName, viewportTargetSpec);
            }
            if (ViewportApplyMode.isEnabled(viewportMode)
                    && !to.hasPrimaryTargetViewportApplyMode(packageName)) {
                to.setTargetViewportApplyMode(packageName, viewportMode);
            }
            String fontMode = from.getTargetFontApplyMode(packageName);
            if (FontApplyMode.isEnabled(fontMode)) {
                String remoteFontMode = to.hasPrimaryTargetFontApplyMode(packageName)
                        ? to.getTargetFontApplyMode(packageName)
                        : FontApplyMode.OFF;
                if (!fontMode.equals(remoteFontMode)) {
                    to.setTargetFontApplyMode(packageName, fontMode);
                }
            }
        }
        if (from.hasSystemServerHooksEnabled() && !to.hasSystemServerHooksEnabled()) {
            to.setSystemServerHooksEnabled(from.isSystemServerHooksEnabled());
        }
        if (from.hasSystemServerSafeModeEnabled() && !to.hasSystemServerSafeModeEnabled()) {
            to.setSystemServerSafeModeEnabled(from.isSystemServerSafeModeEnabled());
        }
        if (from.hasGlobalLogEnabled() && !to.hasGlobalLogEnabled()) {
            to.setGlobalLogEnabled(from.isGlobalLogEnabled());
        }
        if (from.hasLauncherIconHidden() && !to.hasLauncherIconHidden()) {
            to.setLauncherIconHidden(from.isLauncherIconHidden());
        }
    }

    private static void publishRuntimeConfig(DpiConfigStore from, DpiConfigStore to) {
        if (from == null || to == null || from == to) {
            return;
        }
        Map<String, Object> snapshot = from.snapshotLocalMirror();
        to.replaceAll(snapshot);
    }
}
