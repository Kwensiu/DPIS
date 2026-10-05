package com.dpis.module.viewport

import android.content.res.Configuration
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.hooks.HookRuntimePolicy
import com.dpis.module.runtime.appprocess.WebApkCarrierResolver
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
     * Display-shaped configuration stays with system_server. A window
     * configuration is still scaled here, because the system result is the
     * full display and does not fit the window.
     */
    @JvmStatic
    fun shouldDefer(
        store: DpisConfigStore?,
        packageName: String?,
        policy: HookRuntimePolicy?,
        config: Configuration?,
    ): Boolean {
        if (!shouldDefer(store, packageName, policy)) return false
        return !belongsToActiveWindow(packageName, config)
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
}
