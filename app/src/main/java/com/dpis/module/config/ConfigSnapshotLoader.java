package com.dpis.module.config;

import com.dpis.module.config.PackageConfigSnapshot;

import com.dpis.module.config.ConfigSnapshotLoader;

import com.dpis.module.config.ConfigSnapshot;

import com.dpis.module.hooks.HookDomainOverrideStore;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class ConfigSnapshotLoader {
    private ConfigSnapshotLoader() {
    }

    public static ConfigSnapshot fromStore(ConfigSnapshotStore store) {
        if (store == null) {
            return ConfigSnapshot.empty();
        }
        Set<String> configuredPackages = store.getConfiguredPackages();
        Map<String, PackageConfigSnapshot> packages = new LinkedHashMap<>();
        for (String packageName : configuredPackages) {
            if (packageName == null || packageName.isBlank()) {
                continue;
            }
            packages.put(packageName, new PackageConfigSnapshot(
                    packageName,
                    store.isTargetDpisEnabled(packageName),
                    store.getTargetViewportSpec(packageName),
                    store.getTargetViewportApplyMode(packageName),
                    store.getTargetFontScalePercent(packageName),
                    store.getTargetFontApplyMode(packageName),
                    store.getTargetTypefaceId(packageName),
                    false,
                    false,
                    false,
                    HookDomainOverrideStore.fromRaw(
                            store.getPackageFontHookDomainsRaw(packageName))));
        }
        return new ConfigSnapshot(
                packages.keySet(),
                packages,
                store.isSystemServerHooksEnabled(),
                store.isSystemServerSafeModeEnabled(),
                store.isGlobalLogEnabled(),
                store.hasSystemServerHooksEnabled(),
                store.hasSystemServerSafeModeEnabled(),
                store.hasGlobalLogEnabled());
    }
}
