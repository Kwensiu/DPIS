package com.dpis.module;

import android.content.res.Configuration;
import android.util.DisplayMetrics;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.api.XposedInterface;

final class ResourcesImplHookInstaller {
    private static volatile boolean hookInstalled;
    private static final Map<String, String> LAST_MESSAGES = new ConcurrentHashMap<>();

    private ResourcesImplHookInstaller() {
    }

    static void install(XposedInterface xposed, String packageName, DpiConfigStore store)
            throws ReflectiveOperationException {
        if (hookInstalled) {
            return;
        }
        synchronized (ResourcesImplHookInstaller.class) {
            if (hookInstalled) {
                return;
            }
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> resourcesImplClass = Class.forName(
                    "android.content.res.ResourcesImpl", false, bootClassLoader);
            Class<?> compatibilityInfoClass = Class.forName(
                    "android.content.res.CompatibilityInfo", false, bootClassLoader);
            Method method = resourcesImplClass.getDeclaredMethod(
                    "updateConfiguration", Configuration.class, DisplayMetrics.class,
                    compatibilityInfoClass);
            xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Configuration config = (Configuration) chain.getArg(0);
                        DisplayMetrics metrics = (DisplayMetrics) chain.getArg(1);
                        applyDensityOverride(packageName, config, metrics, store);
                        return chain.proceed();
                    });
            hookInstalled = true;
            DpisLog.i("ResourcesImpl hook ready");
        }
    }

    static void applyDensityOverride(String packageName, Configuration config, DisplayMetrics metrics,
                                     DpiConfigStore store) {
        if (config == null) {
            logIfChanged(packageName + ":skip", "ResourcesImpl skip: config is null");
            return;
        }
        FontScaleOverride.Result fontScale = FontScaleOverride.resolve(
                store, packageName, config.fontScale);
        FontScaleOverride.applyToConfiguration(config, fontScale);
        int originalWidthDp = config.screenWidthDp;
        int originalHeightDp = config.screenHeightDp;
        int originalSmallestWidthDp = config.smallestScreenWidthDp;
        int originalDensityDpi = config.densityDpi;
        Integer targetViewportWidth = TargetViewportWidthResolver.resolve(store, packageName);
        ViewportOverride.Result result = ViewportOverride.derive(
                config, targetViewportWidth != null ? targetViewportWidth : 0);
        if (result == null) {
            FontScaleOverride.applyScaledDensity(metrics, config);
            logIfChanged(packageName + ":observe",
                    "DPIS_FONT ResourcesImpl observe: widthDp=" + originalWidthDp
                            + ", heightDp=" + originalHeightDp
                            + ", smallestWidthDp=" + originalSmallestWidthDp
                            + ", densityDpi=" + originalDensityDpi
                            + ", fontScale=" + fontScale.original + " -> " + config.fontScale);
            return;
        }
        boolean needsViewportUpdate = result.widthDp != originalWidthDp
                || result.heightDp != originalHeightDp
                || result.smallestWidthDp != originalSmallestWidthDp
                || result.densityDpi != originalDensityDpi;
        boolean applyToConfiguration = ViewportModePolicy.shouldApplyConfigurationOverride(
                store, packageName);
        int sourceWidthPx = metrics != null && metrics.widthPixels > 0
                ? metrics.widthPixels
                : Math.round(originalWidthDp * (originalDensityDpi / 160.0f));
        int sourceHeightPx = metrics != null && metrics.heightPixels > 0
                ? metrics.heightPixels
                : Math.round(originalHeightDp * (originalDensityDpi / 160.0f));
        VirtualDisplayOverride.Result sharedResult = VirtualDisplayOverride.derive(
                originalWidthDp > 0 ? originalWidthDp : result.widthDp,
                originalHeightDp > 0 ? originalHeightDp : result.heightDp,
                originalDensityDpi > 0 ? originalDensityDpi : result.densityDpi,
                sourceWidthPx,
                sourceHeightPx,
                result.smallestWidthDp);
        if (sharedResult != null) {
            VirtualDisplayState.set(sharedResult);
        }
        String viewportMode = ViewportModePolicy.resolve(store, packageName);
        ViewportDebugReporter.report(
                store,
                packageName,
                viewportMode,
                originalWidthDp,
                originalHeightDp,
                originalDensityDpi,
                result,
                sharedResult,
                applyToConfiguration);
        if (!needsViewportUpdate) {
            FontScaleOverride.applyScaledDensity(metrics, config);
            logIfChanged(packageName + ":observe",
                    "DPIS_FONT ResourcesImpl observe: widthDp=" + originalWidthDp
                            + ", heightDp=" + originalHeightDp
                            + ", smallestWidthDp=" + originalSmallestWidthDp
                            + ", densityDpi=" + originalDensityDpi
                            + ", fontScale=" + fontScale.original + " -> " + config.fontScale);
            return;
        }
        if (applyToConfiguration) {
            ViewportOverride.apply(config, result);
        }
        float targetDensity = DensityOverride.densityFromDpi(result.densityDpi);
        float targetScaledDensity = DensityOverride.scaledDensityFrom(
                result.densityDpi, config.fontScale);
        if (metrics != null) {
            VirtualDisplayOverride.Result applied = VirtualDisplayState.get();
            metrics.densityDpi = result.densityDpi;
            metrics.density = targetDensity;
            metrics.scaledDensity = targetScaledDensity;
            if (applied != null) {
                metrics.widthPixels = applied.widthPx;
                metrics.heightPixels = applied.heightPx;
            }
        }
        String modeLabel = applyToConfiguration ? "emulation" : "replace";
        logIfChanged(packageName + ":override",
                "DPIS_FONT ResourcesImpl (" + modeLabel + ") override: widthDp "
                        + originalWidthDp + " -> " + result.widthDp
                        + ", heightDp " + originalHeightDp + " -> " + result.heightDp
                        + ", smallestWidthDp " + originalSmallestWidthDp + " -> "
                        + result.smallestWidthDp
                        + ", densityDpi " + originalDensityDpi + " -> " + result.densityDpi
                        + ", fontScale " + fontScale.original + " -> " + config.fontScale
                        + ", metricsDensityDpi=" + (metrics != null ? metrics.densityDpi : -1)
                        + ", metricsWidthPx=" + (metrics != null ? metrics.widthPixels : -1)
                        + ", metricsHeightPx=" + (metrics != null ? metrics.heightPixels : -1));
    }

    private static void logIfChanged(String key, String message) {
        String previous = LAST_MESSAGES.put(key, message);
        if (!message.equals(previous)) {
            DpisLog.i(message);
        }
    }
}

