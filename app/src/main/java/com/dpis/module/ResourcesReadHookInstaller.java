package com.dpis.module;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.api.XposedInterface;

final class ResourcesReadHookInstaller {
    private static volatile boolean hookInstalled;
    private static volatile boolean viewportReadHandlingEnabled = true;
    private static volatile boolean configurationFontOverrideEnabled = true;
    private static volatile boolean metricsTargetFontOverrideEnabled;
    private static final ThreadLocal<Boolean> INTERNAL_UPDATE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final Map<String, String> LAST_MESSAGES = new ConcurrentHashMap<>();
    private static final RuntimeHotPathEvidenceSampler HOTPATH_SAMPLER =
            new RuntimeHotPathEvidenceSampler();

    private ResourcesReadHookInstaller() {
    }

    static void resetForHotReload() {
        hookInstalled = false;
    }

    static void install(XposedInterface xposed, String packageName, DpiConfigStore store)
            throws ReflectiveOperationException {
        install(xposed, packageName, store, true, false);
    }

    static void install(XposedInterface xposed,
                        String packageName,
                        DpiConfigStore store,
                        boolean viewportHandlingEnabled)
            throws ReflectiveOperationException {
        install(xposed, packageName, store, new ResourcesReadHookPolicy(
                viewportHandlingEnabled,
                true,
                false), null);
    }

    static void install(XposedInterface xposed,
                        String packageName,
                        DpiConfigStore store,
                        boolean viewportHandlingEnabled,
                        boolean fontConfigurationOverrideEnabled)
            throws ReflectiveOperationException {
        install(xposed, packageName, store, new ResourcesReadHookPolicy(
                viewportHandlingEnabled,
                fontConfigurationOverrideEnabled,
                false), null);
    }

    static void install(XposedInterface xposed,
                        String packageName,
                        DpiConfigStore store,
                        ResourcesReadHookPolicy policy,
                        ModernHookRegistry hookRegistry)
            throws ReflectiveOperationException {
        if (hookInstalled) {
            return;
        }
        synchronized (ResourcesReadHookInstaller.class) {
            if (hookInstalled) {
                return;
            }
            ResourcesReadHookPolicy resolvedPolicy = policy != null
                    ? policy
                    : ResourcesReadHookPolicy.FULL;
            viewportReadHandlingEnabled = resolvedPolicy.viewportHandlingEnabled;
            configurationFontOverrideEnabled =
                    resolvedPolicy.configurationFontOverrideEnabled;
            metricsTargetFontOverrideEnabled =
                    resolvedPolicy.metricsTargetFontOverrideEnabled;
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> resourcesClass = Class.forName("android.content.res.Resources", false, bootClassLoader);

            Method getConfigurationMethod = resourcesClass.getDeclaredMethod("getConfiguration");
            XposedInterface.HookHandle configurationHandle = xposed.hook(getConfigurationMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .setId("resources_read_get_configuration")
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (!(result instanceof Configuration configuration)) {
                            return result;
                        }
                        // Skip override work when this getConfiguration call is a
                        // re-entrant read from our own getDisplayMetrics/getSystem
                        // hooks. Those paths run their own explicit overrides and
                        // want the raw system configuration as the event-gate
                        // observation baseline, so re-applying here is redundant
                        // (writes are idempotent) and avoids feeding DPIS output
                        // back in as input on a hot path.
                        if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                            return result;
                        }
                        Object thisObject = chain.getThisObject();
                        applyConfigurationOverride(
                                thisObject instanceof Resources ? thisObject : null,
                                configuration, packageName, store,
                                "ResourcesRead(getConfiguration)",
                                null,
                                viewportReadHandlingEnabled,
                                configurationFontOverrideEnabled);
                        return result;
                    });
            if (hookRegistry != null) {
                hookRegistry.register("resources_read_get_configuration", configurationHandle);
            }

            Method getDisplayMetricsMethod = resourcesClass.getDeclaredMethod("getDisplayMetrics");
            XposedInterface.HookHandle displayMetricsHandle = xposed.hook(getDisplayMetricsMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .setId("resources_read_get_display_metrics")
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (!(result instanceof DisplayMetrics metrics)) {
                            return result;
                        }
                        if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                            return result;
                        }
                        Object thisObject = chain.getThisObject();
                        if (!(thisObject instanceof Resources resources)) {
                            return result;
                        }
                        INTERNAL_UPDATE.set(Boolean.TRUE);
                        try {
                            Configuration config = resources.getConfiguration();
                            applyMetricsOverride(
                                    resources,
                                    metrics,
                                    config,
                                    packageName,
                                    store,
                                    viewportReadHandlingEnabled,
                                    ResourcesReadHookInstaller.metricsTargetFontOverrideEnabled);
                        } finally {
                            INTERNAL_UPDATE.set(Boolean.FALSE);
                        }
                        return result;
                    });
            if (hookRegistry != null) {
                hookRegistry.register("resources_read_get_display_metrics", displayMetricsHandle);
            }

            Method getSystemMethod = resourcesClass.getDeclaredMethod("getSystem");
            XposedInterface.HookHandle systemHandle = xposed.hook(getSystemMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .setId("resources_read_get_system")
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (!(result instanceof Resources resources)) {
                            return result;
                        }
                        // Suppress the getConfiguration/getDisplayMetrics hooks for
                        // the internal reads below; this method applies both
                        // overrides explicitly, so without the guard each read would
                        // re-trigger the other hooks and run override work twice on
                        // a hot path.
                        boolean reentrant = Boolean.TRUE.equals(INTERNAL_UPDATE.get());
                        if (!reentrant) {
                            INTERNAL_UPDATE.set(Boolean.TRUE);
                        }
                        try {
                            Configuration config = resources.getConfiguration();
                            applyConfigurationOverride(resources, config, packageName, store,
                                    "ResourcesRead(getSystem)",
                                    null,
                                    viewportReadHandlingEnabled,
                                    configurationFontOverrideEnabled);
                            DisplayMetrics metrics = resources.getDisplayMetrics();
                            applyMetricsOverride(
                                    resources,
                                    metrics,
                                    config,
                                    packageName,
                                    store,
                                    viewportReadHandlingEnabled,
                                    metricsTargetFontOverrideEnabled);
                        } finally {
                            if (!reentrant) {
                                INTERNAL_UPDATE.set(Boolean.FALSE);
                            }
                        }
                        return result;
                    });
            if (hookRegistry != null) {
                hookRegistry.register("resources_read_get_system", systemHandle);
            }

            hookInstalled = true;
            DpisLog.i("Resources read hook ready");
            FeedbackDiagnosticRuntimeEvents.recordHotReload(
                    packageName,
                    "resources",
                    "installed",
                    "resources read hook ready");
        }
    }

    static void applyConfigurationOverride(Configuration config,
                                           String packageName,
                                           DpiConfigStore store,
                                           String sourceTag) {
        applyConfigurationOverride(null, config, packageName, store, sourceTag);
    }

    static void applyConfigurationOverride(Object resourceScope,
                                           Configuration config,
                                           String packageName,
                                           DpiConfigStore store,
                                           String sourceTag) {
        applyConfigurationOverride(resourceScope, config, packageName, store, sourceTag, null);
    }

    static void applyConfigurationOverrideForTest(Object resourceScope,
                                                  Configuration config,
                                                  String packageName,
                                                  DpiConfigStore store,
                                                  String sourceTag,
                                                  boolean windowScoped) {
        applyConfigurationOverride(
                resourceScope,
                config,
                packageName,
                store,
                sourceTag,
                Boolean.valueOf(windowScoped));
    }

    private static void applyConfigurationOverride(Object resourceScope,
                                                   Configuration config,
                                                   String packageName,
                                                   DpiConfigStore store,
                                                   String sourceTag,
                                                   Boolean windowScopedOverride) {
        applyConfigurationOverride(
                resourceScope,
                config,
                packageName,
                store,
                sourceTag,
                windowScopedOverride,
                true,
                true);
    }

    static void applyConfigurationOverrideForTest(Object resourceScope,
                                                  Configuration config,
                                                  String packageName,
                                                  DpiConfigStore store,
                                                  String sourceTag,
                                                  boolean windowScoped,
                                                  boolean viewportHandlingEnabled) {
        applyConfigurationOverrideForTest(
                resourceScope,
                config,
                packageName,
                store,
                sourceTag,
                windowScoped,
                viewportHandlingEnabled,
                true);
    }

    static void applyConfigurationOverrideForTest(Object resourceScope,
                                                  Configuration config,
                                                  String packageName,
                                                  DpiConfigStore store,
                                                  String sourceTag,
                                                  boolean windowScoped,
                                                  boolean viewportHandlingEnabled,
                                                  boolean fontConfigurationOverrideEnabled) {
        applyConfigurationOverride(
                resourceScope,
                config,
                packageName,
                store,
                sourceTag,
                Boolean.valueOf(windowScoped),
                viewportHandlingEnabled,
                fontConfigurationOverrideEnabled);
    }

    private static void applyConfigurationOverride(Object resourceScope,
                                                   Configuration config,
                                                   String packageName,
                                                   DpiConfigStore store,
                                                   String sourceTag,
                                                   boolean viewportHandlingEnabled) {
        applyConfigurationOverride(
                resourceScope,
                config,
                packageName,
                store,
                sourceTag,
                null,
                viewportHandlingEnabled,
                true);
    }

    private static void applyConfigurationOverride(Object resourceScope,
                                                   Configuration config,
                                                   String packageName,
                                                   DpiConfigStore store,
                                                   String sourceTag,
                                                   Boolean windowScopedOverride,
                                                   boolean viewportHandlingEnabled,
                                                   boolean fontConfigurationOverrideEnabled) {
        if (config == null) {
            return;
        }
        FontScaleOverride.Result fontScale = FontScaleOverride.resolveForResources(resourceScope,
                store, packageName, config.fontScale);
        boolean fontScaleApplied = fontConfigurationOverrideEnabled
                && FontScaleOverride.applyToConfiguration(config, fontScale);
        if (!viewportHandlingEnabled) {
            if (fontScaleApplied) {
                logIfChanged(packageName + ":" + sourceTag + ":font-only",
                        "DPIS_FONT " + sourceTag + " override: package=" + packageName
                                + ", fontScale "
                                + fontScale.original + " -> " + config.fontScale);
            }
            return;
        }

        int originalWidthDp = config.screenWidthDp;
        int originalHeightDp = config.screenHeightDp;
        int originalSmallestWidthDp = config.smallestScreenWidthDp;
        int originalDensityDpi = config.densityDpi;

        ViewportSourceSnapshot source = ViewportSourceSnapshot.fromConfiguration(
                ViewportSourceSnapshot.ORIGIN_RESOURCES_READ, config, null);
        ViewportTargetResolution resolution =
                TargetViewportWidthResolver.resolve(store, packageName, source);
        Integer targetViewportWidth = resolution.hasTarget()
                ? resolution.effectiveSmallestWidthDp
                : null;
        if (targetViewportWidth != null && resolution.spec.isEnabled()) {
            ViewportRuntimeMarkerProbe.observeAppProcessProbe(
                    packageName, resolution.spec, sourceTag);
        }
        boolean windowScoped = windowScopedOverride != null
                ? windowScopedOverride
                : ViewportConfigurationScope.isWindowScoped(config);
        if (windowScoped && resolution.isAppProcessBorrowTarget()) {
            if (fontScaleApplied) {
                logIfChanged(packageName + ":" + sourceTag + ":window-borrow-font-only",
                        "DPIS_FONT " + sourceTag + " window borrow: package=" + packageName
                                + ", fontScale "
                                + fontScale.original + " -> " + config.fontScale);
            }
            return;
        }
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
            if (fontScaleApplied) {
                logIfChanged(packageName + ":" + sourceTag + ":font-only",
                        "DPIS_FONT " + sourceTag + " override: package=" + packageName
                                + ", fontScale "
                                + fontScale.original + " -> " + config.fontScale);
            }
            return;
        }

        boolean needsViewportUpdate = result.widthDp != originalWidthDp
                || result.heightDp != originalHeightDp
                || result.smallestWidthDp != originalSmallestWidthDp
                || (result.densityDpi > 0 && result.densityDpi != originalDensityDpi);
        boolean applyToConfiguration = ViewportModePolicy.shouldApplyConfigurationOverride(
                store, packageName, resolution, needsViewportUpdate);
        if (needsViewportUpdate) {
            if (applyToConfiguration) {
                ViewportOverride.apply(config, result);
            }
        }

        VirtualDisplayOverride.Result sharedResult = VirtualDisplayPlan.derivePublishableResult(
                originalWidthDp,
                originalHeightDp,
                originalSmallestWidthDp,
                originalDensityDpi,
                0,
                0,
                result.smallestWidthDp);
        if (!windowScoped && !resolution.isAppProcessBorrowTarget()) {
            if (resolution.spec.isEnabled()) {
                VirtualDisplayState.publish(
                        packageName,
                        resolution.spec,
                        source,
                        result,
                        sharedResult,
                        ViewportRuntimeRecord.PROVENANCE_APP_PROCESS);
            } else if (sharedResult != null) {
                VirtualDisplayState.setUnlessDerivedFromTargetConfig(
                        sharedResult, originalSmallestWidthDp, targetViewportWidth);
            }
        }

        if (result.widthDp == originalWidthDp
                && result.heightDp == originalHeightDp
                && result.smallestWidthDp == originalSmallestWidthDp
                && (result.densityDpi <= 0 || result.densityDpi == originalDensityDpi)
                && !fontScaleApplied) {
            VirtualDisplayOverride.Result stableResult =
                    VirtualDisplayState.getStableTargetResult(
                            originalSmallestWidthDp, targetViewportWidth);
            if (result.densityDpi <= 0
                    && stableResult != null && stableResult.densityDpi > 0
                    && config.densityDpi != stableResult.densityDpi) {
                config.densityDpi = stableResult.densityDpi;
                String detail = "source=" + sourceTag
                        + ", widthDp=" + config.screenWidthDp
                        + ", heightDp=" + config.screenHeightDp
                        + ", smallestWidthDp=" + config.smallestScreenWidthDp
                        + ", densityDpi=" + originalDensityDpi + "->" + config.densityDpi
                        + ", fontScale=" + fontScale.original + "->" + config.fontScale;
                if (logIfChanged(packageName + ":" + sourceTag + ":stable-target",
                        "DPIS_VIEWPORT " + sourceTag + " stable target: package=" + packageName
                                + ", targetViewportWidthDp=" + describeNullable(targetViewportWidth)
                                + ", widthDp="
                                + config.screenWidthDp
                                + ", heightDp=" + config.screenHeightDp
                                + ", smallestWidthDp=" + config.smallestScreenWidthDp
                                + ", densityDpi " + originalDensityDpi
                                + " -> " + config.densityDpi
                                + ", fontScale " + fontScale.original
                                + " -> " + config.fontScale)) {
                    FeedbackDiagnosticRuntimeHotPathEvents.applied(
                            packageName,
                            "viewport",
                            "resources_read_configuration_stable_target",
                            detail);
                }
            }
            return;
        }
        String detail = "source=" + sourceTag
                + ", mode=" + (applyToConfiguration ? "config" : "metrics")
                + ", scope=" + (windowScoped ? "window" : "display")
                + ", targetViewportWidthDp=" + describeNullable(targetViewportWidth)
                + ", widthDp=" + originalWidthDp + "->" + config.screenWidthDp
                + ", heightDp=" + originalHeightDp + "->" + config.screenHeightDp
                + ", smallestWidthDp=" + originalSmallestWidthDp + "->"
                + config.smallestScreenWidthDp
                + ", densityDpi=" + originalDensityDpi + "->" + config.densityDpi
                + ", fontScale=" + fontScale.original + "->" + config.fontScale;
        if (logIfChanged(packageName + ":" + sourceTag,
                "DPIS_VIEWPORT " + sourceTag + " override: package=" + packageName
                        + ", targetViewportWidthDp=" + describeNullable(targetViewportWidth)
                        + ", scope=" + (windowScoped ? "window" : "display")
                        + ", mode="
                        + (applyToConfiguration ? "config" : "metrics")
                        + ", target=" + describeViewportResult(result)
                        + ", actual=" + describeConfiguration(config)
                        + ", widthDp " + originalWidthDp
                        + " -> " + config.screenWidthDp
                        + ", heightDp " + originalHeightDp + " -> " + config.screenHeightDp
                        + ", smallestWidthDp " + originalSmallestWidthDp + " -> "
                        + config.smallestScreenWidthDp
                        + ", densityDpi " + originalDensityDpi + " -> " + config.densityDpi
                        + ", fontScale " + fontScale.original + " -> " + config.fontScale)) {
            FeedbackDiagnosticRuntimeHotPathEvents.applied(
                    packageName,
                    "viewport",
                    "resources_read_configuration_override",
                    detail);
        }
    }

    static void applyMetricsOverride(DisplayMetrics metrics,
                                     Configuration config,
                                     String packageName) {
        applyMetricsOverride(null, metrics, config, packageName);
    }

    static void applyMetricsOverride(Object resourceScope,
                                     DisplayMetrics metrics,
                                     Configuration config,
                                     String packageName) {
        applyMetricsOverride(resourceScope, metrics, config, packageName, null);
    }

    static void applyMetricsOverride(Object resourceScope,
                                     DisplayMetrics metrics,
                                     Configuration config,
                                     String packageName,
                                     DpiConfigStore store) {
        applyMetricsOverride(resourceScope, metrics, config, packageName,
                ViewportConfigurationScope.isWindowScoped(config), store, true, false);
    }

    private static void applyMetricsOverride(Object resourceScope,
                                             DisplayMetrics metrics,
                                             Configuration config,
                                             String packageName,
                                             DpiConfigStore store,
                                             boolean viewportHandlingEnabled) {
        applyMetricsOverride(resourceScope, metrics, config, packageName,
                ViewportConfigurationScope.isWindowScoped(config), store, viewportHandlingEnabled,
                false);
    }

    private static void applyMetricsOverride(Object resourceScope,
                                             DisplayMetrics metrics,
                                             Configuration config,
                                             String packageName,
                                             DpiConfigStore store,
                                             boolean viewportHandlingEnabled,
                                             boolean metricsTargetFontOverrideEnabled) {
        applyMetricsOverride(resourceScope, metrics, config, packageName,
                ViewportConfigurationScope.isWindowScoped(config), store, viewportHandlingEnabled,
                metricsTargetFontOverrideEnabled);
    }

    static void applyMetricsOverrideForTest(Object resourceScope,
                                            DisplayMetrics metrics,
                                            Configuration config,
                                            String packageName,
                                            boolean windowScoped) {
        applyMetricsOverride(resourceScope, metrics, config, packageName, windowScoped);
    }

    static void applyMetricsOverrideForTest(Object resourceScope,
                                            DisplayMetrics metrics,
                                            Configuration config,
                                            String packageName,
                                            boolean windowScoped,
                                            DpiConfigStore store) {
        applyMetricsOverride(
                resourceScope,
                metrics,
                config,
                packageName,
                windowScoped,
                store,
                true,
                false);
    }

    static void applyMetricsOverrideForTest(Object resourceScope,
                                            DisplayMetrics metrics,
                                            Configuration config,
                                            String packageName,
                                            boolean windowScoped,
                                            DpiConfigStore store,
                                            boolean viewportHandlingEnabled) {
        applyMetricsOverride(
                resourceScope,
                metrics,
                config,
                packageName,
                windowScoped,
                store,
                viewportHandlingEnabled,
                false);
    }

    static void applyMetricsOverrideForTest(Object resourceScope,
                                            DisplayMetrics metrics,
                                            Configuration config,
                                            String packageName,
                                            boolean windowScoped,
                                            DpiConfigStore store,
                                            boolean viewportHandlingEnabled,
                                            boolean metricsTargetFontOverrideEnabled) {
        applyMetricsOverride(
                resourceScope,
                metrics,
                config,
                packageName,
                windowScoped,
                store,
                viewportHandlingEnabled,
                metricsTargetFontOverrideEnabled);
    }

    private static void applyMetricsOverride(Object resourceScope,
                                             DisplayMetrics metrics,
                                             Configuration config,
                                             String packageName,
                                             boolean windowScoped) {
        applyMetricsOverride(resourceScope, metrics, config, packageName, windowScoped, null);
    }

    private static void applyMetricsOverride(Object resourceScope,
                                             DisplayMetrics metrics,
                                             Configuration config,
                                             String packageName,
                                             boolean windowScoped,
                                             DpiConfigStore store) {
        applyMetricsOverride(
                resourceScope,
                metrics,
                config,
                packageName,
                windowScoped,
                store,
                true,
                false);
    }

    private static void applyMetricsOverride(Object resourceScope,
                                             DisplayMetrics metrics,
                                             Configuration config,
                                             String packageName,
                                             boolean windowScoped,
                                             DpiConfigStore store,
                                             boolean viewportHandlingEnabled,
                                             boolean metricsTargetFontOverrideEnabled) {
        if (metrics == null || config == null) {
            recordMetricsSkip(packageName, "null_input",
                    "source=ResourcesRead(getDisplayMetrics), reason=null_input");
            return;
        }
        int targetDensityDpi = config.densityDpi > 0 ? config.densityDpi : metrics.densityDpi;
        String densitySource = config.densityDpi > 0 ? "configuration" : "metrics";
        if (targetDensityDpi <= 0) {
            recordMetricsSkip(packageName, "invalid_target_density",
                    "source=ResourcesRead(getDisplayMetrics), reason=invalid_target_density");
            return;
        }
        int originalDensityDpi = metrics.densityDpi;
        float originalDensity = metrics.density;
        float originalScaledDensity = metrics.scaledDensity;
        int originalWidthPixels = metrics.widthPixels;
        int originalHeightPixels = metrics.heightPixels;
        float targetDensity = DensityOverride.densityFromDpi(targetDensityDpi);
        float targetFontFactor = FontScaleOverride.targetFactorForResources(store, packageName);
        float observedFontScale = config.fontScale > 0f ? config.fontScale : 1.0f;
        ResourcesFontScheduler.observeResourcesFontScale(
                resourceScope,
                packageName,
                observedFontScale,
                targetFontFactor);
        float fontScale = resolveMetricsFontScale(
                resourceScope,
                packageName,
                observedFontScale,
                targetFontFactor,
                metricsTargetFontOverrideEnabled);
        float targetScaledDensity = DensityOverride.scaledDensityFrom(targetDensityDpi, fontScale);
        boolean metricsChanged = originalDensityDpi != targetDensityDpi
                || Math.abs(originalDensity - targetDensity) > FontScaleOverride.EPSILON
                || Math.abs(originalScaledDensity - targetScaledDensity) > FontScaleOverride.EPSILON;
        if (metricsChanged) {
            metrics.densityDpi = targetDensityDpi;
            metrics.density = targetDensity;
            metrics.scaledDensity = targetScaledDensity;
        }
        if (!viewportHandlingEnabled) {
            logFontMetricsIfChanged(
                    metricsChanged,
                    packageName,
                    densitySource,
                    config,
                    originalDensityDpi,
                    originalDensity,
                    originalScaledDensity,
                    originalWidthPixels,
                    originalHeightPixels,
                    metrics);
            if (!metricsChanged) {
                recordMetricsSkip(packageName,
                        "stable_metrics",
                        "source=ResourcesRead(getDisplayMetrics), reason=stable_metrics");
            }
            return;
        }
        LocalMetricsViewportResult localViewportResult =
                resolveLocalMetricsViewportResult(config, packageName, store, windowScoped);
        if (localViewportResult != null && localViewportResult.densityDpi > 0) {
            targetDensityDpi = localViewportResult.densityDpi;
            densitySource = "viewport-target";
        }
        VirtualDisplayOverride.Result applied = matchingVirtualDisplayState(config, windowScoped);
        if (applied != null && applied.densityDpi > 0) {
            targetDensityDpi = applied.densityDpi;
            densitySource = "virtual-display-state";
        }
        targetDensity = DensityOverride.densityFromDpi(targetDensityDpi);
        targetScaledDensity = DensityOverride.scaledDensityFrom(targetDensityDpi, fontScale);
        metricsChanged = metricsChanged
                || originalDensityDpi != targetDensityDpi
                || Math.abs(originalDensity - targetDensity) > FontScaleOverride.EPSILON
                || Math.abs(originalScaledDensity - targetScaledDensity) > FontScaleOverride.EPSILON;
        if (metricsChanged) {
            metrics.densityDpi = targetDensityDpi;
            metrics.density = targetDensity;
            metrics.scaledDensity = targetScaledDensity;
        }
        if (applied != null) {
            if (metrics.widthPixels != applied.widthPx || metrics.heightPixels != applied.heightPx) {
                metrics.widthPixels = applied.widthPx;
                metrics.heightPixels = applied.heightPx;
                metricsChanged = true;
            }
        }
        if (!metricsChanged) {
            recordMetricsSkip(packageName,
                    "stable_metrics",
                    "source=ResourcesRead(getDisplayMetrics), reason=stable_metrics");
        }

        logMetricsIfChanged(
                metricsChanged,
                packageName,
                densitySource,
                config,
                localViewportResult,
                applied,
                originalDensityDpi,
                originalDensity,
                originalScaledDensity,
                originalWidthPixels,
                originalHeightPixels,
                metrics);
    }

    private static float resolveMetricsFontScale(Object resourceScope,
                                                 String packageName,
                                                 float observedFontScale,
                                                 float targetFontFactor,
                                                 boolean metricsTargetFontOverrideEnabled) {
        if (metricsTargetFontOverrideEnabled && targetFontFactor > 0f) {
            return targetFontFactor;
        }
        return ResourcesFontScheduler.maybeSuppressMetricsFontScale(
                resourceScope,
                packageName,
                observedFontScale,
                targetFontFactor);
    }

    private static void logMetricsIfChanged(boolean metricsChanged,
                                            String packageName,
                                            String densitySource,
                                            Configuration config,
                                            LocalMetricsViewportResult localViewportResult,
                                            VirtualDisplayOverride.Result applied,
                                            int originalDensityDpi,
                                            float originalDensity,
                                            float originalScaledDensity,
                                            int originalWidthPixels,
                                            int originalHeightPixels,
                                            DisplayMetrics metrics) {
        if (!metricsChanged) {
            return;
        }
        String detail = "source=ResourcesRead(getDisplayMetrics)"
                + ", densitySource=" + densitySource
                + ", densityDpi=" + originalDensityDpi + "->" + metrics.densityDpi
                + ", density=" + originalDensity + "->" + metrics.density
                + ", scaledDensity=" + originalScaledDensity + "->" + metrics.scaledDensity
                + ", widthPx=" + originalWidthPixels + "->" + metrics.widthPixels
                + ", heightPx=" + originalHeightPixels + "->" + metrics.heightPixels;
        if (logIfChanged(packageName + ":ResourcesRead(getDisplayMetrics)",
                "DPIS_VIEWPORT ResourcesRead(getDisplayMetrics) override: package=" + packageName
                        + ", densitySource=" + densitySource
                        + ", resolution=" + describeResolution(
                        localViewportResult != null ? localViewportResult.resolution : null)
                        + ", config=" + describeConfiguration(config)
                        + ", localTarget=" + describeLocalMetricsResult(localViewportResult)
                        + ", virtualDisplay=" + describeVirtualDisplayResult(applied)
                        + ", densityDpi "
                        + originalDensityDpi + " -> " + metrics.densityDpi
                        + ", density " + originalDensity + " -> " + metrics.density
                        + ", scaledDensity " + originalScaledDensity + " -> "
                        + metrics.scaledDensity
                        + ", widthPx " + originalWidthPixels + " -> " + metrics.widthPixels
                        + ", heightPx " + originalHeightPixels + " -> " + metrics.heightPixels)) {
            FeedbackDiagnosticRuntimeHotPathEvents.applied(
                    packageName,
                    "viewport",
                    "resources_read_display_metrics_override",
                    detail);
        }
    }

    private static void recordMetricsSkip(String packageName, String reason, String detail) {
        RuntimeHotPathEvidenceSampler.Sample sample =
                HOTPATH_SAMPLER.sample("skip|metrics|" + packageName + "|" + reason, detail);
        if (sample.emit) {
            FeedbackDiagnosticRuntimeHotPathEvents.skipped(
                    packageName,
                    "viewport",
                    "resources_read_display_metrics_override",
                    sample.detail);
        }
    }

    static void resetHotPathSamplerForTest() {
        HOTPATH_SAMPLER.resetForTest();
    }

    private static void logFontMetricsIfChanged(boolean metricsChanged,
                                                String packageName,
                                                String densitySource,
                                                Configuration config,
                                                int originalDensityDpi,
                                                float originalDensity,
                                                float originalScaledDensity,
                                                int originalWidthPixels,
                                                int originalHeightPixels,
                                                DisplayMetrics metrics) {
        if (!metricsChanged) {
            return;
        }
        logIfChanged(packageName + ":ResourcesRead(getDisplayMetrics):font-only",
                "DPIS_FONT ResourcesRead(getDisplayMetrics) override: package=" + packageName
                        + ", densitySource=" + densitySource
                        + ", config=" + describeConfiguration(config)
                        + ", densityDpi "
                        + originalDensityDpi + " -> " + metrics.densityDpi
                        + ", density " + originalDensity + " -> " + metrics.density
                        + ", scaledDensity " + originalScaledDensity + " -> "
                        + metrics.scaledDensity
                        + ", widthPx " + originalWidthPixels + " -> " + metrics.widthPixels
                        + ", heightPx " + originalHeightPixels + " -> " + metrics.heightPixels);
    }

    private static VirtualDisplayOverride.Result matchingVirtualDisplayState(Configuration config,
                                                                           boolean windowScoped) {
        VirtualDisplayOverride.Result current = VirtualDisplayState.get();
        if (windowScoped) {
            return null;
        }
        // The shared display state may have been produced by an earlier target.
        // Only reuse it when it describes the same logical viewport.
        if (current == null
                || config.smallestScreenWidthDp <= 0
                || current.smallestWidthDp != config.smallestScreenWidthDp) {
            return null;
        }
        return current;
    }

    private static LocalMetricsViewportResult resolveLocalMetricsViewportResult(
            Configuration config,
            String packageName,
            DpiConfigStore store,
            boolean windowScoped) {
        if (store == null || config == null) {
            return null;
        }
        ViewportSourceSnapshot source = ViewportSourceSnapshot.fromConfiguration(
                ViewportSourceSnapshot.ORIGIN_RESOURCES_READ, config, null);
        ViewportTargetResolution resolution =
                TargetViewportWidthResolver.resolve(store, packageName, source);
        if (resolution == null || !resolution.isAppProcessBorrowTarget()) {
            return null;
        }
        VirtualDisplayOverride.Result stableTarget =
                ViewportResolvedTarget.virtualDisplayResult(
                        resolution, resolution.effectiveSmallestWidthDp);
        if (windowScoped) {
            ViewportOverride.Result result = ViewportResolvedTarget.appProcessWindowMetricsResult(
                    config,
                    resolution,
                    resolution.effectiveSmallestWidthDp,
                    stableTarget);
            if (result != null) {
                return new LocalMetricsViewportResult(resolution, result);
            }
        }
        boolean deriveAsDisplay = windowScoped && stableTarget == null;
        ViewportOverride.Result result = ViewportOverride.derive(
                config,
                resolution.effectiveSmallestWidthDp,
                deriveAsDisplay ? false : windowScoped,
                stableTarget);
        return result != null ? new LocalMetricsViewportResult(resolution, result) : null;
    }

    private static boolean logIfChanged(String key, String message) {
        String previous = LAST_MESSAGES.put(key, message);
        if (!message.equals(previous)) {
            DpisLog.i(message);
            return true;
        }
        return false;
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

    private static String describeVirtualDisplayResult(VirtualDisplayOverride.Result result) {
        if (result == null) {
            return "none";
        }
        return "{widthDp=" + result.widthDp
                + ",heightDp=" + result.heightDp
                + ",smallestWidthDp=" + result.smallestWidthDp
                + ",densityDpi=" + result.densityDpi
                + ",widthPx=" + result.widthPx
                + ",heightPx=" + result.heightPx + "}";
    }

    private static String describeResolution(ViewportTargetResolution resolution) {
        if (resolution == null) {
            return "none";
        }
        return "{reason=" + resolution.reason
                + ",target=" + resolution.effectiveSmallestWidthDp
                + ",record=" + (resolution.record != null ? "yes" : "no") + "}";
    }

    private static String describeLocalMetricsResult(LocalMetricsViewportResult result) {
        if (result == null || result.viewportResult == null) {
            return "none";
        }
        return describeViewportResult(result.viewportResult);
    }

    private static String describeNullable(Integer value) {
        return value == null ? "none" : String.valueOf(value);
    }

    private static final class LocalMetricsViewportResult {
        final ViewportTargetResolution resolution;
        final ViewportOverride.Result viewportResult;
        final int densityDpi;

        LocalMetricsViewportResult(ViewportTargetResolution resolution,
                                   ViewportOverride.Result viewportResult) {
            this.resolution = resolution;
            this.viewportResult = viewportResult;
            this.densityDpi = viewportResult != null ? viewportResult.densityDpi : 0;
        }
    }
}
