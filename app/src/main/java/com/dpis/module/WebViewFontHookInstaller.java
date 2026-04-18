package com.dpis.module;

import android.webkit.WebSettings;
import android.webkit.WebView;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.api.XposedInterface;

final class WebViewFontHookInstaller {
    private static final String FONT_LOG_KEY_PREFIX = "font";
    private static volatile boolean hookInstalled;
    private static final Map<String, String> LAST_MESSAGES = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> INTERNAL_UPDATE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    private WebViewFontHookInstaller() {
    }

    static void install(XposedInterface xposed, String packageName, DpiConfigStore store)
            throws ReflectiveOperationException {
        if (hookInstalled) {
            return;
        }
        synchronized (WebViewFontHookInstaller.class) {
            if (hookInstalled) {
                return;
            }
            FontScaleOverride.Result fontScale = FontScaleOverride.resolve(store, packageName, 1.0f);
            final Integer targetPercent = fontScale.targetPercent;
            if (!isTargetPercentActive(targetPercent)) {
                return;
            }
            final int targetZoom = clampTextZoom(targetPercent);

            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> webViewClass = Class.forName("android.webkit.WebView", false, bootClassLoader);
            Method getSettingsMethod = webViewClass.getDeclaredMethod("getSettings");
            xposed.hook(getSettingsMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (!(result instanceof WebSettings settings)) {
                            return result;
                        }
                        if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                            return result;
                        }
                        INTERNAL_UPDATE.set(Boolean.TRUE);
                        try {
                            settings.setTextZoom(targetZoom);
                        } finally {
                            INTERNAL_UPDATE.set(Boolean.FALSE);
                        }
                        logIfChanged(buildFontLogKey(packageName, "webview-getsettings"),
                                "DPIS_FONT WebView getSettings override: textZoom=" + targetZoom);
                        return result;
                    });

            installAndroidWebSettingsHook(xposed, packageName, targetZoom, bootClassLoader);

            installX5Hooks(xposed, packageName, targetZoom);
            hookInstalled = true;
            DpisLog.i("WebView font hook ready");
        }
    }

    private static void installAndroidWebSettingsHook(XposedInterface xposed,
                                                      String packageName,
                                                      int targetZoom,
                                                      ClassLoader bootClassLoader) {
        try {
            Class<?> webSettingsClass =
                    Class.forName("android.webkit.WebSettings", false, bootClassLoader);
            Method setTextZoomMethod = webSettingsClass.getDeclaredMethod("setTextZoom", int.class);
            if (Modifier.isAbstract(setTextZoomMethod.getModifiers())) {
                logIfChanged(buildFontLogKey(packageName, "websettings-abstract"),
                        "DPIS_FONT skip abstract WebSettings#setTextZoom hook");
                return;
            }
            xposed.hook(setTextZoomMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                            return result;
                        }
                        Object thisObject = chain.getThisObject();
                        if (!(thisObject instanceof WebSettings settings)) {
                            return result;
                        }
                        INTERNAL_UPDATE.set(Boolean.TRUE);
                        try {
                            settings.setTextZoom(targetZoom);
                        } finally {
                            INTERNAL_UPDATE.set(Boolean.FALSE);
                        }
                        int incomingZoom = (Integer) chain.getArg(0);
                        if (incomingZoom != targetZoom) {
                            logIfChanged(buildFontLogKey(packageName, "websettings-settextzoom"),
                                    "DPIS_FONT WebSettings setTextZoom override: in="
                                            + incomingZoom + ", out=" + targetZoom);
                        }
                        return result;
                    });
        } catch (Throwable t) {
            logIfChanged(buildFontLogKey(packageName, "websettings-hook-failed"),
                    "DPIS_FONT WebSettings#setTextZoom hook skipped: "
                            + t.getClass().getSimpleName());
        }
    }

    private static void installX5Hooks(XposedInterface xposed,
                                       String packageName,
                                       int targetZoom) {
        ClassLoader appClassLoader = Thread.currentThread().getContextClassLoader();
        Class<?> x5WebViewClass = findClassOptional("com.tencent.smtt.sdk.WebView", appClassLoader);
        Class<?> x5WebSettingsClass = findClassOptional("com.tencent.smtt.sdk.WebSettings", appClassLoader);
        if (x5WebViewClass == null || x5WebSettingsClass == null) {
            return;
        }
        try {
            Method getSettingsMethod = x5WebViewClass.getDeclaredMethod("getSettings");
            xposed.hook(getSettingsMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                            return result;
                        }
                        if (!x5WebSettingsClass.isInstance(result)) {
                            return result;
                        }
                        Method setTextZoom = x5WebSettingsClass.getMethod("setTextZoom", int.class);
                        INTERNAL_UPDATE.set(Boolean.TRUE);
                        try {
                            setTextZoom.invoke(result, targetZoom);
                        } finally {
                            INTERNAL_UPDATE.set(Boolean.FALSE);
                        }
                        logIfChanged(buildFontLogKey(packageName, "x5-webview-getsettings"),
                                "DPIS_FONT X5 WebView getSettings override: textZoom=" + targetZoom);
                        return result;
                    });
        } catch (Throwable ignored) {
            return;
        }
        try {
            Method setTextZoomMethod = x5WebSettingsClass.getDeclaredMethod("setTextZoom", int.class);
            xposed.hook(setTextZoomMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                            return result;
                        }
                        Object thisObject = chain.getThisObject();
                        if (!x5WebSettingsClass.isInstance(thisObject)) {
                            return result;
                        }
                        Method setTextZoom = x5WebSettingsClass.getMethod("setTextZoom", int.class);
                        INTERNAL_UPDATE.set(Boolean.TRUE);
                        try {
                            setTextZoom.invoke(thisObject, targetZoom);
                        } finally {
                            INTERNAL_UPDATE.set(Boolean.FALSE);
                        }
                        int incomingZoom = (Integer) chain.getArg(0);
                        if (incomingZoom != targetZoom) {
                            logIfChanged(buildFontLogKey(packageName, "x5-websettings-settextzoom"),
                                    "DPIS_FONT X5 WebSettings setTextZoom override: in="
                                            + incomingZoom + ", out=" + targetZoom);
                        }
                        return result;
                    });
        } catch (Throwable ignored) {
            return;
        }
        logIfChanged(buildFontLogKey(packageName, "x5-ready"), "DPIS_FONT X5 font hook ready");
    }

    private static boolean isTargetPercentActive(Integer targetPercent) {
        return targetPercent != null && targetPercent > 0 && targetPercent != 100;
    }

    private static int clampTextZoom(int targetPercent) {
        return Math.max(50, Math.min(500, targetPercent));
    }

    private static Class<?> findClassOptional(String className, ClassLoader classLoader) {
        if (classLoader != null) {
            try {
                return Class.forName(className, false, classLoader);
            } catch (Throwable ignored) {
            }
        }
        try {
            return Class.forName(className);
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
