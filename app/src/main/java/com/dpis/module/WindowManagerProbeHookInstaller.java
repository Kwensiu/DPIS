package com.dpis.module;

import android.graphics.Rect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

import io.github.libxposed.api.XposedInterface;

final class WindowManagerProbeHookInstaller {
    private static volatile String targetPackageName;
    private static volatile boolean hookInstalled;

    private WindowManagerProbeHookInstaller() {
    }

    static void install(XposedInterface xposed, String packageName) throws ReflectiveOperationException {
        if (hookInstalled) {
            return;
        }
        synchronized (WindowManagerProbeHookInstaller.class) {
            if (hookInstalled) {
                return;
            }
            targetPackageName = packageName;
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            for (Class<?> windowManagerClass : resolveWindowManagerClasses(bootClassLoader)) {
                hookProbeMethod(xposed, windowManagerClass, "getCurrentWindowMetrics");
                hookProbeMethod(xposed, windowManagerClass, "getMaximumWindowMetrics");
            }
            hookInstalled = true;
            DpisLog.i("WindowManager probe hook ready");
        }
    }

    private static Set<Class<?>> resolveWindowManagerClasses(ClassLoader classLoader)
            throws ReflectiveOperationException {
        Set<Class<?>> classes = new LinkedHashSet<>();
        classes.add(Class.forName("android.view.WindowManagerImpl", false, classLoader));
        classes.add(Class.forName("android.view.WindowManager", false, classLoader));
        return classes;
    }

    static String buildProbeLog(String methodName, Object result) {
        if (result == null) {
            return buildProbeLog(methodName, "null", null);
        }
        return buildProbeLog(methodName, result.getClass().getName(), extractBoundsSummary(result));
    }

    static String buildProbeLog(String methodName, String resultType, String boundsSummary) {
        if (boundsSummary == null || boundsSummary.isEmpty()) {
            return "WindowManager probe(" + methodName + "): result=" + resultType;
        }
        return "WindowManager probe(" + methodName + "): result=" + resultType
                + ", bounds=" + boundsSummary;
    }

    private static String extractBoundsSummary(Object result) {
        if (result == null) {
            return null;
        }
        try {
            Method getBoundsMethod = result.getClass().getMethod("getBounds");
            Object bounds = getBoundsMethod.invoke(result);
            if (bounds instanceof Rect rect) {
                return rect.width() + "x" + rect.height();
            }
        } catch (NoSuchMethodException | IllegalAccessException |
                 InvocationTargetException ignored) {
            return null;
        }
        return null;
    }

    private static void hookProbeMethod(XposedInterface xposed, Class<?> windowManagerClass,
                                        String methodName) throws ReflectiveOperationException {
        Method method = windowManagerClass.getMethod(methodName);
        xposed.hook(method)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    DpisLog.i(appendCaller(buildProbeLog(methodName, result)));
                    return result;
                });
    }

    private static String appendCaller(String message) {
        String caller = CallerTrace.capture(targetPackageName);
        if (caller == null) {
            return message;
        }
        return message + ", caller=" + caller;
    }
}
