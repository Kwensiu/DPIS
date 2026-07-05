package com.dpis.module;

import com.dpis.module.fonts.hookdomain.FontHookDomainDecision;

import com.dpis.module.viewport.ViewportPropertyBridge;
import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;

import java.util.Set;

public final class PerAppDisplayConfigSource {
    public interface SnapshotProvider {
        ConfigSnapshot get();
    }

    public interface PackageFallbackProvider {
        PackageConfigSnapshot get(String packageName);
    }

    private final SnapshotProvider snapshotProvider;
    private final PackageFallbackProvider packageFallbackProvider;

    public PerAppDisplayConfigSource(DpisConfigStore store) {
        this(ConfigSnapshotLoader.fromStore(store));
    }

    public PerAppDisplayConfigSource(ConfigSnapshot snapshot) {
        this(() -> snapshot);
    }

    public PerAppDisplayConfigSource(SnapshotProvider snapshotProvider) {
        this(snapshotProvider, null);
    }

    public PerAppDisplayConfigSource(SnapshotProvider snapshotProvider,
                              PackageFallbackProvider packageFallbackProvider) {
        this.snapshotProvider = snapshotProvider;
        this.packageFallbackProvider = packageFallbackProvider;
    }

    public static PerAppDisplayConfigSource withLegacyRuntimePropertyFallback(
            SnapshotProvider snapshotProvider) {
        return new PerAppDisplayConfigSource(
                snapshotProvider,
                PerAppDisplayConfigSource::loadLegacyRuntimePropertyConfig);
    }

    public PerAppDisplayConfig get(String packageName) {
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
        ViewportTargetSpec runtimeTargetSpec = ViewportPropertyBridge.readTargetSpec(packageName);
        ViewportTargetSpec targetViewportSpec = runtimeTargetSpec.isEnabled()
                ? runtimeTargetSpec
                : packageConfig.targetViewportSpec;
        String viewportMode = ViewportApplyMode.normalize(packageConfig.targetViewportMode);
        if (ViewportApplyMode.COMPAT.equals(viewportMode) || ViewportApplyMode.OFF.equals(viewportMode)) {
            targetViewportSpec = ViewportTargetSpec.off();
        }
        Integer targetFontScalePercent = packageConfig.targetFontScalePercent;
        String targetFontMode = packageConfig.targetFontMode;
        boolean fontConfigured = FontApplyMode.isEnabled(targetFontMode)
                && targetFontScalePercent != null;
        if (!targetViewportSpec.isEnabled() && !fontConfigured) {
            return null;
        }
        boolean hyperOsNativeFlutterEnabled = FontHookDomainDecision
                .isHyperOsNativeFlutterEnabled(snapshot, packageConfig);
        return new PerAppDisplayConfig(packageName, targetViewportSpec, viewportMode,
                targetFontScalePercent, targetFontMode,
                hyperOsNativeFlutterEnabled,
                packageConfig.hookDomainOverride);
    }

    public Set<String> getConfiguredPackages() {
        return getSnapshot().getConfiguredPackages();
    }

    public boolean isSystemServerHooksEnabled() {
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

    private static PackageConfigSnapshot loadLegacyRuntimePropertyConfig(String packageName) {
        ConfigSnapshot snapshot = ConfigSnapshotLoader.fromStore(
                new DpisConfigStore(new RuntimePropertyConfigPreferences(packageName)));
        return snapshot.getPackage(packageName);
    }
}

