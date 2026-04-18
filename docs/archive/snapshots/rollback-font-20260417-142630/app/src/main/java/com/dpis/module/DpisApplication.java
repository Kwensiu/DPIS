package com.dpis.module;

import android.app.Application;

import com.google.android.material.color.DynamicColors;

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public final class DpisApplication extends Application implements XposedServiceHelper.OnServiceListener {
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
        configStore = ConfigStoreFactory.createForModuleApp(this);
        DpisLog.setLoggingEnabled(configStore.isGlobalLogEnabled());
        XposedServiceHelper.registerListener(this);
    }

    @Override
    public void onServiceBind(XposedService service) {
        DpiConfigStore localStore = configStore != null
                ? configStore
                : ConfigStoreFactory.createForModuleApp(this);
        DpiConfigStore remoteStore = ConfigStoreFactory.createForModuleApp(this, service);
        migrateConfig(localStore, remoteStore);
        configStore = remoteStore;
        DpisLog.setLoggingEnabled(remoteStore.isGlobalLogEnabled());
        xposedService = service;
        notifyServiceStateChanged();
    }

    @Override
    public void onServiceDied(XposedService service) {
        configStore = ConfigStoreFactory.createForModuleApp(this);
        DpisLog.setLoggingEnabled(configStore.isGlobalLogEnabled());
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
        LinkedHashMap<String, Integer> seedViewportWidthDps = new LinkedHashMap<>();
        for (String packageName : from.getConfiguredPackages()) {
            Integer viewportWidthDp = from.getTargetViewportWidthDp(packageName);
            if (viewportWidthDp != null && viewportWidthDp > 0) {
                seedViewportWidthDps.put(packageName, viewportWidthDp);
            }
        }
        if (!seedViewportWidthDps.isEmpty()) {
            to.ensureSeedConfig(seedViewportWidthDps);
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
    }
}
