package com.dpis.module.config

import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.hooks.HookDomainOverride
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec

class PackageConfigSnapshot(
    packageName: String?,
    @JvmField val dpisEnabled: Boolean,
    targetViewportSpec: ViewportTargetSpec,
    targetViewportMode: String?,
    @JvmField val targetFontScalePercent: Int?,
    targetFontMode: String?,
    @JvmField val targetTypefaceId: String?,
    @JvmField val flutterFontHookEnabled: Boolean,
    @JvmField val flutterSettingsFontHookEnabled: Boolean,
    @JvmField val hyperOsFlutterFontHookEnabled: Boolean,
    hookDomainOverride: HookDomainOverride,
) {
    @JvmField val packageName: String = packageName ?: ""
    @JvmField val targetViewportSpec: ViewportTargetSpec = targetViewportSpec
    @JvmField val targetViewportMode: String = ViewportApplyMode.normalize(targetViewportMode)
    @JvmField val targetFontMode: String = FontApplyMode.normalize(targetFontMode)
    @JvmField val hookDomainOverride: HookDomainOverride = hookDomainOverride

    constructor(
        packageName: String,
        dpisEnabled: Boolean,
        targetViewportWidthDp: Int?,
        targetViewportMode: String?,
        targetFontScalePercent: Int?,
        targetFontMode: String?,
        targetTypefaceId: String?,
        flutterFontHookEnabled: Boolean,
        flutterSettingsFontHookEnabled: Boolean,
        hyperOsFlutterFontHookEnabled: Boolean,
    ) : this(
        packageName,
        dpisEnabled,
        targetViewportWidthDp?.let(ViewportTargetSpec::absoluteDp) ?: ViewportTargetSpec.off(),
        targetViewportMode ?: ViewportApplyMode.OFF,
        targetFontScalePercent,
        targetFontMode ?: FontApplyMode.OFF,
        targetTypefaceId,
        flutterFontHookEnabled,
        flutterSettingsFontHookEnabled,
        hyperOsFlutterFontHookEnabled,
        HookDomainOverride.automatic(),
    )

    constructor(
        packageName: String,
        dpisEnabled: Boolean,
        targetViewportWidthDp: Int?,
        targetViewportMode: String?,
        targetFontScalePercent: Int?,
        targetFontMode: String?,
        targetTypefaceId: String?,
        flutterFontHookEnabled: Boolean,
        flutterSettingsFontHookEnabled: Boolean,
        hyperOsFlutterFontHookEnabled: Boolean,
        hookDomainOverride: HookDomainOverride?,
    ) : this(
        packageName,
        dpisEnabled,
        targetViewportWidthDp?.let(ViewportTargetSpec::absoluteDp) ?: ViewportTargetSpec.off(),
        targetViewportMode,
        targetFontScalePercent,
        targetFontMode,
        targetTypefaceId,
        flutterFontHookEnabled,
        flutterSettingsFontHookEnabled,
        hyperOsFlutterFontHookEnabled,
        hookDomainOverride ?: HookDomainOverride.automatic(),
    )

    constructor(
        packageName: String,
        dpisEnabled: Boolean,
        targetViewportSpec: ViewportTargetSpec,
        targetViewportWidthDp: Int?,
        targetViewportMode: String?,
        targetFontScalePercent: Int?,
        targetFontMode: String?,
        targetTypefaceId: String?,
        flutterFontHookEnabled: Boolean,
        flutterSettingsFontHookEnabled: Boolean,
        hyperOsFlutterFontHookEnabled: Boolean,
        hookDomainOverride: HookDomainOverride,
    ) : this(
        packageName,
        dpisEnabled,
        targetViewportSpec,
        targetViewportMode,
        targetFontScalePercent,
        targetFontMode,
        targetTypefaceId,
        flutterFontHookEnabled,
        flutterSettingsFontHookEnabled,
        hyperOsFlutterFontHookEnabled,
        hookDomainOverride,
    )

}
