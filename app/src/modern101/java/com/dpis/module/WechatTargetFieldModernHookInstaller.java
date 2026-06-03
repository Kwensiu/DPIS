package com.dpis.module;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.libxposed.api.XposedInterface;

final class WechatTargetFieldModernHookInstaller {
    private static final AtomicBoolean HOOKED = new AtomicBoolean(false);

    private WechatTargetFieldModernHookInstaller() {
    }

    static void install(XposedInterface xposed, ClassLoader classLoader,
            ApplicationInfo applicationInfo, String packageName) {
        if (!WechatTargetFieldConfig.appliesTo(packageName)
                || xposed == null
                || classLoader == null
                || !HOOKED.compareAndSet(false, true)) {
            return;
        }
        if (!installRoute(xposed, classLoader, applicationInfo, packageName)) {
            HOOKED.set(false);
        }
    }

    private static boolean installRoute(XposedInterface xposed, ClassLoader classLoader,
            ApplicationInfo applicationInfo, String packageName) {
        long versionCode = resolveWechatVersionCode(applicationInfo, packageName);
        WechatTargetFieldRoutes.Route route = WechatTargetFieldRoutes.forVersionCode(versionCode);
        if (route == null) {
            DpisLog.i("modern101 WeChat target-field hook unsupported: versionCode="
                    + versionCode);
            return false;
        }
        switch (route.kind) {
            case GETTER:
                return installGetterHook(xposed, classLoader, route, versionCode);
            case CONSTRUCTOR_FIELD:
                return installConstructorFieldHook(xposed, classLoader, route, versionCode);
            default:
                DpisLog.i("modern101 WeChat target-field hook unsupported kind: "
                        + route.kind + ", versionCode=" + versionCode);
                return false;
        }
    }

    private static boolean installGetterHook(XposedInterface xposed, ClassLoader classLoader,
            WechatTargetFieldRoutes.Route route, long versionCode) {
        try {
            Class<?> densityManagerClass = Class.forName(route.className, false, classLoader);
            Method targetGetter = densityManagerClass.getDeclaredMethod(route.memberName);
            targetGetter.setAccessible(true);
            xposed.hook(targetGetter)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        int target = resolveWechatTargetField();
                        return target > 0 ? target : chain.proceed();
                    });
            DpisLog.i("modern101 WeChat target-field getter hook ready: "
                    + route.routeKey() + ", versionCode=" + versionCode);
            return true;
        } catch (Throwable throwable) {
            DpisLog.e("modern101 WeChat target-field getter hook failed: "
                    + route.routeKey() + ", versionCode=" + versionCode + ", "
                    + throwable.getClass().getName() + ": " + throwable.getMessage(), throwable);
        }
        return false;
    }

    private static boolean installConstructorFieldHook(XposedInterface xposed,
            ClassLoader classLoader, WechatTargetFieldRoutes.Route route, long versionCode) {
        try {
            Class<?> densityManagerClass = Class.forName(route.className, false, classLoader);
            Field targetField = densityManagerClass.getDeclaredField(route.memberName);
            targetField.setAccessible(true);
            for (Constructor<?> constructor : densityManagerClass.getDeclaredConstructors()) {
                constructor.setAccessible(true);
                xposed.hook(constructor)
                        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                        .intercept(chain -> {
                            Object result = chain.proceed();
                            Object instance = chain.getThisObject();
                            int target = resolveWechatTargetField();
                            if (target > 0 && instance != null) {
                                targetField.setInt(instance, target);
                            }
                            return result;
                        });
            }
            DpisLog.i("modern101 WeChat target-field constructor-field hook ready: "
                    + route.routeKey() + ", versionCode=" + versionCode);
            return true;
        } catch (Throwable throwable) {
            DpisLog.e("modern101 WeChat target-field constructor-field hook failed: "
                    + route.routeKey() + ", versionCode=" + versionCode + ", "
                    + throwable.getClass().getName() + ": " + throwable.getMessage(), throwable);
        }
        return false;
    }

    private static long resolveWechatVersionCode(ApplicationInfo applicationInfo,
            String packageName) {
        long versionCode = resolveWechatVersionCode((Object) applicationInfo);
        if (versionCode > 0L) {
            return versionCode;
        }
        versionCode = resolveWechatVersionCode(resolveApplicationContext(), packageName);
        if (versionCode > 0L) {
            return versionCode;
        }
        return resolveWechatVersionCode(resolveSystemContext(), packageName);
    }

    private static long resolveWechatVersionCode(Object applicationInfo) {
        try {
            if (applicationInfo == null) {
                return 0L;
            }
            Object value = applicationInfo.getClass().getField("longVersionCode")
                    .get(applicationInfo);
            if (value instanceof Long versionCode) {
                return versionCode;
            }
        } catch (Throwable ignored) {
        }
        return 0L;
    }

    private static long resolveWechatVersionCode(Context context, String packageName) {
        try {
            if (context == null || packageName == null || packageName.isBlank()) {
                return 0L;
            }
            PackageManager packageManager = context.getPackageManager();
            if (packageManager == null) {
                return 0L;
            }
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            return packageInfo != null ? packageInfo.getLongVersionCode() : 0L;
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    private static Context resolveApplicationContext() {
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Method currentApplication = activityThread.getDeclaredMethod("currentApplication");
            Object context = currentApplication.invoke(null);
            return context instanceof Context ? (Context) context : null;
        } catch (Throwable ignored) {
            return null;
        }
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

    private static int resolveWechatTargetField() {
        int value = readSystemPropertyInt(WechatTargetFieldPropertyBridge.propertyNameForPackage(
                WechatTargetFieldConfig.PACKAGE_NAME));
        return WechatTargetFieldConfig.normalize(value) != null ? value : 0;
    }

    private static int readSystemPropertyInt(String propertyName) {
        if (propertyName == null || propertyName.isBlank()) {
            return 0;
        }
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Method get = systemProperties.getDeclaredMethod("get", String.class, String.class);
            Object raw = get.invoke(null, propertyName, "0");
            if (raw instanceof String value) {
                return Integer.parseInt(value.trim());
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }
}
