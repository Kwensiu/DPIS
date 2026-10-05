package com.dpis.module.viewport

import android.content.res.Configuration
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.hooks.HookRuntimePolicy
import com.dpis.module.runtime.appprocess.WebApkCarrierResolver
import com.dpis.module.runtime.probe.RuntimeClock
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
     * A display-shaped configuration is left with system_server only after it
     * already carries the relative result. A still-physical configuration is
     * written once here, with the same density, so a title and a list cannot
     * use two sizes. A window configuration is still scaled here, because the
     * system result is the full display and does not fit the window.
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
        return alreadyCarriesRelativeResult(store, packageName, config)
    }

    /**
     * Display metrics stay untouched only when their density is already the
     * relative result. Physical metrics are still rewritten once.
     */
    @JvmStatic
    fun metricsCarryRelativeResult(
        store: DpisConfigStore?,
        packageName: String?,
        densityDpi: Int,
    ): Boolean {
        if (densityDpi <= 0) return false
        val record = relativeResult(store, packageName) ?: return false
        return record.resultDensityDpi == densityDpi
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
        return ViewportApplyMode.SYSTEM == EffectiveModeResolver.resolveViewportMode(
            store.getTargetViewportApplyMode(packageName),
            true,
        )
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
        return width > 0 && height > 0 && !WindowBoundsState.matchesDisplayPixels(width, height)
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
