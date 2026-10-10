package com.dpis.module.hooks

import com.dpis.module.fonts.hookdomain.FontHookArbitration
import com.dpis.module.runtime.appprocess.ResourcesReadHookPolicy

class HookExecutionPlan(
    @JvmField val fontMode: FontMode,
    @JvmField val viewportEnabled: Boolean,
    @JvmField val resourcesHooksEnabled: Boolean,
    @JvmField val resourcesWriteHooksEnabled: Boolean,
    @JvmField val resourcesImplHookEnabled: Boolean,
    @JvmField val resourcesReadHooksEnabled: Boolean,
    resourcesReadPolicy: ResourcesReadHookPolicy?,
    @JvmField val activityThreadFontEnabled: Boolean,
    @JvmField val textViewHooksEnabled: Boolean,
    @JvmField val webViewTextZoomEnabled: Boolean,
    @JvmField val flutterSettingsEnabled: Boolean,
    @JvmField val hyperOsNativeFlutterEnabled: Boolean,
    @JvmField val resourcesProbeEnabled: Boolean,
    @JvmField val viewportProbeEnabled: Boolean,
    @JvmField val fontDomainPlan: FontHookArbitration.FontDomainPlan,
    @JvmField val domainPlan: HookDomainPlan,
    @JvmField val reason: PlanReason,
    @JvmField val resolvedViewportMode: String,
    @JvmField val resolvedFontMode: String,
    @JvmField val debugForceFlutterSettings: Boolean,
    @JvmField val debugFlutterSettingsOnly: Boolean,
    @JvmField val debugDisableTextViewAbsoluteRewrite: Boolean,
    @JvmField val debugDisableActivityThreadFont: Boolean,
    @JvmField val probeHooksRequested: Boolean,
    @JvmField val probeInstallMode: String,
    @JvmField val hookDomains: String,
    @JvmField val hookDomainSource: String,
    @JvmField val builtinDomains: String,
    @JvmField val unknownCustomDomains: String,
) {
    @JvmField
    val resourcesReadPolicy: ResourcesReadHookPolicy =
        resourcesReadPolicy ?: ResourcesReadHookPolicy.FULL
}
