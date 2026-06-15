package com.dpis.module;

import android.content.res.Configuration;
import android.util.DisplayMetrics;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.api.XposedInterface;

final class ResourcesImplHookInstaller {
    private static volatile boolean hookInstalled;
    private static final Map<String, String> LAST_MESSAGES = new ConcurrentHashMap<>();

    private ResourcesImplHookInstaller() {
    }

    static void install(XposedInterface xposed, String packageName, DpiConfigStore store)
            throws ReflectiveOperationException {
        if (hookInstalled) {
            return;
        }
        synchronized (ResourcesImplHookInstaller.class) {
            if (hookInstalled) {
                return;
            }
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> resourcesImplClass = Class.forName(
                    "android.content.res.ResourcesImpl", false, bootClassLoader);
            Class<?> compatibilityInfoClass = Class.forName(
                    "android.content.res.CompatibilityInfo", false, bootClassLoader);
            Method method = resourcesImplClass.getDeclaredMethod(
                    "updateConfiguration", Configuration.class, DisplayMetrics.class,
                    compatibilityInfoClass);
            xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Configuration config = (Configuration) chain.getArg(0);
                        DisplayMetrics metrics = (DisplayMetrics) chain.getArg(1);
                        applyDensityOverride(packageName, config, metrics, store);
                        return chain.proceed();
                    });
            hookInstalled = true;
            DpisLog.i("ResourcesImpl hook ready");
        }
    }

    static void applyDensityOverride(String packageName, Configuration config, DisplayMetrics metrics,
                                     DpiConfigStore store) {
        applyDensityOverride(packageName, config, metrics, store, null);
    }

    static void applyDensityOverrideForTest(String packageName,
                                            Configuration config,
                                            DisplayMetrics metrics,
                                            DpiConfigStore store,
                                            boolean windowScoped) {
        applyDensityOverride(packageName, config, metrics, store, windowScoped);
    }

    private static void applyDensityOverride(String packageName,
                                             Configuration config,
                                             DisplayMetrics metrics,
                                             DpiConfigStore store,
                                             Boolean windowScopedOverride) {
        if (config == null) {
            logIfChanged(packageName + ":skip", "ResourcesImpl skip: config is null");
            return;
        }
        FontScaleOverride.Result fontScale = FontScaleOverride.resolveForResources(
                store, packageName, config.fontScale);
        boolean fontScaleApplied = FontScaleOverride.applyToConfiguration(config, fontScale);
        int originalWidthDp = config.screenWidthDp;
        int originalHeightDp = config.screenHeightDp;
        int originalSmallestWidthDp = config.smallestScreenWidthDp;
        int originalDensityDpi = config.densityDpi;
        ViewportSourceSnapshot source = ViewportSourceSnapshot.fromConfiguration(
                ViewportSourceSnapshot.ORIGIN_RESOURCES_IMPL, config, metrics);
        ViewportTargetResolution resolution =
                TargetViewportWidthResolver.resolve(store, packageName, source);
        Integer targetViewportWidth = resolution.hasTarget()
                ? resolution.effectiveSmallestWidthDp
                : null;
        if (targetViewportWidth != null && resolution.spec.isEnabled()) {
            ViewportRuntimeMarkerProbe.observeAppProcessProbe(
                    packageName, resolution.spec, "ResourcesImpl");
        }
        boolean windowScoped = windowScopedOverride != null
                ? windowScopedOverride
                : ViewportConfigurationScope.isWindowScoped(config);
        int sourceWidthPx = metrics != null ? metrics.widthPixels : 0;
        int sourceHeightPx = metrics != null ? metrics.heightPixels : 0;
        VirtualDisplayOverride.Result stableTarget =
                ViewportResolvedTarget.virtualDisplayResult(resolution, targetViewportWidth);
        VirtualDisplayOverride.Result stableTargetForResult =
                stableTarget != null
                        && targetViewportWidth != null
                        && stableTarget.smallestWidthDp == targetViewportWidth
                        && originalDensityDpi != stableTarget.densityDpi
                        ? stableTarget
                        : null;
        VirtualDisplayOverride.Result pixelDerivedTarget =
                !windowScoped
                        && stableTargetForResult == null
                        && targetViewportWidth != null
                        && resolution.spec.isAbsoluteDp()
                        && originalDensityDpi > 0
                        && (originalSmallestWidthDp != targetViewportWidth
                        || (metrics != null && originalDensityDpi != metrics.densityDpi))
                        ? VirtualDisplayPlan.deriveAbsoluteResultFromPhysicalPixels(
                        originalWidthDp,
                        originalHeightDp,
                        originalSmallestWidthDp,
                        sourceWidthPx,
                        sourceHeightPx,
                        targetViewportWidth)
                        : null;
        VirtualDisplayOverride.Result trustedDisplayTarget =
                stableTargetForResult != null ? stableTargetForResult : pixelDerivedTarget;
        ViewportOverride.Result resolvedRecordResult =
                ViewportResolvedTarget.viewportResult(resolution, windowScoped);
        ViewportOverride.Result windowLikeBorrowResult =
                resolveWindowLikeBorrowResult(config, resolution, windowScoped);
        ViewportOverride.Result appProcessWindowMetricsResult =
                windowScoped
                        ? ViewportResolvedTarget.appProcessWindowMetricsResult(
                        config, resolution, targetViewportWidth, stableTarget)
                        : null;
        boolean windowLikeBorrow = windowLikeBorrowResult != null;
        boolean appProcessWindowMetricsOnly = appProcessWindowMetricsResult != null;
        ViewportOverride.Result trustedDisplayResult =
                resolution.isAppProcessBorrowTarget()
                        ? null
                        : ViewportResolvedTarget.viewportResult(trustedDisplayTarget);
        ViewportOverride.Result result = windowLikeBorrowResult != null
                ? windowLikeBorrowResult
                : appProcessWindowMetricsResult != null
                ? appProcessWindowMetricsResult
                : resolvedRecordResult != null
                ? resolvedRecordResult
                : trustedDisplayResult != null
                ? trustedDisplayResult
                : ViewportOverride.derive(
                config,
                targetViewportWidth != null ? targetViewportWidth : 0,
                windowScoped,
                stableTarget);
        if (result == null) {
            float originalScaledDensity = metrics != null ? metrics.scaledDensity : -1f;
            boolean metricsApplied = applyScaledDensityIfChanged(metrics, config);
            if (fontScaleApplied || metricsApplied) {
                logIfChanged(packageName + ":observe",
                        "DPIS_VIEWPORT ResourcesImpl observe: widthDp=" + originalWidthDp
                                + ", heightDp=" + originalHeightDp
                                + ", smallestWidthDp=" + originalSmallestWidthDp
                                + ", densityDpi=" + originalDensityDpi
                                + ", fontScale=" + fontScale.original + " -> " + config.fontScale
                                + ", scaledDensity=" + originalScaledDensity + " -> "
                                + (metrics != null ? metrics.scaledDensity : -1f));
            }
            return;
        }
        boolean needsViewportUpdate = result.widthDp != originalWidthDp
                || result.heightDp != originalHeightDp
                || result.smallestWidthDp != originalSmallestWidthDp
                || (result.densityDpi > 0 && result.densityDpi != originalDensityDpi);
        boolean applyToConfiguration = ViewportModePolicy.shouldApplyConfigurationOverride(
                store, packageName, resolution, needsViewportUpdate)
                && !appProcessWindowMetricsOnly;
        VirtualDisplayOverride.Result sharedResult =
                trustedDisplayTarget != null
                        ? trustedDisplayTarget
                        : VirtualDisplayPlan.derivePublishableResult(
                        originalWidthDp,
                        originalHeightDp,
                        originalSmallestWidthDp,
                        originalDensityDpi,
                        sourceWidthPx,
                        sourceHeightPx,
                        result.smallestWidthDp);
        VirtualDisplayOverride.Result publishableSharedResult = windowScoped ? null : sharedResult;
        boolean canPublishFromResourcesImpl = !windowLikeBorrow
                && !appProcessWindowMetricsOnly
                && shouldPublishResourcesImplResult(resolution, needsViewportUpdate);
        if (canPublishFromResourcesImpl && publishableSharedResult != null) {
            boolean canPublishState = VirtualDisplayState.setUnlessDerivedFromTargetConfig(
                    publishableSharedResult, originalSmallestWidthDp, targetViewportWidth);
            if (canPublishState && resolution.spec.isEnabled() && source != null) {
                VirtualDisplayState.publish(
                        packageName,
                        resolution.spec,
                        source,
                        result,
                        publishableSharedResult,
                        ViewportRuntimeRecord.PROVENANCE_APP_PROCESS);
            }
        }
        if (canPublishFromResourcesImpl) {
            String viewportMode = ViewportModePolicy.resolve(store, packageName);
            ViewportDebugReporter.report(
                    store,
                    packageName,
                    viewportMode,
                    originalWidthDp,
                    originalHeightDp,
                    originalDensityDpi,
                    result,
                    publishableSharedResult,
                    applyToConfiguration);
        }
        if (!needsViewportUpdate) {
            VirtualDisplayOverride.Result stableResult =
                    VirtualDisplayState.getStableTargetResult(
                            originalSmallestWidthDp, targetViewportWidth);
            if (windowLikeBorrow
                    && stableResult != null
                    && stableResult.densityDpi > 0
                    && metrics != null) {
                metrics.densityDpi = stableResult.densityDpi;
                metrics.density = DensityOverride.densityFromDpi(stableResult.densityDpi);
                metrics.scaledDensity = DensityOverride.scaledDensityFrom(
                        stableResult.densityDpi, config.fontScale);
                metrics.widthPixels = stableResult.widthPx;
                metrics.heightPixels = stableResult.heightPx;
                logIfChanged(packageName + ":window-like-borrow",
                        "DPIS_VIEWPORT ResourcesImpl window-like borrow: widthDp="
                                + config.screenWidthDp
                                + ", heightDp=" + config.screenHeightDp
                                + ", smallestWidthDp=" + config.smallestScreenWidthDp
                                + ", metricsDensityDpi=" + metrics.densityDpi
                                + ", metricsWidthPx=" + metrics.widthPixels
                                + ", metricsHeightPx=" + metrics.heightPixels);
                return;
            }
            if (result.densityDpi <= 0
                    && stableResult != null && stableResult.densityDpi > 0
                    && config.densityDpi != stableResult.densityDpi) {
                config.densityDpi = stableResult.densityDpi;
                if (metrics != null) {
                    metrics.densityDpi = stableResult.densityDpi;
                    metrics.density = DensityOverride.densityFromDpi(stableResult.densityDpi);
                    metrics.scaledDensity = DensityOverride.scaledDensityFrom(
                            stableResult.densityDpi, config.fontScale);
                    metrics.widthPixels = stableResult.widthPx;
                    metrics.heightPixels = stableResult.heightPx;
                }
                logIfChanged(packageName + ":stable-target",
                        "DPIS_VIEWPORT ResourcesImpl stable target: widthDp="
                                + config.screenWidthDp
                                + ", heightDp=" + config.screenHeightDp
                                + ", smallestWidthDp=" + config.smallestScreenWidthDp
                                + ", densityDpi " + originalDensityDpi
                                + " -> " + config.densityDpi
                                + ", metricsDensityDpi="
                                + (metrics != null ? metrics.densityDpi : -1)
                                + ", metricsWidthPx="
                                + (metrics != null ? metrics.widthPixels : -1)
                                + ", metricsHeightPx="
                                + (metrics != null ? metrics.heightPixels : -1));
                return;
            }
            applyScaledDensityIfChanged(metrics, config);
            return;
        }
        if (applyToConfiguration) {
            ViewportOverride.apply(config, result);
        }
        float targetDensity = DensityOverride.densityFromDpi(result.densityDpi);
        float targetScaledDensity = DensityOverride.scaledDensityFrom(
                result.densityDpi, config.fontScale);
        if (metrics != null) {
            metrics.densityDpi = result.densityDpi;
            metrics.density = targetDensity;
            metrics.scaledDensity = targetScaledDensity;
            if (publishableSharedResult != null) {
                metrics.widthPixels = publishableSharedResult.widthPx;
                metrics.heightPixels = publishableSharedResult.heightPx;
            }
        }
        String modeLabel = applyToConfiguration ? "config" : "metrics";
        logIfChanged(packageName + ":override",
                "DPIS_VIEWPORT ResourcesImpl (" + modeLabel + ") override: scope="
                        + (windowScoped ? "window" : "display")
                        + ", widthDp "
                        + originalWidthDp + " -> " + result.widthDp
                        + ", heightDp " + originalHeightDp + " -> " + result.heightDp
                        + ", smallestWidthDp " + originalSmallestWidthDp + " -> "
                        + result.smallestWidthDp
                        + ", resolution=" + describeResolution(resolution)
                        + ", densityDpi " + originalDensityDpi + " -> " + result.densityDpi
                        + ", fontScale " + fontScale.original + " -> " + config.fontScale
                        + ", metricsDensityDpi=" + (metrics != null ? metrics.densityDpi : -1)
                        + ", metricsWidthPx=" + (metrics != null ? metrics.widthPixels : -1)
                        + ", metricsHeightPx=" + (metrics != null ? metrics.heightPixels : -1));
    }

    private static void logIfChanged(String key, String message) {
        String previous = LAST_MESSAGES.put(key, message);
        if (!message.equals(previous)) {
            DpisLog.i(message);
        }
    }

    private static boolean applyScaledDensityIfChanged(DisplayMetrics metrics,
                                                       Configuration config) {
        if (metrics == null || config == null) {
            return false;
        }
        int baseDensityDpi = metrics.densityDpi > 0 ? metrics.densityDpi : config.densityDpi;
        if (baseDensityDpi <= 0) {
            return false;
        }
        float targetScaledDensity = DensityOverride.scaledDensityFrom(baseDensityDpi,
                config.fontScale);
        if (Math.abs(metrics.scaledDensity - targetScaledDensity) <= FontScaleOverride.EPSILON) {
            return false;
        }
        metrics.scaledDensity = targetScaledDensity;
        return true;
    }

    private static boolean shouldPublishResourcesImplResult(ViewportTargetResolution resolution,
                                                            boolean needsViewportUpdate) {
        if (resolution == null || resolution.spec == null || !resolution.spec.isEnabled()) {
            return false;
        }
        if (resolution.isAppProcessDisplayBorrowTarget()) {
            return false;
        }
        if (needsViewportUpdate) {
            return true;
        }
        return resolution.spec.isAbsoluteDp();
    }

    private static ViewportOverride.Result resolveWindowLikeBorrowResult(
            Configuration config,
            ViewportTargetResolution resolution,
            boolean windowScoped) {
        if (config == null
                || windowScoped
                || resolution == null
                || resolution.record == null
                || resolution.record.viewportResult == null
                || resolution.spec == null
                || !resolution.spec.isRelativeScale()) {
            return null;
        }
        ViewportOverride.Result displayResult = resolution.record.viewportResult;
        boolean matchesTargetWidth = config.screenWidthDp == displayResult.widthDp
                && config.smallestScreenWidthDp == displayResult.smallestWidthDp;
        boolean shorterThanDisplay = config.screenHeightDp > 0
                && config.screenHeightDp < displayResult.heightDp;
        if (!matchesTargetWidth || !shorterThanDisplay) {
            return null;
        }
        return new ViewportOverride.Result(
                config.screenWidthDp,
                config.screenHeightDp,
                config.smallestScreenWidthDp,
                displayResult.densityDpi);
    }

    private static String describeResolution(ViewportTargetResolution resolution) {
        if (resolution == null) {
            return "none";
        }
        return "{reason=" + resolution.reason
                + ",target=" + resolution.effectiveSmallestWidthDp
                + ",record=" + (resolution.record != null ? "yes" : "no") + "}";
    }
}

