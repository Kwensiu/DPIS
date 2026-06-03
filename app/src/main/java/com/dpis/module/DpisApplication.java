package com.dpis.module;

import android.app.Application;
import com.google.android.material.color.DynamicColors;

import java.util.LinkedHashMap;
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

    private static volatile DpiConfigStore configStore;
    private static volatile XposedService xposedService;

    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
        HyperOsNativeProxyAssetExporter.exportBundledNativeProxyLibrary(this);
        configStore = ConfigStoreFactory.createForModuleApp(this);
        DpisLog.setLoggingEnabled(configStore.isGlobalLogEnabled());
        RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(configStore);
        XposedServiceHelper.registerListener(this);
        UpdatePackageInstaller.clearStaleUpdateCache(this, UPDATE_CACHE_STARTUP_MAX_AGE_MS);
    }

    @Override
    public void onServiceBind(XposedService service) {
        DpiConfigStore localStore = ConfigStoreFactory.createForModuleApp(this);
        DpiConfigStore remoteStore = ConfigStoreFactory.createForModuleApp(this, service);
        migrateConfig(localStore, remoteStore);
        remoteStore.migrateWechatViewportToTargetFieldIfNeeded();
        // Keep local SharedPreferences as a cold-start mirror before the Xposed
        // service is rebound. Compat100 app processes cannot load XSharedPreferences.
        mirrorConfig(remoteStore, localStore);
        configStore = remoteStore;
        DpisLog.setLoggingEnabled(remoteStore.isGlobalLogEnabled());
        RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(remoteStore);
        xposedService = service;
        notifyServiceStateChanged();
    }

    @Override
    public void onServiceDied(XposedService service) {
        configStore = ConfigStoreFactory.createForModuleApp(this);
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
        migrateWechatTargetField(from, to);
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

    private static void migrateWechatTargetField(DpiConfigStore from, DpiConfigStore to) {
        if (from == null || to == null) {
            return;
        }
        String packageName = WechatTargetFieldConfig.PACKAGE_NAME;
        Integer targetField = from.getWechatTargetField(packageName);
        if (targetField == null) {
            targetField = WechatTargetFieldConfig.normalize(
                    from.getTargetViewportWidthDp(packageName));
        }
        if (targetField == null || to.getWechatTargetField(packageName) != null) {
            return;
        }
        to.setWechatTargetField(packageName, targetField);
        to.clearTargetViewportWidthDp(packageName);
        to.setTargetViewportApplyMode(packageName, ViewportApplyMode.OFF);
    }

    private static void mirrorConfig(DpiConfigStore from, DpiConfigStore to) {
        if (from == null || to == null || from == to) {
            return;
        }
        to.replaceAll(from.snapshotAll());
    }
}
