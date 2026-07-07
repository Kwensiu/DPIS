package com.dpis.module;

import com.dpis.module.diagnostics.FeedbackDiagnosticRuntimeHotPathEvents;

import com.dpis.module.viewport.DpiConfig;

import com.dpis.module.quirks.WechatDpiMethodLocator;
import com.dpis.module.quirks.WechatDpiRuntime;

import com.dpis.module.appconfig.WechatDpiConfig;
import com.dpis.module.runtime.WechatDpiPropertyBridge;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.DisplayMetrics;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

final class WechatDpiLegacyHookInstaller {
    private static final AtomicBoolean HOOKED = new AtomicBoolean(false);
    private static final AtomicBoolean WECHAT_DPI_CALLBACK_LOGGED = new AtomicBoolean(false);
    private static final AtomicBoolean WECHAT_DPI_MUTATION_LOGGED = new AtomicBoolean(false);

    private WechatDpiLegacyHookInstaller() {
    }

    static void install(XC_LoadPackage.LoadPackageParam lpparam) {
        ClassLoader classLoader = lpparam != null ? lpparam.classLoader : null;
        if (classLoader == null || !HOOKED.compareAndSet(false, true)) {
            return;
        }
        if (!installRoute(classLoader, lpparam)) {
            HOOKED.set(false);
        }
    }

    private static boolean installRoute(
            ClassLoader classLoader,
            XC_LoadPackage.LoadPackageParam lpparam) {
        long versionCode = resolveWechatVersionCode(lpparam);
        WechatDpiMethodLocator.Result result = WechatDpiMethodLocator.locate(
                classLoader, lpparam != null ? lpparam.appInfo : null, versionCode);
        if (result.methods.isEmpty()) {
            DpisLog.i("legacy WeChat DPI hook skipped: locator="
                    + result.source.logName + ", versionCode=" + versionCode
                    + ", reason=" + result.failure);
            FeedbackDiagnosticRuntimeHotPathEvents.event(
                    WechatDpiConfig.PACKAGE_NAME,
                    "wechat_dpi",
                    "displaymetrics",
                    "skipped",
                    "locator=" + result.source.logName + ", versionCode=" + versionCode
                            + ", reason=" + result.failure);
            return false;
        }
        return installWechatDpiHook(result, versionCode);
    }

    private static boolean installWechatDpiHook(WechatDpiMethodLocator.Result locatorResult,
            long versionCode) {
        List<Method> metricsMethods = locatorResult.methods;
        if (metricsMethods.isEmpty()) {
            return false;
        }
        int installed = 0;
        try {
            for (Method metricsMethod : metricsMethods) {
                metricsMethod.setAccessible(true);
                XposedBridge.hookMethod(metricsMethod, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (WECHAT_DPI_CALLBACK_LOGGED.compareAndSet(false, true)) {
                            FeedbackDiagnosticRuntimeHotPathEvents.event(
                                    WechatDpiConfig.PACKAGE_NAME,
                                    "wechat_dpi",
                                    "displaymetrics",
                                    "route_callback_entered",
                                    "method=" + methodName(metricsMethod)
                                            + ", configuredDpi="
                                            + WechatDpiPropertyBridge.readDpi(
                                                    WechatDpiConfig.PACKAGE_NAME));
                        }
                        if (param.getResult() instanceof DisplayMetrics metrics) {
                            applyWechatDpi(metrics,
                                    methodName(metricsMethod));
                        }
                    }
                });
                installed++;
            }
            DpisLog.i("legacy WeChat DPI hook ready: "
                    + methodNames(metricsMethods)
                    + ", installed=" + installed + ", locator="
                    + locatorResult.source.logName + ", versionCode=" + versionCode);
            FeedbackDiagnosticRuntimeHotPathEvents.event(
                    WechatDpiConfig.PACKAGE_NAME,
                    "wechat_dpi",
                    "displaymetrics",
                    "hook_ready",
                    "installed=" + installed + ", locator=" + locatorResult.source.logName
                            + ", versionCode=" + versionCode);
            return installed > 0;
        } catch (Throwable throwable) {
            DpisLog.e("legacy WeChat DPI hook failed: "
                    + methodNames(metricsMethods)
                    + ", versionCode=" + versionCode + ", "
                    + throwable.getClass().getName() + ": " + throwable.getMessage(), throwable);
            FeedbackDiagnosticRuntimeHotPathEvents.event(
                    WechatDpiConfig.PACKAGE_NAME,
                    "wechat_dpi",
                    "displaymetrics",
                    "skipped",
                    "hookFailed=true, versionCode=" + versionCode
                            + ", error=" + throwable.getClass().getSimpleName());
        }
        return false;
    }

    private static String methodNames(List<Method> methods) {
        StringBuilder builder = new StringBuilder();
        for (Method method : methods) {
            if (builder.length() > 0) {
                builder.append('|');
            }
            builder.append(methodName(method));
        }
        return builder.toString();
    }

    private static String methodName(Method method) {
        return method.getDeclaringClass().getName() + "#" + method.getName();
    }

    private static void applyWechatDpi(DisplayMetrics metrics, String methodName) {
        int dpi = WechatDpiPropertyBridge.readDpi(
                WechatDpiConfig.PACKAGE_NAME);
        if (dpi <= 0 || metrics == null) {
            return;
        }
        float oldDensity = metrics.density;
        int oldDensityDpi = metrics.densityDpi;
        float oldScaledDensity = metrics.scaledDensity;
        if (WechatDpiRuntime.apply(metrics, dpi)
                && WECHAT_DPI_MUTATION_LOGGED.compareAndSet(false, true)) {
            DpisLog.i("legacy WeChat DPI applied: method=" + methodName
                    + ", targetDpi=" + dpi
                    + ", densityDpi " + oldDensityDpi + " -> " + metrics.densityDpi
                    + ", density " + oldDensity + " -> " + metrics.density
                    + ", scaledDensity " + oldScaledDensity + " -> "
                    + metrics.scaledDensity);
            FeedbackDiagnosticRuntimeHotPathEvents.event(
                    WechatDpiConfig.PACKAGE_NAME,
                    "wechat_dpi",
                    "displaymetrics",
                    "mutation_applied",
                    "method=" + methodName + ", targetDpi=" + dpi
                            + ", densityDpi=" + oldDensityDpi + "->" + metrics.densityDpi
                            + ", density=" + oldDensity + "->" + metrics.density
                            + ", scaledDensity=" + oldScaledDensity + "->"
                            + metrics.scaledDensity);
        }
    }

    private static long resolveWechatVersionCode(XC_LoadPackage.LoadPackageParam lpparam) {
        long versionCode = resolveWechatVersionCode(lpparam != null ? lpparam.appInfo : null);
        if (versionCode > 0L) {
            return versionCode;
        }
        versionCode = resolveWechatVersionCode(AndroidAppHelper.currentApplication());
        if (versionCode > 0L) {
            return versionCode;
        }
        return resolveWechatVersionCode(resolveSystemContext());
    }

    private static long resolveWechatVersionCode(Object appInfo) {
        try {
            if (appInfo == null) {
                return 0L;
            }
            Object value = appInfo.getClass().getField("longVersionCode").get(appInfo);
            if (value instanceof Long versionCode) {
                return versionCode;
            }
        } catch (Throwable ignored) {
        }
        return 0L;
    }

    private static long resolveWechatVersionCode(Context context) {
        try {
            if (context == null) {
                return 0L;
            }
            PackageManager packageManager = context.getPackageManager();
            if (packageManager == null) {
                return 0L;
            }
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    WechatDpiConfig.PACKAGE_NAME, 0);
            return resolvePackageVersionCode(packageInfo);
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    private static long resolvePackageVersionCode(PackageInfo packageInfo) {
        if (packageInfo == null) {
            return 0L;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return packageInfo.getLongVersionCode();
        }
        //noinspection deprecation
        return packageInfo.versionCode;
    }

    private static Context resolveSystemContext() {
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Method currentActivityThread = activityThread.getDeclaredMethod("currentActivityThread");
            Object thread = currentActivityThread.invoke(null);
            if (thread == null) {
                return null;
            }
            Method getSystemContext = activityThread.getDeclaredMethod("getSystemContext");
            Object context = getSystemContext.invoke(thread);
            return context instanceof Context ? (Context) context : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

}
