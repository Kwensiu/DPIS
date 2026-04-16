package com.dpis.module;

import android.graphics.Point;
import android.util.DisplayMetrics;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.api.XposedInterface;

final class DisplayHookInstaller {
    private static final String PACKAGE_XIAOHEIHE = "com.max.xiaoheihe";
    private static volatile boolean hookInstalled;
    private static volatile String targetPackageName;
    private static final Map<String, String> LAST_MESSAGES = new ConcurrentHashMap<>();

    private DisplayHookInstaller() {
    }

    static void install(XposedInterface xposed, String packageName) throws ReflectiveOperationException {
        if (hookInstalled) {
            return;
        }
        synchronized (DisplayHookInstaller.class) {
            if (hookInstalled) {
                return;
            }
            targetPackageName = packageName;
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> displayClass = Class.forName("android.view.Display", false, bootClassLoader);
            hookDisplayMetricsMethod(xposed, displayClass, "getMetrics");
            hookDisplayMetricsMethod(xposed, displayClass, "getRealMetrics");
            hookPointMethod(xposed, displayClass, "getSize");
            hookPointMethod(xposed, displayClass, "getRealSize");
            hookInstalled = true;
            DpisLog.i("Display hook ready");
        }
    }

    private static void hookDisplayMetricsMethod(XposedInterface xposed, Class<?> displayClass,
                                                 String methodName)
            throws ReflectiveOperationException {
        Method method = displayClass.getDeclaredMethod(methodName, DisplayMetrics.class);
        xposed.hook(method)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    DisplayMetrics metrics = (DisplayMetrics) chain.getArg(0);
                    applyDisplayMetrics(metrics, methodName);
                    return result;
                });
    }

    private static void hookPointMethod(XposedInterface xposed, Class<?> displayClass,
                                        String methodName) throws ReflectiveOperationException {
        Method method = displayClass.getDeclaredMethod(methodName, Point.class);
        xposed.hook(method)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    Point point = (Point) chain.getArg(0);
                    applyPoint(point, methodName);
                    return result;
                });
    }

    static void applyDisplayMetrics(DisplayMetrics metrics, String sourceTag) {
        if (metrics == null) {
            return;
        }
        if (!shouldApplyOverrideForPackage(targetPackageName)) {
            return;
        }
        VirtualDisplayOverride.Result override = VirtualDisplayState.get();
        if (override == null) {
            return;
        }
        metrics.densityDpi = override.densityDpi;
        metrics.density = DensityOverride.densityFromDpi(override.densityDpi);
        metrics.scaledDensity = metrics.density;
        String message = "Display override(" + sourceTag + "): widthPx=" + metrics.widthPixels
                + ", heightPx=" + metrics.heightPixels
                + ", densityDpi=" + metrics.densityDpi;
        logIfChanged("metrics:" + sourceTag, message);
    }

    static void applyPoint(Point point, String sourceTag) {
        if (point == null) {
            return;
        }
        if (!shouldApplyOverrideForPackage(targetPackageName)) {
            return;
        }
        if (!WindowFrameOverride.isEnabled()) {
            return;
        }
        String message = "Display override(" + sourceTag + "): size=" + point.x + "x" + point.y;
        logIfChanged("point:" + sourceTag, message);
    }

    static boolean shouldApplyOverrideForPackage(String packageName) {
        return PACKAGE_XIAOHEIHE.equals(packageName);
    }

    private static void logIfChanged(String key, String message) {
        String previous = LAST_MESSAGES.put(key, message);
        if (!message.equals(previous)) {
            DpisLog.i(message);
        }
    }
}
