package com.dpis.module;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class LegacySystemServerHookInstaller {
    private static final AtomicBoolean LAUNCH_ACTIVITY_ITEM_INSTALLED = new AtomicBoolean(false);
    private static final AtomicBoolean RUST_PROCESS_INSTALLED = new AtomicBoolean(false);

    private LegacySystemServerHookInstaller() {
    }

    static void install() {
        PerAppDisplayConfigSource source =
                PerAppDisplayConfigSource.withLegacyRuntimePropertyFallback(
                        new RefreshingConfigSnapshotProvider(
                                () -> ConfigSnapshotLoader.fromStore(
                                        ConfigStoreFactory.createForLegacySystemServerHost()),
                                ConfigSnapshotRefreshPolicy.SYSTEM_SERVER_TTL_MILLIS));
        int hookedCount = 0;
        int constructorHookCount = 0;
        boolean attempted = false;

        // initZygote can run before HyperOS exposes android.os.RustProcessImpl.
        // Keep RustProcess retryable so the later system_server entry can install it.
        if (RUST_PROCESS_INSTALLED.compareAndSet(false, true)) {
            attempted = true;
            try {
                if (LegacyRustProcessHookInstaller.install(source)) {
                    hookedCount++;
                } else {
                    RUST_PROCESS_INSTALLED.set(false);
                }
            } catch (Throwable throwable) {
                RUST_PROCESS_INSTALLED.set(false);
                logError("legacy system_server rust-process hook failed: "
                        + throwable.getClass().getName() + ": " + throwable.getMessage());
            }
        }

        if (!LAUNCH_ACTIVITY_ITEM_INSTALLED.compareAndSet(false, true)) {
            logInstallSummaryIfAttempted(attempted, hookedCount, constructorHookCount);
            return;
        }
        attempted = true;
        try {
            // The Legacy classic-Xposed entrypoint has no libxposed system_server
            // entry, so keep this launch Configuration path wired explicitly here.
            Class<?> launchActivityItemClass = Class.forName(
                    "android.app.servertransaction.LaunchActivityItem");
            for (Constructor<?> constructor : launchActivityItemClass.getDeclaredConstructors()) {
                XposedBridge.hookMethod(constructor, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        applyLaunchActivityItemArgs(source, param.args);
                    }
                });
                hookedCount++;
                constructorHookCount++;
            }
        } catch (Throwable throwable) {
            LAUNCH_ACTIVITY_ITEM_INSTALLED.set(false);
            logError("legacy system_server launch-activity-item hook failed: "
                    + throwable.getClass().getName() + ": " + throwable.getMessage());
        }
        logInstallSummaryIfAttempted(attempted, hookedCount, constructorHookCount);
    }

    static void applyLaunchActivityItemArgs(PerAppDisplayConfigSource source, Object[] args) {
        if (source == null || args == null || !source.isSystemServerHooksEnabled()) {
            return;
        }
        String packageName = findActivityInfoPackage(args);
        if (packageName == null) {
            return;
        }
        PerAppDisplayConfig config = source.get(packageName);
        if (config == null) {
            return;
        }
        Configuration baseConfiguration = findFirstConfiguration(args);
        if (baseConfiguration == null) {
            return;
        }
        PerAppDisplayEnvironment environment = null;
        if (config.hasViewportOverride()) {
            environment = resolveTargetEnvironment(packageName, baseConfiguration, config);
        }
        boolean changed = false;
        for (Object arg : args) {
            if (arg instanceof Configuration configuration) {
                // Keep launch-time ActivityRecord configuration stable. The
                // published marker lets app-process hooks apply viewport state
                // without forcing ActivityTaskManager to relaunch the activity
                // for screen size / density drift during navigation.
                changed |= applyFontScale(configuration, config);
            }
        }
        if (changed) {
            logDebug("legacy system_server launch-activity-item apply: package=" + packageName
                    + ", widthDp=" + baseConfiguration.screenWidthDp
                    + ", densityDpi=" + baseConfiguration.densityDpi
                    + ", fontScale=" + baseConfiguration.fontScale);
        }
    }

    private static String findActivityInfoPackage(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof ActivityInfo activityInfo
                    && activityInfo.packageName != null
                    && !activityInfo.packageName.isBlank()) {
                return activityInfo.packageName;
            }
        }
        return null;
    }

    private static Configuration findFirstConfiguration(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Configuration configuration) {
                return configuration;
            }
        }
        return null;
    }

    private static boolean applyConfiguration(Configuration configuration,
                                              PerAppDisplayEnvironment environment) {
        if (configuration == null || environment == null) {
            return false;
        }
        boolean changed = configuration.screenWidthDp != environment.widthDp
                || configuration.screenHeightDp != environment.heightDp
                || configuration.smallestScreenWidthDp != environment.smallestWidthDp
                || configuration.densityDpi != environment.densityDpi;
        if (changed) {
            ViewportOverride.apply(configuration, new ViewportOverride.Result(
                    environment.widthDp,
                    environment.heightDp,
                    environment.smallestWidthDp,
                    environment.densityDpi));
        }
        return changed;
    }

    private static PerAppDisplayEnvironment resolveTargetEnvironment(String packageName,
                                                                     Configuration configuration,
                                                                     PerAppDisplayConfig config) {
        if (configuration == null || config == null || !config.hasViewportOverride()) {
            return null;
        }
        int widthPx = resolveWidthPx(configuration);
        int heightPx = resolveHeightPx(configuration);
        PerAppDisplayEnvironment alreadyApplied = resolveAlreadyAppliedEnvironment(
                packageName, configuration, widthPx, heightPx, config);
        if (alreadyApplied != null) {
            return alreadyApplied;
        }
        PerAppDisplayEnvironment environment = PerAppDisplayOverrideCalculator.calculate(
                configuration,
                widthPx,
                heightPx,
                config.targetViewportSpec);
        publishViewportRuntimeMarker(packageName, configuration, environment, config);
        return environment;
    }

    private static PerAppDisplayEnvironment resolveAlreadyAppliedEnvironment(
            String packageName,
            Configuration configuration,
            int widthPx,
            int heightPx,
            PerAppDisplayConfig config) {
        if (packageName == null || packageName.isBlank() || configuration == null
                || config == null || !config.targetViewportSpec.isEnabled()) {
            return null;
        }
        ViewportRuntimeMarkerBridge.ParseResult marker = ViewportRuntimeMarkerBridge.read(
                packageName,
                config.targetViewportSpec.fingerprint(),
                RuntimeClock.crossProcessMarkerMillis());
        if (!matchesCurrentConfiguration(configuration, marker)) {
            return null;
        }
        return new PerAppDisplayEnvironment(
                configuration.screenWidthDp,
                configuration.screenHeightDp,
                configuration.smallestScreenWidthDp,
                configuration.densityDpi,
                widthPx,
                heightPx);
    }

    private static boolean publishViewportRuntimeMarker(String packageName,
                                                        Configuration source,
                                                        PerAppDisplayEnvironment environment,
                                                        PerAppDisplayConfig config) {
        if (packageName == null || packageName.isBlank() || source == null
                || environment == null || config == null
                || !config.targetViewportSpec.isEnabled()) {
            return false;
        }
        return ViewportRuntimeMarkerBridge.publishSystemServerRecord(
                packageName,
                config.targetViewportSpec,
                new ConfigurationMarker(source),
                new EnvironmentMarker(environment),
                ViewportSourceSnapshot.SCOPE_DISPLAY,
                RuntimeClock.crossProcessMarkerMillis());
    }

    private static boolean matchesCurrentConfiguration(
            Configuration configuration,
            ViewportRuntimeMarkerBridge.ParseResult marker) {
        if (configuration == null || marker == null || !marker.hit || marker.record == null) {
            return false;
        }
        String signature = ViewportRuntimeMarkerBridge.configurationSignature(
                configuration.screenWidthDp,
                configuration.screenHeightDp,
                configuration.smallestScreenWidthDp,
                configuration.densityDpi,
                ViewportSourceSnapshot.SCOPE_DISPLAY);
        return signature.equals(marker.record.resultSignature);
    }

    private static boolean applyFontScale(Configuration configuration, PerAppDisplayConfig config) {
        if (configuration == null
                || config == null
                || !FontApplyMode.SYSTEM_EMULATION.equals(config.targetFontMode)
                || config.targetFontScalePercent == null
                || config.targetFontScalePercent <= 0) {
            return false;
        }
        float fontScale = config.targetFontScalePercent / 100.0f;
        if (Math.abs(configuration.fontScale - fontScale) < 0.0001f) {
            return false;
        }
        configuration.fontScale = fontScale;
        return true;
    }

    private static int resolveWidthPx(Configuration configuration) {
        if (configuration == null || configuration.screenWidthDp <= 0 || configuration.densityDpi <= 0) {
            return 0;
        }
        return Math.round(configuration.screenWidthDp * (configuration.densityDpi / 160.0f));
    }

    private static int resolveHeightPx(Configuration configuration) {
        if (configuration == null || configuration.screenHeightDp <= 0 || configuration.densityDpi <= 0) {
            return 0;
        }
        return Math.round(configuration.screenHeightDp * (configuration.densityDpi / 160.0f));
    }

    private static void logDebug(String message) {
        if (!DpisLog.isLoggingEnabled()) {
            return;
        }
        DpisLog.i(message);
    }

    private static void logInstallSummaryIfAttempted(boolean attempted,
                                                     int hookedCount,
                                                     int constructorHookCount) {
        if (!attempted) {
            return;
        }
        logDebug("legacy system_server hook ready: hooks=" + hookedCount
                + ", launchActivityItemConstructors=" + constructorHookCount
                + ", rustProcessInstalled=" + RUST_PROCESS_INSTALLED.get());
    }

    private static void logError(String message) {
        try {
            XposedBridge.log("DPIS " + message);
        } catch (Throwable ignored) {
        }
        try {
            Log.e(DpisLog.TAG, message);
        } catch (Throwable ignored) {
        }
    }

    static boolean matchesCurrentConfigurationForTest(
            Configuration configuration,
            ViewportRuntimeMarkerBridge.ParseResult marker) {
        return matchesCurrentConfiguration(configuration, marker);
    }

    private static final class ConfigurationMarker implements ViewportRuntimeMarkerBridge.ConfigurationLike {
        private final Configuration configuration;

        ConfigurationMarker(Configuration configuration) {
            this.configuration = configuration;
        }

        @Override
        public int widthDp() {
            return configuration.screenWidthDp;
        }

        @Override
        public int heightDp() {
            return configuration.screenHeightDp;
        }

        @Override
        public int smallestWidthDp() {
            return configuration.smallestScreenWidthDp;
        }

        @Override
        public int densityDpi() {
            return configuration.densityDpi;
        }
    }

    private static final class EnvironmentMarker implements ViewportRuntimeMarkerBridge.ConfigurationLike {
        private final PerAppDisplayEnvironment environment;

        EnvironmentMarker(PerAppDisplayEnvironment environment) {
            this.environment = environment;
        }

        @Override
        public int widthDp() {
            return environment.widthDp;
        }

        @Override
        public int heightDp() {
            return environment.heightDp;
        }

        @Override
        public int smallestWidthDp() {
            return environment.smallestWidthDp;
        }

        @Override
        public int densityDpi() {
            return environment.densityDpi;
        }
    }
}
