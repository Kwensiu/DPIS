package com.dpis.module;

import com.dpis.module.fonts.FontApplyMode;

import com.dpis.module.fonts.hookdomain.FontHookDomainDecision;

import com.dpis.module.runtime.systemserver.PerAppDisplayConfig;

import com.dpis.module.viewport.ViewportPropertyBridge;
import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;

import android.os.SystemClock;

import java.util.Set;
import java.util.function.Supplier;

public final class PerAppDisplayConfigSource {
    public interface SnapshotProvider {
        ConfigSnapshot get();
    }

    public interface PackageFallbackProvider {
        PackageConfigSnapshot get(String packageName);
    }

    public static final class RefreshingSnapshotProvider implements SnapshotProvider {
        private static final long FAILURE_LOG_MIN_INTERVAL_MILLIS = 30_000L;

        interface Clock {
            long nowMillis();
        }

        private final Supplier<ConfigSnapshot> loader;
        private final long ttlMillis;
        private final Clock clock;
        private volatile ConfigSnapshot snapshot;
        private volatile long loadedAtMillis;
        private volatile long lastRefreshAttemptAtMillis = Long.MIN_VALUE;
        private volatile long lastFailureLoggedAtMillis;
        private volatile String lastFailureMessage;

        public RefreshingSnapshotProvider(
                Supplier<ConfigSnapshot> loader,
                long ttlMillis) {
            this(loader, ttlMillis, SystemClock::elapsedRealtime);
        }

        RefreshingSnapshotProvider(
                Supplier<ConfigSnapshot> loader,
                long ttlMillis,
                Clock clock) {
            this.loader = loader;
            this.ttlMillis = Math.max(0L, ttlMillis);
            this.clock = clock != null ? clock : System::currentTimeMillis;
        }

        @Override
        public ConfigSnapshot get() {
            long now = clock.nowMillis();
            ConfigSnapshot current = snapshot;
            if (!shouldRefresh(current, now)) {
                return current;
            }
            synchronized (this) {
                now = clock.nowMillis();
                current = snapshot;
                if (!shouldRefresh(current, now)) {
                    return current;
                }
                try {
                    lastRefreshAttemptAtMillis = now;
                    ConfigSnapshot loaded = loader != null ? loader.get() : null;
                    snapshot = loaded != null ? loaded : ConfigSnapshot.empty();
                    loadedAtMillis = now;
                    lastFailureMessage = null;
                    return snapshot;
                } catch (Throwable throwable) {
                    logFailureIfNeeded(throwable, now);
                    return current != null ? current : ConfigSnapshot.empty();
                }
            }
        }

        private boolean shouldRefresh(ConfigSnapshot current, long nowMillis) {
            if (ttlMillis == 0L) {
                return true;
            }
            if (current != null) {
                return (nowMillis - loadedAtMillis) >= ttlMillis;
            }
            return lastRefreshAttemptAtMillis == Long.MIN_VALUE
                    || (nowMillis - lastRefreshAttemptAtMillis) >= ttlMillis;
        }

        private void logFailureIfNeeded(Throwable throwable, long nowMillis) {
            String message = throwable.getClass().getName()
                    + ": "
                    + throwable.getMessage();
            if (message.equals(lastFailureMessage)
                    && (nowMillis - lastFailureLoggedAtMillis)
                    < FAILURE_LOG_MIN_INTERVAL_MILLIS) {
                return;
            }
            lastFailureMessage = message;
            lastFailureLoggedAtMillis = nowMillis;
            DpisLog.e("config snapshot refresh failed: " + message, throwable);
        }
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
