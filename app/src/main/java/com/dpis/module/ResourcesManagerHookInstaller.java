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
    private static final String PROP_DISABLE_VIEWPORT_RESOURCES_MANAGER_KEY_PACKAGE =
            "debug.dpis.viewport.disable_resources_manager_key_package";
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
        if (DebugPackageOverride.matches(
                PROP_DISABLE_VIEWPORT_RESOURCES_MANAGER_KEY_PACKAGE, packageName)) {
            logIfChanged(packageName + ":ResourcesManagerKey(" + sourceTag + "):debug-skip",
                    "ResourcesManagerKey(" + sourceTag
                            + ") skipped by debug property for " + packageName);
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
        if (!isEffectivelyEmpty(overrideConfig)
                && shouldPreserveWindowLikeResourcesKeyOverride(
                sourceConfig, store, packageName, sourceTag)) {
            return;
        }
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

    private static boolean shouldPreserveWindowLikeResourcesKeyOverride(Configuration sourceConfig,
                                                                       DpiConfigStore store,
                                                                       String packageName,
                                                                       String sourceTag) {
        if (sourceConfig == null
                || sourceConfig.screenWidthDp <= 0
                || sourceConfig.screenHeightDp <= 0
                || sourceConfig.smallestScreenWidthDp <= 0
                || sourceConfig.densityDpi <= 0) {
            return false;
        }
        ViewportSourceSnapshot source = ViewportSourceSnapshot.fromConfiguration(
                ViewportSourceSnapshot.ORIGIN_RESOURCES_MANAGER, sourceConfig, null);
        ViewportTargetResolution resolution =
                TargetViewportWidthResolver.resolve(store, packageName, source);
        if (resolution == null || resolution.record == null) {
            return false;
        }
        ViewportOverride.Result displayResult =
                ViewportResolvedTarget.viewportResult(resolution, false);
        if (displayResult == null
                || displayResult.widthDp <= 0
                || displayResult.heightDp <= 0
                || displayResult.smallestWidthDp <= 0
                || displayResult.densityDpi <= 0) {
            return false;
        }
        boolean targetWidthAndDensity = sourceConfig.screenWidthDp == displayResult.widthDp
                && sourceConfig.smallestScreenWidthDp == displayResult.smallestWidthDp
                && sourceConfig.densityDpi == displayResult.densityDpi;
        boolean shorterThanDisplayTarget = sourceConfig.screenHeightDp < displayResult.heightDp;
        if (!targetWidthAndDensity || !shorterThanDisplayTarget) {
            return false;
        }
        String message = "DPIS_VIEWPORT ResourcesManagerKey(" + sourceTag
                + ") preserve window-like key: package=" + packageName
                + ", source=" + describeConfiguration(sourceConfig)
                + ", displayTarget=" + describeViewportResult(displayResult);
        logIfChanged(packageName + ":" + sourceTag + ":preserve-window-like-key", message);
        return true;
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
        FontScaleOverride.Result fontScale = FontScaleOverride.resolveForResources(
                store, packageName, config.fontScale);
        FontScaleOverride.applyToConfiguration(config, fontScale);
        int originalWidthDp = config.screenWidthDp;
        int originalHeightDp = config.screenHeightDp;
        int originalSmallestWidthDp = config.smallestScreenWidthDp;
        int originalDensityDpi = config.densityDpi;
        if (originalWidthDp <= 0 && originalHeightDp <= 0 && originalDensityDpi <= 0) {
            if (fontScale.changed) {
                String fontMessage = "DPIS_FONT " + sourceTag + " override: package="
                        + packageName + ", fontScale "
                        + fontScale.original + " -> " + config.fontScale;
                logIfChanged(packageName + ":" + sourceTag + ":font-only", fontMessage);
            }
            return;
        }
        ViewportSourceSnapshot source = ViewportSourceSnapshot.fromConfiguration(
                ViewportSourceSnapshot.ORIGIN_RESOURCES_MANAGER, config, null);
        ViewportTargetResolution resolution =
                TargetViewportWidthResolver.resolve(store, packageName, source);
        Integer targetViewportWidth = resolution.hasTarget()
                ? resolution.effectiveSmallestWidthDp
                : null;
        if (targetViewportWidth != null && resolution.spec.isEnabled()) {
            ViewportRuntimeMarkerProbe.observeAppProcessProbe(
                    packageName, resolution.spec, sourceTag);
        }
        boolean windowScoped = ViewportConfigurationScope.isWindowScoped(config);
        VirtualDisplayOverride.Result stableTarget =
                ViewportResolvedTarget.virtualDisplayResult(resolution, targetViewportWidth);
        ViewportOverride.Result resolvedRecordResult =
                ViewportResolvedTarget.viewportResult(resolution, windowScoped);
        ViewportOverride.Result result = resolvedRecordResult != null
                ? resolvedRecordResult
                : ViewportOverride.derive(
                config,
                targetViewportWidth != null ? targetViewportWidth : 0,
                windowScoped,
                stableTarget);
        if (result == null) {
            if (fontScale.changed) {
                String fontMessage = "DPIS_FONT " + sourceTag + " override: package="
                        + packageName + ", fontScale "
                        + fontScale.original + " -> " + config.fontScale;
                logIfChanged(packageName + ":" + sourceTag + ":font-only", fontMessage);
            }
            return;
        }
        VirtualDisplayOverride.Result sharedResult = VirtualDisplayPlan.derivePublishableResult(
                originalWidthDp,
                originalHeightDp,
                originalSmallestWidthDp,
                originalDensityDpi,
                0,
                0,
                result.smallestWidthDp);
        if (!windowScoped && resolution.spec.isEnabled() && source != null) {
            boolean canPublishRecord = true;
            if (sharedResult != null) {
                canPublishRecord = VirtualDisplayState.setUnlessDerivedFromTargetConfig(
                        sharedResult, originalSmallestWidthDp, targetViewportWidth);
            }
            if (canPublishRecord) {
                VirtualDisplayState.publish(
                        packageName,
                        resolution.spec,
                        source,
                        result,
                        sharedResult,
                        ViewportRuntimeRecord.PROVENANCE_APP_PROCESS);
            }
        }
        boolean needsViewportUpdate = result.widthDp != originalWidthDp
                || result.heightDp != originalHeightDp
                || result.smallestWidthDp != originalSmallestWidthDp
                || (result.densityDpi > 0 && result.densityDpi != originalDensityDpi);
        boolean applyToConfiguration = ViewportModePolicy.shouldApplyConfigurationOverride(
                store, packageName, resolution, needsViewportUpdate);
        if (!needsViewportUpdate
                && !fontScale.changed) {
            VirtualDisplayOverride.Result stableResult =
                    VirtualDisplayState.getStableTargetResult(
                            originalSmallestWidthDp, targetViewportWidth);
            if (result.densityDpi <= 0
                    && stableResult != null && stableResult.densityDpi > 0
                    && config.densityDpi != stableResult.densityDpi) {
                config.densityDpi = stableResult.densityDpi;
                String message = "DPIS_VIEWPORT " + sourceTag
                        + " stable target: package=" + packageName
                        + ", targetViewportWidthDp=" + describeNullable(targetViewportWidth)
                        + ", actual=" + describeConfiguration(config)
                        + ", widthDp " + originalWidthDp
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
        if (applyToConfiguration && needsViewportUpdate) {
            ViewportOverride.apply(config, result);
        }
        String modeLabel = applyToConfiguration ? "config" : "metrics";
        String message = "DPIS_VIEWPORT " + sourceTag + " (" + modeLabel
                + ") override: package=" + packageName
                + ", targetViewportWidthDp=" + describeNullable(targetViewportWidth)
                + ", scope=" + (windowScoped ? "window" : "display")
                + ", target=" + describeViewportResult(result)
                + ", actual=" + describeConfiguration(config)
                + ", widthDp "
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

    private static String describeConfiguration(Configuration config) {
        if (config == null) {
            return "null";
        }
        return "{widthDp=" + config.screenWidthDp
                + ",heightDp=" + config.screenHeightDp
                + ",smallestWidthDp=" + config.smallestScreenWidthDp
                + ",densityDpi=" + config.densityDpi
                + ",fontScale=" + config.fontScale + "}";
    }

    private static String describeViewportResult(ViewportOverride.Result result) {
        if (result == null) {
            return "null";
        }
        return "{widthDp=" + result.widthDp
                + ",heightDp=" + result.heightDp
                + ",smallestWidthDp=" + result.smallestWidthDp
                + ",densityDpi=" + result.densityDpi + "}";
    }

    private static String describeNullable(Integer value) {
        return value == null ? "none" : String.valueOf(value);
    }
}
