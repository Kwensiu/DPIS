package com.dpis.module;

import android.content.res.Configuration;
import android.util.DisplayMetrics;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;

final class ResourcesManagerHookInstaller {
    private static volatile boolean hookInstalled;

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
                        applyViewportOverride(config, store, packageName, "ResourcesManager");
                        return chain.proceed();
                    });

            Class<?> iBinderClass = Class.forName("android.os.IBinder", false, bootClassLoader);
            Method updateResourcesForActivityMethod = resourcesManagerClass.getDeclaredMethod(
                    "updateResourcesForActivity", iBinderClass, Configuration.class, int.class);
            xposed.hook(updateResourcesForActivityMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Configuration overrideConfig = (Configuration) chain.getArg(1);
                        applyViewportOverride(overrideConfig, store, packageName,
                                "ResourcesManagerActivity");
                        return chain.proceed();
                    });
            hookInstalled = true;
            DpisLog.i("ResourcesManager hook ready");
        }
    }

    private static void applyViewportOverride(Configuration config, DpiConfigStore store,
                                              String packageName, String sourceTag) {
        if (config == null) {
            return;
        }
        int originalWidthDp = config.screenWidthDp;
        int originalHeightDp = config.screenHeightDp;
        int originalSmallestWidthDp = config.smallestScreenWidthDp;
        int originalDensityDpi = config.densityDpi;
        Integer targetViewportWidth = store.getEffectiveViewportWidthDp(packageName);
        ViewportOverride.Result result = ViewportOverride.derive(
                config, targetViewportWidth != null ? targetViewportWidth : 0);
        if (result == null) {
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
        if (result.widthDp == originalWidthDp
                && result.heightDp == originalHeightDp
                && result.smallestWidthDp == originalSmallestWidthDp
                && result.densityDpi == originalDensityDpi) {
            return;
        }
        ViewportOverride.apply(config, result);
        DpisLog.i(sourceTag + " override: widthDp "
                + originalWidthDp + " -> " + result.widthDp
                + ", heightDp " + originalHeightDp + " -> " + result.heightDp
                + ", smallestWidthDp " + originalSmallestWidthDp + " -> "
                + result.smallestWidthDp
                + ", densityDpi " + originalDensityDpi + " -> "
                + result.densityDpi);
    }
}
