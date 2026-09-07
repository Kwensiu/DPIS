package com.dpis.module.runtime.appprocess

import android.content.res.Configuration
import android.content.res.Resources
import android.util.DisplayMetrics
import com.dpis.module.DpisConfigStore
import com.dpis.module.DpisLog
import com.dpis.module.diagnostics.RuntimeEvents
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.hooks.HookRuntimePolicy
import com.dpis.module.runtime.RuntimeHotPathEvidenceSampler
import com.dpis.module.runtime.font.FontScaleOverride
import com.dpis.module.runtime.font.ResourcesFontScheduler
import com.dpis.module.runtime.hookapi.ModernApiCapabilities
import com.dpis.module.runtime.hookapi.ModernApiCapabilitiesResolver
import com.dpis.module.viewport.DensityOverride
import com.dpis.module.viewport.TargetViewportWidthResolver
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportConfigurationScope
import com.dpis.module.viewport.ViewportModePolicy
import com.dpis.module.viewport.ViewportOverride
import com.dpis.module.viewport.ViewportResolvedTarget
import com.dpis.module.viewport.ViewportRuntimeMarkerProbe
import com.dpis.module.viewport.ViewportRuntimeRecord
import com.dpis.module.viewport.ViewportSourceSnapshot
import com.dpis.module.viewport.ViewportTargetResolution
import com.dpis.module.viewport.VirtualDisplayOverride
import com.dpis.module.viewport.VirtualDisplayPlan
import com.dpis.module.viewport.VirtualDisplayState
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.HookBuilder
import io.github.libxposed.api.XposedInterface.Hooker
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.Volatile
import kotlin.math.abs

object ResourcesReadHookInstaller {
    @Volatile
    private var hookInstalled = false

    @Volatile
    private var viewportReadHandlingEnabled = true

    @Volatile
    private var configurationFontOverrideEnabled = true

    @Volatile
    private var metricsTargetFontOverrideEnabled = false
    private val INTERNAL_UPDATE = ThreadLocal<Boolean>()
    private val LAST_MESSAGES = ConcurrentHashMap<String, String>()
    private val HOTPATH_SAMPLER = RuntimeHotPathEvidenceSampler()

    @JvmStatic
    fun resetForHotReload() {
        hookInstalled = false
    }

    @Throws(ReflectiveOperationException::class)
    @JvmStatic
    fun install(
        xposed: XposedInterface,
        packageName: String?,
        store: DpisConfigStore,
        viewportHandlingEnabled: Boolean
    ) {
        install(
            xposed, packageName, store, ResourcesReadHookPolicy(
                viewportHandlingEnabled,
                true,
                false
            ), ModernApiCapabilitiesResolver.fromXposed(xposed)
        )
    }

    @Throws(ReflectiveOperationException::class)
    @JvmStatic
    fun install(
        xposed: XposedInterface,
        packageName: String?,
        store: DpisConfigStore,
        viewportHandlingEnabled: Boolean = true,
        fontConfigurationOverrideEnabled: Boolean = false
    ) {
        install(
            xposed, packageName, store, ResourcesReadHookPolicy(
                viewportHandlingEnabled,
                fontConfigurationOverrideEnabled,
                false
            ), ModernApiCapabilitiesResolver.fromXposed(xposed)
        )
    }

    @Throws(ReflectiveOperationException::class)
    @JvmStatic
    fun install(
        xposed: XposedInterface,
        packageName: String?,
        store: DpisConfigStore,
        policy: ResourcesReadHookPolicy?,
        apiCapabilities: ModernApiCapabilities
    ) {
        install(
            xposed, packageName, store, HookRuntimePolicy.fromStore(store), policy,
            apiCapabilities
        )
    }

    @JvmStatic
    @Throws(ReflectiveOperationException::class)
    fun install(
        xposed: XposedInterface,
        packageName: String?,
        store: DpisConfigStore?,
        runtimePolicy: HookRuntimePolicy?,
        policy: ResourcesReadHookPolicy?,
        apiCapabilities: ModernApiCapabilities
    ) {
        if (hookInstalled) {
            return
        }
        synchronized(ResourcesReadHookInstaller::class.java) {
            if (hookInstalled) {
                return
            }
            val resolvedPolicy = if (policy != null)
                policy
            else
                ResourcesReadHookPolicy.FULL
            viewportReadHandlingEnabled = resolvedPolicy.viewportHandlingEnabled
            configurationFontOverrideEnabled =
                resolvedPolicy.configurationFontOverrideEnabled
            metricsTargetFontOverrideEnabled =
                resolvedPolicy.metricsTargetFontOverrideEnabled
            val bootClassLoader = ClassLoader.getSystemClassLoader()
            val resourcesClass =
                Class.forName("android.content.res.Resources", false, bootClassLoader)

            val getConfigurationMethod = resourcesClass.getDeclaredMethod("getConfiguration")
            apiCapabilities.applyStableHookId<HookBuilder>(
                xposed.hook(getConfigurationMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                "resources_read_get_configuration"
            )
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    val result = chain!!.proceed()
                    if (result !is Configuration) {
                        return@Hooker result
                    }
                    // Skip override work when this getConfiguration call is a
                    // re-entrant read from our own getDisplayMetrics/getSystem
                    // hooks. Those paths run their own explicit overrides and
                    // want the raw system configuration as the event-gate
                    // observation baseline, so re-applying here is redundant
                    // (writes are idempotent) and avoids feeding DPIS output
                    // back in as input on a hot path.
                    if (true == INTERNAL_UPDATE.get()) {
                        return@Hooker result
                    }
                    val thisObject = chain.thisObject
                    applyConfigurationOverride(
                        if (thisObject is Resources) thisObject else null,
                        result, packageName, store,
                        "ResourcesRead(getConfiguration)",
                        null,
                        viewportReadHandlingEnabled,
                        configurationFontOverrideEnabled,
                        runtimePolicy
                    )
                    result
                })
            val getDisplayMetricsMethod = resourcesClass.getDeclaredMethod("getDisplayMetrics")
            apiCapabilities.applyStableHookId<HookBuilder>(
                xposed.hook(getDisplayMetricsMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                "resources_read_get_display_metrics"
            )
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    val result = chain!!.proceed()
                    if (result !is DisplayMetrics) {
                        return@Hooker result
                    }
                    if (true == INTERNAL_UPDATE.get()) {
                        return@Hooker result
                    }
                    val thisObject = chain.thisObject
                    if (thisObject !is Resources) {
                        return@Hooker result
                    }
                    INTERNAL_UPDATE.set(true)
                    try {
                        val config = thisObject.configuration
                        applyMetricsOverride(
                            thisObject,
                            result,
                            config,
                            packageName,
                            store,
                            viewportReadHandlingEnabled,
                            metricsTargetFontOverrideEnabled
                        )
                    } finally {
                        INTERNAL_UPDATE.set(false)
                    }
                    result
                })
            val getSystemMethod = resourcesClass.getDeclaredMethod("getSystem")
            apiCapabilities.applyStableHookId<HookBuilder>(
                xposed.hook(getSystemMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                "resources_read_get_system"
            )
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    val result = chain!!.proceed()
                    if (result !is Resources) {
                        return@Hooker result
                    }
                    // Suppress the getConfiguration/getDisplayMetrics hooks for
                    // the internal reads below; this method applies both
                    // overrides explicitly, so without the guard each read would
                    // re-trigger the other hooks and run override work twice on
                    // a hot path.
                    val reentrant = true == INTERNAL_UPDATE.get()
                    if (!reentrant) {
                        INTERNAL_UPDATE.set(true)
                    }
                    try {
                        val config = result.configuration
                        applyConfigurationOverride(
                            result, config, packageName, store,
                            "ResourcesRead(getSystem)",
                            null,
                            viewportReadHandlingEnabled,
                            configurationFontOverrideEnabled,
                            runtimePolicy
                        )
                        val metrics = result.displayMetrics
                        applyMetricsOverride(
                            result,
                            metrics,
                            config,
                            packageName,
                            store,
                            viewportReadHandlingEnabled,
                            metricsTargetFontOverrideEnabled
                        )
                    } finally {
                        if (!reentrant) {
                            INTERNAL_UPDATE.set(false)
                        }
                    }
                    result
                })
            hookInstalled = true
            DpisLog.i("Resources read hook ready")
            RuntimeEvents.recordHotReload(
                packageName,
                "resources",
                "installed",
                "resources read hook ready"
            )
        }
    }

    @JvmStatic
    fun applyConfigurationOverride(
        config: Configuration?,
        packageName: String?,
        store: DpisConfigStore,
        sourceTag: String
    ) {
        applyConfigurationOverride(null, config, packageName, store, sourceTag)
    }

    @JvmStatic
    fun applyConfigurationOverride(
        resourceScope: Any?,
        config: Configuration?,
        packageName: String?,
        store: DpisConfigStore,
        sourceTag: String
    ) {
        applyConfigurationOverride(
            resourceScope,
            config,
            packageName,
            store,
            sourceTag,
            null,
            true,
            true,
            HookRuntimePolicy.fromStore(store)
        )
    }

    @JvmStatic
    fun applyConfigurationOverrideForTest(
        resourceScope: Any?,
        config: Configuration?,
        packageName: String?,
        store: DpisConfigStore,
        sourceTag: String,
        windowScoped: Boolean
    ) {
        applyConfigurationOverride(
            resourceScope,
            config,
            packageName,
            store,
            sourceTag,
            windowScoped,
            true,
            true,
            HookRuntimePolicy.fromStore(store)
        )
    }

    @JvmStatic
    @JvmOverloads
    fun applyConfigurationOverrideForTest(
        resourceScope: Any?,
        config: Configuration?,
        packageName: String?,
        store: DpisConfigStore?,
        sourceTag: String,
        windowScoped: Boolean,
        viewportHandlingEnabled: Boolean,
        fontConfigurationOverrideEnabled: Boolean = true,
        policy: HookRuntimePolicy? = HookRuntimePolicy.fromStore(store)
    ) {
        applyConfigurationOverride(
            resourceScope,
            config,
            packageName,
            store,
            sourceTag,
            windowScoped,
            viewportHandlingEnabled,
            fontConfigurationOverrideEnabled,
            policy
        )
    }

    private fun applyConfigurationOverride(
        resourceScope: Any?,
        config: Configuration?,
        packageName: String?,
        store: DpisConfigStore?,
        sourceTag: String,
        windowScopedOverride: Boolean?,
        viewportHandlingEnabled: Boolean = true,
        fontConfigurationOverrideEnabled: Boolean = true,
        policy: HookRuntimePolicy? = HookRuntimePolicy.fromStore(store)
    ) {
        var packageName = packageName
        var store = store
        packageName = WebApkRuntimeOwnerBridge.resolveEffectivePackage(store, packageName)
        store = WebApkRuntimeOwnerBridge.resolveEffectiveStore(store, packageName)
        if (config == null) {
            return
        }
        val fontScale = FontScaleOverride.resolveForResources(
            resourceScope,
            store, packageName, config.fontScale
        )
        val fontScaleApplied = fontConfigurationOverrideEnabled
                && FontScaleOverride.applyToConfiguration(config, fontScale)
        if (!viewportHandlingEnabled) {
            if (fontScaleApplied) {
                logIfChanged(
                    packageName + ":" + sourceTag + ":font-only",
                    ("DPIS_FONT " + sourceTag + " override: package=" + packageName
                            + ", fontScale "
                            + fontScale.original + " -> " + config.fontScale)
                )
            }
            return
        }
        if (!ViewportConfigurationScope.isValidDisplayConfiguration(config)) {
            logIfChanged(
                packageName + ":" + sourceTag + ":invalid-config",
                ("DPIS_VIEWPORT " + sourceTag + " skip: invalid display configuration"
                        + ", widthDp=" + config.screenWidthDp
                        + ", heightDp=" + config.screenHeightDp
                        + ", smallestWidthDp=" + config.smallestScreenWidthDp
                        + ", densityDpi=" + config.densityDpi
                        + ", fontScale=" + config.fontScale)
            )
            return
        }

        val originalWidthDp = config.screenWidthDp
        val originalHeightDp = config.screenHeightDp
        val originalSmallestWidthDp = config.smallestScreenWidthDp
        val originalDensityDpi = config.densityDpi

        val source = ViewportSourceSnapshot.fromConfiguration(
            ViewportSourceSnapshot.ORIGIN_RESOURCES_READ, config, null
        )
        val resolution =
            TargetViewportWidthResolver.resolve(store, packageName, source)
        val targetViewportWidth = if (resolution.hasTarget())
            resolution.effectiveSmallestWidthDp
        else
            null
        if (targetViewportWidth != null && resolution.spec.isEnabled) {
            ViewportRuntimeMarkerProbe.observeAppProcessProbe(
                packageName, resolution.spec, sourceTag
            )
        }
        val windowScoped = if (windowScopedOverride != null)
            windowScopedOverride
        else
            ViewportConfigurationScope.isWindowScoped(config)
        if (windowScoped && resolution.isAppProcessBorrowTarget) {
            if (fontScaleApplied) {
                logIfChanged(
                    packageName + ":" + sourceTag + ":window-borrow-font-only",
                    ("DPIS_FONT " + sourceTag + " window borrow: package=" + packageName
                            + ", fontScale "
                            + fontScale.original + " -> " + config.fontScale)
                )
            }
            return
        }
        val stableTarget =
            ViewportResolvedTarget.virtualDisplayResult(resolution, targetViewportWidth)
        val resolvedRecordResult =
            ViewportResolvedTarget.viewportResult(resolution, windowScoped)
        val result = if (resolvedRecordResult != null)
            resolvedRecordResult
        else
            ViewportOverride.derive(
                config,
                if (targetViewportWidth != null) targetViewportWidth else 0,
                windowScoped,
                stableTarget
            )
        if (result == null) {
            if (fontScaleApplied) {
                logIfChanged(
                    packageName + ":" + sourceTag + ":font-only",
                    ("DPIS_FONT " + sourceTag + " override: package=" + packageName
                            + ", fontScale "
                            + fontScale.original + " -> " + config.fontScale)
                )
            }
            return
        }

        val needsViewportUpdate =
            result.widthDp != originalWidthDp || result.heightDp != originalHeightDp || result.smallestWidthDp != originalSmallestWidthDp || (result.densityDpi > 0 && result.densityDpi != originalDensityDpi)
        val applyToConfiguration = ViewportModePolicy.shouldApplyConfigurationOverride(
            policy, store, packageName, resolution, needsViewportUpdate
        )
        if (needsViewportUpdate) {
            if (applyToConfiguration) {
                ViewportOverride.apply(config, result)
            }
        }

        val sharedResult = VirtualDisplayPlan.derivePublishableResult(
            originalWidthDp,
            originalHeightDp,
            originalSmallestWidthDp,
            originalDensityDpi,
            0,
            0,
            result.smallestWidthDp
        )
        if (!windowScoped && !resolution.isAppProcessBorrowTarget) {
            if (resolution.spec.isEnabled) {
                VirtualDisplayState.publish(
                    packageName,
                    resolution.spec,
                    source,
                    result,
                    sharedResult,
                    ViewportRuntimeRecord.PROVENANCE_APP_PROCESS
                )
            } else if (sharedResult != null) {
                VirtualDisplayState.setUnlessDerivedFromTargetConfig(
                    sharedResult, originalSmallestWidthDp, targetViewportWidth
                )
            }
        }

        if (result.widthDp == originalWidthDp && result.heightDp == originalHeightDp && result.smallestWidthDp == originalSmallestWidthDp && (result.densityDpi <= 0 || result.densityDpi == originalDensityDpi)
            && !fontScaleApplied
        ) {
            val stableResult =
                VirtualDisplayState.getStableTargetResult(
                    originalSmallestWidthDp, targetViewportWidth
                )
            if (result.densityDpi <= 0 && stableResult != null && stableResult.densityDpi > 0 && config.densityDpi != stableResult.densityDpi) {
                config.densityDpi = stableResult.densityDpi
                val detail = ("source=" + sourceTag
                        + ", widthDp=" + config.screenWidthDp
                        + ", heightDp=" + config.screenHeightDp
                        + ", smallestWidthDp=" + config.smallestScreenWidthDp
                        + ", densityDpi=" + originalDensityDpi + "->" + config.densityDpi
                        + ", fontScale=" + fontScale.original + "->" + config.fontScale)
                if (logIfChanged(
                        packageName + ":" + sourceTag + ":stable-target",
                        ("DPIS_VIEWPORT " + sourceTag + " stable target: package=" + packageName
                                + ", targetViewportWidthDp=" + describeNullable(targetViewportWidth)
                                + ", widthDp="
                                + config.screenWidthDp
                                + ", heightDp=" + config.screenHeightDp
                                + ", smallestWidthDp=" + config.smallestScreenWidthDp
                                + ", densityDpi " + originalDensityDpi
                                + " -> " + config.densityDpi
                                + ", fontScale " + fontScale.original
                                + " -> " + config.fontScale)
                    )
                ) {
                    RuntimeHotPathEvents.applied(
                        packageName,
                        "viewport",
                        "resources_read_configuration_stable_target",
                        detail
                    )
                }
            }
            return
        }
        val detail = ("source=" + sourceTag
                + ", mode=" + (if (applyToConfiguration) "config" else "metrics")
                + ", scope=" + (if (windowScoped) "window" else "display")
                + ", targetViewportWidthDp=" + describeNullable(targetViewportWidth)
                + ", widthDp=" + originalWidthDp + "->" + config.screenWidthDp
                + ", heightDp=" + originalHeightDp + "->" + config.screenHeightDp
                + ", smallestWidthDp=" + originalSmallestWidthDp + "->"
                + config.smallestScreenWidthDp
                + ", densityDpi=" + originalDensityDpi + "->" + config.densityDpi
                + ", fontScale=" + fontScale.original + "->" + config.fontScale)
        if (logIfChanged(
                packageName + ":" + sourceTag,
                ("DPIS_VIEWPORT " + sourceTag + " override: package=" + packageName
                        + ", targetViewportWidthDp=" + describeNullable(targetViewportWidth)
                        + ", scope=" + (if (windowScoped) "window" else "display")
                        + ", mode="
                        + (if (applyToConfiguration) "config" else "metrics")
                        + ", target=" + describeViewportResult(result)
                        + ", actual=" + describeConfiguration(config)
                        + ", widthDp " + originalWidthDp
                        + " -> " + config.screenWidthDp
                        + ", heightDp " + originalHeightDp + " -> " + config.screenHeightDp
                        + ", smallestWidthDp " + originalSmallestWidthDp + " -> "
                        + config.smallestScreenWidthDp
                        + ", densityDpi " + originalDensityDpi + " -> " + config.densityDpi
                        + ", fontScale " + fontScale.original + " -> " + config.fontScale)
            )
        ) {
            RuntimeHotPathEvents.applied(
                packageName,
                "viewport",
                "resources_read_configuration_override",
                detail
            )
            maybeLogLegacyAutoFallbackSuccess(
                store,
                packageName,
                sourceTag,
                originalWidthDp,
                originalHeightDp,
                originalSmallestWidthDp,
                originalDensityDpi,
                config
            )
        }
    }

    private fun maybeLogLegacyAutoFallbackSuccess(
        store: DpisConfigStore?,
        packageName: String?,
        sourceTag: String?,
        originalWidthDp: Int,
        originalHeightDp: Int,
        originalSmallestWidthDp: Int,
        originalDensityDpi: Int,
        config: Configuration
    ) {
        if (store == null || packageName == null || packageName.isBlank()
            || sourceTag == null || !sourceTag.startsWith("LegacyResourcesRead(") || (ViewportApplyMode.AUTO != ViewportApplyMode.normalize(
                store.getTargetViewportApplyMode(packageName)
            ))
        ) {
            return
        }
        logIfChanged(
            packageName + ":" + sourceTag + ":legacy-auto-fallback",
            ("DPIS_VIEWPORT legacy auto fallback success: package=" + packageName
                    + ", source=" + sourceTag
                    + ", widthDp " + originalWidthDp + " -> " + config.screenWidthDp
                    + ", heightDp " + originalHeightDp + " -> " + config.screenHeightDp
                    + ", smallestWidthDp " + originalSmallestWidthDp + " -> "
                    + config.smallestScreenWidthDp
                    + ", densityDpi " + originalDensityDpi + " -> " + config.densityDpi)
        )
    }

    @JvmStatic
    fun applyMetricsOverride(
        metrics: DisplayMetrics?,
        config: Configuration?,
        packageName: String?
    ) {
        applyMetricsOverride(null, metrics, config, packageName)
    }

    @JvmStatic
    @JvmOverloads
    fun applyMetricsOverride(
        resourceScope: Any?,
        metrics: DisplayMetrics?,
        config: Configuration?,
        packageName: String?,
        store: DpisConfigStore? = null
    ) {
        applyMetricsOverride(
            resourceScope, metrics, config, packageName,
            ViewportConfigurationScope.isWindowScoped(config), store, true, false
        )
    }

    private fun applyMetricsOverride(
        resourceScope: Any?,
        metrics: DisplayMetrics?,
        config: Configuration?,
        packageName: String?,
        store: DpisConfigStore?,
        viewportHandlingEnabled: Boolean,
        metricsTargetFontOverrideEnabled: Boolean
    ) {
        applyMetricsOverride(
            resourceScope, metrics, config, packageName,
            ViewportConfigurationScope.isWindowScoped(config), store, viewportHandlingEnabled,
            metricsTargetFontOverrideEnabled
        )
    }

    @JvmStatic
    fun applyMetricsOverrideForTest(
        resourceScope: Any?,
        metrics: DisplayMetrics?,
        config: Configuration?,
        packageName: String?,
        windowScoped: Boolean
    ) {
        applyMetricsOverride(resourceScope, metrics, config, packageName, windowScoped)
    }

    @JvmStatic
    fun applyMetricsOverrideForTest(
        resourceScope: Any?,
        metrics: DisplayMetrics?,
        config: Configuration?,
        packageName: String?,
        windowScoped: Boolean,
        store: DpisConfigStore?
    ) {
        applyMetricsOverride(
            resourceScope,
            metrics,
            config,
            packageName,
            windowScoped,
            store,
            true,
            false
        )
    }

    @JvmStatic
    fun applyMetricsOverrideForTest(
        resourceScope: Any?,
        metrics: DisplayMetrics?,
        config: Configuration?,
        packageName: String?,
        windowScoped: Boolean,
        store: DpisConfigStore?,
        viewportHandlingEnabled: Boolean
    ) {
        applyMetricsOverride(
            resourceScope,
            metrics,
            config,
            packageName,
            windowScoped,
            store,
            viewportHandlingEnabled,
            false
        )
    }

    @JvmStatic
    fun applyMetricsOverrideForTest(
        resourceScope: Any?,
        metrics: DisplayMetrics?,
        config: Configuration?,
        packageName: String?,
        windowScoped: Boolean,
        store: DpisConfigStore?,
        viewportHandlingEnabled: Boolean,
        metricsTargetFontOverrideEnabled: Boolean
    ) {
        applyMetricsOverride(
            resourceScope,
            metrics,
            config,
            packageName,
            windowScoped,
            store,
            viewportHandlingEnabled,
            metricsTargetFontOverrideEnabled
        )
    }

    private fun applyMetricsOverride(
        resourceScope: Any?,
        metrics: DisplayMetrics?,
        config: Configuration?,
        packageName: String?,
        windowScoped: Boolean,
        store: DpisConfigStore? = null,
        viewportHandlingEnabled: Boolean = true,
        metricsTargetFontOverrideEnabled: Boolean = false
    ) {
        var packageName = packageName
        var store = store
        packageName = WebApkRuntimeOwnerBridge.resolveEffectivePackage(store, packageName)
        store = WebApkRuntimeOwnerBridge.resolveEffectiveStore(store, packageName)
        if (metrics == null || config == null) {
            recordMetricsSkip(
                packageName, "null_input",
                "source=ResourcesRead(getDisplayMetrics), reason=null_input"
            )
            return
        }
        var targetDensityDpi = if (config.densityDpi > 0) config.densityDpi else metrics.densityDpi
        var densitySource = if (config.densityDpi > 0) "configuration" else "metrics"
        if (targetDensityDpi <= 0) {
            recordMetricsSkip(
                packageName, "invalid_target_density",
                "source=ResourcesRead(getDisplayMetrics), reason=invalid_target_density"
            )
            return
        }
        val originalDensityDpi = metrics.densityDpi
        val originalDensity = metrics.density
        val originalScaledDensity = metrics.scaledDensity
        val originalWidthPixels = metrics.widthPixels
        val originalHeightPixels = metrics.heightPixels
        var targetDensity = DensityOverride.densityFromDpi(targetDensityDpi)
        val targetFontFactor = FontScaleOverride.targetFactorForResources(store, packageName)
        val observedFontScale = if (config.fontScale > 0f) config.fontScale else 1.0f
        ResourcesFontScheduler.observeResourcesFontScale(
            resourceScope,
            packageName,
            observedFontScale,
            targetFontFactor
        )
        val fontScale = resolveMetricsFontScale(
            resourceScope,
            packageName,
            observedFontScale,
            targetFontFactor,
            metricsTargetFontOverrideEnabled
        )
        var targetScaledDensity = DensityOverride.scaledDensityFrom(targetDensityDpi, fontScale)
        var metricsChanged =
            originalDensityDpi != targetDensityDpi || abs(originalDensity - targetDensity) > FontScaleOverride.EPSILON || abs(
                originalScaledDensity - targetScaledDensity
            ) > FontScaleOverride.EPSILON
        if (metricsChanged) {
            metrics.densityDpi = targetDensityDpi
            metrics.density = targetDensity
            metrics.scaledDensity = targetScaledDensity
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
                metrics
            )
            if (!metricsChanged) {
                recordMetricsSkip(
                    packageName,
                    "stable_metrics",
                    "source=ResourcesRead(getDisplayMetrics), reason=stable_metrics"
                )
            }
            return
        }
        val localViewportResult =
            resolveLocalMetricsViewportResult(config, packageName, store, windowScoped)
        if (localViewportResult != null && localViewportResult.densityDpi > 0) {
            targetDensityDpi = localViewportResult.densityDpi
            densitySource = "viewport-target"
        }
        val applied = matchingVirtualDisplayState(config, windowScoped)
        if (applied != null && applied.densityDpi > 0) {
            targetDensityDpi = applied.densityDpi
            densitySource = "virtual-display-state"
        }
        targetDensity = DensityOverride.densityFromDpi(targetDensityDpi)
        targetScaledDensity = DensityOverride.scaledDensityFrom(targetDensityDpi, fontScale)
        metricsChanged = metricsChanged
                || originalDensityDpi != targetDensityDpi || abs(originalDensity - targetDensity) > FontScaleOverride.EPSILON || abs(
            originalScaledDensity - targetScaledDensity
        ) > FontScaleOverride.EPSILON
        if (metricsChanged) {
            metrics.densityDpi = targetDensityDpi
            metrics.density = targetDensity
            metrics.scaledDensity = targetScaledDensity
        }
        if (applied != null) {
            if (metrics.widthPixels != applied.widthPx || metrics.heightPixels != applied.heightPx) {
                metrics.widthPixels = applied.widthPx
                metrics.heightPixels = applied.heightPx
                metricsChanged = true
            }
        }
        if (!metricsChanged) {
            recordMetricsSkip(
                packageName,
                "stable_metrics",
                "source=ResourcesRead(getDisplayMetrics), reason=stable_metrics"
            )
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
            metrics
        )
    }

    private fun resolveMetricsFontScale(
        resourceScope: Any?,
        packageName: String?,
        observedFontScale: Float,
        targetFontFactor: Float,
        metricsTargetFontOverrideEnabled: Boolean
    ): Float {
        if (metricsTargetFontOverrideEnabled && targetFontFactor > 0f) {
            return targetFontFactor
        }
        return ResourcesFontScheduler.maybeSuppressMetricsFontScale(
            resourceScope,
            packageName,
            observedFontScale,
            targetFontFactor
        )
    }

    private fun logMetricsIfChanged(
        metricsChanged: Boolean,
        packageName: String?,
        densitySource: String?,
        config: Configuration?,
        localViewportResult: LocalMetricsViewportResult?,
        applied: VirtualDisplayOverride.Result?,
        originalDensityDpi: Int,
        originalDensity: Float,
        originalScaledDensity: Float,
        originalWidthPixels: Int,
        originalHeightPixels: Int,
        metrics: DisplayMetrics
    ) {
        if (!metricsChanged) {
            return
        }
        val detail = ("source=ResourcesRead(getDisplayMetrics)"
                + ", densitySource=" + densitySource
                + ", densityDpi=" + originalDensityDpi + "->" + metrics.densityDpi
                + ", density=" + originalDensity + "->" + metrics.density
                + ", scaledDensity=" + originalScaledDensity + "->" + metrics.scaledDensity
                + ", widthPx=" + originalWidthPixels + "->" + metrics.widthPixels
                + ", heightPx=" + originalHeightPixels + "->" + metrics.heightPixels)
        if (logIfChanged(
                packageName + ":ResourcesRead(getDisplayMetrics)",
                ("DPIS_VIEWPORT ResourcesRead(getDisplayMetrics) override: package=" + packageName
                        + ", densitySource=" + densitySource
                        + ", resolution=" + describeResolution(
                    if (localViewportResult != null) localViewportResult.resolution else null
                )
                        + ", config=" + describeConfiguration(config)
                        + ", localTarget=" + describeLocalMetricsResult(localViewportResult)
                        + ", virtualDisplay=" + describeVirtualDisplayResult(applied)
                        + ", densityDpi "
                        + originalDensityDpi + " -> " + metrics.densityDpi
                        + ", density " + originalDensity + " -> " + metrics.density
                        + ", scaledDensity " + originalScaledDensity + " -> "
                        + metrics.scaledDensity
                        + ", widthPx " + originalWidthPixels + " -> " + metrics.widthPixels
                        + ", heightPx " + originalHeightPixels + " -> " + metrics.heightPixels)
            )
        ) {
            RuntimeHotPathEvents.applied(
                packageName,
                "viewport",
                "resources_read_display_metrics_override",
                detail
            )
        }
    }

    private fun recordMetricsSkip(packageName: String?, reason: String?, detail: String?) {
        val sample =
            HOTPATH_SAMPLER.sample("skip|metrics|" + packageName + "|" + reason, detail)
        if (sample.emit) {
            RuntimeHotPathEvents.skipped(
                packageName,
                "viewport",
                "resources_read_display_metrics_override",
                sample.detail
            )
        }
    }

    @JvmStatic
    fun resetHotPathSamplerForTest() {
        HOTPATH_SAMPLER.resetForTest()
    }

    private fun logFontMetricsIfChanged(
        metricsChanged: Boolean,
        packageName: String?,
        densitySource: String?,
        config: Configuration?,
        originalDensityDpi: Int,
        originalDensity: Float,
        originalScaledDensity: Float,
        originalWidthPixels: Int,
        originalHeightPixels: Int,
        metrics: DisplayMetrics
    ) {
        if (!metricsChanged) {
            return
        }
        logIfChanged(
            packageName + ":ResourcesRead(getDisplayMetrics):font-only",
            ("DPIS_FONT ResourcesRead(getDisplayMetrics) override: package=" + packageName
                    + ", densitySource=" + densitySource
                    + ", config=" + describeConfiguration(config)
                    + ", densityDpi "
                    + originalDensityDpi + " -> " + metrics.densityDpi
                    + ", density " + originalDensity + " -> " + metrics.density
                    + ", scaledDensity " + originalScaledDensity + " -> "
                    + metrics.scaledDensity
                    + ", widthPx " + originalWidthPixels + " -> " + metrics.widthPixels
                    + ", heightPx " + originalHeightPixels + " -> " + metrics.heightPixels)
        )
    }

    private fun matchingVirtualDisplayState(
        config: Configuration,
        windowScoped: Boolean
    ): VirtualDisplayOverride.Result? {
        val current = VirtualDisplayState.get()
        if (windowScoped) {
            return null
        }
        // The shared display state may have been produced by an earlier target.
        // Only reuse it when it describes the same logical viewport.
        if (current == null || config.smallestScreenWidthDp <= 0 || current.smallestWidthDp != config.smallestScreenWidthDp) {
            return null
        }
        return current
    }

    private fun resolveLocalMetricsViewportResult(
        config: Configuration?,
        packageName: String?,
        store: DpisConfigStore?,
        windowScoped: Boolean
    ): LocalMetricsViewportResult? {
        if (store == null || config == null) {
            return null
        }
        val source = ViewportSourceSnapshot.fromConfiguration(
            ViewportSourceSnapshot.ORIGIN_RESOURCES_READ, config, null
        )
        val resolution =
            TargetViewportWidthResolver.resolve(store, packageName, source)
        if (resolution == null || !resolution.isAppProcessBorrowTarget) {
            return null
        }
        val stableTarget =
            ViewportResolvedTarget.virtualDisplayResult(
                resolution, resolution.effectiveSmallestWidthDp
            )
        if (windowScoped) {
            val result = ViewportResolvedTarget.appProcessWindowMetricsResult(
                config,
                resolution,
                resolution.effectiveSmallestWidthDp,
                stableTarget
            )
            if (result != null) {
                return LocalMetricsViewportResult(resolution, result)
            }
        }
        val deriveAsDisplay = windowScoped && stableTarget == null
        val result = ViewportOverride.derive(
            config,
            resolution.effectiveSmallestWidthDp,
            if (deriveAsDisplay) false else windowScoped,
            stableTarget
        )
        return if (result != null) LocalMetricsViewportResult(resolution, result) else null
    }

    private fun logIfChanged(key: String?, message: String): Boolean {
        if (key == null) {
            DpisLog.i(message)
            return true
        }
        val previous = LAST_MESSAGES.put(key, message)
        if (message != previous) {
            DpisLog.i(message)
            return true
        }
        return false
    }

    private fun describeConfiguration(config: Configuration?): String {
        if (config == null) {
            return "null"
        }
        return ("{widthDp=" + config.screenWidthDp
                + ",heightDp=" + config.screenHeightDp
                + ",smallestWidthDp=" + config.smallestScreenWidthDp
                + ",densityDpi=" + config.densityDpi
                + ",fontScale=" + config.fontScale + "}")
    }

    private fun describeViewportResult(result: ViewportOverride.Result?): String {
        if (result == null) {
            return "null"
        }
        return ("{widthDp=" + result.widthDp
                + ",heightDp=" + result.heightDp
                + ",smallestWidthDp=" + result.smallestWidthDp
                + ",densityDpi=" + result.densityDpi + "}")
    }

    private fun describeVirtualDisplayResult(result: VirtualDisplayOverride.Result?): String {
        if (result == null) {
            return "none"
        }
        return ("{widthDp=" + result.widthDp
                + ",heightDp=" + result.heightDp
                + ",smallestWidthDp=" + result.smallestWidthDp
                + ",densityDpi=" + result.densityDpi
                + ",widthPx=" + result.widthPx
                + ",heightPx=" + result.heightPx + "}")
    }

    private fun describeResolution(resolution: ViewportTargetResolution?): String {
        if (resolution == null) {
            return "none"
        }
        return ("{reason=" + resolution.reason
                + ",target=" + resolution.effectiveSmallestWidthDp
                + ",record=" + (if (resolution.record != null) "yes" else "no") + "}")
    }

    private fun describeLocalMetricsResult(result: LocalMetricsViewportResult?): String {
        if (result == null || result.viewportResult == null) {
            return "none"
        }
        return describeViewportResult(result.viewportResult)
    }

    private fun describeNullable(value: Int?): String {
        return if (value == null) "none" else value.toString()
    }

    private class LocalMetricsViewportResult(
        val resolution: ViewportTargetResolution?,
        val viewportResult: ViewportOverride.Result?
    ) {
        val densityDpi: Int

        init {
            this.densityDpi = if (viewportResult != null) viewportResult.densityDpi else 0
        }
    }
}
