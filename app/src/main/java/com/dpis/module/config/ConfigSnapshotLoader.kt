package com.dpis.module.config

import com.dpis.module.hooks.HookDomainOverrideStore

object ConfigSnapshotLoader {
    @JvmStatic
    fun fromStore(store: ConfigSnapshotStore?): ConfigSnapshot {
        if (store == null) return ConfigSnapshot.empty()
        val configuredPackages = store.getConfiguredPackages()
        val packages = LinkedHashMap<String, PackageConfigSnapshot>()
        for (packageName in configuredPackages) {
            if (packageName.isNullOrBlank()) continue
            packages[packageName] = PackageConfigSnapshot(
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
                    store.getPackageFontHookDomainsRaw(packageName),
                ),
            )
        }
        return ConfigSnapshot(
            packages.keys,
            packages,
            store.isSystemServerHooksEnabled(),
            store.isSystemServerSafeModeEnabled(),
            store.isGlobalLogEnabled(),
            store.hasSystemServerHooksEnabled(),
            store.hasSystemServerSafeModeEnabled(),
            store.hasGlobalLogEnabled(),
        )
    }
}
