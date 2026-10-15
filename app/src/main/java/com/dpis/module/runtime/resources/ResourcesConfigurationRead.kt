package com.dpis.module.runtime.resources


import android.content.res.Configuration
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.hooks.HookRuntimePolicy
import com.dpis.module.runtime.font.FontScaleOverride
import com.dpis.module.runtime.webapk.WebApkRuntimeOwnerBridge
import com.dpis.module.viewport.RelativeViewportOwnership
import com.dpis.module.viewport.TargetViewportWidthResolver
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportConfigurationScope
import com.dpis.module.viewport.ViewportModePolicy
import com.dpis.module.viewport.ViewportOverride
import com.dpis.module.viewport.ViewportResolvedTarget
import com.dpis.module.viewport.ViewportRuntimeMarkerProbe
import com.dpis.module.viewport.ViewportRuntimeRecord
import com.dpis.module.viewport.ViewportSourceSnapshot
import com.dpis.module.viewport.VirtualDisplayPlan
import com.dpis.module.viewport.VirtualDisplayState
import com.dpis.module.viewport.window.WindowBoundsState

/** Resources configuration read. DisplayMetrics writes live in ResourcesMetricsRead. */
internal object ResourcesConfigurationRead {
    fun apply(
        resourceScope: Any?,
        config: Configuration?,
        packageName: String?,
        store: DpisConfigStore?,
        sourceTag: String,
        windowScopedOverride: Boolean?,
        viewportHandlingEnabled: Boolean = true,
        fontConfigurationOverrideEnabled: Boolean = true,
        policy: HookRuntimePolicy? = HookRuntimePolicy.fromStore(store),
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
            store, packageName, config.fontScale,
        )
        val fontScaleApplied = fontConfigurationOverrideEnabled
                && FontScaleOverride.applyToConfiguration(config, fontScale)
        if (!viewportHandlingEnabled) {
            if (fontScaleApplied) {
                ResourcesReadHookInstaller.logIfChanged(
                    packageName + ":" + sourceTag + ":font-only",
                    ("DPIS_FONT " + sourceTag + " override: package=" + packageName
                            + ", fontScale "
                            + fontScale.original + " -> " + config.fontScale),
                )
            }
            return
        }
        if (sourceTag.startsWith("ResourcesRead(getConfiguration)")
            && RelativeViewportOwnership.shouldDefer(store, packageName, policy, config)
        ) {
            ResourcesReadHookInstaller.retainPublishedDisplay(
                store,
                packageName,
                config,
                "resources_read_configuration_override",
                sourceTag,
                "config",
            )
            ResourcesReadHookInstaller.recordConfigurationSkip(
                packageName,
                "system_server_owns_relative_viewport",
                "source=" + sourceTag + ", reason=system_server_owns_relative_viewport",
            )
            if (fontScaleApplied) {
                ResourcesReadHookInstaller.logIfChanged(
                    packageName + ":" + sourceTag + ":font-only",
                    ("DPIS_FONT " + sourceTag + " override: package=" + packageName
                            + ", fontScale " + fontScale.original + " -> " + config.fontScale),
                )
            }
            return
        }
        if (!ViewportConfigurationScope.isValidDisplayConfiguration(config)) {
            ResourcesReadHookInstaller.logIfChanged(
                packageName + ":" + sourceTag + ":invalid-config",
                ("DPIS_VIEWPORT " + sourceTag + " skip: invalid display configuration"
                        + ", widthDp=" + config.screenWidthDp
                        + ", heightDp=" + config.screenHeightDp
                        + ", smallestWidthDp=" + config.smallestScreenWidthDp
                        + ", densityDpi=" + config.densityDpi
                        + ", fontScale=" + config.fontScale),
            )
            return
        }

        val originalWidthDp = config.screenWidthDp
        val originalHeightDp = config.screenHeightDp
        val originalSmallestWidthDp = config.smallestScreenWidthDp
        val originalDensityDpi = config.densityDpi

        val source = ViewportSourceSnapshot.fromConfiguration(
            ViewportSourceSnapshot.ORIGIN_RESOURCES_READ, config, null,
        )
        val resolution =
            TargetViewportWidthResolver.resolve(store, packageName, source)
        val targetViewportWidth = if (resolution.hasTarget()) {
            resolution.effectiveSmallestWidthDp
        } else {
            null
        }
        if (targetViewportWidth != null && resolution.spec.isEnabled) {
            ViewportRuntimeMarkerProbe.observeAppProcessProbe(
                packageName, resolution.spec, sourceTag,
            )
        }
        val windowScoped = if (windowScopedOverride != null) {
            windowScopedOverride
        } else {
            ViewportConfigurationScope.isWindowScoped(config)
                    || WindowBoundsState.hasActiveWindow(packageName)
                    || WindowBoundsState.matchesWindowConfiguration(packageName, config)
        }
        val physicalWindowBounds = if (windowScoped) {
            WindowBoundsState.pixelsForConfiguration(packageName, config)
        } else {
            null
        }
        if (windowScoped && resolution.isAppProcessBorrowTarget) {
            if (physicalWindowBounds == null) {
                // Without physical bounds the existing configuration is the
                // only trustworthy window geometry; do not derive from a
                // display target that may belong to another resource scope.
                if (fontScaleApplied) {
                    ResourcesReadHookInstaller.logIfChanged(
                        packageName + ":" + sourceTag + ":window-borrow-font-only",
                        ("DPIS_FONT " + sourceTag + " window borrow: package=" + packageName
                                + ", fontScale "
                                + fontScale.original + " -> " + config.fontScale),
                    )
                }
                return
            }
        }
        val stableTarget =
            ViewportResolvedTarget.virtualDisplayResult(resolution, targetViewportWidth)
        val resolvedRecordResult =
            ViewportResolvedTarget.viewportResult(resolution, windowScoped)
        val result = if (physicalWindowBounds != null) {
            ViewportOverride.deriveWindowScoped(
                config,
                if (targetViewportWidth != null) targetViewportWidth else 0,
                stableTarget,
                physicalWindowBounds.width,
                physicalWindowBounds.height,
            )
        } else if (resolvedRecordResult != null) {
            resolvedRecordResult
        } else {
            ViewportOverride.derive(
                config,
                if (targetViewportWidth != null) targetViewportWidth else 0,
                windowScoped,
                stableTarget,
            )
        }
        if (result == null) {
            if (fontScaleApplied) {
                ResourcesReadHookInstaller.logIfChanged(
                    packageName + ":" + sourceTag + ":font-only",
                    ("DPIS_FONT " + sourceTag + " override: package=" + packageName
                            + ", fontScale "
                            + fontScale.original + " -> " + config.fontScale),
                )
            }
            return
        }

        val needsViewportUpdate =
            result.widthDp != originalWidthDp || result.heightDp != originalHeightDp ||
                    result.smallestWidthDp != originalSmallestWidthDp ||
                    (result.densityDpi > 0 && result.densityDpi != originalDensityDpi)
        val applyToConfiguration = ViewportModePolicy.shouldApplyConfigurationOverride(
            policy, store, packageName, resolution, needsViewportUpdate,
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
            result.smallestWidthDp,
        )
        if (!windowScoped && !resolution.isAppProcessBorrowTarget) {
            if (resolution.spec.isEnabled) {
                VirtualDisplayState.publish(
                    packageName,
                    resolution.spec,
                    source,
                    result,
                    sharedResult,
                    ViewportRuntimeRecord.PROVENANCE_APP_PROCESS,
                )
            } else if (sharedResult != null) {
                VirtualDisplayState.setUnlessDerivedFromTargetConfig(
                    sharedResult, originalSmallestWidthDp, targetViewportWidth,
                )
            }
        }

        if (result.widthDp == originalWidthDp && result.heightDp == originalHeightDp &&
            result.smallestWidthDp == originalSmallestWidthDp &&
            (result.densityDpi <= 0 || result.densityDpi == originalDensityDpi) &&
            !fontScaleApplied
        ) {
            val stableResult =
                VirtualDisplayState.getStableTargetResult(
                    originalSmallestWidthDp, targetViewportWidth,
                )
            if (result.densityDpi <= 0 && stableResult != null && stableResult.densityDpi > 0 &&
                config.densityDpi != stableResult.densityDpi
            ) {
                config.densityDpi = stableResult.densityDpi
                val detail = ("source=" + sourceTag
                        + ", widthDp=" + config.screenWidthDp
                        + ", heightDp=" + config.screenHeightDp
                        + ", smallestWidthDp=" + config.smallestScreenWidthDp
                        + ", densityDpi=" + originalDensityDpi + "->" + config.densityDpi
                        + ", fontScale=" + fontScale.original + "->" + config.fontScale)
                if (ResourcesReadHookInstaller.logIfChanged(
                        packageName + ":" + sourceTag + ":stable-target",
                        ("DPIS_VIEWPORT " + sourceTag + " stable target: package=" + packageName
                                + ", targetViewportWidthDp="
                                + ResourcesReadHookInstaller.describeNullable(targetViewportWidth)
                                + ", widthDp="
                                + config.screenWidthDp
                                + ", heightDp=" + config.screenHeightDp
                                + ", smallestWidthDp=" + config.smallestScreenWidthDp
                                + ", densityDpi " + originalDensityDpi
                                + " -> " + config.densityDpi
                                + ", fontScale " + fontScale.original
                                + " -> " + config.fontScale),
                    )
                ) {
                    RuntimeHotPathEvents.applied(
                        packageName,
                        "viewport",
                        "resources_read_configuration_stable_target",
                        detail,
                    )
                }
            }
            return
        }
        val detail = ("source=" + sourceTag
                + ", mode=" + (if (applyToConfiguration) "config" else "metrics")
                + ", scope=" + (if (windowScoped) "window" else "display")
                + ", targetViewportWidthDp="
                + ResourcesReadHookInstaller.describeNullable(targetViewportWidth)
                + ", widthDp=" + originalWidthDp + "->" + config.screenWidthDp
                + ", heightDp=" + originalHeightDp + "->" + config.screenHeightDp
                + ", smallestWidthDp=" + originalSmallestWidthDp + "->"
                + config.smallestScreenWidthDp
                + ", densityDpi=" + originalDensityDpi + "->" + config.densityDpi
                + ", fontScale=" + fontScale.original + "->" + config.fontScale)
        if (ResourcesReadHookInstaller.logIfChanged(
                packageName + ":" + sourceTag,
                ("DPIS_VIEWPORT " + sourceTag + " override: package=" + packageName
                        + ", targetViewportWidthDp="
                        + ResourcesReadHookInstaller.describeNullable(targetViewportWidth)
                        + ", scope=" + (if (windowScoped) "window" else "display")
                        + ", mode="
                        + (if (applyToConfiguration) "config" else "metrics")
                        + ", target=" + ResourcesReadHookInstaller.describeViewportResult(result)
                        + ", actual=" + ResourcesReadHookInstaller.describeConfiguration(config)
                        + ", widthDp " + originalWidthDp
                        + " -> " + config.screenWidthDp
                        + ", heightDp " + originalHeightDp + " -> " + config.screenHeightDp
                        + ", smallestWidthDp " + originalSmallestWidthDp + " -> "
                        + config.smallestScreenWidthDp
                        + ", densityDpi " + originalDensityDpi + " -> " + config.densityDpi
                        + ", fontScale " + fontScale.original + " -> " + config.fontScale),
            )
        ) {
            RuntimeHotPathEvents.applied(
                packageName,
                "viewport",
                "resources_read_configuration_override",
                detail,
            )
            maybeLogLegacyAutoFallbackSuccess(
                store,
                packageName,
                sourceTag,
                originalWidthDp,
                originalHeightDp,
                originalSmallestWidthDp,
                originalDensityDpi,
                config,
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
        config: Configuration,
    ) {
        if (store == null || packageName == null || packageName.isBlank()
            || sourceTag == null || !sourceTag.startsWith("LegacyResourcesRead(") ||
            (ViewportApplyMode.AUTO != ViewportApplyMode.normalize(
                store.getTargetViewportApplyMode(packageName),
            ))
        ) {
            return
        }
        ResourcesReadHookInstaller.logIfChanged(
            packageName + ":" + sourceTag + ":legacy-auto-fallback",
            ("DPIS_VIEWPORT legacy auto fallback success: package=" + packageName
                    + ", source=" + sourceTag
                    + ", widthDp " + originalWidthDp + " -> " + config.screenWidthDp
                    + ", heightDp " + originalHeightDp + " -> " + config.screenHeightDp
                    + ", smallestWidthDp " + originalSmallestWidthDp + " -> "
                    + config.smallestScreenWidthDp
                    + ", densityDpi " + originalDensityDpi + " -> " + config.densityDpi),
        )
    }
}
