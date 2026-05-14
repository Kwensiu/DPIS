package com.dpis.module;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.util.DisplayMetrics;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.libxposed.api.XposedInterface;

final class ResourcesManagerHookInstaller {
    private static volatile boolean hookInstalled;
    private static final Map<String, String> LAST_MESSAGES = new ConcurrentHashMap<>();
    private static final int MAX_KEY_PROBE_LOGS = 24;
    private static final AtomicInteger KEY_PROBE_LOG_COUNT = new AtomicInteger();

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
                        Object result = chain.proceed();
                        logResourcesKeyProbe(packageName, methodName, key, result);
                        return result;
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
        if (!isEffectivelyEmpty(overrideConfig)) {
            return;
        }
        Configuration baseConfig = readResourcesManagerConfiguration(resourcesManager);
        if (baseConfig == null) {
            return;
        }
        Configuration targetConfig = new Configuration();
        copyViewportConfiguration(baseConfig, targetConfig);
        targetConfig.fontScale = baseConfig.fontScale;
        applyResourceOverrides(targetConfig, store, packageName,
                "ResourcesManagerKey(" + sourceTag + ")");
        if (!hasViewportOverride(targetConfig, baseConfig)) {
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
                String message = "DPIS_FONT " + sourceTag
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

    private static void logResourcesKeyProbe(String packageName,
                                             String methodName,
                                             Object key,
                                             Object impl) {
        if (KEY_PROBE_LOG_COUNT.incrementAndGet() > MAX_KEY_PROBE_LOGS) {
            return;
        }
        DpisLog.i("ResourcesManager key probe(" + methodName + "): key="
                + describeResourcesKey(key)
                + ", impl=" + describeResourcesImpl(impl)
                + appendCaller(packageName));
    }

    private static String describeResourcesKey(Object key) {
        if (key == null) {
            return "null";
        }
        int displayId = readIntField(key, "mDisplayId", Integer.MIN_VALUE);
        Object override = readField(key, "mOverrideConfiguration");
        return "id=" + System.identityHashCode(key)
                + ", displayId=" + (displayId == Integer.MIN_VALUE ? "?" : displayId)
                + ", override=" + describeConfiguration(override);
    }

    private static String describeResourcesImpl(Object impl) {
        if (impl == null) {
            return "null";
        }
        Object config = readField(impl, "mConfiguration");
        Object metrics = readField(impl, "mMetrics");
        return "id=" + System.identityHashCode(impl)
                + ", config=" + describeConfiguration(config)
                + ", metrics=" + (metrics instanceof DisplayMetrics displayMetrics
                ? describeDisplayMetrics(displayMetrics) : String.valueOf(metrics));
    }

    private static String describeConfiguration(Object config) {
        if (!(config instanceof Configuration configuration)) {
            return String.valueOf(config);
        }
        return "config{widthDp=" + configuration.screenWidthDp
                + ",heightDp=" + configuration.screenHeightDp
                + ",smallestWidthDp=" + configuration.smallestScreenWidthDp
                + ",densityDpi=" + configuration.densityDpi
                + ",fontScale=" + configuration.fontScale + "}";
    }

    private static String describeDisplayMetrics(DisplayMetrics metrics) {
        if (metrics == null) {
            return "null";
        }
        return "metrics{widthPx=" + metrics.widthPixels
                + ",heightPx=" + metrics.heightPixels
                + ",densityDpi=" + metrics.densityDpi
                + ",density=" + metrics.density
                + ",scaledDensity=" + metrics.scaledDensity + "}";
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

    private static int readIntField(Object target, String fieldName, int fallback) {
        Object value = readField(target, fieldName);
        return value instanceof Integer integer ? integer : fallback;
    }

    private static String appendCaller(String packageName) {
        String caller = CallerTrace.capture(packageName);
        if (caller == null) {
            return "";
        }
        return ", caller=" + caller;
    }
}

