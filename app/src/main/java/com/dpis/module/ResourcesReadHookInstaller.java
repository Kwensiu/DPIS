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
                        applyConfigurationOverride(configuration, packageName, store,
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
                            applyMetricsOverride(metrics, config, packageName);
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
                        applyConfigurationOverride(config, packageName, store,
                                "ResourcesRead(getSystem)");
                        DisplayMetrics metrics = resources.getDisplayMetrics();
                        applyMetricsOverride(metrics, config, packageName);
                        return result;
                    });

            hookInstalled = true;
            DpisLog.i("Resources read hook ready");
        }
    }

    private static void applyConfigurationOverride(Configuration config,
                                                   String packageName,
                                                   DpiConfigStore store,
                                                   String sourceTag) {
        if (config == null) {
            return;
        }
        FontScaleOverride.Result fontScale = FontScaleOverride.resolve(store, packageName, config.fontScale);
        FontScaleOverride.applyToConfiguration(config, fontScale);

        int originalWidthDp = config.screenWidthDp;
        int originalHeightDp = config.screenHeightDp;
        int originalSmallestWidthDp = config.smallestScreenWidthDp;
        int originalDensityDpi = config.densityDpi;

        Integer targetViewportWidth = TargetViewportWidthResolver.resolve(store, packageName);
        ViewportOverride.Result result = ViewportOverride.derive(
                config, targetViewportWidth != null ? targetViewportWidth : 0);
        if (result == null) {
            if (fontScale.changed) {
                logIfChanged(packageName + ":" + sourceTag + ":font-only",
                        "DPIS_FONT " + sourceTag + " override: fontScale "
                                + fontScale.original + " -> " + config.fontScale);
            }
            return;
        }

        if (result.widthDp != originalWidthDp
                || result.heightDp != originalHeightDp
                || result.smallestWidthDp != originalSmallestWidthDp
                || result.densityDpi != originalDensityDpi) {
            ViewportOverride.apply(config, result);
        }

        VirtualDisplayOverride.Result sharedResult = VirtualDisplayOverride.derive(
                originalWidthDp > 0 ? originalWidthDp : result.widthDp,
                originalHeightDp > 0 ? originalHeightDp : result.heightDp,
                originalDensityDpi > 0 ? originalDensityDpi : result.densityDpi,
                Math.round((originalWidthDp > 0 ? originalWidthDp : result.widthDp)
                        * ((originalDensityDpi > 0 ? originalDensityDpi : result.densityDpi) / 160.0f)),
                Math.round((originalHeightDp > 0 ? originalHeightDp : result.heightDp)
                        * ((originalDensityDpi > 0 ? originalDensityDpi : result.densityDpi) / 160.0f)),
                result.widthDp);
        if (sharedResult != null) {
            VirtualDisplayState.set(sharedResult);
        }

        if (result.widthDp == originalWidthDp
                && result.heightDp == originalHeightDp
                && result.smallestWidthDp == originalSmallestWidthDp
                && result.densityDpi == originalDensityDpi
                && !fontScale.changed) {
            return;
        }
        logIfChanged(packageName + ":" + sourceTag,
                "DPIS_FONT " + sourceTag + " override: widthDp " + originalWidthDp
                        + " -> " + config.screenWidthDp
                        + ", heightDp " + originalHeightDp + " -> " + config.screenHeightDp
                        + ", smallestWidthDp " + originalSmallestWidthDp + " -> "
                        + config.smallestScreenWidthDp
                        + ", densityDpi " + originalDensityDpi + " -> " + config.densityDpi
                        + ", fontScale " + fontScale.original + " -> " + config.fontScale);
    }

    private static void applyMetricsOverride(DisplayMetrics metrics,
                                             Configuration config,
                                             String packageName) {
        if (metrics == null || config == null) {
            return;
        }
        int targetDensityDpi = config.densityDpi > 0 ? config.densityDpi : metrics.densityDpi;
        if (targetDensityDpi <= 0) {
            return;
        }
        metrics.densityDpi = targetDensityDpi;
        metrics.density = DensityOverride.densityFromDpi(targetDensityDpi);
        float fontScale = config.fontScale > 0f ? config.fontScale : 1.0f;
        metrics.scaledDensity = DensityOverride.scaledDensityFrom(targetDensityDpi, fontScale);

        VirtualDisplayOverride.Result applied = VirtualDisplayState.get();
        if (applied != null) {
            metrics.widthPixels = applied.widthPx;
            metrics.heightPixels = applied.heightPx;
        }

        logIfChanged(packageName + ":ResourcesRead(getDisplayMetrics)",
                "DPIS_FONT ResourcesRead(getDisplayMetrics) override: densityDpi="
                        + targetDensityDpi
                        + ", density=" + metrics.density
                        + ", scaledDensity=" + metrics.scaledDensity
                        + ", widthPx=" + metrics.widthPixels
                        + ", heightPx=" + metrics.heightPixels);
    }

    private static void logIfChanged(String key, String message) {
        String previous = LAST_MESSAGES.put(key, message);
        if (!message.equals(previous)) {
            DpisLog.i(message);
        }
    }
}
