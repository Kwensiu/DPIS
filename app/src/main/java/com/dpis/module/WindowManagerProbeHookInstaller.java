package com.dpis.module;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;

final class WindowManagerProbeHookInstaller {
    private static volatile boolean hookInstalled;

    private WindowManagerProbeHookInstaller() {
    }

    static void install(XposedInterface xposed) throws ReflectiveOperationException {
        if (hookInstalled) {
            return;
        }
        synchronized (WindowManagerProbeHookInstaller.class) {
            if (hookInstalled) {
                return;
            }
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> windowManagerClass = Class.forName(
                    "android.view.WindowManager", false, bootClassLoader);
            hookProbeMethod(xposed, windowManagerClass, "getCurrentWindowMetrics");
            hookProbeMethod(xposed, windowManagerClass, "getMaximumWindowMetrics");
            hookInstalled = true;
            DpisLog.i("WindowManager probe hook ready");
        }
    }

    static String buildProbeLog(String methodName, Object result) {
        return "WindowManager probe(" + methodName + "): result="
                + (result != null ? result.getClass().getName() : "null");
    }

    private static void hookProbeMethod(XposedInterface xposed, Class<?> windowManagerClass,
                                        String methodName) throws ReflectiveOperationException {
        Method method = windowManagerClass.getMethod(methodName);
        xposed.hook(method)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    DpisLog.i(buildProbeLog(methodName, result));
                    return result;
                });
    }
}
