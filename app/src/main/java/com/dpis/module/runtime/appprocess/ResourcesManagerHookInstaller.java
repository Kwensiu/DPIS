package com.dpis.module.runtime.appprocess;

import com.dpis.module.DpisConfigStore;

import com.dpis.module.diagnostics.RuntimeHotPathEvents;

import com.dpis.module.*;
import com.dpis.module.runtime.font.FontScaleOverride;

import com.dpis.module.viewport.VirtualDisplayPlan;

import com.dpis.module.viewport.VirtualDisplayOverride;
import com.dpis.module.viewport.VirtualDisplayState;

import com.dpis.module.viewport.TargetViewportWidthResolver;
import com.dpis.module.viewport.ViewportRuntimeMarkerProbe;
import com.dpis.module.viewport.ViewportRuntimeRecord;
import com.dpis.module.viewport.ViewportConfigurationScope;
import com.dpis.module.viewport.ViewportModePolicy;
import com.dpis.module.viewport.ViewportOverride;
import com.dpis.module.viewport.ViewportResolvedTarget;
import com.dpis.module.viewport.ViewportSourceSnapshot;
import com.dpis.module.viewport.ViewportTargetResolution;

import com.dpis.module.hooks.HookRuntimePolicy;
import com.dpis.module.runtime.hookapi.ModernApiCapabilities;

import com.dpis.module.runtime.DebugPackageOverride;
import com.dpis.module.runtime.RuntimeHotPathEvidenceSampler;

import android.annotation.SuppressLint;
import android.content.res.Configuration;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.api.XposedInterface;

@SuppressWarnings("java:S1872")
public final class ResourcesManagerHookInstaller {
    private static final String HOOK_ID_APPLY_CONFIGURATION =
            "resources_manager_apply_configuration_to_resources";
    private static final String HOOK_ID_UPDATE_RESOURCES_FOR_ACTIVITY =
            "resources_manager_update_resources_for_activity";
    private static final String HOOK_ID_RESOURCE_CREATION_PREFIX =
            "resources_manager_resource_creation_config";
    private static final String HOOK_ID_RESOURCES_KEY_PREFIX =
            "resources_manager_create_resources_impl_key";
    private static final String PROP_DISABLE_VIEWPORT_RESOURCES_MANAGER_KEY_PACKAGE =
            "debug.dpis.viewport.disable_resources_manager_key_package";
    private static volatile boolean hookInstalled;
    private static final Map<String, String> LAST_MESSAGES = new ConcurrentHashMap<>();
    private static final RuntimeHotPathEvidenceSampler HOTPATH_SAMPLER =
            new RuntimeHotPathEvidenceSampler();

    private ResourcesManagerHookInstaller() {
    }

    public static void resetForHotReload() {
        hookInstalled = false;
    }

    public static void install(XposedInterface xposed,
                        String packageName,
                        DpisConfigStore store,
                        ModernApiCapabilities apiCapabilities)
            throws ReflectiveOperationException {
        install(xposed, packageName, store, HookRuntimePolicy.fromStore(store), apiCapabilities);
    }

    public static void install(XposedInterface xposed,
                        String packageName,
                        DpisConfigStore store,
                        HookRuntimePolicy policy,
                        ModernApiCapabilities apiCapabilities)
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
            apiCapabilities.applyStableHookId(
                            xposed.hook(applyConfigurationMethod)
                                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                            HOOK_ID_APPLY_CONFIGURATION)
                    .intercept(chain -> {
                        Configuration config = (Configuration) chain.getArg(0);
                        applyResourceOverrides(config, store, packageName, "ResourcesManager",
                                policy);
                        return chain.proceed();
                    });

            Method updateResourcesForActivityMethod = resolveUpdateResourcesForActivityMethod(
                    resourcesManagerClass, bootClassLoader);
            apiCapabilities.applyStableHookId(
                            xposed.hook(updateResourcesForActivityMethod)
                                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                            HOOK_ID_UPDATE_RESOURCES_FOR_ACTIVITY)
                    .intercept(chain -> {
                        Configuration overrideConfig = (Configuration) chain.getArg(1);
                        applyResourceOverrides(overrideConfig, store, packageName,
                                "ResourcesManagerActivity", policy);
                        return chain.proceed();
                    });

            int createHookCount = installResourceCreationHooks(
                    xposed, resourcesManagerClass, packageName, store, policy, apiCapabilities);
            int keyHookCount = installResourcesKeyHooks(
                    xposed, resourcesManagerClass, packageName, store, policy, apiCapabilities);
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
                                                    DpisConfigStore store,
                                                    HookRuntimePolicy policy,
                                                    ModernApiCapabilities apiCapabilities) {
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
            apiCapabilities.applyStableHookId(
                            xposed.hook(method)
                                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                            HOOK_ID_RESOURCE_CREATION_PREFIX + "#" + method.toGenericString())
                    .intercept(chain -> {
                        Configuration config = (Configuration) chain.getArg(configArgIndex);
                        applyResourceOverrides(config, store, packageName,
                                "ResourcesManagerCreate(" + methodName + ")", policy);
                        return chain.proceed();
                    });
            hookedCount++;
        }
        return hookedCount;
    }

    private static int installResourcesKeyHooks(XposedInterface xposed,
                                                Class<?> resourcesManagerClass,
                                                String packageName,
                                                DpisConfigStore store,
                                                HookRuntimePolicy policy,
                                                ModernApiCapabilities apiCapabilities) {
        int hookedCount = 0;
        Set<Method> hookedMethods = new HashSet<>();
        for (Method method : resourcesManagerClass.getDeclaredMethods()) {
            String methodName = method.getName();
            if (!"createResourcesImpl".equals(methodName)
                    || !hasResourcesKeyFirstArg(method)
                    || !hookedMethods.add(method)) {
                continue;
            }
            apiCapabilities.applyStableHookId(
                            xposed.hook(method)
                                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                            HOOK_ID_RESOURCES_KEY_PREFIX + "#" + method.toGenericString())
                    .intercept(chain -> {
                        Object key = chain.getArg(0);
                        maybeApplyKeyOverride(
                                chain.getThisObject(), key, store, packageName, methodName,
                                policy);
                        return chain.proceed();
                    });
            hookedCount++;
        }
        return hookedCount;
    }

    public static void maybeApplyKeyOverride(Object resourcesManager,
                                      Object key,
                                      DpisConfigStore store,
                                      String packageName,
                                      String sourceTag) {
        maybeApplyKeyOverride(resourcesManager, key, store, packageName, sourceTag,
                HookRuntimePolicy.fromStore(store));
    }

    public static void maybeApplyKeyOverride(Object resourcesManager,
                                      Object key,
                                      DpisConfigStore store,
                                      String packageName,
                                      String sourceTag,
                                      HookRuntimePolicy policy) {
        packageName = WebApkRuntimeOwnerBridge.resolveEffectivePackage(store, packageName);
        store = WebApkRuntimeOwnerBridge.resolveEffectiveStore(store, packageName);
        if (resourcesManager == null || key == null) {
            recordViewportSkip(packageName, "resources_manager_key_override",
                    "missing_resources_manager_or_key",
                    "source=ResourcesManagerKey(" + sourceTag
                            + "), reason=missing_resources_manager_or_key");
            return;
        }
        if (!ViewportModePolicy.shouldApplyConfigurationOverride(policy, store, packageName)) {
            recordViewportSkip(packageName, "resources_manager_key_override",
                    "configuration_override_disabled",
                    "source=ResourcesManagerKey(" + sourceTag
                            + "), reason=configuration_override_disabled");
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
            recordViewportSkip(packageName, "resources_manager_key_override",
                    "missing_override_configuration",
                    "source=ResourcesManagerKey(" + sourceTag
                            + "), reason=missing_override_configuration");
            return;
        }
        Configuration baseConfig = readResourcesManagerConfiguration(resourcesManager);
        if (baseConfig == null) {
            recordViewportSkip(packageName, "resources_manager_key_override",
                    "missing_base_configuration",
                    "source=ResourcesManagerKey(" + sourceTag
                            + "), reason=missing_base_configuration");
            return;
        }
        if (!shouldReplaceResourcesKeyOverride(overrideConfig, baseConfig)) {
            recordViewportSkip(packageName, "resources_manager_key_override",
                    "preserve_existing_override",
                    "source=ResourcesManagerKey(" + sourceTag
                            + "), reason=preserve_existing_override");
            return;
        }
        Configuration targetConfig = new Configuration();
        Configuration sourceConfig = isEffectivelyEmpty(overrideConfig) ? baseConfig : overrideConfig;
        if (!isEffectivelyEmpty(overrideConfig)
                && shouldPreserveWindowLikeResourcesKeyOverride(
                sourceConfig, store, packageName, sourceTag)) {
            recordViewportSkip(packageName, "resources_manager_key_override",
                    "preserve_window_like_override",
                    "source=ResourcesManagerKey(" + sourceTag
                            + "), reason=preserve_window_like_override");
            return;
        }
        copyViewportConfiguration(sourceConfig, targetConfig);
        targetConfig.fontScale = sourceConfig.fontScale;
        applyResourceOverrides(targetConfig, store, packageName,
                "ResourcesManagerKey(" + sourceTag + ")", policy);
        if (!hasViewportOverride(targetConfig, sourceConfig)) {
            recordViewportSkip(packageName, "resources_manager_key_override",
                    "no_viewport_delta_after_resolution",
                    "source=ResourcesManagerKey(" + sourceTag
                            + "), reason=no_viewport_delta_after_resolution");
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
                                                                       DpisConfigStore store,
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

    public static void applyResourceOverrides(Configuration config, DpisConfigStore store,
                                       String packageName, String sourceTag) {
        applyResourceOverrides(config, store, packageName, sourceTag,
                HookRuntimePolicy.fromStore(store));
    }

    public static void applyResourceOverrides(Configuration config,
                                       DpisConfigStore store,
                                       String packageName,
                                       String sourceTag,
                                       HookRuntimePolicy policy) {
        packageName = WebApkRuntimeOwnerBridge.resolveEffectivePackage(store, packageName);
        store = WebApkRuntimeOwnerBridge.resolveEffectiveStore(store, packageName);
        if (config == null) {
            recordViewportSkip(packageName, "resources_manager_config_override",
                    "null_configuration",
                    "source=" + sourceTag + ", reason=null_configuration");
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
            } else {
                recordViewportSkip(packageName, "resources_manager_config_override",
                        "empty_viewport_configuration",
                        "source=" + sourceTag + ", reason=empty_viewport_configuration");
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
                ViewportResolvedTarget.viewportResult(resolution, windowScoped, config);
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
            } else {
                recordViewportSkip(packageName, "resources_manager_config_override",
                        "no_viewport_result",
                        "source=" + sourceTag
                                + ", reason=no_viewport_result"
                                + ", targetViewportWidthDp="
                                + describeNullable(targetViewportWidth));
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
                policy, store, packageName, resolution, needsViewportUpdate);
        if (!needsViewportUpdate
                && !fontScale.changed) {
            VirtualDisplayOverride.Result stableResult =
                    VirtualDisplayState.getStableTargetResult(
                            originalSmallestWidthDp, targetViewportWidth);
            boolean stableTargetApplied = false;
            if (result.densityDpi <= 0
                    && stableResult != null && stableResult.densityDpi > 0
                    && config.densityDpi != stableResult.densityDpi) {
                config.densityDpi = stableResult.densityDpi;
                stableTargetApplied = true;
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
            if (!stableTargetApplied) {
                recordViewportSkip(packageName, "resources_manager_config_override",
                        "stable_configuration",
                        "source=" + sourceTag
                                + ", reason=stable_configuration"
                                + ", targetViewportWidthDp="
                                + describeNullable(targetViewportWidth));
            }
            return;
        }
        if (applyToConfiguration && needsViewportUpdate) {
            ViewportOverride.apply(config, result);
        }
        String modeLabel = applyToConfiguration ? "config" : "metrics";
        String detail = "source=" + sourceTag
                + ", mode=" + modeLabel
                + ", scope=" + (windowScoped ? "window" : "display")
                + ", targetViewportWidthDp=" + describeNullable(targetViewportWidth)
                + ", widthDp=" + originalWidthDp + "->" + result.widthDp
                + ", heightDp=" + originalHeightDp + "->" + result.heightDp
                + ", smallestWidthDp=" + originalSmallestWidthDp + "->"
                + result.smallestWidthDp
                + ", densityDpi=" + originalDensityDpi + "->" + result.densityDpi
                + ", fontScale=" + fontScale.original + "->" + config.fontScale;
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
        if (logIfChanged(packageName + ":" + sourceTag, message)) {
            RuntimeHotPathEvents.applied(
                    packageName,
                    "viewport",
                    "resources_manager_config_override",
                    detail);
        }
    }

    private static boolean logIfChanged(String key, String message) {
        String previous = LAST_MESSAGES.put(key, message);
        if (!message.equals(previous)) {
            DpisLog.i(message);
            return true;
        }
        return false;
    }

    private static void recordViewportSkip(String packageName,
                                           String routeName,
                                           String reason,
                                           String detail) {
        RuntimeHotPathEvidenceSampler.Sample sample =
                HOTPATH_SAMPLER.sample("skip|" + packageName + "|" + routeName + "|" + reason,
                        detail);
        if (sample.emit) {
            RuntimeHotPathEvents.skipped(
                    packageName,
                    "viewport",
                    routeName,
                    sample.detail);
        }
    }

    public static void resetHotPathSamplerForTest() {
        HOTPATH_SAMPLER.resetForTest();
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
