package com.dpis.module.viewport;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Rect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ViewportConfigurationScope {
    private static final float WINDOW_AREA_RATIO_THRESHOLD = 0.85f;
    private static final float WINDOW_SHORT_SIDE_RATIO_THRESHOLD = 0.90f;

    // --- reflection metadata cache (stage 1 perf optimization, issue #54 item 6) ---
    // The windowConfiguration field and its accessor methods are stable for the
    // lifetime of a process (always android.content.res.WindowConfiguration on
    // API 24+). Resolving them via getDeclaredMethod + setAccessible on every
    // Resources.getConfiguration() call was ~7% of process CPU on measured
    // scroll workloads. Cache the reflection metadata once; the Rect/int values
    // themselves are still read fresh on each call because Configuration mutates.
    private static volatile Field cachedWindowConfigurationField;
    private static volatile Class<?> cachedWindowConfigurationClass;
    private static volatile Method cachedGetWindowingModeMethod;
    private static volatile Method cachedGetBoundsMethod;
    private static volatile Method cachedGetAppBoundsMethod;
    private static volatile Method cachedGetMaxBoundsMethod;

    private ViewportConfigurationScope() {
    }

    public static boolean isWindowScoped(Configuration config) {
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

    public static boolean isWindowScopedBounds(Rect bounds, Rect maxBounds) {
        if (bounds == null || maxBounds == null || bounds.isEmpty() || maxBounds.isEmpty()) {
            return false;
        }
        return isWindowScopedBounds(
                Math.abs(bounds.right - bounds.left),
                Math.abs(bounds.bottom - bounds.top),
                Math.abs(maxBounds.right - maxBounds.left),
                Math.abs(maxBounds.bottom - maxBounds.top));
    }

    public static boolean isWindowScopedBounds(int boundsWidth, int boundsHeight,
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
        Field field = cachedWindowConfigurationField;
        if (field == null) {
            try {
                field = Configuration.class.getDeclaredField("windowConfiguration");
                field.setAccessible(true);
                cachedWindowConfigurationField = field;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return null;
            }
        }
        try {
            return field.get(config);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Rect readRectMethod(Object target, String methodName) {
        Method method = resolveMethod(target, methodName);
        if (method == null) {
            return null;
        }
        try {
            Object value = method.invoke(target);
            return value instanceof Rect ? (Rect) value : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static int readIntMethod(Object target, String methodName, int fallback) {
        Method method = resolveMethod(target, methodName);
        if (method == null) {
            return fallback;
        }
        try {
            Object value = method.invoke(target);
            return value instanceof Integer ? (Integer) value : fallback;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return fallback;
        }
    }

    // Resolve a declared method on target's class, caching the Method object per
    // windowConfiguration class. The class is framework-stable so this resolves
    // once and reuses; a different class (theoretical) triggers a one-off re-resolve.
    private static Method resolveMethod(Object target, String methodName) {
        Class<?> clazz = target.getClass();
        Method cached = cachedMethodFor(methodName);
        if (cached != null && cachedWindowConfigurationClass == clazz) {
            return cached;
        }
        try {
            Method method = clazz.getDeclaredMethod(methodName);
            method.setAccessible(true);
            cachedWindowConfigurationClass = clazz;
            cacheMethodFor(methodName, method);
            return method;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Method cachedMethodFor(String methodName) {
        switch (methodName) {
            case "getWindowingMode": return cachedGetWindowingModeMethod;
            case "getBounds": return cachedGetBoundsMethod;
            case "getAppBounds": return cachedGetAppBoundsMethod;
            case "getMaxBounds": return cachedGetMaxBoundsMethod;
            default: return null;
        }
    }

    private static void cacheMethodFor(String methodName, Method method) {
        switch (methodName) {
            case "getWindowingMode": cachedGetWindowingModeMethod = method; break;
            case "getBounds": cachedGetBoundsMethod = method; break;
            case "getAppBounds": cachedGetAppBoundsMethod = method; break;
            case "getMaxBounds": cachedGetMaxBoundsMethod = method; break;
            default: break;
        }
    }

    // --- test seam ---
    public static void resetReflectionCacheForTest() {
        cachedWindowConfigurationField = null;
        cachedWindowConfigurationClass = null;
        cachedGetWindowingModeMethod = null;
        cachedGetBoundsMethod = null;
        cachedGetAppBoundsMethod = null;
        cachedGetMaxBoundsMethod = null;
    }
}
