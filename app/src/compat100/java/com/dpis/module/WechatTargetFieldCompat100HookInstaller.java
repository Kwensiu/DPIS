package com.dpis.module;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

final class WechatTargetFieldCompat100HookInstaller {
    private static final AtomicBoolean HOOKED = new AtomicBoolean(false);

    private WechatTargetFieldCompat100HookInstaller() {
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
        WechatTargetFieldRoutes.Route route = WechatTargetFieldRoutes.forVersionCode(versionCode);
        if (route == null) {
            DpisLog.i("compat100 WeChat target-field hook unsupported: versionCode="
                    + versionCode);
            return false;
        }
        switch (route.kind) {
            case GETTER:
                return installGetterHook(classLoader, route, versionCode);
            case CONSTRUCTOR_FIELD:
                return installConstructorFieldHook(classLoader, route, versionCode);
            default:
                DpisLog.i("compat100 WeChat target-field hook unsupported kind: "
                        + route.kind + ", versionCode=" + versionCode);
                return false;
        }
    }

    private static boolean installGetterHook(ClassLoader classLoader,
            WechatTargetFieldRoutes.Route route, long versionCode) {
        try {
            Class<?> densityManagerClass = Class.forName(route.className, false, classLoader);
            Method targetGetter = densityManagerClass.getDeclaredMethod(route.memberName);
            targetGetter.setAccessible(true);
            XposedBridge.hookMethod(targetGetter, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    int target = resolveWechatTargetField();
                    if (target > 0) {
                        param.setResult(target);
                    }
                }
            });
            DpisLog.i("compat100 WeChat target-field getter hook ready: "
                    + route.routeKey() + ", versionCode=" + versionCode);
            installSetterHook(densityManagerClass, route, versionCode);
            return true;
        } catch (Throwable throwable) {
            DpisLog.e("compat100 WeChat target-field getter hook failed: "
                    + route.routeKey() + ", versionCode=" + versionCode + ", "
                    + throwable.getClass().getName() + ": " + throwable.getMessage(), throwable);
        }
        return false;
    }

    private static void installSetterHook(Class<?> densityManagerClass,
            WechatTargetFieldRoutes.Route route, long versionCode) {
        if (route.setterName == null || route.setterName.isBlank()) {
            DpisLog.i("compat100 WeChat target-field setter hook skipped: "
                    + "no setter route for " + route.routeKey()
                    + ", versionCode=" + versionCode);
            return;
        }
        try {
            Method setter = densityManagerClass.getDeclaredMethod(route.setterName, int.class);
            if (!java.lang.reflect.Modifier.isStatic(setter.getModifiers())
                    || setter.getReturnType() != void.class) {
                DpisLog.i("compat100 WeChat target-field setter hook skipped: "
                        + "route is not static void(int): " + route.setterRouteKey()
                        + ", versionCode=" + versionCode);
                return;
            }
            setter.setAccessible(true);
            XposedBridge.hookMethod(setter, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    int target = resolveWechatTargetField();
                    if (target > 0 && param.args != null && param.args.length > 0) {
                        param.args[0] = target;
                    }
                }
            });
            DpisLog.i("compat100 WeChat target-field setter hook ready: "
                    + route.setterRouteKey() + ", versionCode=" + versionCode);
        } catch (Throwable throwable) {
            DpisLog.e("compat100 WeChat target-field setter hook failed: "
                    + route.setterRouteKey() + ", versionCode=" + versionCode + ", "
                    + throwable.getClass().getName() + ": " + throwable.getMessage(), throwable);
        }
    }

    private static boolean installConstructorFieldHook(ClassLoader classLoader,
            WechatTargetFieldRoutes.Route route, long versionCode) {
        try {
            Class<?> densityManagerClass = Class.forName(route.className, false, classLoader);
            Field targetField = densityManagerClass.getDeclaredField(route.memberName);
            targetField.setAccessible(true);
            XposedBridge.hookAllConstructors(densityManagerClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    int target = resolveWechatTargetField();
                    if (target <= 0 || param.thisObject == null) {
                        return;
                    }
                    targetField.setInt(param.thisObject, target);
                }
            });
            DpisLog.i("compat100 WeChat target-field constructor-field hook ready: "
                    + route.routeKey() + ", versionCode=" + versionCode);
            return true;
        } catch (Throwable throwable) {
            DpisLog.e("compat100 WeChat target-field constructor-field hook failed: "
                    + route.routeKey() + ", versionCode=" + versionCode + ", "
                    + throwable.getClass().getName() + ": " + throwable.getMessage(), throwable);
        }
        return false;
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
                    WechatTargetFieldConfig.PACKAGE_NAME, 0);
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

    private static int resolveWechatTargetField() {
        return WechatTargetFieldPropertyBridge.readTargetField(
                WechatTargetFieldConfig.PACKAGE_NAME);
    }
}
