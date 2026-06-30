package com.dpis.module;

import android.webkit.WebSettings;
import android.webkit.WebView;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.api.XposedInterface;

final class WebViewFontHookInstaller {
    private static final String BRIDGE_LOG_PREFIX = "DPIS ";
    private static final String FONT_LOG_KEY_PREFIX = "font";
    private static final String HOOK_ID_WEBVIEW_GET_SETTINGS = "webview_get_settings";
    private static final String HOOK_ID_WEBSETTINGS_SET_TEXT_ZOOM =
            "websettings_set_text_zoom";
    private static final String HOOK_ID_X5_WEBVIEW_GET_SETTINGS =
            "x5_webview_get_settings";
    private static final String HOOK_ID_X5_WEBSETTINGS_SET_TEXT_ZOOM =
            "x5_websettings_set_text_zoom";
    private static volatile int installedPid = -1;
    private static final Map<String, String> LAST_MESSAGES = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> INTERNAL_UPDATE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    private WebViewFontHookInstaller() {
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
        synchronized (WebViewFontHookInstaller.class) {
            if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
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
            apiCapabilities.applyStableHookId(
                            xposed.hook(getSettingsMethod)
                                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                            HOOK_ID_WEBVIEW_GET_SETTINGS)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (!(result instanceof WebSettings settings)) {
                            return result;
                        }
                        if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                            return result;
                        }
                        String detail = "textZoom=" + targetZoom
                                + ", settings=" + settings.getClass().getName();
                        FeedbackDiagnosticRuntimeHotPathEvents.begin(
                                packageName,
                                "webview_text_zoom",
                                detail
                        );
                        INTERNAL_UPDATE.set(Boolean.TRUE);
                        try {
                            settings.setTextZoom(targetZoom);
                            FeedbackDiagnosticRuntimeHotPathEvents.applied(
                                    packageName,
                                    "webview_text_zoom",
                                    detail
                            );
                        } finally {
                            INTERNAL_UPDATE.set(Boolean.FALSE);
                            FeedbackDiagnosticRuntimeHotPathEvents.end(
                                    packageName,
                                    "webview_text_zoom",
                                    detail
                            );
                        }
                        logIfChanged(buildFontLogKey(packageName, "webview-getsettings"),
                                "DPIS_FONT WebView getSettings override: textZoom=" + targetZoom);
                        bridgeLog(xposed, "DPIS_FONT WebView getSettings override applied: package="
                                + packageName + ", hookId=" + HOOK_ID_WEBVIEW_GET_SETTINGS
                                + ", textZoom=" + targetZoom);
                        return result;
                    });

            installAndroidWebSettingsHook(
                    xposed, packageName, targetZoom, bootClassLoader, apiCapabilities);

            installX5Hooks(xposed, packageName, targetZoom, apiCapabilities);
            installedPid = ProcessScopedInstallGate.currentPid();
            DpisLog.i("WebView font hook ready: hookIds="
                    + HOOK_ID_WEBVIEW_GET_SETTINGS + "," + HOOK_ID_WEBSETTINGS_SET_TEXT_ZOOM);
            bridgeLog(xposed, "DPIS_FONT WebView font hook ready: package=" + packageName
                    + ", hookIds=" + HOOK_ID_WEBVIEW_GET_SETTINGS + ","
                    + HOOK_ID_WEBSETTINGS_SET_TEXT_ZOOM);
            FeedbackDiagnosticRuntimeEvents.recordHotReload(
                    packageName,
                    "font",
                    "installed",
                    "webview font hook ready: textZoom=" + targetZoom);
        }
    }

    private static void installAndroidWebSettingsHook(XposedInterface xposed,
                                                      String packageName,
                                                      int targetZoom,
                                                      ClassLoader bootClassLoader,
                                                      ModernApiCapabilities apiCapabilities) {
        try {
            Class<?> webSettingsClass =
                    Class.forName("android.webkit.WebSettings", false, bootClassLoader);
            Method setTextZoomMethod = webSettingsClass.getDeclaredMethod("setTextZoom", int.class);
            if (Modifier.isAbstract(setTextZoomMethod.getModifiers())) {
                logIfChanged(buildFontLogKey(packageName, "websettings-abstract"),
                        "DPIS_FONT skip abstract WebSettings#setTextZoom hook");
                return;
            }
            apiCapabilities.applyStableHookId(
                            xposed.hook(setTextZoomMethod)
                                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                            HOOK_ID_WEBSETTINGS_SET_TEXT_ZOOM)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                            return result;
                        }
                        Object thisObject = chain.getThisObject();
                        if (!(thisObject instanceof WebSettings settings)) {
                            return result;
                        }
                        int incomingZoom = (Integer) chain.getArg(0);
                        String detail = "in=" + incomingZoom
                                + ", out=" + targetZoom
                                + ", settings=" + settings.getClass().getName();
                        FeedbackDiagnosticRuntimeHotPathEvents.begin(
                                packageName,
                                "webview_text_zoom",
                                detail
                        );
                        INTERNAL_UPDATE.set(Boolean.TRUE);
                        try {
                            settings.setTextZoom(targetZoom);
                            FeedbackDiagnosticRuntimeHotPathEvents.applied(
                                    packageName,
                                    "webview_text_zoom",
                                    detail
                            );
                        } finally {
                            INTERNAL_UPDATE.set(Boolean.FALSE);
                            FeedbackDiagnosticRuntimeHotPathEvents.end(
                                    packageName,
                                    "webview_text_zoom",
                                    detail
                            );
                        }
                        if (incomingZoom != targetZoom) {
                            logIfChanged(buildFontLogKey(packageName, "websettings-settextzoom"),
                                    "DPIS_FONT WebSettings setTextZoom override: in="
                                            + incomingZoom + ", out=" + targetZoom);
                            bridgeLog(xposed,
                                    "DPIS_FONT WebSettings setTextZoom override applied: package="
                                            + packageName + ", hookId="
                                            + HOOK_ID_WEBSETTINGS_SET_TEXT_ZOOM
                                            + ", in=" + incomingZoom + ", out=" + targetZoom);
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
                                       int targetZoom,
                                       ModernApiCapabilities apiCapabilities) {
        ClassLoader appClassLoader = Thread.currentThread().getContextClassLoader();
        Class<?> x5WebViewClass = findClassOptional("com.tencent.smtt.sdk.WebView", appClassLoader);
        Class<?> x5WebSettingsClass = findClassOptional("com.tencent.smtt.sdk.WebSettings", appClassLoader);
        if (x5WebViewClass == null || x5WebSettingsClass == null) {
            return;
        }
        try {
            Method getSettingsMethod = x5WebViewClass.getDeclaredMethod("getSettings");
            apiCapabilities.applyStableHookId(
                            xposed.hook(getSettingsMethod)
                                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                            HOOK_ID_X5_WEBVIEW_GET_SETTINGS)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                            return result;
                        }
                        if (!x5WebSettingsClass.isInstance(result)) {
                            return result;
                        }
                        Method setTextZoom = x5WebSettingsClass.getMethod("setTextZoom", int.class);
                        String detail = "textZoom=" + targetZoom
                                + ", settings=" + result.getClass().getName();
                        FeedbackDiagnosticRuntimeHotPathEvents.begin(
                                packageName,
                                "x5_webview_text_zoom",
                                detail
                        );
                        INTERNAL_UPDATE.set(Boolean.TRUE);
                        try {
                            setTextZoom.invoke(result, targetZoom);
                            FeedbackDiagnosticRuntimeHotPathEvents.applied(
                                    packageName,
                                    "x5_webview_text_zoom",
                                    detail
                            );
                        } finally {
                            INTERNAL_UPDATE.set(Boolean.FALSE);
                            FeedbackDiagnosticRuntimeHotPathEvents.end(
                                    packageName,
                                    "x5_webview_text_zoom",
                                    detail
                            );
                        }
                        logIfChanged(buildFontLogKey(packageName, "x5-webview-getsettings"),
                                "DPIS_FONT X5 WebView getSettings override: textZoom=" + targetZoom);
                        bridgeLog(xposed, "DPIS_FONT X5 WebView getSettings override applied: package="
                                + packageName + ", hookId=" + HOOK_ID_X5_WEBVIEW_GET_SETTINGS
                                + ", textZoom=" + targetZoom);
                        return result;
                    });
        } catch (Throwable ignored) {
            return;
        }
        try {
            Method setTextZoomMethod = x5WebSettingsClass.getDeclaredMethod("setTextZoom", int.class);
            apiCapabilities.applyStableHookId(
                            xposed.hook(setTextZoomMethod)
                                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                            HOOK_ID_X5_WEBSETTINGS_SET_TEXT_ZOOM)
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
                        int incomingZoom = (Integer) chain.getArg(0);
                        String detail = "in=" + incomingZoom
                                + ", out=" + targetZoom
                                + ", settings=" + thisObject.getClass().getName();
                        FeedbackDiagnosticRuntimeHotPathEvents.begin(
                                packageName,
                                "x5_webview_text_zoom",
                                detail
                        );
                        INTERNAL_UPDATE.set(Boolean.TRUE);
                        try {
                            setTextZoom.invoke(thisObject, targetZoom);
                            FeedbackDiagnosticRuntimeHotPathEvents.applied(
                                    packageName,
                                    "x5_webview_text_zoom",
                                    detail
                            );
                        } finally {
                            INTERNAL_UPDATE.set(Boolean.FALSE);
                            FeedbackDiagnosticRuntimeHotPathEvents.end(
                                    packageName,
                                    "x5_webview_text_zoom",
                                    detail
                            );
                        }
                        if (incomingZoom != targetZoom) {
                            logIfChanged(buildFontLogKey(packageName, "x5-websettings-settextzoom"),
                                    "DPIS_FONT X5 WebSettings setTextZoom override: in="
                                            + incomingZoom + ", out=" + targetZoom);
                            bridgeLog(xposed,
                                    "DPIS_FONT X5 WebSettings setTextZoom override applied: package="
                                            + packageName + ", hookId="
                                            + HOOK_ID_X5_WEBSETTINGS_SET_TEXT_ZOOM
                                            + ", in=" + incomingZoom + ", out=" + targetZoom);
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

    private static String buildFontLogKey(String packageName, String suffix) {
        String pkg = packageName == null ? "unknown" : packageName;
        return pkg + ":" + FONT_LOG_KEY_PREFIX + ":" + suffix;
    }
}
