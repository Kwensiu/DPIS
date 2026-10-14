package com.dpis.module.runtime.resources


import android.content.res.Configuration
import android.util.DisplayMetrics
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.runtime.font.FontScaleOverride
import com.dpis.module.runtime.font.ResourcesFontScheduler
import com.dpis.module.runtime.webapk.WebApkRuntimeOwnerBridge
import com.dpis.module.viewport.DensityOverride
import com.dpis.module.viewport.ResourcesMetricsReadReuse
import com.dpis.module.viewport.TargetViewportWidthResolver
import com.dpis.module.viewport.ViewportOverride
import com.dpis.module.viewport.ViewportResolvedTarget
import com.dpis.module.viewport.ViewportSourceSnapshot
import com.dpis.module.viewport.ViewportTargetResolution
import com.dpis.module.viewport.VirtualDisplayOverride
import com.dpis.module.viewport.VirtualDisplayReuse
import com.dpis.module.viewport.VirtualDisplayState
import com.dpis.module.viewport.window.WindowBoundsState
import kotlin.math.abs

/** DisplayMetrics writes for the Resources read hooks. */
internal object ResourcesMetricsRead {
    fun apply(
        resourceScope: Any?,
        metrics: DisplayMetrics?,
        config: Configuration?,
        packageName: String?,
        windowScoped: Boolean,
        store: DpisConfigStore? = null,
        viewportHandlingEnabled: Boolean = true,
        metricsTargetFontOverrideEnabled: Boolean = false,
    ) {
        var packageName = packageName
        var store = store
        packageName = WebApkRuntimeOwnerBridge.resolveEffectivePackage(store, packageName)
        store = WebApkRuntimeOwnerBridge.resolveEffectiveStore(store, packageName)
        if (metrics == null || config == null) {
            ResourcesReadHookInstaller.recordMetricsSkip(
                packageName, "null_input",
                "source=ResourcesRead(getDisplayMetrics), reason=null_input"
            )
            return
        }
        var targetDensityDpi = if (config.densityDpi > 0) config.densityDpi else metrics.densityDpi
        var densitySource = if (config.densityDpi > 0) "configuration" else "metrics"
        if (targetDensityDpi <= 0) {
            ResourcesReadHookInstaller.recordMetricsSkip(
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
        if (windowScoped) {
            WindowBoundsState.pixelsForCallback(
                packageName, originalWidthPixels, originalHeightPixels,
            )
        }
        val observedWindowBounds = if (windowScoped)
            WindowBoundsState.currentWindowBounds(packageName)
        else
            null
        val metricsMatchObservedWindow = observedWindowBounds == null
                || metrics.widthPixels == observedWindowBounds.width
                && metrics.heightPixels == observedWindowBounds.height
        if (metricsMatchObservedWindow && reuseResolvedMetrics(
                resourceScope,
                packageName,
                metrics
            )
        ) {
            return
        }
        var targetDensity = DensityOverride.densityFromDpi(targetDensityDpi)
        val targetFontFactor = FontScaleOverride.targetFactorForResources(store, packageName)
        val display = VirtualDisplayState.get()
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
                ResourcesReadHookInstaller.recordMetricsSkip(
                    packageName,
                    "stable_metrics",
                    "source=ResourcesRead(getDisplayMetrics), reason=stable_metrics"
                )
            }
            rememberResolvedMetrics(resourceScope, metrics)
            return
        }
        val effectiveWindowScoped = windowScoped &&
                !differsFromRecordedWindow(
                    packageName,
                    originalWidthPixels,
                    originalHeightPixels,
                )
        val localViewportResult =
            resolveLocalMetricsViewportResult(config, packageName, store, effectiveWindowScoped)
        if (localViewportResult != null && localViewportResult.densityDpi > 0) {
            targetDensityDpi = localViewportResult.densityDpi
            densitySource = "viewport-target"
        }
        val applied = VirtualDisplayReuse.forMetrics(
            packageName,
            config,
            effectiveWindowScoped,
            originalWidthPixels,
            originalHeightPixels,
        )
        if (applied != null && applied.densityDpi > 0) {
            targetDensityDpi = applied.densityDpi
            densitySource = "virtual-display-state"
        }
        val viewportDensityKnown =
            (localViewportResult != null && localViewportResult.densityDpi > 0) ||
                    (applied != null && applied.densityDpi > 0)
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
            // A still-physical configuration must not reset the process bitmap
            // density. Only a resolved viewport density may replace it.
            if (viewportDensityKnown) {
                DensityOverride.alignProcessBitmapDensity(targetDensityDpi)
            }
        }
        if (applied != null) {
            if (metrics.widthPixels != applied.widthPx || metrics.heightPixels != applied.heightPx) {
                metrics.widthPixels = applied.widthPx
                metrics.heightPixels = applied.heightPx
                metricsChanged = true
            }
        }
        if (effectiveWindowScoped && applied == null &&
            !WindowBoundsState.matchesDisplayPixels(originalWidthPixels, originalHeightPixels)
        ) {
            WindowBoundsState.currentWindowBounds(packageName)?.let { bounds ->
                if (metrics.widthPixels != bounds.width || metrics.heightPixels != bounds.height) {
                    metrics.widthPixels = bounds.width
                    metrics.heightPixels = bounds.height
                    metricsChanged = true
                }
            }
        }
        if (!metricsChanged) {
            ResourcesReadHookInstaller.recordMetricsSkip(
                packageName,
                "stable_metrics",
                "source=ResourcesRead(getDisplayMetrics), reason=stable_metrics"
            )
        }
        rememberResolvedMetrics(resourceScope, metrics)

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

    /** Display pixels differ from the recorded window, so they stay on the display path. */
    private fun differsFromRecordedWindow(
        packageName: String?,
        widthPixels: Int,
        heightPixels: Int,
    ): Boolean {
        val window = WindowBoundsState.currentWindowBounds(packageName) ?: return false
        return abs(widthPixels - window.width) > 1 || abs(heightPixels - window.height) > 1
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
        if (ResourcesReadHookInstaller.logIfChanged(
                packageName + ":ResourcesRead(getDisplayMetrics)",
                ("DPIS_VIEWPORT ResourcesRead(getDisplayMetrics) override: package=" + packageName
                        + ", densitySource=" + densitySource
                        + ", resolution=" + describeResolution(
                    if (localViewportResult != null) localViewportResult.resolution else null
                )
                        + ", config=" + ResourcesReadHookInstaller.describeConfiguration(config)
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

    private fun reuseResolvedMetrics(
        resourceScope: Any?,
        packageName: String?,
        metrics: DisplayMetrics,
    ): Boolean {
        if (!ResourcesMetricsReadReuse.matchesDisplayMetrics(
                resourceScope,
                metrics.densityDpi,
                metrics.density.toBits(),
                metrics.scaledDensity.toBits(),
                metrics.widthPixels,
                metrics.heightPixels,
                System.nanoTime(),
            )
        ) {
            return false
        }
        ResourcesReadHookInstaller.recordMetricsSkip(
            packageName,
            "reused_stable_metrics",
            "source=ResourcesRead(getDisplayMetrics), reason=reused_stable_metrics",
        )
        return true
    }

    private fun rememberResolvedMetrics(resourceScope: Any?, metrics: DisplayMetrics) {
        ResourcesMetricsReadReuse.remember(
            resourceScope,
            metrics.densityDpi,
            metrics.density.toBits(),
            metrics.scaledDensity.toBits(),
            metrics.widthPixels,
            metrics.heightPixels,
            System.nanoTime(),
        )
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
        ResourcesReadHookInstaller.logIfChanged(
            packageName + ":ResourcesRead(getDisplayMetrics):font-only",
            ("DPIS_FONT ResourcesRead(getDisplayMetrics) override: package=" + packageName
                    + ", densitySource=" + densitySource
                    + ", config=" + ResourcesReadHookInstaller.describeConfiguration(config)
                    + ", densityDpi "
                    + originalDensityDpi + " -> " + metrics.densityDpi
                    + ", density " + originalDensity + " -> " + metrics.density
                    + ", scaledDensity " + originalScaledDensity + " -> "
                    + metrics.scaledDensity
                    + ", widthPx " + originalWidthPixels + " -> " + metrics.widthPixels
                    + ", heightPx " + originalHeightPixels + " -> " + metrics.heightPixels)
        )
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
            val physicalBounds = WindowBoundsState.currentWindowBounds(packageName)
            if (physicalBounds != null) {
                val physicalResult = ViewportOverride.deriveWindowScoped(
                    config,
                    resolution.effectiveSmallestWidthDp,
                    stableTarget,
                    physicalBounds.width,
                    physicalBounds.height,
                )
                return LocalMetricsViewportResult(resolution, physicalResult)
            }
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
        return ResourcesReadHookInstaller.describeViewportResult(result.viewportResult)
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
