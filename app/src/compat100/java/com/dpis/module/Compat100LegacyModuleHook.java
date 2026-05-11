package com.dpis.module;

import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

@SuppressWarnings("unused")
public final class Compat100LegacyModuleHook implements IXposedHookLoadPackage {
    private static final AtomicBoolean RESOURCES_IMPL_HOOKED = new AtomicBoolean(false);
    private static final AtomicBoolean RESOURCES_MANAGER_HOOKED = new AtomicBoolean(false);

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        String packageName = lpparam.packageName;
        compatLog("compat100 legacy handleLoadPackage: package=" + packageName
                + ", process=" + lpparam.processName);
        DpiConfigStore store = ConfigStoreFactory.createForCompat100Host(packageName);
        ModulePackagePlan plan = ModulePackagePlan.resolve(store, packageName);
        if (!plan.shouldInstallCompat100LegacyHooks()) {
            compatLog("compat100 legacy package skipped: package=" + packageName
                    + ", configuredPackages=" + store.getConfiguredPackages());
            return;
        }
        compatLog("compat100 legacy package matched: package=" + packageName
                + ", targetViewportWidthDp=" + plan.targetViewportWidthDp
                + ", targetFontScalePercent=" + plan.targetFontScalePercent);
        installResourcesImplHook(packageName, store);
        installResourcesManagerHook(packageName, store);
    }

    private static void installResourcesImplHook(String packageName, DpiConfigStore store) {
        if (!RESOURCES_IMPL_HOOKED.compareAndSet(false, true)) {
            return;
        }
        try {
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> resourcesImplClass = Class.forName(
                    "android.content.res.ResourcesImpl", false, bootClassLoader);
            Class<?> compatibilityInfoClass = Class.forName(
                    "android.content.res.CompatibilityInfo", false, bootClassLoader);
            Method method = resourcesImplClass.getDeclaredMethod(
                    "updateConfiguration",
                    Configuration.class,
                    DisplayMetrics.class,
                    compatibilityInfoClass);
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    ResourcesImplHookInstaller.applyDensityOverride(
                            packageName,
                            (Configuration) param.args[0],
                            (DisplayMetrics) param.args[1],
                            store);
                }
            });
            compatLog("compat100 legacy ResourcesImpl hook ready");
        } catch (Throwable throwable) {
            RESOURCES_IMPL_HOOKED.set(false);
            compatLog("compat100 legacy ResourcesImpl hook failed: "
                    + throwable.getClass().getName() + ": " + throwable.getMessage());
        }
    }

    private static void installResourcesManagerHook(String packageName, DpiConfigStore store) {
        if (!RESOURCES_MANAGER_HOOKED.compareAndSet(false, true)) {
            return;
        }
        try {
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> resourcesManagerClass = Class.forName(
                    "android.app.ResourcesManager", false, bootClassLoader);
            Class<?> compatibilityInfoClass = Class.forName(
                    "android.content.res.CompatibilityInfo", false, bootClassLoader);
            Method method = resourcesManagerClass.getDeclaredMethod(
                    "applyConfigurationToResources",
                    Configuration.class,
                    compatibilityInfoClass);
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    applyResourceOverrides(
                            (Configuration) param.args[0],
                            store,
                            packageName,
                            "Compat100LegacyResourcesManager");
                }
            });
            int activityHookCount = installUpdateResourcesForActivityHook(
                    resourcesManagerClass, packageName, store);
            int createHookCount = installResourceCreationHooks(
                    resourcesManagerClass, packageName, store);
            compatLog("compat100 legacy ResourcesManager hook ready"
                    + " (activityHooks=" + activityHookCount
                    + ", createHooks=" + createHookCount + ")");
        } catch (Throwable throwable) {
            RESOURCES_MANAGER_HOOKED.set(false);
            compatLog("compat100 legacy ResourcesManager hook failed: "
                    + throwable.getClass().getName() + ": " + throwable.getMessage());
        }
    }

    private static int installUpdateResourcesForActivityHook(Class<?> resourcesManagerClass,
                                                             String packageName,
                                                             DpiConfigStore store) {
        try {
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> iBinderClass = Class.forName("android.os.IBinder", false, bootClassLoader);
            Method method = resourcesManagerClass.getDeclaredMethod(
                    "updateResourcesForActivity",
                    iBinderClass,
                    Configuration.class,
                    int.class);
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    applyResourceOverrides(
                            (Configuration) param.args[1],
                            store,
                            packageName,
                            "Compat100LegacyResourcesManagerActivity");
                }
            });
            return 1;
        } catch (Throwable throwable) {
            compatLog("compat100 legacy ResourcesManager activity hook failed: "
                    + throwable.getClass().getName() + ": " + throwable.getMessage());
            return 0;
        }
    }

    private static int installResourceCreationHooks(Class<?> resourcesManagerClass,
                                                    String packageName,
                                                    DpiConfigStore store) {
        int hookedCount = 0;
        Set<Method> hookedMethods = new HashSet<>();
        for (Method method : resourcesManagerClass.getDeclaredMethods()) {
            int configArgIndex = findConfigurationArgIndex(method);
            if (configArgIndex < 0 || !isResourceCreationMethod(method.getName())) {
                continue;
            }
            if (!hookedMethods.add(method)) {
                continue;
            }
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    applyResourceOverrides(
                            (Configuration) param.args[configArgIndex],
                            store,
                            packageName,
                            "Compat100LegacyResourcesManagerCreate(" + method.getName() + ")");
                }
            });
            hookedCount++;
        }
        return hookedCount;
    }

    private static int findConfigurationArgIndex(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (Configuration.class.equals(parameterTypes[i])) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isResourceCreationMethod(String methodName) {
        return methodName != null
                && (methodName.contains("createResources")
                || methodName.contains("getOrCreateResources")
                || methodName.contains("createBaseTokenResources"));
    }

    private static void applyResourceOverrides(Configuration config,
                                               DpiConfigStore store,
                                               String packageName,
                                               String sourceTag) {
        try {
            Method method = ResourcesManagerHookInstaller.class.getDeclaredMethod(
                    "applyResourceOverrides",
                    Configuration.class,
                    DpiConfigStore.class,
                    String.class,
                    String.class);
            method.setAccessible(true);
            method.invoke(null, config, store, packageName, sourceTag);
            if (config != null && store.getConfiguredPackages().contains(packageName)) {
                compatLog("compat100 legacy resource override applied: package="
                        + packageName + ", source=" + sourceTag
                        + ", widthDp=" + config.screenWidthDp
                        + ", densityDpi=" + config.densityDpi
                        + ", fontScale=" + config.fontScale);
            }
        } catch (Throwable throwable) {
            compatLog("compat100 legacy resource override failed: "
                    + throwable.getClass().getName() + ": " + throwable.getMessage());
        }
    }

    private static void compatLog(String message) {
        try {
            XposedBridge.log("DPIS " + message);
        } catch (Throwable ignored) {
        }
        try {
            Log.i("DPIS", message);
        } catch (Throwable ignored) {
        }
        DpisLog.i(message);
    }
}
