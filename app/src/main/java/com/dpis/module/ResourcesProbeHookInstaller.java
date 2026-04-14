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
    private static volatile boolean hookInstalled;

    private ResourcesProbeHookInstaller() {
    }

    static void install(XposedInterface xposed) throws ReflectiveOperationException {
        if (hookInstalled) {
            return;
        }
        synchronized (ResourcesProbeHookInstaller.class) {
            if (hookInstalled) {
                return;
            }
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> resourcesClass = Class.forName("android.content.res.Resources", false,
                    bootClassLoader);

            Method getDisplayMetricsMethod = resourcesClass.getDeclaredMethod("getDisplayMetrics");
            xposed.hook(getDisplayMetricsMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (result instanceof DisplayMetrics metrics
                                && DISPLAY_METRICS_LOG_COUNT.incrementAndGet() <= MAX_LOGS_PER_METHOD) {
                            DpisLog.i(buildDisplayMetricsLog(metrics));
                        }
                        return result;
                    });

            Method getConfigurationMethod = resourcesClass.getDeclaredMethod("getConfiguration");
            xposed.hook(getConfigurationMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (result instanceof Configuration configuration
                                && CONFIGURATION_LOG_COUNT.incrementAndGet() <= MAX_LOGS_PER_METHOD) {
                            DpisLog.i(buildConfigurationLog(configuration));
                        }
                        return result;
                    });

            hookInstalled = true;
            DpisLog.i("Resources probe hook ready");
        }
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
}
