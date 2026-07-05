package com.dpis.module;

import android.content.res.Configuration;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dpis.module.runtime.ProcessScopedInstallGate;

import io.github.libxposed.api.XposedInterface;

final class ActivityThreadFontHookInstaller {
    private static final String BRIDGE_LOG_PREFIX = "DPIS ";
    private static final String FONT_LOG_KEY_PREFIX = "font";
    private static final String HOOK_ID_HANDLE_BIND_APPLICATION =
            "activity_thread_handle_bind_application";
    private static volatile int installedPid = -1;
    private static final Map<String, String> LAST_MESSAGES = new ConcurrentHashMap<>();

    private ActivityThreadFontHookInstaller() {
    }

    static void resetForHotReload() {
        installedPid = -1;
    }

    static void install(XposedInterface xposed,
                        String packageName,
                        DpisConfigStore store,
                        ModernApiCapabilities apiCapabilities)
            throws ReflectiveOperationException {
        if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
            return;
        }
        synchronized (ActivityThreadFontHookInstaller.class) {
            if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
                return;
            }
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> activityThreadClass =
                    Class.forName("android.app.ActivityThread", false, bootClassLoader);
            Method handleBindApplication = resolveHandleBindApplication(activityThreadClass);
            // This is a single stable process-entry hook, so it is a good 102
            // replace target and easy to verify from runtime logs.
            apiCapabilities.applyStableHookId(
                            xposed.hook(handleBindApplication)
                                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                            HOOK_ID_HANDLE_BIND_APPLICATION)
                    .intercept(chain -> {
                        Object bindData = chain.getArg(0);
                        boolean changed = applyFontScaleToBindData(bindData, packageName, store);
                        if (changed) {
                            bridgeLog(xposed, "DPIS_FONT ActivityThread bind override applied: package="
                                    + packageName + ", hookId=" + HOOK_ID_HANDLE_BIND_APPLICATION);
                        }
                        return chain.proceed();
                    });
            installedPid = ProcessScopedInstallGate.currentPid();
            DpisLog.i("ActivityThread font hook ready: hookId="
                    + HOOK_ID_HANDLE_BIND_APPLICATION);
            bridgeLog(xposed, "DPIS_FONT ActivityThread font hook ready: package="
                    + packageName + ", hookId=" + HOOK_ID_HANDLE_BIND_APPLICATION);
        }
    }

    static boolean applyFontScaleToBindData(Object bindData, String packageName, DpisConfigStore store) {
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

    private static void bridgeLog(XposedInterface xposed, String message) {
        if (xposed == null || (!BuildConfig.DEBUG && !DpisLog.isLoggingEnabled())) {
            return;
        }
        try {
            xposed.log(android.util.Log.INFO, DpisLog.TAG, BRIDGE_LOG_PREFIX + message);
        } catch (Throwable ignored) {
            // Bridge evidence must not affect target app behavior.
        }
    }
}
