package com.dpis.module;

import com.dpis.module.fonts.FontApplyMode;

import com.dpis.module.viewport.ViewportApplyMode;

import com.dpis.module.hooks.HookDomainOverride;
import com.dpis.module.hooks.HookDomainOverrideStore;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class ConfigSnapshotLoader {
    private ConfigSnapshotLoader() {
    }

    public static ConfigSnapshot fromStore(DpisConfigStore store) {
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
