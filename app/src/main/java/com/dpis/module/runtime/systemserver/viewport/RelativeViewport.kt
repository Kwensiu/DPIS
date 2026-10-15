package com.dpis.module.runtime.systemserver.viewport

import android.content.res.Configuration
import com.dpis.module.config.PerAppDisplayConfig
import com.dpis.module.runtime.probe.RuntimeClock
import com.dpis.module.runtime.systemserver.diagnostics.HookLog
import com.dpis.module.viewport.PerAppDisplayEnvironment
import com.dpis.module.viewport.PerAppDisplayOverrideCalculator
import com.dpis.module.viewport.ViewportConfigurationScope
import com.dpis.module.viewport.ViewportRuntimeMarkerBridge
import com.dpis.module.viewport.ViewportSourceSnapshot
import kotlin.math.abs
import kotlin.math.round

/**
 * Relative scale on system_server. A current marker keeps the configuration.
 * A complete marker may be reused. An empty display marker may be scaled once
 * and then published. A window-scoped resize must not publish a new baseline,
 * but it may reuse a complete result already proven by marker evidence.
 */
object RelativeViewport {
    @JvmStatic
    fun resolve(
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
        var marker = ViewportRuntimeMarkerBridge.read(
            packageName,
            config.targetViewportSpec.fingerprint(),
            RuntimeClock.crossProcessMarkerMillis(),
        )
        if (isStaleMarker(marker)) {
            marker = ViewportRuntimeMarkerBridge.readAllowingStale(
                packageName,
                config.targetViewportSpec.fingerprint(),
                RuntimeClock.crossProcessMarkerMillis(),
            )
        }
        val scope = if (ViewportConfigurationScope.isWindowScoped(configuration)) {
            ViewportSourceSnapshot.SCOPE_WINDOW
        } else {
            ViewportSourceSnapshot.SCOPE_DISPLAY
        }
        if (isAlreadyApplied(configuration, scope, marker)) {
            return keepingCurrentDp(configuration, widthPx, heightPx)
        }
        val record = marker.record
        if (record != null && canReuseCompleteMarkerResult(configuration, marker)) {
            return PerAppDisplayEnvironment(
                record.resultWidthDp,
                record.resultHeightDp,
                record.resultSmallestWidthDp,
                record.resultDensityDpi,
                widthPx,
                heightPx,
            )
        }
        if (!canDeriveFromSystemServerSource(configuration, marker)) {
            return null
        }
        return PerAppDisplayOverrideCalculator.calculate(
            configuration, widthPx, heightPx, config.targetViewportSpec,
        )
    }

    @JvmStatic
    fun gate(
        entryName: String?,
        packageName: String?,
        source: Configuration?,
        environment: PerAppDisplayEnvironment?,
        config: PerAppDisplayConfig?,
    ): PerAppDisplayEnvironment? {
        if (environment == null || config == null || !config.targetViewportSpec.isRelativeScale()) {
            return environment
        }
        if (canApplyWindowScopedMarkerResult(packageName, source, environment, config)) {
            return environment
        }
        val displaySizedWindow = ViewportConfigurationScope.isWindowScoped(source) &&
                coversEnvironmentPixels(source, environment)
        if (publish(packageName, source, environment, config, displaySizedWindow)) {
            return environment
        }
        HookLog.logRelativeMarkerPublishFailed(
            entryName,
            packageName,
            "${config.targetViewportSpec}",
        )
        return null
    }

    @JvmStatic
    fun gateLaunchViewportMarker(
        packageName: String?,
        baseConfiguration: Configuration?,
        environment: PerAppDisplayEnvironment?,
        config: PerAppDisplayConfig?,
    ): PerAppDisplayEnvironment? = resolveMarkerGatedEnvironment(
        "launch-activity-item",
        packageName,
        baseConfiguration,
        null,
        environment,
        config,
    )

    /**
     * DisplayInfo is accepted because callers already captured it with the
     * configuration. The marker itself is read from the configuration.
     */
    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun resolveMarkerGatedEnvironment(
        entryName: String?,
        packageName: String?,
        configuration: Configuration?,
        displayInfo: Any?,
        environment: PerAppDisplayEnvironment?,
        config: PerAppDisplayConfig?,
    ): PerAppDisplayEnvironment? = gate(
        entryName,
        packageName,
        configuration,
        environment,
        config,
    )

    @JvmStatic
    fun isAlreadyApplied(
        configuration: Configuration?,
        scope: String?,
        marker: ViewportRuntimeMarkerBridge.ParseResult?,
    ): Boolean {
        val record = marker?.record
        if (configuration == null || marker == null || !marker.hit || record == null) {
            return false
        }
        val sourceSignature = ViewportRuntimeMarkerBridge.configurationSignature(
            configuration.screenWidthDp,
            configuration.screenHeightDp,
            configuration.smallestScreenWidthDp,
            configuration.densityDpi,
            scope,
        )
        return sourceSignature == record.resultSignature
    }

    @JvmStatic
    fun hasCompleteMarkerResult(marker: ViewportRuntimeMarkerBridge.ParseResult?): Boolean {
        val record = marker?.record
        return marker != null &&
                marker.hit &&
                record != null &&
                record.resultWidthDp > 0 &&
                record.resultHeightDp > 0 &&
                record.resultSmallestWidthDp > 0 &&
                record.resultDensityDpi > 0
    }

    @JvmStatic
    fun canReuseCompleteMarkerResult(
        configuration: Configuration?,
        marker: ViewportRuntimeMarkerBridge.ParseResult?,
    ): Boolean {
        val record = marker?.record ?: return false
        return hasCompleteMarkerResult(marker) &&
                sameOrientation(configuration, record.resultWidthDp, record.resultHeightDp)
    }

    @JvmStatic
    fun environmentMatchesMarkerResult(
        environment: PerAppDisplayEnvironment?,
        marker: ViewportRuntimeMarkerBridge.ParseResult?,
    ): Boolean {
        val record = marker?.record ?: return false
        return environment != null &&
                hasCompleteMarkerResult(marker) &&
                environment.widthDp == record.resultWidthDp &&
                environment.heightDp == record.resultHeightDp &&
                environment.smallestWidthDp == record.resultSmallestWidthDp &&
                environment.densityDpi == record.resultDensityDpi
    }

    @JvmStatic
    fun canDeriveFromSystemServerSource(
        configuration: Configuration?,
        marker: ViewportRuntimeMarkerBridge.ParseResult?,
    ): Boolean = configuration != null &&
            !ViewportConfigurationScope.isWindowScoped(configuration) &&
            marker != null &&
            marker.reason == "empty"

    @JvmStatic
    fun isStaleMarker(marker: ViewportRuntimeMarkerBridge.ParseResult?): Boolean =
        marker != null && !marker.hit && marker.reason == "stale"

    private fun keepingCurrentDp(
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

    private fun sameOrientation(
        configuration: Configuration?,
        widthDp: Int,
        heightDp: Int
    ): Boolean {
        if (configuration == null ||
            configuration.screenWidthDp <= 0 ||
            configuration.screenHeightDp <= 0 ||
            widthDp <= 0 ||
            heightDp <= 0
        ) {
            return true
        }
        val sourceCompare = configuration.screenWidthDp.compareTo(configuration.screenHeightDp)
        val resultCompare = widthDp.compareTo(heightDp)
        return sourceCompare == 0 || resultCompare == 0 || sourceCompare == resultCompare
    }

    private fun canApplyWindowScopedMarkerResult(
        packageName: String?,
        source: Configuration?,
        environment: PerAppDisplayEnvironment?,
        config: PerAppDisplayConfig?,
    ): Boolean {
        if (source == null || environment == null || config == null ||
            !config.targetViewportSpec.isRelativeScale() ||
            !ViewportConfigurationScope.isWindowScoped(source)
        ) {
            return false
        }
        val marker = ViewportRuntimeMarkerBridge.readAllowingStale(
            packageName,
            config.targetViewportSpec.fingerprint(),
            RuntimeClock.crossProcessMarkerMillis(),
        )
        return environmentMatchesMarkerResult(environment, marker)
    }

    /**
     * A full-screen window reconstructs the same pixels as the display. It is
     * the launch configuration WeChat reads, so the relative result is written
     * there and recorded as the display baseline. A smaller window is not.
     */
    private fun coversEnvironmentPixels(
        source: Configuration?,
        environment: PerAppDisplayEnvironment?,
    ): Boolean {
        if (source == null || environment == null || source.densityDpi <= 0 ||
            source.screenWidthDp <= 0 || source.screenHeightDp <= 0 ||
            environment.widthPx <= 0 || environment.heightPx <= 0
        ) {
            return false
        }
        val density = source.densityDpi / 160f
        val width = round(source.screenWidthDp * density).toInt()
        val height = round(source.screenHeightDp * density).toInt()
        return abs(width - environment.widthPx) <= 1 && abs(height - environment.heightPx) <= 1
    }

    private fun publish(
        packageName: String?,
        source: Configuration?,
        environment: PerAppDisplayEnvironment?,
        config: PerAppDisplayConfig?,
        displaySizedWindow: Boolean = false,
    ): Boolean {
        if (source == null || environment == null || config == null ||
            !config.targetViewportSpec.isEnabled()
        ) {
            return false
        }
        val windowScoped = ViewportConfigurationScope.isWindowScoped(source)
        if (config.targetViewportSpec.isRelativeScale() && windowScoped && !displaySizedWindow) {
            return false
        }
        val scope = if (windowScoped && !displaySizedWindow) {
            ViewportSourceSnapshot.SCOPE_WINDOW
        } else {
            ViewportSourceSnapshot.SCOPE_DISPLAY
        }
        return ViewportRuntimeMarkerBridge.publishSystemServerRecord(
            packageName,
            config.targetViewportSpec,
            ConfigurationMarker(source),
            EnvironmentMarker(environment),
            scope,
            RuntimeClock.crossProcessMarkerMillis(),
        )
    }

    private class ConfigurationMarker(
        private val configuration: Configuration,
    ) : ViewportRuntimeMarkerBridge.ConfigurationLike {
        override fun widthDp(): Int = configuration.screenWidthDp

        override fun heightDp(): Int = configuration.screenHeightDp

        override fun smallestWidthDp(): Int = configuration.smallestScreenWidthDp

        override fun densityDpi(): Int = configuration.densityDpi
    }

    private class EnvironmentMarker(
        private val environment: PerAppDisplayEnvironment,
    ) : ViewportRuntimeMarkerBridge.ConfigurationLike {
        override fun widthDp(): Int = environment.widthDp

        override fun heightDp(): Int = environment.heightDp

        override fun smallestWidthDp(): Int = environment.smallestWidthDp

        override fun densityDpi(): Int = environment.densityDpi
    }
}
