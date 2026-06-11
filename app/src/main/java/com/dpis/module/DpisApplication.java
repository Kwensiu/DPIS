package com.dpis.module;

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
    private static volatile DpiConfigStore configStore;
    private static volatile XposedService xposedService;
    private static volatile boolean xposedSelfLoaded;
    public boolean xposedSelfLoadedByLegacyConstructorHook;

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
        DpiConfigStore runtimeDeliveryStore =
                ConfigStoreFactory.createRuntimeDeliveryModuleConfigStore(service);
        localStore.migrateLegacyWechatDpi();
        publishRuntimeConfig(localStore, runtimeDeliveryStore);
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
        publishRuntimeConfig(localStore,
                ConfigStoreFactory.createRuntimeDeliveryModuleConfigStore(service));
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

    private static void publishRuntimeConfig(DpiConfigStore from, DpiConfigStore to) {
        if (from == null || to == null || from == to) {
            return;
        }
        // LSPosed remote preferences are a runtime delivery copy, not a migration
        // source or backup. Publish only runtime-shared config from the local store.
        Map<String, Object> snapshot = from.snapshotRuntimeDelivery();
        to.replaceAll(snapshot);
    }
}
