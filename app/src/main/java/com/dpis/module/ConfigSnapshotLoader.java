package com.dpis.module;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class ConfigSnapshotLoader {
    private ConfigSnapshotLoader() {
    }

    static ConfigSnapshot fromStore(DpiConfigStore store) {
        if (store == null) {
            return ConfigSnapshot.empty();
        }
        Set<String> configuredPackages = store.getConfiguredPackages();
        Map<String, PackageConfigSnapshot> packages = new LinkedHashMap<>();
        HookDomainOverrideStore overrideStore = new HookDomainOverrideStore(store);
        for (String packageName : configuredPackages) {
            if (packageName == null || packageName.isBlank()) {
                continue;
            }
            packages.put(packageName, new PackageConfigSnapshot(
                    packageName,
                    store.isTargetDpisEnabled(packageName),
                    store.getTargetViewportSpec(packageName),
                    store.getTargetViewportWidthDp(packageName),
                    store.getTargetViewportApplyMode(packageName),
                    store.getTargetFontScalePercent(packageName),
                    store.getTargetFontApplyMode(packageName),
                    store.getTargetTypefaceId(packageName),
                    false,
                    false,
                    false,
                    overrideStore.read(packageName)));
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
