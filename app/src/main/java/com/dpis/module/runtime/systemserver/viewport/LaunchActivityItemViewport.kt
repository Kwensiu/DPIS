package com.dpis.module.runtime.systemserver.viewport

import android.content.res.Configuration
import com.dpis.module.config.PerAppDisplayConfig
import com.dpis.module.runtime.probe.RuntimeClock
import com.dpis.module.viewport.PerAppDisplayEnvironment
import com.dpis.module.viewport.PerAppDisplayOverrideCalculator
import com.dpis.module.viewport.ViewportConfigurationScope
import com.dpis.module.viewport.ViewportRuntimeMarkerBridge
import com.dpis.module.viewport.ViewportSourceSnapshot
import kotlin.math.roundToInt

/**
 * LaunchActivityItem is hooked before and after construction. The second pass
 * must keep the first result: dp rebuilt from the density the first pass wrote
 * does not invert the scale.
 */
object LaunchActivityItemViewport {
    @JvmStatic
    fun resolve(
        packageName: String?,
        baseConfiguration: Configuration?,
        widthPx: Int,
        heightPx: Int,
        config: PerAppDisplayConfig?,
    ): PerAppDisplayEnvironment? {
        if (baseConfiguration == null || config == null) {
            return null
        }
        val alreadyApplied = alreadyAppliedRelativeScale(
            packageName, baseConfiguration, widthPx, heightPx, config,
        )
        if (alreadyApplied != null) {
            return alreadyApplied
        }
        if (alreadyAtAbsoluteTarget(baseConfiguration, config)) {
            return environmentKeepingCurrentDp(baseConfiguration, widthPx, heightPx)
        }
        val physicalConfiguration = configurationForPhysicalDisplay(
            baseConfiguration, widthPx, heightPx,
        )
        val environment = PerAppDisplayOverrideCalculator.calculate(
            physicalConfiguration, widthPx, heightPx, config.targetViewportSpec,
        )
        return RelativeViewport.gateLaunchViewportMarker(
            packageName,
            baseConfiguration,
            environment,
            config,
        )
    }

    private fun alreadyAtAbsoluteTarget(
        configuration: Configuration?,
        config: PerAppDisplayConfig?,
    ): Boolean {
        if (configuration == null || config == null ||
            !config.targetViewportSpec.isAbsoluteDp()
        ) {
            return false
        }
        val targetWidthDp = config.targetViewportSpec.absoluteWidthDp()
        return targetWidthDp > 0 &&
                configuration.smallestScreenWidthDp == targetWidthDp &&
                configuration.screenWidthDp > 0 &&
                configuration.screenHeightDp > 0 &&
                configuration.densityDpi > 0
    }

    private fun environmentKeepingCurrentDp(
        configuration: Configuration,
        widthPx: Int,
        heightPx: Int,
    ): PerAppDisplayEnvironment = PerAppDisplayEnvironment(
        configuration.screenWidthDp,
        configuration.screenHeightDp,
        configuration.smallestScreenWidthDp,
        configuration.densityDpi,
        widthPx,
        heightPx,
    )

    private fun alreadyAppliedRelativeScale(
        packageName: String?,
        configuration: Configuration?,
        widthPx: Int,
        heightPx: Int,
        config: PerAppDisplayConfig?,
    ): PerAppDisplayEnvironment? {
        if (packageName == null || configuration == null || config == null ||
            !config.targetViewportSpec.isRelativeScale()
        ) {
            return null
        }
        val scope = if (ViewportConfigurationScope.isWindowScoped(configuration)) {
            ViewportSourceSnapshot.SCOPE_WINDOW
        } else {
            ViewportSourceSnapshot.SCOPE_DISPLAY
        }
        val marker = ViewportRuntimeMarkerBridge.read(
            packageName,
            config.targetViewportSpec.fingerprint(),
            RuntimeClock.crossProcessMarkerMillis(),
        )
        if (!RelativeViewport.isAlreadyApplied(configuration, scope, marker)) {
            return null
        }
        return environmentKeepingCurrentDp(configuration, widthPx, heightPx)
    }

    private fun configurationForPhysicalDisplay(
        source: Configuration?,
        widthPx: Int,
        heightPx: Int,
    ): Configuration? {
        if (source == null || widthPx <= 0 || heightPx <= 0 || source.densityDpi <= 0) {
            return source
        }
        val physical = Configuration(source)
        // The copy constructor used in tests leaves densityDpi at 0. Scale from
        // the incoming density on both the device and the JVM.
        physical.densityDpi = source.densityDpi
        physical.screenWidthDp =
            (widthPx * 160.0f / source.densityDpi).roundToInt().coerceAtLeast(1)
        physical.screenHeightDp =
            (heightPx * 160.0f / source.densityDpi).roundToInt().coerceAtLeast(1)
        physical.smallestScreenWidthDp = minOf(physical.screenWidthDp, physical.screenHeightDp)
        return physical
    }
}
