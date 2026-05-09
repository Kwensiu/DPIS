package com.dpis.module;

import java.util.Collections;
import java.util.Set;

final class PerAppDisplayConfigSource {
    interface StoreProvider {
        DpiConfigStore get();
    }

    private final StoreProvider storeProvider;

    PerAppDisplayConfigSource(DpiConfigStore store) {
        this(() -> store);
    }

    PerAppDisplayConfigSource(StoreProvider storeProvider) {
        this.storeProvider = storeProvider;
    }

    PerAppDisplayConfig get(String packageName) {
        DpiConfigStore store = getStore();
        if (store != null && !store.isTargetDpisEnabled(packageName)) {
            return null;
        }
        Integer targetViewportWidthDp = TargetViewportWidthResolver.resolve(store, packageName);
        Integer targetFontScalePercent = store != null
                ? store.getTargetFontScalePercent(packageName)
                : null;
        String targetFontMode = store != null
                ? store.getTargetFontApplyMode(packageName)
                : FontApplyMode.OFF;
        boolean fontConfigured = FontApplyMode.isEnabled(targetFontMode)
                && targetFontScalePercent != null;
        if (targetViewportWidthDp == null && !fontConfigured) {
            return null;
        }
        return new PerAppDisplayConfig(packageName, targetViewportWidthDp,
                targetFontScalePercent, targetFontMode,
                store != null && store.isHyperOsFlutterFontHookEnabled());
    }

    Set<String> getConfiguredPackages() {
        DpiConfigStore store = getStore();
        if (store == null) {
            return Collections.emptySet();
        }
        return store.getConfiguredPackages();
    }

    boolean isSystemServerHooksEnabled() {
        DpiConfigStore store = getStore();
        return store == null || store.isSystemServerHooksEnabled();
    }

    private DpiConfigStore getStore() {
        return storeProvider != null ? storeProvider.get() : null;
    }
}

