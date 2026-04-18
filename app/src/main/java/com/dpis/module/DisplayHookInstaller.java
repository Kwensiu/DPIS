package com.dpis.module;

import android.graphics.Point;
import android.util.DisplayMetrics;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.api.XposedInterface;

final class DisplayHookInstaller {
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
            hookDisplayInfoMethod(xposed, displayClass, bootClassLoader);
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

    private static void hookDisplayInfoMethod(XposedInterface xposed,
                                              Class<?> displayClass,
                                              ClassLoader bootClassLoader) {
        try {
            Class<?> displayInfoClass = Class.forName("android.view.DisplayInfo", false, bootClassLoader);
            Method method = displayClass.getDeclaredMethod("getDisplayInfo", displayInfoClass);
            xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        Object displayInfo = chain.getArg(0);
                        applyDisplayInfo(displayInfo, "getDisplayInfo");
                        return result;
                    });
        } catch (ReflectiveOperationException ignored) {
            DpisLog.i("Display getDisplayInfo hook skipped");
        }
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
        float fontScale = metrics.density > 0f ? (metrics.scaledDensity / metrics.density) : 1.0f;
        if (fontScale <= 0f) {
            fontScale = 1.0f;
        }
        metrics.densityDpi = override.densityDpi;
        metrics.density = DensityOverride.densityFromDpi(override.densityDpi);
        metrics.scaledDensity = metrics.density * fontScale;
        metrics.widthPixels = override.widthPx;
        metrics.heightPixels = override.heightPx;
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
        VirtualDisplayOverride.Result override = VirtualDisplayState.get();
        if (override == null) {
            return;
        }
        point.x = override.widthPx;
        point.y = override.heightPx;
        String message = "Display override(" + sourceTag + "): size=" + point.x + "x" + point.y;
        logIfChanged("point:" + sourceTag, message);
    }

    static void applyDisplayInfo(Object displayInfo, String sourceTag) {
        if (displayInfo == null) {
            return;
        }
        if (!shouldApplyOverrideForPackage(targetPackageName)) {
            return;
        }
        VirtualDisplayOverride.Result override = VirtualDisplayState.get();
        if (override == null) {
            return;
        }
        boolean changed = false;
        changed |= writeIntField(displayInfo, "logicalDensityDpi", override.densityDpi);
        changed |= writeIntField(displayInfo, "logicalWidth", override.widthPx);
        changed |= writeIntField(displayInfo, "logicalHeight", override.heightPx);
        changed |= writeIntField(displayInfo, "appWidth", override.widthPx);
        changed |= writeIntField(displayInfo, "appHeight", override.heightPx);
        changed |= writeIntField(displayInfo, "smallestNominalAppWidth", override.widthPx);
        changed |= writeIntField(displayInfo, "smallestNominalAppHeight", override.heightPx);
        changed |= writeIntField(displayInfo, "largestNominalAppWidth", override.widthPx);
        changed |= writeIntField(displayInfo, "largestNominalAppHeight", override.heightPx);
        if (!changed) {
            return;
        }
        String message = "Display override(" + sourceTag + "): logical=" + override.widthPx + "x"
                + override.heightPx + ", densityDpi=" + override.densityDpi;
        logIfChanged("displayInfo:" + sourceTag, message);
    }

    static boolean shouldApplyOverrideForPackage(String packageName) {
        return packageName != null && !packageName.isBlank();
    }

    private static boolean writeIntField(Object target, String fieldName, int value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            int current = field.getInt(target);
            if (current == value) {
                return false;
            }
            field.setInt(target, value);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void logIfChanged(String key, String message) {
        String previous = LAST_MESSAGES.put(key, message);
        if (!message.equals(previous)) {
            DpisLog.i(message);
        }
    }
}
