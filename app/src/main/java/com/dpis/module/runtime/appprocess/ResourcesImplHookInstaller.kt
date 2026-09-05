package com.dpis.module.runtime.appprocess

import android.content.res.Configuration
import android.util.DisplayMetrics
import com.dpis.module.DpisConfigStore
import com.dpis.module.DpisLog
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.hooks.HookRuntimePolicy
import com.dpis.module.runtime.font.FontScaleOverride
import com.dpis.module.runtime.hookapi.ModernApiCapabilities
import com.dpis.module.viewport.DensityOverride
import com.dpis.module.viewport.TargetViewportWidthResolver
import com.dpis.module.viewport.ViewportConfigurationScope
import com.dpis.module.viewport.ViewportDebugReporter
import com.dpis.module.viewport.ViewportModePolicy
import com.dpis.module.viewport.ViewportOverride
import com.dpis.module.viewport.ViewportResolvedTarget
import com.dpis.module.viewport.ViewportRuntimeMarkerProbe
import com.dpis.module.viewport.ViewportRuntimeRecord
import com.dpis.module.viewport.ViewportSourceSnapshot
import com.dpis.module.viewport.ViewportTargetResolution
import com.dpis.module.viewport.VirtualDisplayPlan
import com.dpis.module.viewport.VirtualDisplayState
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.HookBuilder
import io.github.libxposed.api.XposedInterface.Hooker
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.Volatile
import kotlin.math.abs

object ResourcesImplHookInstaller {
    @Volatile
    private var hookInstalled = false
    private val LAST_MESSAGES = ConcurrentHashMap<String, String>()

    @JvmStatic
    fun resetForHotReload() {
        hookInstalled = false
    }

    @JvmStatic
    @Throws(ReflectiveOperationException::class)
    fun install(
        xposed: XposedInterface,
        packageName: String?,
        store: DpisConfigStore?,
        policy: HookRuntimePolicy?,
        apiCapabilities: ModernApiCapabilities
    ) {
        if (hookInstalled) {
            return
        }
        synchronized(ResourcesImplHookInstaller::class.java) {
            if (hookInstalled) {
                return
            }
            val bootClassLoader = ClassLoader.getSystemClassLoader()
            val resourcesImplClass = Class.forName(
                "android.content.res.ResourcesImpl", false, bootClassLoader
            )
            val compatibilityInfoClass = Class.forName(
                "android.content.res.CompatibilityInfo", false, bootClassLoader
            )
            val method = resourcesImplClass.getDeclaredMethod(
                "updateConfiguration", Configuration::class.java, DisplayMetrics::class.java,
                compatibilityInfoClass
            )
            apiCapabilities.applyStableHookId<HookBuilder>(
                xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                "resources_impl_update_configuration"
            )
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    val config = chain!!.getArg(0) as Configuration?
                    val metrics = chain.getArg(1) as DisplayMetrics?
                    applyDensityOverride(packageName, config, metrics, store, policy)
                    chain.proceed()
                })
            hookInstalled = true
            DpisLog.i("ResourcesImpl hook ready")
        }
    }

    @JvmStatic
    @JvmOverloads
    fun applyDensityOverride(
        packageName: String?,
        config: Configuration?,
        metrics: DisplayMetrics?,
        store: DpisConfigStore?,
        policy: HookRuntimePolicy? = null
    ) {
        applyDensityOverride(packageName, config, metrics, store, policy, null)
    }

    @JvmStatic
    fun applyDensityOverrideForTest(
        packageName: String?,
        config: Configuration?,
        metrics: DisplayMetrics?,
        store: DpisConfigStore?,
        windowScoped: Boolean
    ) {
        applyDensityOverride(packageName, config, metrics, store, null, windowScoped)
    }

    private fun applyDensityOverride(
        packageName: String?,
        config: Configuration?,
        metrics: DisplayMetrics?,
        store: DpisConfigStore?,
        policy: HookRuntimePolicy?,
        windowScopedOverride: Boolean?
    ) {
        var packageName = packageName
        var store = store
        packageName = WebApkRuntimeOwnerBridge.resolveEffectivePackage(store, packageName)
        store = WebApkRuntimeOwnerBridge.resolveEffectiveStore(store, packageName)
        if (config == null) {
            logIfChanged(packageName + ":skip", "ResourcesImpl skip: config is null")
            return
        }
        val fontScale = FontScaleOverride.resolveForResources(
            store, packageName, config.fontScale
        )
        val fontScaleApplied = FontScaleOverride.applyToConfiguration(config, fontScale)
        if (!ViewportConfigurationScope.isValidDisplayConfiguration(config)) {
            logIfChanged(
                packageName + ":invalid-config",
                ("ResourcesImpl viewport skip: invalid display configuration"
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
            ViewportSourceSnapshot.ORIGIN_RESOURCES_IMPL, config, metrics
        )
        val resolution =
            TargetViewportWidthResolver.resolve(store, packageName, source)
        val targetViewportWidth = if (resolution.hasTarget())
            resolution.effectiveSmallestWidthDp
        else
            null
        if (targetViewportWidth != null && resolution.spec.isEnabled()) {
            ViewportRuntimeMarkerProbe.observeAppProcessProbe(
                packageName, resolution.spec, "ResourcesImpl"
            )
        }
        val windowScoped = if (windowScopedOverride != null)
            windowScopedOverride
        else
            ViewportConfigurationScope.isWindowScoped(config)
        val sourceWidthPx = if (metrics != null) metrics.widthPixels else 0
        val sourceHeightPx = if (metrics != null) metrics.heightPixels else 0
        val stableTarget =
            ViewportResolvedTarget.virtualDisplayResult(resolution, targetViewportWidth)
        val stableTargetForResult =
            if (stableTarget != null && targetViewportWidth != null && stableTarget.smallestWidthDp == targetViewportWidth && originalDensityDpi != stableTarget.densityDpi)
                stableTarget
            else
                null
        val pixelDerivedTarget =
            if (!windowScoped && stableTargetForResult == null && targetViewportWidth != null && resolution.spec.isAbsoluteDp()
                && originalDensityDpi > 0 && (originalSmallestWidthDp != targetViewportWidth
                        || (metrics != null && originalDensityDpi != metrics.densityDpi))
            )
                VirtualDisplayPlan.deriveAbsoluteResultFromPhysicalPixels(
                    originalWidthDp,
                    originalHeightDp,
                    originalSmallestWidthDp,
                    sourceWidthPx,
                    sourceHeightPx,
                    targetViewportWidth
                )
            else
                null
        val trustedDisplayTarget =
            if (stableTargetForResult != null) stableTargetForResult else pixelDerivedTarget
        val resolvedRecordResult =
            ViewportResolvedTarget.viewportResult(resolution, windowScoped, config)
        val windowLikeBorrowResult =
            resolveWindowLikeBorrowResult(config, resolution, windowScoped)
        val appProcessWindowMetricsResult =
            if (windowScoped)
                ViewportResolvedTarget.appProcessWindowMetricsResult(
                    config, resolution, targetViewportWidth, stableTarget
                )
            else
                null
        val windowLikeBorrow = windowLikeBorrowResult != null
        val appProcessWindowMetricsOnly = appProcessWindowMetricsResult != null
        val trustedDisplayResult =
            if (resolution.isAppProcessBorrowTarget())
                null
            else
                ViewportResolvedTarget.viewportResult(trustedDisplayTarget)
        val result = if (windowLikeBorrowResult != null)
            windowLikeBorrowResult
        else
            if (appProcessWindowMetricsResult != null)
                appProcessWindowMetricsResult
            else
                if (resolvedRecordResult != null)
                    resolvedRecordResult
                else
                    if (trustedDisplayResult != null)
                        trustedDisplayResult
                    else
                        ViewportOverride.derive(
                            config,
                            if (targetViewportWidth != null) targetViewportWidth else 0,
                            windowScoped,
                            stableTarget
                        )
        if (result == null) {
            val originalScaledDensity = if (metrics != null) metrics.scaledDensity else -1f
            val metricsApplied = applyScaledDensityIfChanged(metrics, config)
            if (fontScaleApplied || metricsApplied) {
                val detail = ("source=ResourcesImpl"
                        + ", widthDp=" + originalWidthDp
                        + ", heightDp=" + originalHeightDp
                        + ", smallestWidthDp=" + originalSmallestWidthDp
                        + ", densityDpi=" + originalDensityDpi
                        + ", fontScale=" + fontScale.original + "->" + config.fontScale
                        + ", scaledDensity=" + originalScaledDensity + "->"
                        + (if (metrics != null) metrics.scaledDensity else -1f))
                if (logIfChanged(
                        packageName + ":observe",
                        ("DPIS_VIEWPORT ResourcesImpl observe: widthDp=" + originalWidthDp
                                + ", heightDp=" + originalHeightDp
                                + ", smallestWidthDp=" + originalSmallestWidthDp
                                + ", densityDpi=" + originalDensityDpi
                                + ", fontScale=" + fontScale.original + " -> " + config.fontScale
                                + ", scaledDensity=" + originalScaledDensity + " -> "
                                + (if (metrics != null) metrics.scaledDensity else -1f))
                    )
                ) {
                    RuntimeHotPathEvents.applied(
                        packageName,
                        "viewport",
                        "resources_impl_observe",
                        detail
                    )
                }
            }
            return
        }
        val needsViewportUpdate =
            result.widthDp != originalWidthDp || result.heightDp != originalHeightDp || result.smallestWidthDp != originalSmallestWidthDp || (result.densityDpi > 0 && result.densityDpi != originalDensityDpi)
        val applyToConfiguration = ViewportModePolicy.shouldApplyConfigurationOverride(
            policy, store, packageName, resolution, needsViewportUpdate
        )
                && !appProcessWindowMetricsOnly
        val sharedResult =
            if (trustedDisplayTarget != null)
                trustedDisplayTarget
            else
                VirtualDisplayPlan.derivePublishableResult(
                    originalWidthDp,
                    originalHeightDp,
                    originalSmallestWidthDp,
                    originalDensityDpi,
                    sourceWidthPx,
                    sourceHeightPx,
                    result.smallestWidthDp
                )
        val publishableSharedResult = if (windowScoped) null else sharedResult
        val canPublishFromResourcesImpl =
            !windowLikeBorrow && !appProcessWindowMetricsOnly && shouldPublishResourcesImplResult(
                packageName,
                resolution,
                needsViewportUpdate
            )
        if (canPublishFromResourcesImpl && publishableSharedResult != null) {
            val canPublishState = VirtualDisplayState.setUnlessDerivedFromTargetConfig(
                publishableSharedResult, originalSmallestWidthDp, targetViewportWidth
            )
            if (canPublishState && resolution.spec.isEnabled() && source != null) {
                VirtualDisplayState.publish(
                    packageName,
                    resolution.spec,
                    source,
                    result,
                    publishableSharedResult,
                    ViewportRuntimeRecord.PROVENANCE_APP_PROCESS
                )
            }
        }
        if (canPublishFromResourcesImpl) {
            val viewportMode = ViewportModePolicy.resolve(store, packageName)
            ViewportDebugReporter.report(
                store,
                packageName,
                viewportMode,
                originalWidthDp,
                originalHeightDp,
                originalDensityDpi,
                result,
                publishableSharedResult,
                applyToConfiguration
            )
        }
        if (!needsViewportUpdate) {
            val stableResult =
                VirtualDisplayState.getStableTargetResult(
                    originalSmallestWidthDp, targetViewportWidth
                )
            if (windowLikeBorrow
                && stableResult != null && stableResult.densityDpi > 0 && metrics != null
            ) {
                metrics.densityDpi = stableResult.densityDpi
                metrics.density = DensityOverride.densityFromDpi(stableResult.densityDpi)
                metrics.scaledDensity = DensityOverride.scaledDensityFrom(
                    stableResult.densityDpi, config.fontScale
                )
                metrics.widthPixels = stableResult.widthPx
                metrics.heightPixels = stableResult.heightPx
                val detail = ("source=ResourcesImpl"
                        + ", widthDp=" + config.screenWidthDp
                        + ", heightDp=" + config.screenHeightDp
                        + ", smallestWidthDp=" + config.smallestScreenWidthDp
                        + ", metricsDensityDpi=" + metrics.densityDpi
                        + ", metricsWidthPx=" + metrics.widthPixels
                        + ", metricsHeightPx=" + metrics.heightPixels)
                if (logIfChanged(
                        packageName + ":window-like-borrow",
                        ("DPIS_VIEWPORT ResourcesImpl window-like borrow: widthDp="
                                + config.screenWidthDp
                                + ", heightDp=" + config.screenHeightDp
                                + ", smallestWidthDp=" + config.smallestScreenWidthDp
                                + ", metricsDensityDpi=" + metrics.densityDpi
                                + ", metricsWidthPx=" + metrics.widthPixels
                                + ", metricsHeightPx=" + metrics.heightPixels)
                    )
                ) {
                    RuntimeHotPathEvents.applied(
                        packageName,
                        "viewport",
                        "resources_impl_window_like_borrow",
                        detail
                    )
                }
                return
            }
            if (result.densityDpi <= 0 && stableResult != null && stableResult.densityDpi > 0 && config.densityDpi != stableResult.densityDpi) {
                config.densityDpi = stableResult.densityDpi
                if (metrics != null) {
                    metrics.densityDpi = stableResult.densityDpi
                    metrics.density = DensityOverride.densityFromDpi(stableResult.densityDpi)
                    metrics.scaledDensity = DensityOverride.scaledDensityFrom(
                        stableResult.densityDpi, config.fontScale
                    )
                    metrics.widthPixels = stableResult.widthPx
                    metrics.heightPixels = stableResult.heightPx
                }
                val detail = ("source=ResourcesImpl"
                        + ", widthDp=" + config.screenWidthDp
                        + ", heightDp=" + config.screenHeightDp
                        + ", smallestWidthDp=" + config.smallestScreenWidthDp
                        + ", densityDpi=" + originalDensityDpi + "->" + config.densityDpi
                        + ", metricsDensityDpi=" + (if (metrics != null) metrics.densityDpi else -1)
                        + ", metricsWidthPx=" + (if (metrics != null) metrics.widthPixels else -1)
                        + ", metricsHeightPx=" + (if (metrics != null) metrics.heightPixels else -1))
                if (logIfChanged(
                        packageName + ":stable-target",
                        ("DPIS_VIEWPORT ResourcesImpl stable target: widthDp="
                                + config.screenWidthDp
                                + ", heightDp=" + config.screenHeightDp
                                + ", smallestWidthDp=" + config.smallestScreenWidthDp
                                + ", densityDpi " + originalDensityDpi
                                + " -> " + config.densityDpi
                                + ", metricsDensityDpi="
                                + (if (metrics != null) metrics.densityDpi else -1)
                                + ", metricsWidthPx="
                                + (if (metrics != null) metrics.widthPixels else -1)
                                + ", metricsHeightPx="
                                + (if (metrics != null) metrics.heightPixels else -1))
                    )
                ) {
                    RuntimeHotPathEvents.applied(
                        packageName,
                        "viewport",
                        "resources_impl_stable_target",
                        detail
                    )
                }
                return
            }
            applyScaledDensityIfChanged(metrics, config)
            return
        }
        if (applyToConfiguration) {
            ViewportOverride.apply(config, result)
        }
        val targetDensity = DensityOverride.densityFromDpi(result.densityDpi)
        val targetScaledDensity = DensityOverride.scaledDensityFrom(
            result.densityDpi, config.fontScale
        )
        if (metrics != null) {
            metrics.densityDpi = result.densityDpi
            metrics.density = targetDensity
            metrics.scaledDensity = targetScaledDensity
            if (publishableSharedResult != null) {
                metrics.widthPixels = publishableSharedResult.widthPx
                metrics.heightPixels = publishableSharedResult.heightPx
            }
        }
        val modeLabel = if (applyToConfiguration) "config" else "metrics"
        val detail = ("source=ResourcesImpl"
                + ", mode=" + modeLabel
                + ", scope=" + (if (windowScoped) "window" else "display")
                + ", widthDp=" + originalWidthDp + "->" + result.widthDp
                + ", heightDp=" + originalHeightDp + "->" + result.heightDp
                + ", smallestWidthDp=" + originalSmallestWidthDp + "->" + result.smallestWidthDp
                + ", densityDpi=" + originalDensityDpi + "->" + result.densityDpi
                + ", fontScale=" + fontScale.original + "->" + config.fontScale
                + ", metricsDensityDpi=" + (if (metrics != null) metrics.densityDpi else -1)
                + ", metricsWidthPx=" + (if (metrics != null) metrics.widthPixels else -1)
                + ", metricsHeightPx=" + (if (metrics != null) metrics.heightPixels else -1))
        if (logIfChanged(
                packageName + ":override",
                ("DPIS_VIEWPORT ResourcesImpl (" + modeLabel + ") override: scope="
                        + (if (windowScoped) "window" else "display")
                        + ", widthDp "
                        + originalWidthDp + " -> " + result.widthDp
                        + ", heightDp " + originalHeightDp + " -> " + result.heightDp
                        + ", smallestWidthDp " + originalSmallestWidthDp + " -> "
                        + result.smallestWidthDp
                        + ", resolution=" + describeResolution(resolution)
                        + ", densityDpi " + originalDensityDpi + " -> " + result.densityDpi
                        + ", fontScale " + fontScale.original + " -> " + config.fontScale
                        + ", metricsDensityDpi=" + (if (metrics != null) metrics.densityDpi else -1)
                        + ", metricsWidthPx=" + (if (metrics != null) metrics.widthPixels else -1)
                        + ", metricsHeightPx=" + (if (metrics != null) metrics.heightPixels else -1))
            )
        ) {
            RuntimeHotPathEvents.applied(
                packageName,
                "viewport",
                "resources_impl_override",
                detail
            )
        }
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

    private fun applyScaledDensityIfChanged(
        metrics: DisplayMetrics?,
        config: Configuration?
    ): Boolean {
        if (metrics == null || config == null) {
            return false
        }
        val baseDensityDpi = if (metrics.densityDpi > 0) metrics.densityDpi else config.densityDpi
        if (baseDensityDpi <= 0) {
            return false
        }
        val targetScaledDensity = DensityOverride.scaledDensityFrom(
            baseDensityDpi,
            config.fontScale
        )
        if (abs(metrics.scaledDensity - targetScaledDensity) <= FontScaleOverride.EPSILON) {
            return false
        }
        metrics.scaledDensity = targetScaledDensity
        return true
    }

    @JvmStatic
    fun shouldPublishResourcesImplResultForTest(
        packageName: String?,
        resolution: ViewportTargetResolution?,
        needsViewportUpdate: Boolean
    ): Boolean {
        return shouldPublishResourcesImplResult(packageName, resolution, needsViewportUpdate)
    }

    private fun shouldPublishResourcesImplResult(
        packageName: String?,
        resolution: ViewportTargetResolution?,
        needsViewportUpdate: Boolean
    ): Boolean {
        if (resolution == null || resolution.spec == null || !resolution.spec.isEnabled()) {
            return false
        }
        if (resolution.isAppProcessDisplayBorrowTarget()
            && !WebApkCarrierResolver.isWebApkOwnerPackage(packageName)
        ) {
            return false
        }
        if (needsViewportUpdate) {
            return true
        }
        return resolution.spec.isAbsoluteDp()
    }

    private fun resolveWindowLikeBorrowResult(
        config: Configuration?,
        resolution: ViewportTargetResolution?,
        windowScoped: Boolean
    ): ViewportOverride.Result? {
        if (config == null || windowScoped
            || resolution == null || resolution.record == null || resolution.record.viewportResult == null || resolution.spec == null || !resolution.spec.isRelativeScale()
        ) {
            return null
        }
        val displayResult = resolution.record.viewportResult
        val matchesTargetWidth = config.screenWidthDp == displayResult.widthDp
                && config.smallestScreenWidthDp == displayResult.smallestWidthDp
        val shorterThanDisplay = config.screenHeightDp > 0
                && config.screenHeightDp < displayResult.heightDp
        if (!matchesTargetWidth || !shorterThanDisplay) {
            return null
        }
        return ViewportOverride.Result(
            config.screenWidthDp,
            config.screenHeightDp,
            config.smallestScreenWidthDp,
            displayResult.densityDpi
        )
    }

    private fun describeResolution(resolution: ViewportTargetResolution?): String {
        if (resolution == null) {
            return "none"
        }
        return ("{reason=" + resolution.reason
                + ",target=" + resolution.effectiveSmallestWidthDp
                + ",record=" + (if (resolution.record != null) "yes" else "no") + "}")
    }
}
