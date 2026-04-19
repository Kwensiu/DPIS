package com.dpis.module;

import android.content.res.Configuration;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.api.XposedInterface;

final class ActivityThreadFontHookInstaller {
    private static final String FONT_LOG_KEY_PREFIX = "font";
    private static volatile boolean hookInstalled;
    private static final Map<String, String> LAST_MESSAGES = new ConcurrentHashMap<>();

    private ActivityThreadFontHookInstaller() {
    }

    static void install(XposedInterface xposed, String packageName, DpiConfigStore store)
            throws ReflectiveOperationException {
        if (hookInstalled) {
            return;
        }
        synchronized (ActivityThreadFontHookInstaller.class) {
            if (hookInstalled) {
                return;
            }
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> activityThreadClass =
                    Class.forName("android.app.ActivityThread", false, bootClassLoader);
            Method handleBindApplication = resolveHandleBindApplication(activityThreadClass);
            xposed.hook(handleBindApplication)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object bindData = chain.getArg(0);
                        applyFontScaleToBindData(bindData, packageName, store);
                        return chain.proceed();
                    });
            hookInstalled = true;
            DpisLog.i("ActivityThread font hook ready");
        }
    }

    static boolean applyFontScaleToBindData(Object bindData, String packageName, DpiConfigStore store) {
        if (bindData == null) {
            return false;
        }
        Configuration config = readConfig(bindData);
        if (config == null) {
            return false;
        }
        FontScaleOverride.Result fontScale = FontScaleOverride.resolve(store, packageName, config.fontScale);
        if (!fontScale.changed) {
            return false;
        }
        config.fontScale = fontScale.effective;
        logIfChanged(buildFontLogKey(packageName, "activity-thread-bind"),
                "DPIS_FONT ActivityThread bind override: fontScale "
                        + fontScale.original + " -> " + config.fontScale);
        FontDebugStatsReporter.record(
                "font-emulation-bind",
                bindData.getClass().getSimpleName(),
                null);
        return true;
    }

    private static Method resolveHandleBindApplication(Class<?> activityThreadClass)
            throws ReflectiveOperationException {
        for (Method method : activityThreadClass.getDeclaredMethods()) {
            if (!"handleBindApplication".equals(method.getName())) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length != 1) {
                continue;
            }
            if (params[0].getName().endsWith("ActivityThread$AppBindData")) {
                return method;
            }
        }
        throw new NoSuchMethodException("handleBindApplication(ActivityThread$AppBindData) not found");
    }

    private static Configuration readConfig(Object bindData) {
        try {
            Field configField = bindData.getClass().getDeclaredField("config");
            configField.setAccessible(true);
            Object value = configField.get(bindData);
            return value instanceof Configuration ? (Configuration) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void logIfChanged(String key, String message) {
        String previous = LAST_MESSAGES.put(key, message);
        if (!message.equals(previous)) {
            DpisLog.i(message);
        }
    }

    private static String buildFontLogKey(String packageName, String suffix) {
        String pkg = packageName == null ? "unknown" : packageName;
        return pkg + ":" + FONT_LOG_KEY_PREFIX + ":" + suffix;
    }
}
