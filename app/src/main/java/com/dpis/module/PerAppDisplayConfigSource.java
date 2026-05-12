package com.dpis.module;

import java.util.Set;

final class PerAppDisplayConfigSource {
    interface SnapshotProvider {
        ConfigSnapshot get();
    }

    private final SnapshotProvider snapshotProvider;

    PerAppDisplayConfigSource(DpiConfigStore store) {
        this(ConfigSnapshotLoader.fromStore(store));
    }

    PerAppDisplayConfigSource(ConfigSnapshot snapshot) {
        this(() -> snapshot);
    }

    PerAppDisplayConfigSource(SnapshotProvider snapshotProvider) {
        this.snapshotProvider = snapshotProvider;
    }

    PerAppDisplayConfig get(String packageName) {
        ConfigSnapshot snapshot = getSnapshot();
        PackageConfigSnapshot packageConfig = snapshot.getPackage(packageName);
        if (packageConfig == null || !packageConfig.dpisEnabled) {
            return null;
        }
        // Runtime viewport properties are intentionally projected per package so
        // app-process changes can apply without rebuilding all configured packages.
        Integer targetViewportWidthDp = TargetViewportWidthResolver.resolve(
                packageConfig.targetViewportWidthDp,
                packageConfig.targetViewportMode,
                snapshot.isSystemServerHooksEnabled(),
                ViewportPropertyBridge.readTargetWidthDp(packageName));
        Integer targetFontScalePercent = packageConfig.targetFontScalePercent;
        String targetFontMode = packageConfig.targetFontMode;
        boolean fontConfigured = FontApplyMode.isEnabled(targetFontMode)
                && targetFontScalePercent != null;
        if (targetViewportWidthDp == null && !fontConfigured) {
            return null;
        }
        return new PerAppDisplayConfig(packageName, targetViewportWidthDp,
                targetFontScalePercent, targetFontMode,
                packageConfig.hyperOsFlutterFontHookEnabled);
    }

    Set<String> getConfiguredPackages() {
        return getSnapshot().getConfiguredPackages();
    }

    boolean isSystemServerHooksEnabled() {
        return getSnapshot().isSystemServerHooksEnabled();
    }

    private ConfigSnapshot getSnapshot() {
        ConfigSnapshot snapshot = snapshotProvider != null ? snapshotProvider.get() : null;
        return snapshot != null ? snapshot : ConfigSnapshot.empty();
    }
}

