package com.dpis.module;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.libxposed.api.XposedInterface;

final class ViewRootProbeHookInstaller {
    private static final int MAX_LOGS = 8;
    private static final AtomicInteger LOG_COUNT = new AtomicInteger();
    private static volatile boolean hookInstalled;
    private static volatile String lastOverrideLog;

    private ViewRootProbeHookInstaller() {
    }

    static void install(XposedInterface xposed) throws ReflectiveOperationException {
        if (hookInstalled) {
            return;
        }
        synchronized (ViewRootProbeHookInstaller.class) {
            if (hookInstalled) {
                return;
            }
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> viewRootImplClass = Class.forName("android.view.ViewRootImpl", false,
                    bootClassLoader);
            Method performTraversalsMethod = viewRootImplClass.getDeclaredMethod("performTraversals");
            xposed.hook(performTraversalsMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object thisObject = chain.getThisObject();
                        if (LOG_COUNT.incrementAndGet() <= MAX_LOGS) {
                            DpisLog.i(buildPerformTraversalsLog(thisObject));
                        }
                        String overrideLog = buildRootOverrideLog(thisObject);
                        if (overrideLog != null && !overrideLog.equals(lastOverrideLog)) {
                            lastOverrideLog = overrideLog;
                            DpisLog.i(overrideLog);
                        }
                        applyRootSizeOverride(thisObject);
                        return chain.proceed();
                    });
            hookInstalled = true;
            DpisLog.i("ViewRoot probe hook ready");
        }
    }

    static String buildPerformTraversalsLog(Object viewRootImpl) {
        return "ViewRoot probe(performTraversals): width="
                + readIntField(viewRootImpl, "mWidth")
                + ", height=" + readIntField(viewRootImpl, "mHeight");
    }

    static boolean applyRootSizeOverride(Object viewRootImpl) {
        VirtualDisplayOverride.Result override = VirtualDisplayState.get();
        if (override == null || viewRootImpl == null) {
            return false;
        }
        int currentWidth = readIntField(viewRootImpl, "mWidth");
        int currentHeight = readIntField(viewRootImpl, "mHeight");
        if (currentWidth <= 0 || currentHeight <= 0) {
            return false;
        }
        boolean changed = false;
        changed |= writeIntField(viewRootImpl, "mWidth", override.widthPx, currentWidth);
        changed |= writeIntField(viewRootImpl, "mHeight", override.heightPx, currentHeight);
        return changed;
    }

    static String buildRootOverrideLog(Object viewRootImpl) {
        VirtualDisplayOverride.Result override = VirtualDisplayState.get();
        if (override == null || viewRootImpl == null) {
            return null;
        }
        int currentWidth = readIntField(viewRootImpl, "mWidth");
        int currentHeight = readIntField(viewRootImpl, "mHeight");
        if (currentWidth <= 0 || currentHeight <= 0) {
            return null;
        }
        if (currentWidth == override.widthPx && currentHeight == override.heightPx) {
            return null;
        }
        return "ViewRoot override: width=" + currentWidth + " -> " + override.widthPx
                + ", height=" + currentHeight + " -> " + override.heightPx;
    }

    private static int readIntField(Object target, String fieldName) {
        if (target == null) {
            return -1;
        }
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.getInt(target);
        } catch (ReflectiveOperationException ignored) {
            return -1;
        }
    }

    private static boolean writeIntField(Object target, String fieldName, int newValue,
                                         int currentValue) {
        if (target == null || currentValue == newValue) {
            return false;
        }
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.setInt(target, newValue);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
