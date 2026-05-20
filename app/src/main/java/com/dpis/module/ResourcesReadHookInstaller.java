package com.dpis.module;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.api.XposedInterface;

final class ResourcesReadHookInstaller {
    private static volatile boolean hookInstalled;
    private static final ThreadLocal<Boolean> INTERNAL_UPDATE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final Map<String, String> LAST_MESSAGES = new ConcurrentHashMap<>();

    private ResourcesReadHookInstaller() {
    }

    static void install(XposedInterface xposed, String packageName, DpiConfigStore store)
            throws ReflectiveOperationException {
        if (hookInstalled) {
            return;
        }
        synchronized (ResourcesReadHookInstaller.class) {
            if (hookInstalled) {
                return;
            }
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> resourcesClass = Class.forName("android.content.res.Resources", false, bootClassLoader);

            Method getConfigurationMethod = resourcesClass.getDeclaredMethod("getConfiguration");
            xposed.hook(getConfigurationMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (!(result instanceof Configuration configuration)) {
                            return result;
                        }
                        Object thisObject = chain.getThisObject();
                        applyConfigurationOverride(
                                thisObject instanceof Resources ? thisObject : null,
                                configuration, packageName, store,
                                "ResourcesRead(getConfiguration)");
                        return result;
                    });

            Method getDisplayMetricsMethod = resourcesClass.getDeclaredMethod("getDisplayMetrics");
            xposed.hook(getDisplayMetricsMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (!(result instanceof DisplayMetrics metrics)) {
                            return result;
                        }
                        if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                            return result;
                        }
                        Object thisObject = chain.getThisObject();
                        if (!(thisObject instanceof Resources resources)) {
                            return result;
                        }
                        INTERNAL_UPDATE.set(Boolean.TRUE);
                        try {
                            Configuration config = resources.getConfiguration();
                            applyMetricsOverride(resources, metrics, config, packageName);
                        } finally {
                            INTERNAL_UPDATE.set(Boolean.FALSE);
                        }
                        return result;
                    });

            Method getSystemMethod = resourcesClass.getDeclaredMethod("getSystem");
            xposed.hook(getSystemMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (!(result instanceof Resources resources)) {
                            return result;
                        }
                        Configuration config = resources.getConfiguration();
                        applyConfigurationOverride(resources, config, packageName, store,
                                "ResourcesRead(getSystem)");
                        DisplayMetrics metrics = resources.getDisplayMetrics();
                        applyMetricsOverride(resources, metrics, config, packageName);
                        return result;
                    });

            hookInstalled = true;
            DpisLog.i("Resources read hook ready");
        }
    }

    static void applyConfigurationOverride(Configuration config,
                                           String packageName,
                                           DpiConfigStore store,
                                           String sourceTag) {
        applyConfigurationOverride(null, config, packageName, store, sourceTag);
    }

    static void applyConfigurationOverride(Object resourceScope,
                                           Configuration config,
                                           String packageName,
                                           DpiConfigStore store,
                                           String sourceTag) {
        if (config == null) {
            return;
        }
        FontScaleOverride.Result fontScale = FontScaleOverride.resolveForResources(resourceScope,
                store, packageName, config.fontScale);
        FontScaleOverride.applyToConfiguration(config, fontScale);

        int originalWidthDp = config.screenWidthDp;
        int originalHeightDp = config.screenHeightDp;
        int originalSmallestWidthDp = config.smallestScreenWidthDp;
        int originalDensityDpi = config.densityDpi;

        Integer targetViewportWidth = TargetViewportWidthResolver.resolve(store, packageName);
        boolean windowScoped = ViewportConfigurationScope.isWindowScoped(config);
        VirtualDisplayOverride.Result stableTarget =
                VirtualDisplayState.getForTarget(targetViewportWidth);
        ViewportOverride.Result result = ViewportOverride.derive(
                config,
                targetViewportWidth != null ? targetViewportWidth : 0,
                windowScoped,
                stableTarget);
        if (result == null) {
            if (fontScale.changed) {
                logIfChanged(packageName + ":" + sourceTag + ":font-only",
                        "DPIS_FONT " + sourceTag + " override: package=" + packageName
                                + ", fontScale "
                                + fontScale.original + " -> " + config.fontScale);
            }
            return;
        }

        if (result.widthDp != originalWidthDp
                || result.heightDp != originalHeightDp
                || result.smallestWidthDp != originalSmallestWidthDp
                || (result.densityDpi > 0 && result.densityDpi != originalDensityDpi)) {
            if (ViewportModePolicy.shouldApplyConfigurationOverride(store, packageName)) {
                ViewportOverride.apply(config, result);
            }
        }

        VirtualDisplayOverride.Result sharedResult = VirtualDisplayPlan.derivePublishableResult(
                originalWidthDp,
                originalHeightDp,
                originalSmallestWidthDp,
                originalDensityDpi,
                0,
                0,
                result.smallestWidthDp);
        if (!windowScoped && sharedResult != null) {
            VirtualDisplayState.setUnlessDerivedFromTargetConfig(
                    sharedResult, originalSmallestWidthDp, targetViewportWidth);
        }

        if (result.widthDp == originalWidthDp
                && result.heightDp == originalHeightDp
                && result.smallestWidthDp == originalSmallestWidthDp
                && (result.densityDpi <= 0 || result.densityDpi == originalDensityDpi)
                && !fontScale.changed) {
            VirtualDisplayOverride.Result stableResult =
                    VirtualDisplayState.getStableTargetResult(
                            originalSmallestWidthDp, targetViewportWidth);
            if (stableResult != null && stableResult.densityDpi > 0
                    && config.densityDpi != stableResult.densityDpi) {
                config.densityDpi = stableResult.densityDpi;
                logIfChanged(packageName + ":" + sourceTag + ":stable-target",
                        "DPIS_VIEWPORT " + sourceTag + " stable target: package=" + packageName
                                + ", targetViewportWidthDp=" + describeNullable(targetViewportWidth)
                                + ", widthDp="
                                + config.screenWidthDp
                                + ", heightDp=" + config.screenHeightDp
                                + ", smallestWidthDp=" + config.smallestScreenWidthDp
                                + ", densityDpi " + originalDensityDpi
                                + " -> " + config.densityDpi
                                + ", fontScale " + fontScale.original
                                + " -> " + config.fontScale);
            }
            return;
        }
        logIfChanged(packageName + ":" + sourceTag,
                "DPIS_VIEWPORT " + sourceTag + " override: package=" + packageName
                        + ", targetViewportWidthDp=" + describeNullable(targetViewportWidth)
                        + ", scope=" + (windowScoped ? "window" : "display")
                        + ", mode="
                        + (ViewportModePolicy.shouldApplyConfigurationOverride(store, packageName)
                        ? "config" : "metrics")
                        + ", target=" + describeViewportResult(result)
                        + ", actual=" + describeConfiguration(config)
                        + ", widthDp " + originalWidthDp
                        + " -> " + config.screenWidthDp
                        + ", heightDp " + originalHeightDp + " -> " + config.screenHeightDp
                        + ", smallestWidthDp " + originalSmallestWidthDp + " -> "
                        + config.smallestScreenWidthDp
                        + ", densityDpi " + originalDensityDpi + " -> " + config.densityDpi
                        + ", fontScale " + fontScale.original + " -> " + config.fontScale);
    }

    static void applyMetricsOverride(DisplayMetrics metrics,
                                     Configuration config,
                                     String packageName) {
        applyMetricsOverride(null, metrics, config, packageName);
    }

    static void applyMetricsOverride(Object resourceScope,
                                     DisplayMetrics metrics,
                                     Configuration config,
                                     String packageName) {
        if (metrics == null || config == null) {
            return;
        }
        int targetDensityDpi = config.densityDpi > 0 ? config.densityDpi : metrics.densityDpi;
        String densitySource = config.densityDpi > 0 ? "configuration" : "metrics";
        if (targetDensityDpi <= 0) {
            return;
        }
        VirtualDisplayOverride.Result applied = matchingVirtualDisplayState(config);
        if (applied != null && applied.densityDpi > 0) {
            targetDensityDpi = applied.densityDpi;
            densitySource = "virtual-display-state";
        }
        metrics.densityDpi = targetDensityDpi;
        metrics.density = DensityOverride.densityFromDpi(targetDensityDpi);
        float fontScale = ComposeResourcesFontScheduler.maybeSuppressMetricsFontScale(
                resourceScope,
                packageName,
                config.fontScale > 0f ? config.fontScale : 1.0f);
        metrics.scaledDensity = DensityOverride.scaledDensityFrom(targetDensityDpi, fontScale);
        if (applied != null) {
            metrics.widthPixels = applied.widthPx;
            metrics.heightPixels = applied.heightPx;
        }

        logIfChanged(packageName + ":ResourcesRead(getDisplayMetrics)",
                "DPIS_VIEWPORT ResourcesRead(getDisplayMetrics) override: package=" + packageName
                        + ", densitySource=" + densitySource
                        + ", config=" + describeConfiguration(config)
                        + ", virtualDisplay=" + describeVirtualDisplayResult(applied)
                        + ", densityDpi="
                        + targetDensityDpi
                        + ", density=" + metrics.density
                        + ", scaledDensity=" + metrics.scaledDensity
                        + ", widthPx=" + metrics.widthPixels
                        + ", heightPx=" + metrics.heightPixels);
    }

    private static VirtualDisplayOverride.Result matchingVirtualDisplayState(Configuration config) {
        VirtualDisplayOverride.Result current = VirtualDisplayState.get();
        // The shared display state may have been produced by an earlier target.
        // Only reuse it when it describes the same logical viewport.
        if (current == null
                || config.smallestScreenWidthDp <= 0
                || current.smallestWidthDp != config.smallestScreenWidthDp) {
            return null;
        }
        return current;
    }

    private static void logIfChanged(String key, String message) {
        String previous = LAST_MESSAGES.put(key, message);
        if (!message.equals(previous)) {
            DpisLog.i(message);
        }
    }

    private static String describeConfiguration(Configuration config) {
        if (config == null) {
            return "null";
        }
        return "{widthDp=" + config.screenWidthDp
                + ",heightDp=" + config.screenHeightDp
                + ",smallestWidthDp=" + config.smallestScreenWidthDp
                + ",densityDpi=" + config.densityDpi
                + ",fontScale=" + config.fontScale + "}";
    }

    private static String describeViewportResult(ViewportOverride.Result result) {
        if (result == null) {
            return "null";
        }
        return "{widthDp=" + result.widthDp
                + ",heightDp=" + result.heightDp
                + ",smallestWidthDp=" + result.smallestWidthDp
                + ",densityDpi=" + result.densityDpi + "}";
    }

    private static String describeVirtualDisplayResult(VirtualDisplayOverride.Result result) {
        if (result == null) {
            return "none";
        }
        return "{widthDp=" + result.widthDp
                + ",heightDp=" + result.heightDp
                + ",smallestWidthDp=" + result.smallestWidthDp
                + ",densityDpi=" + result.densityDpi
                + ",widthPx=" + result.widthPx
                + ",heightPx=" + result.heightPx + "}";
    }

    private static String describeNullable(Integer value) {
        return value == null ? "none" : String.valueOf(value);
    }
}
