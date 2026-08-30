package com.dpis.module.config

import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.hooks.HookDomainOverride
import com.dpis.module.hooks.HookExecutionPlan
import com.dpis.module.hooks.HookExecutionPlanner
import com.dpis.module.hooks.HookRuntimePolicy
import com.dpis.module.runtime.appprocess.AppProcessHookInstaller
import com.dpis.module.runtime.font.DebugFontOverride
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec

class ModulePackagePlan private constructor(
    @JvmField val packageName: String?,
    targetViewportSpec: ViewportTargetSpec?,
    @JvmField val targetViewportMode: String?,
    @JvmField val targetFontScalePercent: Int?,
    @JvmField val targetFontMode: String?,
    @JvmField val targetTypefaceId: String?,
    @JvmField val targetDpisEnabled: Boolean,
    @JvmField val viewportConfigured: Boolean,
    @JvmField val viewportEnabled: Boolean,
    @JvmField val fontScaleActive: Boolean,
    @JvmField val fontEnabled: Boolean,
    @JvmField val typefaceActive: Boolean,
    @JvmField val typefaceEnabled: Boolean,
    @JvmField val flutterSettingsFontEnabled: Boolean,
    @JvmField val hyperOsNativeFlutterFontEnabled: Boolean,
    hookDomainOverride: HookDomainOverride?,
) {
    @JvmField val targetViewportSpec = targetViewportSpec ?: ViewportTargetSpec.off()
    @JvmField val hookDomainOverride = hookDomainOverride ?: HookDomainOverride.automatic()

    fun shouldInstallHooks(): Boolean =
        targetDpisEnabled && (viewportEnabled || fontEnabled || typefaceEnabled)

    fun shouldInstallLegacyHooks(): Boolean =
        targetDpisEnabled && (
            viewportEnabled ||
                typefaceEnabled ||
                (fontScaleActive && FontApplyMode.isEnabled(targetFontMode))
            )

    fun hasSecondaryProcessSafeRoute(): Boolean = fontEnabled || typefaceEnabled

    fun withoutViewportRoute(): ModulePackagePlan = create(
        packageName,
        ViewportTargetSpec.off(),
        ViewportApplyMode.OFF,
        targetFontScalePercent,
        targetFontMode,
        targetTypefaceId,
        targetDpisEnabled,
        false,
        false,
        fontScaleActive,
        fontEnabled,
        typefaceActive,
        typefaceEnabled,
        flutterSettingsFontEnabled,
        hyperOsNativeFlutterFontEnabled,
        hookDomainOverride,
    )

    fun buildExecutionPlan(
        policy: HookRuntimePolicy,
        debugOverride: DebugFontOverride?,
    ): HookExecutionPlan = HookExecutionPlanner.buildPlan(
        policy,
        packageName,
        viewportConfigured,
        targetViewportMode,
        fontScaleActive,
        targetFontMode,
        flutterSettingsFontEnabled,
        hyperOsNativeFlutterFontEnabled,
        hookDomainOverride,
        debugOverride,
    )

    fun targetViewportWidthDp(): Int? =
        if (targetViewportSpec.isAbsoluteDp()) targetViewportSpec.absoluteWidthDp() else null

    companion object {
        @JvmStatic
        fun resolve(store: ConfigSnapshotStore?, packageName: String?): ModulePackagePlan =
            resolve(ConfigSnapshotLoader.fromStore(store), packageName)

        @JvmStatic
        fun resolve(snapshot: ConfigSnapshot?, packageName: String?): ModulePackagePlan {
            if (snapshot == null || packageName.isNullOrBlank() || !snapshot.isConfigured(packageName)) {
                return inactive(packageName)
            }
            val config = snapshot.getPackage(packageName) ?: return inactive(packageName)
            val viewport = config.targetViewportSpec
            val fontScale = config.targetFontScalePercent
            val typeface = config.targetTypefaceId
            val fontScaleActive = fontScale != null && fontScale > 0 && fontScale != 100
            val typefaceActive = !typeface.isNullOrBlank()
            val flutterSettings = config.flutterFontHookEnabled && config.flutterSettingsFontHookEnabled
            val hyperOs = config.flutterFontHookEnabled && config.hyperOsFlutterFontHookEnabled
            if (!config.dpisEnabled || (!viewport.isEnabled() && !fontScaleActive && !typefaceActive)) {
                return create(
                    packageName,
                    viewport,
                    config.targetViewportMode,
                    fontScale,
                    config.targetFontMode,
                    typeface,
                    config.dpisEnabled,
                    viewport.isEnabled(),
                    false,
                    fontScaleActive,
                    false,
                    typefaceActive,
                    false,
                    flutterSettings,
                    hyperOs,
                    config.hookDomainOverride,
                )
            }
            val policy = HookRuntimePolicy.fromSnapshot(snapshot)
            val viewportEnabled = AppProcessHookInstaller.resolveViewportHookEnabled(
                policy, viewport.isEnabled(), config.targetViewportMode,
            )
            val fontPlan = AppProcessHookInstaller.resolveFontHookPlan(
                policy, fontScaleActive, config.targetFontMode,
            )
            return create(
                packageName,
                viewport,
                config.targetViewportMode,
                fontScale,
                config.targetFontMode,
                typeface,
                config.dpisEnabled,
                viewport.isEnabled(),
                viewportEnabled,
                fontScaleActive,
                fontPlan.emulationEnabled || fontPlan.fieldRewriteEnabled,
                typefaceActive,
                typefaceActive,
                flutterSettings,
                hyperOs,
                config.hookDomainOverride,
            )
        }

        private fun inactive(packageName: String?) = create(
            packageName,
            ViewportTargetSpec.off(),
            ViewportApplyMode.OFF,
            null,
            FontApplyMode.OFF,
            null,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            HookDomainOverride.automatic(),
        )

        private fun create(
            packageName: String?,
            viewport: ViewportTargetSpec?,
            viewportMode: String?,
            fontScale: Int?,
            fontMode: String?,
            typeface: String?,
            dpisEnabled: Boolean,
            viewportConfigured: Boolean,
            viewportEnabled: Boolean,
            fontScaleActive: Boolean,
            fontEnabled: Boolean,
            typefaceActive: Boolean,
            typefaceEnabled: Boolean,
            flutterSettings: Boolean,
            hyperOs: Boolean,
            override: HookDomainOverride?,
        ) = ModulePackagePlan(
            packageName,
            viewport,
            viewportMode,
            fontScale,
            fontMode,
            typeface,
            dpisEnabled,
            viewportConfigured,
            viewportEnabled,
            fontScaleActive,
            fontEnabled,
            typefaceActive,
            typefaceEnabled,
            flutterSettings,
            hyperOs,
            override,
        )
    }
}
