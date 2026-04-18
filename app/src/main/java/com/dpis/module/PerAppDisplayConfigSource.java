package com.dpis.module;

import java.util.Collections;
import java.util.Set;

final class PerAppDisplayConfigSource {
    private final DpiConfigStore store;

    PerAppDisplayConfigSource(DpiConfigStore store) {
        this.store = store;
    }

    PerAppDisplayConfig get(String packageName) {
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
                targetFontScalePercent, targetFontMode);
    }

    Set<String> getConfiguredPackages() {
        if (store == null) {
            return Collections.emptySet();
        }
        return store.getConfiguredPackages();
    }
}

