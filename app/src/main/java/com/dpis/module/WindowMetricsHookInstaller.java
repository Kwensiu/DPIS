package com.dpis.module;

import android.graphics.Rect;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;

final class WindowMetricsHookInstaller {
    private static volatile boolean hookInstalled;

    private WindowMetricsHookInstaller() {
    }

    static void install(XposedInterface xposed) throws ReflectiveOperationException {
        if (hookInstalled) {
            return;
        }
        synchronized (WindowMetricsHookInstaller.class) {
            if (hookInstalled) {
                return;
            }
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> windowMetricsClass = Class.forName(
                    "android.view.WindowMetrics", false, bootClassLoader);
            Method getBoundsMethod = windowMetricsClass.getDeclaredMethod("getBounds");
            xposed.hook(getBoundsMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (!(result instanceof Rect rect)) {
                            return result;
                        }
                        if (!WindowFrameOverride.isEnabled()) {
                            return result;
                        }
                        VirtualDisplayOverride.Result override = VirtualDisplayState.get();
                        if (override == null) {
                            return result;
                        }
                        Rect newRect = new Rect(rect.left, rect.top,
                                rect.left + override.widthPx, rect.top + override.heightPx);
                        DpisLog.i("WindowMetrics override: bounds=" + rect.width() + "x"
                                + rect.height() + " -> " + newRect.width() + "x"
                                + newRect.height());
                        return newRect;
                    });
            hookInstalled = true;
            DpisLog.i("WindowMetrics hook ready");
        }
    }
}
