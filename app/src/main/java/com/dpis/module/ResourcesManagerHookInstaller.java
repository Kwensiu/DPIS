package com.dpis.module;

import android.annotation.SuppressLint;
import android.content.res.Configuration;

import java.lang.reflect.Field;
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
            int keyHookCount = installResourcesKeyHooks(
                    xposed, resourcesManagerClass, packageName, store);
            hookInstalled = true;
            DpisLog.i("ResourcesManager hook ready (createHooks=" + createHookCount
                    + ", keyHooks=" + keyHookCount + ")");
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

    private static int installResourcesKeyHooks(XposedInterface xposed,
                                                Class<?> resourcesManagerClass,
                                                String packageName,
                                                DpiConfigStore store) {
        int hookedCount = 0;
        Set<Method> hookedMethods = new HashSet<>();
        for (Method method : resourcesManagerClass.getDeclaredMethods()) {
            String methodName = method.getName();
            if (!"createResourcesImpl".equals(methodName)
                    || !hasResourcesKeyFirstArg(method)
                    || !hookedMethods.add(method)) {
                continue;
            }
            xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object key = chain.getArg(0);
                        maybeApplyKeyOverride(
                                chain.getThisObject(), key, store, packageName, methodName);
                        return chain.proceed();
                    });
            hookedCount++;
        }
        return hookedCount;
    }

    static void maybeApplyKeyOverride(Object resourcesManager,
                                      Object key,
                                      DpiConfigStore store,
                                      String packageName,
                                      String sourceTag) {
        if (resourcesManager == null || key == null) {
            return;
        }
        if (!ViewportModePolicy.shouldApplyConfigurationOverride(store, packageName)) {
            return;
        }
        Object override = readField(key, "mOverrideConfiguration");
        if (!(override instanceof Configuration overrideConfig)) {
            return;
        }
        Configuration baseConfig = readResourcesManagerConfiguration(resourcesManager);
        if (baseConfig == null) {
            return;
        }
        if (!shouldReplaceResourcesKeyOverride(overrideConfig, baseConfig)) {
            return;
        }
        Configuration targetConfig = new Configuration();
        Configuration sourceConfig = isEffectivelyEmpty(overrideConfig) ? baseConfig : overrideConfig;
        copyViewportConfiguration(sourceConfig, targetConfig);
        targetConfig.fontScale = sourceConfig.fontScale;
        applyResourceOverrides(targetConfig, store, packageName,
                "ResourcesManagerKey(" + sourceTag + ")");
        if (!hasViewportOverride(targetConfig, sourceConfig)) {
            return;
        }
        copyViewportConfiguration(targetConfig, overrideConfig);
    }

    private static Configuration readResourcesManagerConfiguration(Object resourcesManager) {
        try {
            Method method = resourcesManager.getClass().getDeclaredMethod("getConfiguration");
            method.setAccessible(true);
            Object result = method.invoke(resourcesManager);
            if (result instanceof Configuration configuration) {
                return configuration;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        Object config = readField(resourcesManager, "mResConfiguration");
        return config instanceof Configuration configuration ? configuration : null;
    }

    private static boolean isEffectivelyEmpty(Configuration config) {
        return config != null
                && config.screenWidthDp <= 0
                && config.screenHeightDp <= 0
                && config.smallestScreenWidthDp <= 0
                && config.densityDpi <= 0;
    }

    private static boolean shouldReplaceResourcesKeyOverride(Configuration overrideConfig,
                                                            Configuration baseConfig) {
        if (overrideConfig == null || baseConfig == null) {
            return false;
        }
        if (isEffectivelyEmpty(overrideConfig)) {
            return true;
        }
        boolean hasViewportFields = overrideConfig.screenWidthDp > 0
                || overrideConfig.screenHeightDp > 0
                || overrideConfig.smallestScreenWidthDp > 0
                || overrideConfig.densityDpi > 0;
        if (!hasViewportFields) {
            return true;
        }
        boolean sameBounds = (overrideConfig.screenWidthDp <= 0
                || overrideConfig.screenWidthDp == baseConfig.screenWidthDp)
                && (overrideConfig.screenHeightDp <= 0
                || overrideConfig.screenHeightDp == baseConfig.screenHeightDp)
                && (overrideConfig.smallestScreenWidthDp <= 0
                || overrideConfig.smallestScreenWidthDp == baseConfig.smallestScreenWidthDp);
        boolean sameDensity = overrideConfig.densityDpi <= 0
                || overrideConfig.densityDpi == baseConfig.densityDpi;
        return sameBounds && sameDensity;
    }

    private static boolean hasViewportOverride(Configuration target, Configuration source) {
        return target != null
                && source != null
                && (target.screenWidthDp != source.screenWidthDp
                || target.screenHeightDp != source.screenHeightDp
                || target.smallestScreenWidthDp != source.smallestScreenWidthDp
                || target.densityDpi != source.densityDpi);
    }

    private static void copyViewportConfiguration(Configuration source, Configuration target) {
        target.screenWidthDp = source.screenWidthDp;
        target.screenHeightDp = source.screenHeightDp;
        target.smallestScreenWidthDp = source.smallestScreenWidthDp;
        target.densityDpi = source.densityDpi;
    }

    private static boolean hasResourcesKeyFirstArg(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        return parameterTypes.length > 0
                && "android.content.res.ResourcesKey".equals(parameterTypes[0].getName());
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

    static void applyResourceOverrides(Configuration config, DpiConfigStore store,
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
                result.smallestWidthDp);
        VirtualDisplayState.setUnlessDerivedFromTargetConfig(
                sharedResult, originalSmallestWidthDp, targetViewportWidth);
        boolean applyToConfiguration = ViewportModePolicy.shouldApplyConfigurationOverride(
                store, packageName);
        if (result.widthDp == originalWidthDp
                && result.heightDp == originalHeightDp
                && result.smallestWidthDp == originalSmallestWidthDp
                && result.densityDpi == originalDensityDpi
                && !fontScale.changed) {
            VirtualDisplayOverride.Result stableResult =
                    VirtualDisplayState.getStableTargetResult(
                            originalSmallestWidthDp, targetViewportWidth);
            if (stableResult != null && stableResult.densityDpi > 0
                    && config.densityDpi != stableResult.densityDpi) {
                config.densityDpi = stableResult.densityDpi;
                String message = "DPIS_VIEWPORT " + sourceTag
                        + " stable target: widthDp " + originalWidthDp
                        + " -> " + config.screenWidthDp
                        + ", heightDp " + originalHeightDp
                        + " -> " + config.screenHeightDp
                        + ", smallestWidthDp " + originalSmallestWidthDp
                        + " -> " + config.smallestScreenWidthDp
                        + ", densityDpi " + originalDensityDpi
                        + " -> " + config.densityDpi
                        + ", fontScale " + fontScale.original
                        + " -> " + config.fontScale;
                logIfChanged(packageName + ":" + sourceTag + ":stable-target", message);
            }
            return;
        }
        if (applyToConfiguration
                && (result.widthDp != originalWidthDp
                || result.heightDp != originalHeightDp
                || result.smallestWidthDp != originalSmallestWidthDp
                || result.densityDpi != originalDensityDpi)) {
            ViewportOverride.apply(config, result);
        }
        String modeLabel = applyToConfiguration ? "config" : "metrics";
        String message = "DPIS_VIEWPORT " + sourceTag + " (" + modeLabel
                + ") override: widthDp "
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

    private static Object readField(Object target, String fieldName) {
        if (target == null) {
            return null;
        }
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }
}
