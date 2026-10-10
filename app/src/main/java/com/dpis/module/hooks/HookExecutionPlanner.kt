package com.dpis.module.hooks

import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.hookdomain.FontHookArbitration
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import com.dpis.module.fonts.hookdomain.PackageFontHookDomainDefaults
import com.dpis.module.runtime.appprocess.ResourcesReadHookPolicy
import com.dpis.module.runtime.font.DebugFontOverride
import com.dpis.module.viewport.EffectiveModeResolver
import com.dpis.module.viewport.ViewportApplyMode

object HookExecutionPlanner {
    @JvmStatic
    fun buildPlan(
        policy: HookRuntimePolicy?, viewportConfigured: Boolean, viewportMode: String?,
        fontScaleActive: Boolean, fontMode: String?, flutterSettingsFontEnabled: Boolean,
        hyperOsNativeFlutterEnabled: Boolean, debugOverride: DebugFontOverride?,
    ): HookExecutionPlan = buildPlan(
        policy, null, viewportConfigured, viewportMode, fontScaleActive, fontMode,
        flutterSettingsFontEnabled, hyperOsNativeFlutterEnabled,
        HookDomainOverride.automatic(), debugOverride,
    )

    @JvmStatic
    fun buildPlan(
        policy: HookRuntimePolicy?, packageName: String?, viewportConfigured: Boolean,
        viewportMode: String?, fontScaleActive: Boolean, fontMode: String?,
        flutterSettingsFontEnabled: Boolean, hyperOsNativeFlutterEnabled: Boolean,
        hookDomainOverride: HookDomainOverride?, debugOverride: DebugFontOverride?,
    ): HookExecutionPlan = buildPlan(
        policy, packageName, viewportConfigured, viewportMode, fontScaleActive, fontMode,
        flutterSettingsFontEnabled, hyperOsNativeFlutterEnabled, hookDomainOverride,
        debugOverride, PackageFontHookDomainDefaults.resolveExactDefaults(packageName),
    )

    @JvmStatic
    fun buildPlanWithBuiltinDomainsForTest(
        policy: HookRuntimePolicy?, packageName: String?, viewportConfigured: Boolean,
        viewportMode: String?, fontScaleActive: Boolean, fontMode: String?,
        flutterSettingsFontEnabled: Boolean, hyperOsNativeFlutterEnabled: Boolean,
        hookDomainOverride: HookDomainOverride?, debugOverride: DebugFontOverride?,
        builtinDomainsForTest: Set<String>?,
    ): HookExecutionPlan = buildPlan(
        policy, packageName, viewportConfigured, viewportMode, fontScaleActive, fontMode,
        flutterSettingsFontEnabled, hyperOsNativeFlutterEnabled, hookDomainOverride,
        debugOverride, builtinDomainsForTest,
    )

    private fun buildPlan(
        policy: HookRuntimePolicy?, packageName: String?, viewportConfigured: Boolean,
        viewportMode: String?, fontScaleActive: Boolean, fontMode: String?,
        flutterSettingsFontEnabled: Boolean, hyperOsNativeFlutterEnabled: Boolean,
        hookDomainOverride: HookDomainOverride?, debugOverride: DebugFontOverride?,
        packageBuiltinDomains: Set<String>?,
    ): HookExecutionPlan {
        val debug = debugOverride ?: DebugFontOverride.none()
        val systemHooksEnabled = policy?.systemServerHooksEnabled ?: true
        val resolvedViewportMode = if (viewportConfigured) {
            EffectiveModeResolver.resolveViewportMode(viewportMode, systemHooksEnabled)
        } else ViewportApplyMode.OFF
        val viewportEnabledBase =
            shouldInstallViewportRoute(viewportConfigured, viewportMode, resolvedViewportMode)
        val resolvedFontMode = resolveFontMode(policy, fontScaleActive, fontMode)
        val fontRouteEnabled = resolvedFontMode != FontMode.OFF
        val fieldRewriteEnabled = resolvedFontMode == FontMode.FIELD_REWRITE
        val emulationEnabled = resolvedFontMode == FontMode.EMULATION
        val automaticDomainPlan = FontHookArbitration.resolveDomainPlan(
            fontRouteEnabled,
            fieldRewriteEnabled,
            flutterSettingsFontEnabled,
            hyperOsNativeFlutterEnabled,
        )
        var automaticDomains = toDomainSet(automaticDomainPlan)
        if (emulationEnabled) automaticDomains = mergeDomains(
            automaticDomains,
            setOf(
                FontHookDomainRegistry.ID_SYSTEM_SERVER_FONT,
                FontHookDomainRegistry.ID_ACTIVITY_THREAD_FONT
            ),
        )
        var builtinDomains =
            FontHookDomainRegistry.orderedKnownSubset(packageBuiltinDomains.orEmpty())
        val override = hookDomainOverride ?: HookDomainOverride.automatic()
        val customPathEffective =
            override.customPathEnabled && resolvedFontMode == FontMode.FIELD_REWRITE
        if (!customPathEffective) automaticDomains = mergeDomains(automaticDomains, builtinDomains)
        else builtinDomains = emptySet()
        val shapedDomains = automaticDomains.toMutableSet()
        if (debug.forceFlutterSettings) {
            if (debug.flutterSettingsOnly) shapedDomains.clear()
            shapedDomains.add(FontHookDomainRegistry.ID_FLUTTER_SETTINGS)
        }
        if (debug.disableTextViewAbsoluteRewrite) shapedDomains.remove(FontHookDomainRegistry.ID_TEXTVIEW_ABSOLUTE_REWRITE)
        if (debug.disableActivityThreadFont) shapedDomains.remove(FontHookDomainRegistry.ID_ACTIVITY_THREAD_FONT)
        val hookDomainSource: String
        val finalDomains: Set<String>
        if (customPathEffective) {
            finalDomains =
                filterDomainsForResolvedMode(override.enabledKnownDomains, resolvedFontMode)
            hookDomainSource = "custom"
        } else {
            finalDomains = shapedDomains
            hookDomainSource = "auto"
        }
        val domainPlan = HookDomainPlan(
            finalDomains, builtinDomains, override.unknownDomains,
            hookDomainSource, automaticDomainPlan?.reason ?: "none",
        )
        val fontDomainPlan = domainPlan.toFontDomainPlan()
        val resourcesWrite = !debug.flutterSettingsOnly && (viewportEnabledBase || emulationEnabled)
        val resourcesImpl =
            resourcesWrite || (!debug.flutterSettingsOnly && domainPlan.hasResourcesFont())
        val resourcesRead =
            !debug.flutterSettingsOnly && (resourcesImpl || domainPlan.hasResourcesFont())
        val readPolicy = ResourcesReadHookPolicy(
            !debug.flutterSettingsOnly && (viewportEnabledBase || emulationEnabled),
            !debug.flutterSettingsOnly && fieldRewriteEnabled && domainPlan.hasResourcesFont(),
            !debug.flutterSettingsOnly && emulationEnabled && domainPlan.hasResourcesFont(),
        )
        val resourcesHooks = resourcesWrite || resourcesImpl || resourcesRead
        val activityThread = domainPlan.hasActivityThreadFont() && !debug.flutterSettingsOnly
        val textView = domainPlan.hasTextViewHooks() && !debug.flutterSettingsOnly
        val webView = domainPlan.hasWebViewTextZoom() && !debug.flutterSettingsOnly
        val probes = policy?.probeHooksEnabled == true
        val probeMode = resolveProbeInstallMode(policy, probes)
        val reason = PlanReason(
            "font=${resolvedFontMode.name.lowercase()}, viewport=$resolvedViewportMode, domain=${domainPlan.reason}",
            resolveFallbackReason(
                viewportConfigured, viewportMode, resolvedViewportMode, fontScaleActive, fontMode,
                resolvedFontMode, systemHooksEnabled, policy
            ),
            if (debug.flutterSettingsOnly) "debug-flutter-settings-only" else "none",
            if (debug.flutterSettingsOnly) "flutter-settings-only" else resolveDebugReason(debug),
        )
        return HookExecutionPlan(
            resolvedFontMode,
            viewportEnabledBase,
            resourcesHooks,
            resourcesWrite,
            resourcesImpl,
            resourcesRead,
            readPolicy,
            activityThread,
            textView,
            webView,
            domainPlan.hasFlutterSettings(),
            domainPlan.hasHyperOsNativeFlutter() && !debug.flutterSettingsOnly,
            resourcesHooks && probes,
            viewportEnabledBase && probes,
            fontDomainPlan,
            domainPlan,
            reason,
            resolvedViewportMode,
            toFontApplyMode(resolvedFontMode),
            debug.forceFlutterSettings,
            debug.flutterSettingsOnly,
            debug.disableTextViewAbsoluteRewrite,
            debug.disableActivityThreadFont,
            probes,
            probeMode,
            domainPlan.enabledDomainsCsv(),
            domainPlan.source,
            domainPlan.builtinDomainsCsv(),
            domainPlan.unknownDomainsCsv(),
        )
    }

    @JvmStatic
    fun resolveViewportHookEnabled(
        policy: HookRuntimePolicy?,
        viewportConfigured: Boolean,
        viewportMode: String?
    ): Boolean {
        if (!viewportConfigured) return false
        val enabled = policy?.systemServerHooksEnabled ?: true
        val resolved = EffectiveModeResolver.resolveViewportMode(viewportMode, enabled)
        return shouldInstallViewportRoute(true, viewportMode, resolved)
    }

    private fun shouldInstallViewportRoute(
        configured: Boolean,
        requested: String?,
        resolved: String
    ): Boolean {
        if (!configured) return false
        ViewportApplyMode.normalize(requested)
        return resolved == ViewportApplyMode.COMPAT || resolved == ViewportApplyMode.SYSTEM
    }

    @JvmStatic
    fun resolveFontMode(
        policy: HookRuntimePolicy?,
        fontScaleActive: Boolean,
        fontMode: String?
    ): FontMode {
        if (!fontScaleActive) return FontMode.OFF
        val normalized = EffectiveModeResolver.resolveFontMode(
            fontMode,
            policy?.systemServerHooksEnabled ?: true
        )
        return when (normalized) {
            FontApplyMode.SYSTEM_EMULATION -> FontMode.EMULATION
            FontApplyMode.FIELD_REWRITE -> FontMode.FIELD_REWRITE
            else -> FontMode.OFF
        }
    }

    @JvmStatic
    fun resolveProbeInstallMode(policy: HookRuntimePolicy?): String =
        resolveProbeInstallMode(policy, policy?.probeHooksEnabled == true)

    private fun resolveProbeInstallMode(policy: HookRuntimePolicy?, requested: Boolean): String =
        when {
            requested -> "full"
            policy?.systemServerSafeModeEnabled == true -> "safe mode"
            else -> "probe disabled"
        }

    private fun toFontApplyMode(mode: FontMode) = when (mode) {
        FontMode.EMULATION -> FontApplyMode.SYSTEM_EMULATION
        FontMode.FIELD_REWRITE -> FontApplyMode.FIELD_REWRITE
        FontMode.OFF -> FontApplyMode.OFF
    }

    private fun resolveFallbackReason(
        viewportConfigured: Boolean, requestedViewport: String?, resolvedViewport: String,
        fontScaleActive: Boolean, requestedFont: String?, resolvedFont: FontMode,
        systemHooks: Boolean, policy: HookRuntimePolicy?,
    ): String = when {
        !systemHooks && ViewportApplyMode.SYSTEM_EMULATION == ViewportApplyMode.normalize(
            requestedViewport
        ) &&
                ViewportApplyMode.OFF == resolvedViewport -> "viewport-system-hooks-off"

        !systemHooks && fontScaleActive && FontApplyMode.SYSTEM_EMULATION == FontApplyMode.normalize(
            requestedFont
        ) &&
                resolvedFont == FontMode.OFF -> "font-system-hooks-off"

        policy?.systemServerSafeModeEnabled == true -> "safe-mode"
        !viewportConfigured && !fontScaleActive -> "inactive"
        else -> "none"
    }

    private fun resolveDebugReason(debug: DebugFontOverride): String = buildList {
        if (debug.forceFlutterSettings) add("force-flutter-settings")
        if (debug.disableTextViewAbsoluteRewrite) add("disable-textview-absolute")
        if (debug.disableActivityThreadFont) add("disable-activity-thread-font")
    }.joinToString("+").ifEmpty { "none" }

    private fun toDomainSet(plan: FontHookArbitration.FontDomainPlan?): Set<String> = buildSet {
        if (plan == null) return@buildSet
        if (plan.resourcesFontEnabled) add(FontHookDomainRegistry.ID_RESOURCES_FONT)
        if (plan.textViewSpRewriteEnabled) add(FontHookDomainRegistry.ID_TEXTVIEW_SP_REWRITE)
        if (plan.textViewAbsoluteRewriteEnabled) add(FontHookDomainRegistry.ID_TEXTVIEW_ABSOLUTE_REWRITE)
        if (plan.textViewCurrentPxFallbackEnabled) add(FontHookDomainRegistry.ID_TEXTVIEW_CURRENT_PX_FALLBACK)
        if (plan.paintFallbackEnabled) add(FontHookDomainRegistry.ID_PAINT_TEXT_SIZE_FALLBACK)
        if (plan.webViewTextZoomEnabled) add(FontHookDomainRegistry.ID_WEBVIEW_TEXT_ZOOM)
        if (plan.flutterSettingsEnabled) add(FontHookDomainRegistry.ID_FLUTTER_SETTINGS)
        if (plan.hyperOsNativeFlutterEnabled) add(FontHookDomainRegistry.ID_HYPEROS_NATIVE_FLUTTER)
    }.let(FontHookDomainRegistry::orderedKnownSubset)

    private fun mergeDomains(left: Set<String>, right: Set<String>) =
        FontHookDomainRegistry.orderedKnownSubset(left + right)

    private fun filterDomainsForResolvedMode(domains: Set<String>, mode: FontMode): Set<String> =
        FontHookDomainRegistry.orderedKnownSubset(domains).toMutableSet().apply {
            if (mode != FontMode.EMULATION) remove(FontHookDomainRegistry.ID_ACTIVITY_THREAD_FONT)
        }
}
