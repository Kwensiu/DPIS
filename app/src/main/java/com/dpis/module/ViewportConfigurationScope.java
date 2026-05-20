package com.dpis.module;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Rect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

final class ViewportConfigurationScope {
    private static final float WINDOW_AREA_RATIO_THRESHOLD = 0.85f;
    private static final float WINDOW_SHORT_SIDE_RATIO_THRESHOLD = 0.90f;

    private ViewportConfigurationScope() {
    }

    static boolean isWindowScoped(Configuration config) {
        Object windowConfiguration = readWindowConfiguration(config);
        if (windowConfiguration == null) {
            return false;
        }
        int windowingMode = readIntMethod(windowConfiguration, "getWindowingMode", -1);
        if (windowingMode > 1) {
            return true;
        }
        Rect bounds = readRectMethod(windowConfiguration, "getBounds");
        Rect appBounds = readRectMethod(windowConfiguration, "getAppBounds");
        Rect maxBounds = readRectMethod(windowConfiguration, "getMaxBounds");
        return isWindowScopedBounds(bounds, maxBounds)
                || isWindowScopedBounds(appBounds, maxBounds);
    }

    static boolean isWindowScopedBounds(Rect bounds, Rect maxBounds) {
        if (bounds == null || maxBounds == null || bounds.isEmpty() || maxBounds.isEmpty()) {
            return false;
        }
        return isWindowScopedBounds(
                Math.abs(bounds.right - bounds.left),
                Math.abs(bounds.bottom - bounds.top),
                Math.abs(maxBounds.right - maxBounds.left),
                Math.abs(maxBounds.bottom - maxBounds.top));
    }

    static boolean isWindowScopedBounds(int boundsWidth, int boundsHeight,
                                        int maxWidth, int maxHeight) {
        if (boundsWidth <= 0 || boundsHeight <= 0 || maxWidth <= 0 || maxHeight <= 0) {
            return false;
        }
        if (boundsWidth > maxWidth || boundsHeight > maxHeight) {
            return false;
        }
        int boundsShort = Math.min(boundsWidth, boundsHeight);
        int maxShort = Math.min(maxWidth, maxHeight);
        long boundsArea = (long) boundsWidth * (long) boundsHeight;
        long maxArea = (long) maxWidth * (long) maxHeight;
        return boundsShort < Math.round(maxShort * WINDOW_SHORT_SIDE_RATIO_THRESHOLD)
                || boundsArea < Math.round(maxArea * WINDOW_AREA_RATIO_THRESHOLD);
    }

    @SuppressLint("BlockedPrivateApi")
    private static Object readWindowConfiguration(Configuration config) {
        if (config == null) {
            return null;
        }
        try {
            Field field = Configuration.class.getDeclaredField("windowConfiguration");
            field.setAccessible(true);
            return field.get(config);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Rect readRectMethod(Object target, String methodName) {
        try {
            Method method = target.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            Object value = method.invoke(target);
            return value instanceof Rect ? (Rect) value : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static int readIntMethod(Object target, String methodName, int fallback) {
        try {
            Method method = target.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            Object value = method.invoke(target);
            return value instanceof Integer ? (Integer) value : fallback;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return fallback;
        }
    }
}
