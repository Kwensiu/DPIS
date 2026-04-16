package com.dpis.module;

import android.content.res.Configuration;
import android.util.DisplayMetrics;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.libxposed.api.XposedInterface;

final class ResourcesProbeHookInstaller {
    private static final int MAX_LOGS_PER_METHOD = 8;
    private static final AtomicInteger DISPLAY_METRICS_LOG_COUNT = new AtomicInteger();
    private static final AtomicInteger CONFIGURATION_LOG_COUNT = new AtomicInteger();
    private static volatile String targetPackageName;
    private static volatile DpiConfigStore configStore;
    private static volatile boolean hookInstalled;

    private ResourcesProbeHookInstaller() {
    }

    static void install(XposedInterface xposed, String packageName, DpiConfigStore store)
            throws ReflectiveOperationException {
        if (hookInstalled) {
            return;
        }
        synchronized (ResourcesProbeHookInstaller.class) {
            if (hookInstalled) {
                return;
            }
            targetPackageName = packageName;
            configStore = store;
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> resourcesClass = Class.forName("android.content.res.Resources", false,
                    bootClassLoader);

            Method getDisplayMetricsMethod = resourcesClass.getDeclaredMethod("getDisplayMetrics");
            xposed.hook(getDisplayMetricsMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (result instanceof DisplayMetrics metrics) {
                            DisplayMetrics overridden = maybeOverrideDisplayMetrics(metrics);
                            if (overridden != metrics) {
                                result = overridden;
                                metrics = overridden;
                            }
                            if (DISPLAY_METRICS_LOG_COUNT.incrementAndGet() <= MAX_LOGS_PER_METHOD) {
                                DpisLog.i(appendCaller(buildDisplayMetricsLog(metrics)));
                            }
                        }
                        return result;
                    });

            Method getConfigurationMethod = resourcesClass.getDeclaredMethod("getConfiguration");
            xposed.hook(getConfigurationMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (result instanceof Configuration configuration) {
                            Configuration overridden = maybeOverrideConfiguration(configuration);
                            if (overridden != configuration) {
                                result = overridden;
                                configuration = overridden;
                            }
                            if (CONFIGURATION_LOG_COUNT.incrementAndGet() <= MAX_LOGS_PER_METHOD) {
                                DpisLog.i(appendCaller(buildConfigurationLog(configuration)));
                            }
                        }
                        return result;
                    });

            hookInstalled = true;
            DpisLog.i("Resources probe hook ready");
        }
    }

    static DisplayMetrics createOverriddenDisplayMetrics(DisplayMetrics metrics, int targetWidthDp,
                                                         float fontScale) {
        if (metrics == null || targetWidthDp <= 0 || metrics.densityDpi <= 0) {
            return metrics;
        }
        int sourceWidthDp = Math.max(1, Math.round(metrics.widthPixels * 160f / metrics.densityDpi));
        int sourceHeightDp = Math.max(1, Math.round(metrics.heightPixels * 160f / metrics.densityDpi));
        VirtualDisplayOverride.Result result = VirtualDisplayOverride.derive(
                sourceWidthDp, sourceHeightDp, metrics.densityDpi,
                metrics.widthPixels, metrics.heightPixels, targetWidthDp);
        if (result == null) {
            return metrics;
        }
        DisplayMetrics copy = new DisplayMetrics();
        copy.widthPixels = metrics.widthPixels;
        copy.heightPixels = metrics.heightPixels;
        copy.densityDpi = result.densityDpi;
        copy.density = DensityOverride.densityFromDpi(result.densityDpi);
        copy.scaledDensity = DensityOverride.scaledDensityFrom(result.densityDpi, fontScale);
        copy.xdpi = metrics.xdpi;
        copy.ydpi = metrics.ydpi;
        return copy;
    }

    static Configuration createOverriddenConfiguration(Configuration configuration,
                                                       int targetWidthDp) {
        if (configuration == null || targetWidthDp <= 0) {
            return configuration;
        }
        ViewportOverride.Result result = ViewportOverride.derive(configuration, targetWidthDp);
        if (result == null) {
            return configuration;
        }
        Configuration copy = new Configuration(configuration);
        ViewportOverride.apply(copy, result);
        return copy;
    }

    private static DisplayMetrics maybeOverrideDisplayMetrics(DisplayMetrics metrics) {
        Integer targetWidthDp = TargetViewportWidthResolver.resolve(configStore, targetPackageName);
        if (targetWidthDp == null) {
            return metrics;
        }
        float fontScale = metrics.density > 0 ? (metrics.scaledDensity / metrics.density) : 1.0f;
        return createOverriddenDisplayMetrics(metrics, targetWidthDp, fontScale);
    }

    private static Configuration maybeOverrideConfiguration(Configuration configuration) {
        Integer targetWidthDp = TargetViewportWidthResolver.resolve(configStore, targetPackageName);
        if (targetWidthDp == null) {
            return configuration;
        }
        return createOverriddenConfiguration(configuration, targetWidthDp);
    }

    static String buildDisplayMetricsLog(DisplayMetrics metrics) {
        return "Resources probe(getDisplayMetrics): widthPx=" + metrics.widthPixels
                + ", heightPx=" + metrics.heightPixels
                + ", densityDpi=" + metrics.densityDpi
                + ", density=" + metrics.density
                + ", scaledDensity=" + metrics.scaledDensity;
    }

    static String buildConfigurationLog(Configuration configuration) {
        return "Resources probe(getConfiguration): widthDp=" + configuration.screenWidthDp
                + ", heightDp=" + configuration.screenHeightDp
                + ", smallestWidthDp=" + configuration.smallestScreenWidthDp
                + ", densityDpi=" + configuration.densityDpi;
    }

    private static String appendCaller(String message) {
        String caller = CallerTrace.capture(targetPackageName);
        if (caller == null) {
            return message;
        }
        return message + ", caller=" + caller;
    }
}

