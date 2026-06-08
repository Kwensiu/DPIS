package com.dpis.module;

import android.app.Application;
import android.content.Context;
import com.google.android.material.color.DynamicColors;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
        DpiConfigStore remoteStore = ConfigStoreFactory.createActiveModuleConfigStore(this, service);
        localStore.migrateLegacyWechatDpi();
        remoteStore.migrateLegacyWechatDpi();
        migrateConfig(localStore, remoteStore);
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
        migrateTemplateAndPrefillConfig(from, to);
    }

    private static void migrateTemplateAndPrefillConfig(DpiConfigStore from, DpiConfigStore to) {
        // Local edits made before service binding must survive the first remote
        // mirror, while remote template/prefill values remain authoritative.
        Map<String, Object> localConfig = from.snapshotBackup();
        Map<String, Object> remoteConfig = to.snapshotAll();
        LinkedHashMap<String, Object> missingEntries = new LinkedHashMap<>();
        mergeTemplateIds(localConfig, remoteConfig, missingEntries);
        mergeTemplateOrder(localConfig, remoteConfig, missingEntries);
        for (Map.Entry<String, Object> entry : localConfig.entrySet()) {
            String key = entry.getKey();
            if (!isTemplateOrPrefillKey(key)
                    || QuickTemplateStore.KEY_TEMPLATE_IDS.equals(key)
                    || QuickTemplateStore.KEY_TEMPLATE_ORDER.equals(key)
                    || remoteConfig.containsKey(key)) {
                continue;
            }
            missingEntries.put(key, entry.getValue());
        }
        if (missingEntries.isEmpty()) {
            return;
        }
        remoteConfig.putAll(missingEntries);
        to.replaceAll(remoteConfig);
    }

    private static void mergeTemplateIds(
            Map<String, Object> localConfig,
            Map<String, Object> remoteConfig,
            Map<String, Object> target
    ) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        addStringSetValues(merged, remoteConfig.get(QuickTemplateStore.KEY_TEMPLATE_IDS));
        int remoteSize = merged.size();
        addStringSetValues(merged, localConfig.get(QuickTemplateStore.KEY_TEMPLATE_IDS));
        if (merged.size() > remoteSize || !remoteConfig.containsKey(QuickTemplateStore.KEY_TEMPLATE_IDS)) {
            target.put(QuickTemplateStore.KEY_TEMPLATE_IDS, merged);
        }
    }

    private static void mergeTemplateOrder(
            Map<String, Object> localConfig,
            Map<String, Object> remoteConfig,
            Map<String, Object> target
    ) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        addTemplateOrderValues(merged, remoteConfig.get(QuickTemplateStore.KEY_TEMPLATE_ORDER));
        int remoteSize = merged.size();
        addTemplateOrderValues(merged, localConfig.get(QuickTemplateStore.KEY_TEMPLATE_ORDER));
        if (merged.size() > remoteSize || !remoteConfig.containsKey(QuickTemplateStore.KEY_TEMPLATE_ORDER)) {
            target.put(QuickTemplateStore.KEY_TEMPLATE_ORDER, String.join("\n", merged));
        }
    }

    private static void addStringSetValues(Set<String> target, Object value) {
        if (!(value instanceof Set<?> values)) {
            return;
        }
        for (Object item : values) {
            if (item instanceof String string && !string.isBlank()) {
                target.add(string);
            }
        }
    }

    private static void addTemplateOrderValues(Set<String> target, Object value) {
        if (!(value instanceof String order)) {
            return;
        }
        for (String item : order.split("\\n")) {
            if (!item.isBlank()) {
                target.add(item.trim());
            }
        }
    }

    private static boolean isTemplateOrPrefillKey(String key) {
        return key != null
                && (key.startsWith("default_config.")
                        || key.startsWith("template."));
    }

    private static void mirrorConfig(DpiConfigStore from, DpiConfigStore to) {
        if (from == null || to == null || from == to) {
            return;
        }
        Map<String, Object> localOnlyValues = to.snapshotLocalOnlyMirrorValues();
        Map<String, Object> snapshot = from.snapshotLocalMirror();
        snapshot.putAll(localOnlyValues);
        to.replaceAll(snapshot);
    }
}
