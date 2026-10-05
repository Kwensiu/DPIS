package com.dpis.module.viewport

import com.dpis.module.config.DpisConfigStore
import com.dpis.module.hooks.HookRuntimePolicy
import com.dpis.module.runtime.appprocess.WebApkCarrierResolver

object ViewportModePolicy {
    @JvmStatic
    fun resolve(store: DpisConfigStore?, packageName: String?): String {
        if (store == null || packageName.isNullOrEmpty()) return ViewportApplyMode.OFF
        return resolve(store.isSystemServerHooksEnabled(), store, packageName)
    }

    @JvmStatic
    fun resolve(policy: HookRuntimePolicy?, store: DpisConfigStore?, packageName: String?): String {
        if (store == null || packageName.isNullOrEmpty()) return ViewportApplyMode.OFF
        return resolve(policy?.systemServerHooksEnabled ?: true, store, packageName)
    }

    private fun resolve(
        systemHooksEnabled: Boolean,
        store: DpisConfigStore,
        packageName: String
    ): String =
        EffectiveModeResolver.resolveViewportMode(
            store.getTargetViewportApplyMode(packageName),
            systemHooksEnabled
        )

    @JvmStatic
    fun shouldApplyConfigurationOverride(store: DpisConfigStore?, packageName: String?): Boolean {
        if (shouldApplyWebApkOwnerConfigurationOverride(store, packageName)) return true
        return ViewportApplyMode.COMPAT == resolve(store, packageName)
    }

    @JvmStatic
    fun shouldApplyConfigurationOverride(
        policy: HookRuntimePolicy?,
        store: DpisConfigStore?,
        packageName: String?,
    ): Boolean {
        if (shouldApplyWebApkOwnerConfigurationOverride(store, packageName)) return true
        return ViewportApplyMode.COMPAT == resolve(policy, store, packageName)
    }

    @JvmStatic
    fun shouldApplyConfigurationOverride(
        store: DpisConfigStore?,
        packageName: String?,
        resolution: ViewportTargetResolution?,
        viewportNeedsUpdate: Boolean,
    ): Boolean {
        if (shouldApplyConfigurationOverride(store, packageName)) return true
        if (!viewportNeedsUpdate || store == null || packageName.isNullOrEmpty() ||
            resolution == null || !resolution.hasTarget()
        ) return false
        val requestedMode =
            ViewportApplyMode.normalize(store.getTargetViewportApplyMode(packageName))
        return ViewportApplyMode.AUTO == requestedMode && store.isSystemServerHooksEnabled()
    }

    @JvmStatic
    fun shouldApplyConfigurationOverride(
        policy: HookRuntimePolicy?,
        store: DpisConfigStore?,
        packageName: String?,
        resolution: ViewportTargetResolution?,
        viewportNeedsUpdate: Boolean,
    ): Boolean {
        if (shouldApplyConfigurationOverride(policy, store, packageName)) return true
        if (!viewportNeedsUpdate || store == null || packageName.isNullOrEmpty() ||
            resolution == null || !resolution.hasTarget()
        ) return false
        val requestedMode =
            ViewportApplyMode.normalize(store.getTargetViewportApplyMode(packageName))
        val systemHooksEnabled = policy?.systemServerHooksEnabled ?: true
        return ViewportApplyMode.AUTO == requestedMode && systemHooksEnabled
    }

    private fun shouldApplyWebApkOwnerConfigurationOverride(
        store: DpisConfigStore?,
        packageName: String?,
    ): Boolean {
        // WebAPK owner routing is an app-process bridge and remains compat-capable.
        return store != null && packageName != null &&
                WebApkCarrierResolver.isWebApkOwnerPackage(packageName) &&
                store.isTargetDpisEnabled(packageName) &&
                store.getTargetViewportSpec(packageName).isEnabled()
    }
}
