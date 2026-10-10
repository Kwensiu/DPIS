package com.dpis.module.fonts.hookdomain

import com.dpis.module.config.ConfigSnapshot
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.config.PackageConfigSnapshot
import com.dpis.module.hooks.HookDomainOverride
import com.dpis.module.hooks.HookDomainOverrideStore
import com.dpis.module.hooks.HookExecutionPlanner
import com.dpis.module.hooks.HookRuntimePolicy
import com.dpis.module.hooks.SystemScopeCoordinator
import com.dpis.module.runtime.font.DebugFontOverride
import com.dpis.module.viewport.ViewportTargetSpec

object FontHookDomainDecision {
    @JvmStatic
    fun isHyperOsNativeFlutterEnabled(store: DpisConfigStore?, packageName: String?): Boolean {
        if (store == null || packageName.isNullOrBlank()) return false
        val fontScalePercent = store.getTargetFontScalePercent(packageName)
        return isHyperOsNativeFlutterEnabled(
            HookRuntimePolicy.fromEffectiveSystemHookState(
                store,
                SystemScopeCoordinator.resolveSystemHookEffectiveEnabled(store)
            ),
            packageName,
            store.getTargetViewportSpec(packageName),
            store.getTargetViewportApplyMode(packageName),
            if (store.isTargetDpisEnabled(packageName)) fontScalePercent else null,
            store.getTargetFontApplyMode(packageName),
            HookDomainOverrideStore(store).read(packageName),
        )
    }

    @JvmStatic
    fun isHyperOsNativeFlutterEnabled(
        snapshot: ConfigSnapshot?,
        packageConfig: PackageConfigSnapshot?
    ): Boolean {
        if (snapshot == null || packageConfig == null || !packageConfig.dpisEnabled) return false
        return isHyperOsNativeFlutterEnabled(
            HookRuntimePolicy.fromEffectiveSystemHookState(
                null,
                snapshot.isSystemServerHooksEnabled()
            ),
            packageConfig.packageName,
            packageConfig.targetViewportSpec,
            packageConfig.targetViewportMode,
            packageConfig.targetFontScalePercent,
            packageConfig.targetFontMode,
            packageConfig.hookDomainOverride,
        )
    }

    private fun isHyperOsNativeFlutterEnabled(
        policy: HookRuntimePolicy,
        packageName: String,
        targetViewportSpec: ViewportTargetSpec?,
        targetViewportMode: String?,
        targetFontScalePercent: Int?,
        targetFontMode: String?,
        hookDomainOverride: HookDomainOverride?,
    ): Boolean {
        val plan = HookExecutionPlanner.buildPlan(
            policy,
            packageName,
            targetViewportSpec?.isEnabled == true,
            targetViewportMode,
            targetFontScalePercent != null && targetFontScalePercent > 0,
            targetFontMode,
            false,
            false,
            hookDomainOverride,
            DebugFontOverride.none(),
        )
        return plan.hyperOsNativeFlutterEnabled
    }
}
