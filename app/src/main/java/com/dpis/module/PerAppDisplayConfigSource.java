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
        if (targetViewportWidthDp == null) {
            return null;
        }
        return new PerAppDisplayConfig(packageName, targetViewportWidthDp);
    }

    Set<String> getConfiguredPackages() {
        if (store == null) {
            return Collections.emptySet();
        }
        return store.getConfiguredPackages();
    }
}

