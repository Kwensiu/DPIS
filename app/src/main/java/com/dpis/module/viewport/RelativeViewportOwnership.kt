package com.dpis.module.viewport

import com.dpis.module.config.DpisConfigStore
import com.dpis.module.hooks.HookRuntimePolicy
import com.dpis.module.runtime.appprocess.WebApkCarrierResolver

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
}
