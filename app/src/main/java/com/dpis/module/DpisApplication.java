package com.dpis.module;

import com.dpis.module.root.RootAccessProbe;
import com.dpis.module.fonts.HyperOsNativeProxyAssetExporter;
import com.dpis.module.updates.UpdatePackageInstaller;

import android.app.Application;
import android.content.Context;
import com.google.android.material.color.DynamicColors;

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
    private static volatile DpisConfigStore configStore;
    private static volatile XposedService xposedService;
    private static volatile boolean xposedSelfLoaded;
    public boolean xposedSelfLoadedByLegacyConstructorHook;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        DpisLog.setAppLogSink(new DpisAppLogStore(this));
        DpisLog.i("app process started");
        RootAccessProbe.warmUpAsync();
        DynamicColors.applyToActivitiesIfAvailable(this);
        HyperOsNativeProxyAssetExporter.exportBundledNativeProxyLibrary(this, DpisLog::e);
        configStore = ConfigStoreFactory.createLocalModuleConfigStore(this);
        migrateLocalConfigStore(configStore);
        DpisLog.setLoggingEnabled(configStore.isGlobalLogEnabled());
        RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(configStore);
        XposedServiceHelper.registerListener(this);
        UpdatePackageInstaller.clearStaleUpdateCache(this, UPDATE_CACHE_STARTUP_MAX_AGE_MS);
    }

    @Override
    public void onServiceBind(XposedService service) {
        DpisConfigStore localStore = ConfigStoreFactory.createLocalModuleConfigStore(this);
        DpisConfigStore runtimeDeliveryStore =
                ConfigStoreFactory.createRuntimeDeliveryModuleConfigStore(service);
        migrateLocalConfigStore(localStore);
        migrateLocalConfigStore(runtimeDeliveryStore);
        configStore = ConfigStoreFactory.createLocalUiModuleConfigStore(this, service);
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

    static DpisConfigStore getConfigStore() {
        return configStore;
    }

    public static XposedService getXposedService() {
        return xposedService;
    }

    static void markXposedSelfLoaded() {
        xposedSelfLoaded = true;
        notifyServiceStateChanged();
    }

    static boolean isXposedSelfLoaded() {
        DpisApplication application = instance;
        return xposedSelfLoaded
                || (application != null && application.xposedSelfLoadedByLegacyConstructorHook);
    }

    static void clearXposedSelfLoadedForTest() {
        xposedSelfLoaded = false;
        DpisApplication application = instance;
        if (application != null) {
            application.xposedSelfLoadedByLegacyConstructorHook = false;
        }
    }

    public static DpisConfigStore getActiveHookConfigStore(Context context) {
        DpisConfigStore sharedStore = configStore;
        if (sharedStore != null) {
            return sharedStore;
        }
        if (context == null) {
            return null;
        }
        XposedService service = xposedService;
        return service != null
                ? ConfigStoreFactory.createLocalUiModuleConfigStore(context, service)
                : ConfigStoreFactory.createLocalModuleConfigStore(context);
    }

    static void reloadConfigStore() {
        DpisApplication application = instance;
        if (application == null) {
            return;
        }
        XposedService service = xposedService;
        DpisConfigStore localStore = ConfigStoreFactory.createLocalModuleConfigStore(application);
        migrateLocalConfigStore(localStore);
        migrateLocalConfigStore(ConfigStoreFactory.createRuntimeDeliveryModuleConfigStore(service));
        DpisConfigStore refreshedStore = service != null
                ? ConfigStoreFactory.createLocalUiModuleConfigStore(application, service)
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

    private static void migrateLocalConfigStore(DpisConfigStore store) {
        if (store == null) {
            return;
        }
        store.migrateLegacyWechatDpi();
        store.migrateLegacyPackageConfigToAggregated();
    }

    private static void publishRuntimeConfig(DpisConfigStore from, DpisConfigStore to) {
        if (from == null || to == null || from == to) {
            return;
        }
        // LSPosed remote preferences are a runtime delivery copy, not a migration
        // source or backup. Publish only runtime-shared config from the local store.
        Map<String, Object> snapshot = from.snapshotRuntimeDelivery();
        to.replaceAll(snapshot);
    }
}
