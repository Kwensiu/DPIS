package com.dpis.module.viewport

import android.content.res.Configuration
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.hooks.HookRuntimePolicy
import com.dpis.module.runtime.probe.RuntimeClock
import com.dpis.module.runtime.webapk.WebApkCarrierResolver
import com.dpis.module.viewport.window.WindowBoundsState
import kotlin.math.round

/** Identifies relative viewport configurations owned by system_server. */
object RelativeViewportOwnership {
    @JvmStatic
    fun shouldDefer(store: DpisConfigStore?, packageName: String?): Boolean =
        shouldDefer(store, packageName, store?.isSystemServerHooksEnabled() == true)

    @JvmStatic
    fun shouldDefer(
        store: DpisConfigStore?,
        packageName: String?,
        policy: HookRuntimePolicy?,
    ): Boolean = shouldDefer(
        store,
        packageName,
        policy?.systemServerHooksEnabled ?: (store?.isSystemServerHooksEnabled() == true),
    )

    /**
     * system_server owns a configuration only after that object already shows
     * the published width and height. The same physical pixels fit both the
     * source width and the scaled width, so pixel equality cannot decide.
     * A configuration still at the source width is scaled here. A smaller
     * window is also scaled here, because system_server copies the full
     * display result onto it.
     */
    @JvmStatic
    fun shouldDefer(
        store: DpisConfigStore?,
        packageName: String?,
        policy: HookRuntimePolicy?,
        config: Configuration?,
    ): Boolean {
        if (!shouldDefer(store, packageName, policy)) return false
        if (belongsToActiveWindow(packageName, config)) return false
        if (displayResultWasCopiedOntoAnotherWindow(store, packageName, config)) return false
        if (configurationCarriesPublishedResultDimensions(store, packageName, config)) return true
        return alreadyCarriesRelativeResult(store, packageName, config)
    }

    /**
     * The published width can arrive while density is still physical.
     * Write that published density and keep the display result for later
     * metrics reads. Do not derive another scale from this configuration.
     */
    fun retainPublishedDisplay(
        store: DpisConfigStore?,
        packageName: String?,
        config: Configuration?,
    ): Boolean {
        val resolvedStore = store ?: return false
        val resolvedPackageName = packageName ?: return false
        val resolvedConfig = config ?: return false
        val record = relativeResult(resolvedStore, resolvedPackageName) ?: return false
        if (!matchesPublishedResultDimensions(record, resolvedConfig)) return false

        var changed = false
        if (resolvedConfig.densityDpi != record.resultDensityDpi) {
            resolvedConfig.densityDpi = record.resultDensityDpi
            changed = true
        }
        if (VirtualDisplayState.get() != null) return changed

        val display = publishedDisplay(record) ?: return changed
        val spec = resolvedStore.getTargetViewportSpec(resolvedPackageName)
        val source = ViewportSourceSnapshot.systemDisplayInfo(
            record.resultWidthDp,
            record.resultHeightDp,
            record.resultSmallestWidthDp,
            record.resultDensityDpi,
            display.widthPx,
            display.heightPx,
        )
        val viewport = ViewportOverride.Result(
            record.resultWidthDp,
            record.resultHeightDp,
            record.resultSmallestWidthDp,
            record.resultDensityDpi,
        )
        return VirtualDisplayState.publish(
            resolvedPackageName,
            spec,
            source,
            viewport,
            display,
            ViewportRuntimeRecord.PROVENANCE_SYSTEM_SERVER,
        ) != null || changed
    }

    /**
     * A configuration can already show the published width while its density
     * is still the physical value. Metrics must use the published density
     * then, so a local derivation does not write the physical density back.
     */
    fun publishedMetricsDensityDpi(
        store: DpisConfigStore?,
        packageName: String?,
        config: Configuration?,
        localDensityDpi: Int,
    ): Int {
        val record = relativeResult(store, packageName) ?: return localDensityDpi
        if (record.resultDensityDpi <= 0 || localDensityDpi == record.resultDensityDpi) {
            return localDensityDpi
        }
        val followsPublishedWidth = config != null &&
                (config.screenWidthDp == record.resultWidthDp ||
                        config.smallestScreenWidthDp == record.resultSmallestWidthDp)
        return if (followsPublishedWidth) record.resultDensityDpi else localDensityDpi
    }

    /**
     * Display metrics stay untouched only when their density is already the
     * relative result. Physical metrics are still rewritten once.
     */
    fun metricsCarryRelativeResult(
        store: DpisConfigStore?,
        packageName: String?,
        densityDpi: Int,
        widthPixels: Int,
        heightPixels: Int,
    ): Boolean {
        if (densityDpi <= 0 || widthPixels <= 0 || heightPixels <= 0) return false
        val record = relativeResult(store, packageName) ?: return false
        if (record.provenance != ViewportRuntimeRecord.PROVENANCE_SYSTEM_SERVER ||
            record.resultDensityDpi != densityDpi
        ) {
            return false
        }
        return pixelsFor(record.resultWidthDp, record.resultDensityDpi) == widthPixels &&
            pixelsFor(record.resultHeightDp, record.resultDensityDpi) == heightPixels
    }

    @JvmStatic
    fun shouldDefer(
        store: DpisConfigStore?,
        packageName: String?,
        systemServerHooksEnabled: Boolean,
    ): Boolean {
        if (store == null || packageName.isNullOrEmpty()) return false
        if (!store.getTargetViewportSpec(packageName).isRelativeScale ||
            !systemServerHooksEnabled ||
            WebApkCarrierResolver.isWebApkOwnerPackage(packageName)
        ) {
            return false
        }
        if (ViewportApplyMode.SYSTEM != EffectiveModeResolver.resolveViewportMode(
            store.getTargetViewportApplyMode(packageName),
            true,
            )
        ) {
            return false
        }
        return relativeResult(store, packageName)?.provenance ==
                ViewportRuntimeRecord.PROVENANCE_SYSTEM_SERVER
    }

    /**
     * system_server copies the full display result onto a smaller window.
     * The dp then reconstructs the display pixels, so the copy has to be
     * recognized from the remembered window's different shape.
     */
    private fun displayResultWasCopiedOntoAnotherWindow(
        store: DpisConfigStore?,
        packageName: String?,
        config: Configuration?,
    ): Boolean {
        if (!alreadyCarriesRelativeResult(store, packageName, config)) return false
        if (!WindowBoundsState.hasActiveWindow(packageName)) return false
        return !WindowBoundsState.matchesWindowConfiguration(packageName, config)
    }

    /**
     * Resources may expose a configuration after its viewport dimensions change
     * but before its density changes. Keep that transaction with system_server
     * instead of converting the published result back to the source viewport.
     */
    private fun configurationCarriesPublishedResultDimensions(
        store: DpisConfigStore?,
        packageName: String?,
        config: Configuration?,
    ): Boolean {
        if (config == null) return false
        val record = relativeResult(store, packageName) ?: return false
        return matchesPublishedResultDimensions(record, config)
    }

    private fun matchesPublishedResultDimensions(
        record: ViewportRuntimeMarkerBridge.MarkerRecord,
        config: Configuration?,
    ): Boolean {
        if (config == null || record.resultWidthDp <= 0 || record.resultHeightDp <= 0 ||
            record.resultDensityDpi <= 0
        ) {
            return false
        }
        return config.screenWidthDp == record.resultWidthDp &&
            config.screenHeightDp == record.resultHeightDp &&
            (config.smallestScreenWidthDp <= 0 ||
                config.smallestScreenWidthDp == record.resultSmallestWidthDp)
    }

    private fun publishedDisplay(
        record: ViewportRuntimeMarkerBridge.MarkerRecord,
    ): VirtualDisplayOverride.Result? {
        val widthPx = pixelsFor(record.resultWidthDp, record.resultDensityDpi)
        val heightPx = pixelsFor(record.resultHeightDp, record.resultDensityDpi)
        if (widthPx <= 0 || heightPx <= 0) return null
        return VirtualDisplayOverride.Result(
            record.resultWidthDp,
            record.resultHeightDp,
            record.resultSmallestWidthDp,
            record.resultDensityDpi,
            widthPx,
            heightPx,
        )
    }

    private fun pixelsFor(sizeDp: Int, densityDpi: Int): Int =
        if (sizeDp <= 0 || densityDpi <= 0) 0 else round(sizeDp * (densityDpi / 160f)).toInt()

    private fun belongsToActiveWindow(packageName: String?, config: Configuration?): Boolean {
        if (config == null) return false
        if (ViewportConfigurationScope.isWindowScoped(config)) return true
        if (WindowBoundsState.matchesWindowConfiguration(packageName, config)) return true
        if (!WindowBoundsState.hasActiveWindow(packageName)) return false
        if (config.densityDpi <= 0 || config.screenWidthDp <= 0 || config.screenHeightDp <= 0) {
            return false
        }
        val width = round(config.screenWidthDp * (config.densityDpi / 160f)).toInt()
        val height = round(config.screenHeightDp * (config.densityDpi / 160f)).toInt()
        return width > 0 && height > 0 &&
            !WindowBoundsState.matchesDisplayPixels(packageName, width, height)
    }

    private fun alreadyCarriesRelativeResult(
        store: DpisConfigStore?,
        packageName: String?,
        config: Configuration?,
    ): Boolean {
        if (config == null) return false
        val record = relativeResult(store, packageName) ?: return false
        val scope = if (ViewportConfigurationScope.isWindowScoped(config)) {
            ViewportSourceSnapshot.SCOPE_WINDOW
        } else {
            ViewportSourceSnapshot.SCOPE_DISPLAY
        }
        return ViewportRuntimeMarkerBridge.configurationSignature(
            config.screenWidthDp,
            config.screenHeightDp,
            config.smallestScreenWidthDp,
            config.densityDpi,
            scope,
        ) == record.resultSignature
    }

    private fun relativeResult(
        store: DpisConfigStore?,
        packageName: String?,
    ): ViewportRuntimeMarkerBridge.MarkerRecord? {
        if (store == null || packageName.isNullOrEmpty()) return null
        val spec = store.getTargetViewportSpec(packageName)
        if (!spec.isRelativeScale) return null
        val marker = ViewportRuntimeMarkerBridge.read(
            packageName,
            spec.fingerprint(),
            RuntimeClock.crossProcessMarkerMillis(),
        )
        val record = marker.record
        if (!marker.hit || record == null || record.resultDensityDpi <= 0) return null
        return record
    }
}
