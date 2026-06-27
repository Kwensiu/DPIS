package com.dpis.module;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class LegacySystemServerHookInstaller {
    private static final int MAX_PACKAGE_RECURSION_DEPTH = 5;
    private static final String[] PACKAGE_STRING_METHOD_NAMES = new String[]{
            "getOwningPackage",
            "getPackageName",
            "getPackage",
            "getOpPackageName"
    };
    private static final String[] PACKAGE_OBJECT_METHOD_NAMES = new String[]{
            "getIntent",
            "getComponent",
            "getActivityInfo",
            "getApplicationInfo",
            "getRequest",
            "getTargetActivity",
            "getOrigActivity",
            "getRealActivity"
    };
    private static final String[] PACKAGE_STRING_FIELD_NAMES = new String[]{
            "packageName", "mPackageName", "package", "launchedFromPackage"
    };
    private static final String[] PACKAGE_OBJECT_FIELD_NAMES = new String[]{
            "intent",
            "mIntent",
            "component",
            "mComponent",
            "activityInfo",
            "applicationInfo",
            "request",
            "mRequest",
            "targetActivity",
            "origActivity",
            "realActivity"
    };
    private static final AtomicBoolean LAUNCH_ACTIVITY_ITEM_INSTALLED = new AtomicBoolean(false);
    private static final AtomicBoolean RUST_PROCESS_INSTALLED = new AtomicBoolean(false);
    private static final Map<String, String> LAST_MESSAGES = new ConcurrentHashMap<>();

    private LegacySystemServerHookInstaller() {
    }

    static void install() {
        PerAppDisplayConfigSource source =
                PerAppDisplayConfigSource.withLegacyRuntimePropertyFallback(
                        new RefreshingConfigSnapshotProvider(
                                () -> ConfigSnapshotLoader.fromStore(
                                        ConfigStoreFactory.createForLegacySystemServerHost()),
                                ConfigSnapshotRefreshPolicy.SYSTEM_SERVER_TTL_MILLIS));
        boolean systemServerHooksEnabled = source.isSystemServerHooksEnabled();
        int hookedCount = 0;
        int constructorHookCount = 0;
        boolean attempted = false;
        logDebug("legacy system_server install enter: hooksEnabled="
                + systemServerHooksEnabled);

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

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        applyLaunchActivityItemObject(source, param.thisObject);
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
        if (source == null || args == null) {
            return;
        }
        if (!source.isSystemServerHooksEnabled()) {
            logDebug("legacy system_server launch-activity-item skipped: reason=hooks-disabled");
            return;
        }
        String packageName = findActivityInfoPackage(args);
        if (packageName == null) {
            logDebug("legacy system_server launch-activity-item skipped: reason=package-unresolved");
            return;
        }
        PerAppDisplayConfig config = source.get(packageName);
        if (config == null) {
            logDebug("legacy system_server launch-activity-item skipped: package="
                    + packageName + ", reason=config-missing");
            return;
        }
        Configuration baseConfiguration = findFirstConfiguration(args);
        if (baseConfiguration == null) {
            logDebug("legacy system_server launch-activity-item skipped: package="
                    + packageName + ", reason=configuration-missing");
            return;
        }
        PerAppDisplayEnvironment environment = null;
        if (config.hasViewportOverride()) {
            environment = resolveTargetEnvironment(packageName, baseConfiguration, config);
        }
        boolean changed = false;
        for (Object arg : args) {
            if (arg instanceof Configuration configuration) {
                // Legacy has only the launch-time system route. When the
                // package resolves to system mode, commit the launch viewport
                // Configuration here so launch-time UI can follow the target.
                changed |= applyConfiguration(configuration, environment);
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

    static void applyLaunchActivityItemObject(PerAppDisplayConfigSource source, Object launchActivityItem) {
        if (source == null || launchActivityItem == null) {
            return;
        }
        ActivityInfo activityInfo = readLaunchActivityInfo(launchActivityItem);
        String packageName = activityInfo != null ? activityInfo.packageName : null;
        if (!isLikelyPackageName(packageName)) {
            return;
        }
        PerAppDisplayConfig config = source.get(packageName);
        if (config == null) {
            return;
        }
        Configuration baseConfiguration = readLaunchActivityConfiguration(launchActivityItem);
        if (baseConfiguration == null) {
            return;
        }
        PerAppDisplayEnvironment environment = null;
        if (config.hasViewportOverride()) {
            environment = resolveTargetEnvironment(packageName, baseConfiguration, config);
        }
        boolean changed = false;
        // Some platform builds keep the effective launch values only on the
        // constructed LaunchActivityItem object, so commit both known fields.
        changed |= applyConfigurationField(launchActivityItem, "mCurConfig", environment);
        changed |= applyConfigurationField(launchActivityItem, "mOverrideConfig", environment);
        changed |= applyFontScaleField(launchActivityItem, "mCurConfig", config);
        changed |= applyFontScaleField(launchActivityItem, "mOverrideConfig", config);
        if (changed) {
            logIfChanged(
                    packageName + ":legacy-system-launch-object-apply",
                    "legacy system_server launch-activity-item object apply: package="
                            + packageName
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
        for (Object arg : args) {
            String packageName = findPackageNameRecursive(arg, 0);
            if (packageName != null) {
                return packageName;
            }
        }
        return null;
    }

    private static String findPackageNameRecursive(Object target, int depth) {
        if (target == null || depth > MAX_PACKAGE_RECURSION_DEPTH) {
            return null;
        }
        if (target instanceof ActivityInfo activityInfo
                && activityInfo.packageName != null
                && !activityInfo.packageName.isBlank()) {
            return activityInfo.packageName;
        }
        if (target instanceof String value && isLikelyPackageName(value)) {
            return value;
        }
        for (String methodName : PACKAGE_STRING_METHOD_NAMES) {
            String fromMethod = invokeStringMethod(target, methodName);
            if (fromMethod != null) {
                return fromMethod;
            }
        }
        for (String methodName : PACKAGE_OBJECT_METHOD_NAMES) {
            Object value = invokeObjectMethod(target, methodName);
            String nestedPackage = findPackageNameRecursive(value, depth + 1);
            if (nestedPackage != null) {
                return nestedPackage;
            }
        }
        for (String fieldName : PACKAGE_STRING_FIELD_NAMES) {
            Object value = readField(target, fieldName);
            if (value instanceof String stringValue && isLikelyPackageName(stringValue)) {
                return stringValue;
            }
        }
        for (String fieldName : PACKAGE_OBJECT_FIELD_NAMES) {
            String nestedPackage = findPackageNameRecursive(readField(target, fieldName), depth + 1);
            if (nestedPackage != null) {
                return nestedPackage;
            }
        }
        for (Field field : target.getClass().getDeclaredFields()) {
            if (field.getType().isPrimitive() || field.getType().isEnum()) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object nested = field.get(target);
                if (nested == null || nested == target || Objects.equals(field.getName(), "this$0")) {
                    continue;
                }
                String value = findPackageNameRecursive(nested, depth + 1);
                if (value != null) {
                    return value;
                }
            } catch (ReflectiveOperationException ignored) {
                // Continue probing.
            }
        }
        return extractPackageFromText(String.valueOf(target));
    }

    private static String invokeStringMethod(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            if (method.getParameterCount() != 0 || method.getReturnType() != String.class) {
                return null;
            }
            Object value = method.invoke(target);
            return value instanceof String stringValue && isLikelyPackageName(stringValue)
                    ? stringValue
                    : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Object invokeObjectMethod(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            if (method.getParameterCount() != 0) {
                return null;
            }
            return method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Object readField(Object target, String fieldName) {
        Class<?> current = target.getClass();
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (ReflectiveOperationException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static boolean isLikelyPackageName(String value) {
        return value != null
                && !value.isBlank()
                && value.indexOf('.') > 0
                && !value.contains(" ")
                && !value.contains("/")
                && Character.isJavaIdentifierStart(value.charAt(0));
    }

    private static String extractPackageFromText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String[] tokens = value.split("[^A-Za-z0-9_.$]+");
        for (String token : tokens) {
            if (isLikelyPackageName(token)) {
                return token;
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

    private static ActivityInfo readLaunchActivityInfo(Object launchActivityItem) {
        Object value = readField(launchActivityItem, "mInfo");
        return value instanceof ActivityInfo info ? info : null;
    }

    private static Configuration readLaunchActivityConfiguration(Object launchActivityItem) {
        Object override = readField(launchActivityItem, "mOverrideConfig");
        if (override instanceof Configuration configuration) {
            return configuration;
        }
        Object current = readField(launchActivityItem, "mCurConfig");
        return current instanceof Configuration configuration ? configuration : null;
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

    private static boolean applyConfigurationField(Object target,
                                                   String fieldName,
                                                   PerAppDisplayEnvironment environment) {
        Object value = readField(target, fieldName);
        if (!(value instanceof Configuration configuration)) {
            return false;
        }
        return applyConfiguration(configuration, environment);
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
            maybeLogMarkerState(packageName, configuration, config, marker);
            return null;
        }
        logIfChanged(
                packageName + ":legacy-system-marker-reuse",
                "legacy system_server launch-activity-item marker reuse: package="
                        + packageName
                        + ", widthDp=" + configuration.screenWidthDp
                        + ", heightDp=" + configuration.screenHeightDp
                        + ", smallestWidthDp=" + configuration.smallestScreenWidthDp
                        + ", densityDpi=" + configuration.densityDpi
                        + ", markerAgeMs=" + marker.ageMillis);
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
        boolean published = ViewportRuntimeMarkerBridge.publishSystemServerRecord(
                packageName,
                config.targetViewportSpec,
                new ConfigurationMarker(source),
                new EnvironmentMarker(environment),
                ViewportSourceSnapshot.SCOPE_DISPLAY,
                RuntimeClock.crossProcessMarkerMillis());
        logIfChanged(
                packageName + ":legacy-system-marker-publish",
                "legacy system_server launch-activity-item marker publish: package="
                        + packageName
                        + ", published=" + published
                        + ", targetFp=" + config.targetViewportSpec.fingerprint()
                        + ", sourceWidthDp=" + source.screenWidthDp
                        + ", sourceHeightDp=" + source.screenHeightDp
                        + ", sourceSmallestWidthDp=" + source.smallestScreenWidthDp
                        + ", sourceDensityDpi=" + source.densityDpi
                        + ", targetWidthDp=" + environment.widthDp
                        + ", targetHeightDp=" + environment.heightDp
                        + ", targetSmallestWidthDp=" + environment.smallestWidthDp
                        + ", targetDensityDpi=" + environment.densityDpi);
        return published;
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

    private static boolean applyFontScaleField(Object target,
                                               String fieldName,
                                               PerAppDisplayConfig config) {
        Object value = readField(target, fieldName);
        if (!(value instanceof Configuration configuration)) {
            return false;
        }
        return applyFontScale(configuration, config);
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

    private static boolean logIfChanged(String key, String message) {
        String previous = LAST_MESSAGES.put(key, message);
        if (message.equals(previous)) {
            return false;
        }
        logDebug(message);
        return true;
    }

    private static void maybeLogMarkerState(String packageName,
                                            Configuration configuration,
                                            PerAppDisplayConfig config,
                                            ViewportRuntimeMarkerBridge.ParseResult marker) {
        if (packageName == null || packageName.isBlank()
                || configuration == null
                || config == null
                || !config.targetViewportSpec.isEnabled()
                || marker == null) {
            return;
        }
        logIfChanged(
                packageName + ":legacy-system-marker-state",
                "legacy system_server launch-activity-item marker state: package="
                        + packageName
                        + ", hit=" + marker.hit
                        + ", reason=" + marker.reason
                        + ", targetFp=" + config.targetViewportSpec.fingerprint()
                        + ", widthDp=" + configuration.screenWidthDp
                        + ", heightDp=" + configuration.screenHeightDp
                        + ", smallestWidthDp=" + configuration.smallestScreenWidthDp
                        + ", densityDpi=" + configuration.densityDpi);
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
