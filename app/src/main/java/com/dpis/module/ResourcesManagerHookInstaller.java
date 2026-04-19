package com.dpis.module;

import android.annotation.SuppressLint;
import android.content.res.Configuration;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.api.XposedInterface;

final class ResourcesManagerHookInstaller {
    private static volatile boolean hookInstalled;
    private static final Map<String, String> LAST_MESSAGES = new ConcurrentHashMap<>();

    private ResourcesManagerHookInstaller() {
    }

    static void install(XposedInterface xposed, String packageName, DpiConfigStore store)
            throws ReflectiveOperationException {
        if (hookInstalled) {
            return;
        }
        synchronized (ResourcesManagerHookInstaller.class) {
            if (hookInstalled) {
                return;
            }
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> resourcesManagerClass = Class.forName(
                    "android.app.ResourcesManager", false, bootClassLoader);
            Class<?> compatibilityInfoClass = Class.forName(
                    "android.content.res.CompatibilityInfo", false, bootClassLoader);
            Method applyConfigurationMethod = resourcesManagerClass.getDeclaredMethod(
                    "applyConfigurationToResources", Configuration.class, compatibilityInfoClass);
            xposed.hook(applyConfigurationMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Configuration config = (Configuration) chain.getArg(0);
                        applyResourceOverrides(config, store, packageName, "ResourcesManager");
                        return chain.proceed();
                    });

            Method updateResourcesForActivityMethod = resolveUpdateResourcesForActivityMethod(
                    resourcesManagerClass, bootClassLoader);
            xposed.hook(updateResourcesForActivityMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Configuration overrideConfig = (Configuration) chain.getArg(1);
                        applyResourceOverrides(overrideConfig, store, packageName,
                                "ResourcesManagerActivity");
                        return chain.proceed();
                    });

            int createHookCount = installResourceCreationHooks(
                    xposed, resourcesManagerClass, packageName, store);
            hookInstalled = true;
            DpisLog.i("ResourcesManager hook ready (createHooks=" + createHookCount + ")");
        }
    }

    @SuppressLint("BlockedPrivateApi")
    private static Method resolveUpdateResourcesForActivityMethod(
            Class<?> resourcesManagerClass,
            ClassLoader bootClassLoader) throws ReflectiveOperationException {
        // Xposed module runtime depends on this hidden framework method to keep
        // activity-scoped resource overrides aligned with viewport spoofing.
        Class<?> iBinderClass = Class.forName("android.os.IBinder", false, bootClassLoader);
        return resourcesManagerClass.getDeclaredMethod(
                "updateResourcesForActivity", iBinderClass, Configuration.class, int.class);
    }

    private static int installResourceCreationHooks(XposedInterface xposed,
                                                    Class<?> resourcesManagerClass,
                                                    String packageName,
                                                    DpiConfigStore store) {
        int hookedCount = 0;
        Set<Method> hookedMethods = new HashSet<>();
        for (Method method : resourcesManagerClass.getDeclaredMethods()) {
            int configArgIndex = findConfigurationArgIndex(method);
            if (configArgIndex < 0) {
                continue;
            }
            String methodName = method.getName();
            if (!isResourceCreationMethod(methodName)) {
                continue;
            }
            if (!hookedMethods.add(method)) {
                continue;
            }
            xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Configuration config = (Configuration) chain.getArg(configArgIndex);
                        applyResourceOverrides(config, store, packageName,
                                "ResourcesManagerCreate(" + methodName + ")");
                        return chain.proceed();
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

    private static void applyResourceOverrides(Configuration config, DpiConfigStore store,
                                               String packageName, String sourceTag) {
        if (config == null) {
            return;
        }
        FontScaleOverride.Result fontScale = FontScaleOverride.resolve(
                store, packageName, config.fontScale);
        FontScaleOverride.applyToConfiguration(config, fontScale);
        int originalWidthDp = config.screenWidthDp;
        int originalHeightDp = config.screenHeightDp;
        int originalSmallestWidthDp = config.smallestScreenWidthDp;
        int originalDensityDpi = config.densityDpi;
        if (originalWidthDp <= 0 && originalHeightDp <= 0 && originalDensityDpi <= 0) {
            if (fontScale.changed) {
                String fontMessage = "DPIS_FONT " + sourceTag + " override: fontScale "
                        + fontScale.original + " -> " + config.fontScale;
                logIfChanged(packageName + ":" + sourceTag + ":font-only", fontMessage);
            }
            return;
        }
        Integer targetViewportWidth = TargetViewportWidthResolver.resolve(store, packageName);
        ViewportOverride.Result result = ViewportOverride.derive(
                config, targetViewportWidth != null ? targetViewportWidth : 0);
        if (result == null) {
            if (fontScale.changed) {
                String fontMessage = "DPIS_FONT " + sourceTag + " override: fontScale "
                        + fontScale.original + " -> " + config.fontScale;
                logIfChanged(packageName + ":" + sourceTag + ":font-only", fontMessage);
            }
            return;
        }
        VirtualDisplayOverride.Result sharedResult = VirtualDisplayOverride.derive(
                originalWidthDp > 0 ? originalWidthDp : result.widthDp,
                originalHeightDp > 0 ? originalHeightDp : result.heightDp,
                originalDensityDpi > 0 ? originalDensityDpi : result.densityDpi,
                originalWidthDp > 0 && originalDensityDpi > 0
                        ? Math.round(originalWidthDp * (originalDensityDpi / 160.0f))
                        : result.widthDp,
                originalHeightDp > 0 && originalDensityDpi > 0
                        ? Math.round(originalHeightDp * (originalDensityDpi / 160.0f))
                        : result.heightDp,
                result.widthDp);
        if (sharedResult != null) {
            VirtualDisplayState.set(sharedResult);
        }
        boolean applyToConfiguration = ViewportModePolicy.shouldApplyConfigurationOverride(
                store, packageName);
        if (result.widthDp == originalWidthDp
                && result.heightDp == originalHeightDp
                && result.smallestWidthDp == originalSmallestWidthDp
                && result.densityDpi == originalDensityDpi
                && !fontScale.changed) {
            return;
        }
        if (applyToConfiguration
                && (result.widthDp != originalWidthDp
                || result.heightDp != originalHeightDp
                || result.smallestWidthDp != originalSmallestWidthDp
                || result.densityDpi != originalDensityDpi)) {
            ViewportOverride.apply(config, result);
        }
        String modeLabel = applyToConfiguration ? "emulation" : "replace";
        String message = "DPIS_FONT " + sourceTag + " (" + modeLabel + ") override: widthDp "
                + originalWidthDp + " -> " + result.widthDp
                + ", heightDp " + originalHeightDp + " -> " + result.heightDp
                + ", smallestWidthDp " + originalSmallestWidthDp + " -> "
                + result.smallestWidthDp
                + ", densityDpi " + originalDensityDpi + " -> "
                + result.densityDpi
                + ", fontScale " + fontScale.original + " -> " + config.fontScale;
        logIfChanged(packageName + ":" + sourceTag, message);
    }

    private static void logIfChanged(String key, String message) {
        String previous = LAST_MESSAGES.put(key, message);
        if (!message.equals(previous)) {
            DpisLog.i(message);
        }
    }
}

