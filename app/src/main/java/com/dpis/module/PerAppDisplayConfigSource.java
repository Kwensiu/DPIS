package com.dpis.module;

import java.util.Set;

final class PerAppDisplayConfigSource {
    interface SnapshotProvider {
        ConfigSnapshot get();
    }

    interface PackageFallbackProvider {
        PackageConfigSnapshot get(String packageName);
    }

    private final SnapshotProvider snapshotProvider;
    private final PackageFallbackProvider packageFallbackProvider;

    PerAppDisplayConfigSource(DpiConfigStore store) {
        this(ConfigSnapshotLoader.fromStore(store));
    }

    PerAppDisplayConfigSource(ConfigSnapshot snapshot) {
        this(() -> snapshot);
    }

    PerAppDisplayConfigSource(SnapshotProvider snapshotProvider) {
        this(snapshotProvider, null);
    }

    PerAppDisplayConfigSource(SnapshotProvider snapshotProvider,
                              PackageFallbackProvider packageFallbackProvider) {
        this.snapshotProvider = snapshotProvider;
        this.packageFallbackProvider = packageFallbackProvider;
    }

    static PerAppDisplayConfigSource withCompat100RuntimePropertyFallback(
            SnapshotProvider snapshotProvider) {
        return new PerAppDisplayConfigSource(
                snapshotProvider,
                PerAppDisplayConfigSource::loadCompat100RuntimePropertyConfig);
    }

    PerAppDisplayConfig get(String packageName) {
        ConfigSnapshot snapshot = getSnapshot();
        PackageConfigSnapshot packageConfig = snapshot.getPackage(packageName);
        if (packageConfig != null && !packageConfig.dpisEnabled) {
            return null;
        }
        PackageConfigSnapshot fallbackConfig = getFallbackPackageConfig(packageName);
        if (fallbackConfig != null) {
            packageConfig = fallbackConfig;
        }
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
                packageConfig.hyperOsFlutterFontHookEnabled,
                packageConfig.hookDomainOverride);
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

    private PackageConfigSnapshot getFallbackPackageConfig(String packageName) {
        if (packageFallbackProvider == null || packageName == null || packageName.isBlank()) {
            return null;
        }
        return packageFallbackProvider.get(packageName);
    }

    private static PackageConfigSnapshot loadCompat100RuntimePropertyConfig(String packageName) {
        ConfigSnapshot snapshot = ConfigSnapshotLoader.fromStore(
                new DpiConfigStore(new SystemPropertyConfigPreferences(packageName)));
        return snapshot.getPackage(packageName);
    }
}

