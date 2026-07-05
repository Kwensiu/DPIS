package com.dpis.module.runtime.appprocess;

import com.dpis.module.DpisLog;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import com.dpis.module.runtime.ProcessScopedInstallGate;

import io.github.libxposed.api.XposedInterface;

public final class ChromiumViewportProbeHookInstaller {
    private static final int MAX_LOGS = 12;
    private static final AtomicInteger LOG_COUNT = new AtomicInteger();
    private static volatile int installedPid = -1;

    private ChromiumViewportProbeHookInstaller() {
    }

    public static void resetForHotReload() {
        installedPid = -1;
        LOG_COUNT.set(0);
    }

    public static void install(XposedInterface xposed, ClassLoader classLoader)
            throws ReflectiveOperationException {
        if (xposed == null || classLoader == null
                || ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
            return;
        }
        synchronized (ChromiumViewportProbeHookInstaller.class) {
            if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
                return;
            }
            hookResourceManagerCreate(xposed, classLoader);
            hookWindowAndroidConstructors(xposed, classLoader);
            installedPid = ProcessScopedInstallGate.currentPid();
            DpisLog.i("DPIS_WEBAPK Chromium viewport probe ready");
        }
    }

    private static void hookResourceManagerCreate(XposedInterface xposed, ClassLoader classLoader)
            throws ReflectiveOperationException {
        Class<?> resourceManagerClass = Class.forName(
                "org.chromium.ui.resources.ResourceManager", false, classLoader);
        Class<?> windowAndroidClass = Class.forName(
                "org.chromium.ui.base.WindowAndroid", false, classLoader);
        Method create = resourceManagerClass.getDeclaredMethod(
                "create", windowAndroidClass, long.class);
        xposed.hook(create)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object windowAndroid = chain.getArg(0);
                    logContext("ResourceManager.create", contextFromWindowAndroid(windowAndroid));
                    return chain.proceed();
                });
    }

    private static void hookWindowAndroidConstructors(XposedInterface xposed, ClassLoader classLoader)
            throws ReflectiveOperationException {
        Class<?> windowAndroidClass = Class.forName(
                "org.chromium.ui.base.WindowAndroid", false, classLoader);
        for (java.lang.reflect.Constructor<?> constructor
                : windowAndroidClass.getDeclaredConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length == 0 || parameterTypes[0] != Context.class) {
                continue;
            }
            xposed.hook(constructor)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        logContext("WindowAndroid.<init>", (Context) chain.getArg(0));
                        return chain.proceed();
                    });
        }
    }

    private static Context contextFromWindowAndroid(Object windowAndroid) {
        Object holder = readField(windowAndroid, "W");
        if (holder == null) {
            return null;
        }
        try {
            Method get = holder.getClass().getMethod("get");
            Object context = get.invoke(holder);
            return context instanceof Context typed ? typed : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Object readField(Object target, String fieldName) {
        if (target == null || fieldName == null) {
            return null;
        }
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static void logContext(String source, Context context) {
        if (context == null || LOG_COUNT.incrementAndGet() > MAX_LOGS) {
            return;
        }
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        DpisLog.i("DPIS_WEBAPK Chromium viewport probe: source=" + source
                + ", context=" + context.getClass().getName()
                + ", widthDp=" + config.screenWidthDp
                + ", heightDp=" + config.screenHeightDp
                + ", smallestWidthDp=" + config.smallestScreenWidthDp
                + ", densityDpi=" + config.densityDpi
                + ", metricsDensityDpi=" + metrics.densityDpi
                + ", metricsDensity=" + metrics.density
                + ", metricsWidthPx=" + metrics.widthPixels
                + ", metricsHeightPx=" + metrics.heightPixels);
    }
}
