package com.dpis.module;

final class HookExecutionPlanner {
    private HookExecutionPlanner() {
    }

    static HookExecutionPlan buildPlan(HookRuntimePolicy policy,
                                       boolean viewportConfigured,
                                       String viewportMode,
                                       boolean fontScaleActive,
                                       String fontMode,
                                       boolean flutterSettingsFontEnabled,
                                       boolean hyperOsNativeFlutterEnabled,
                                       DebugFontOverride debugOverride) {
        DebugFontOverride resolvedDebug = debugOverride == null
                ? DebugFontOverride.none()
                : debugOverride;
        boolean systemHooksEnabled = policy == null || policy.systemServerHooksEnabled;
        String resolvedViewportMode = viewportConfigured
                ? EffectiveModeResolver.resolveViewportMode(viewportMode, systemHooksEnabled)
                : ViewportApplyMode.OFF;
        boolean viewportEnabledBase = viewportConfigured
                && !ViewportApplyMode.OFF.equals(resolvedViewportMode);

        FontMode resolvedFontMode = resolveFontMode(policy, fontScaleActive, fontMode);
        boolean fontRouteEnabled = resolvedFontMode != FontMode.OFF;
        boolean fieldRewriteEnabled = resolvedFontMode == FontMode.FIELD_REWRITE;
        boolean emulationEnabled = resolvedFontMode == FontMode.EMULATION;

        FontHookArbitration.FontDomainPlan domainPlan = FontHookArbitration.resolveDomainPlan(
                fontRouteEnabled,
                fieldRewriteEnabled,
                flutterSettingsFontEnabled,
                hyperOsNativeFlutterEnabled);
        if (resolvedDebug.forceFlutterSettings) {
            domainPlan = createDebugFlutterSettingsPlan(domainPlan, resolvedDebug.flutterSettingsOnly);
        }
        if (resolvedDebug.disableTextViewAbsoluteRewrite) {
            domainPlan = createDebugDisableTextViewAbsoluteRewritePlan(domainPlan);
        }

        boolean viewportEnabled = viewportEnabledBase;
        boolean resourcesHooksEnabled = !resolvedDebug.flutterSettingsOnly
                && (viewportEnabled || emulationEnabled
                || (domainPlan != null && domainPlan.resourcesFontEnabled));
        boolean activityThreadFontEnabled = emulationEnabled && !resolvedDebug.flutterSettingsOnly;
        boolean textViewHooksEnabled = domainPlan != null
                && domainPlan.textViewHooksEnabled
                && !resolvedDebug.flutterSettingsOnly;
        boolean webViewTextZoomEnabled = domainPlan != null
                && domainPlan.webViewTextZoomEnabled
                && !resolvedDebug.flutterSettingsOnly;
        boolean flutterSettingsEnabled = domainPlan != null && domainPlan.flutterSettingsEnabled;
        boolean hyperOsNativeFlutterEnabledFinal = domainPlan != null
                && domainPlan.hyperOsNativeFlutterEnabled
                && !resolvedDebug.flutterSettingsOnly;

        boolean probeHooksRequested = policy != null && policy.probeHooksEnabled;
        boolean resourcesProbeEnabled = probeHooksRequested && resourcesHooksEnabled;
        boolean viewportProbeEnabled = probeHooksRequested && viewportEnabled;
        String probeInstallMode = resolveProbeInstallMode(policy, probeHooksRequested);

        String fallback = resolveFallbackReason(
                viewportConfigured,
                viewportMode,
                resolvedViewportMode,
                fontScaleActive,
                fontMode,
                resolvedFontMode,
                systemHooksEnabled,
                policy);
        String suppressed = resolvedDebug.flutterSettingsOnly
                ? "debug-flutter-settings-only"
                : "none";
        String debugReason = resolvedDebug.flutterSettingsOnly
                ? "flutter-settings-only"
                : resolveDebugReason(resolvedDebug);
        PlanReason reason = new PlanReason(
                "font=" + resolvedFontMode.name().toLowerCase()
                        + ", viewport=" + resolvedViewportMode
                        + ", domain=" + (domainPlan == null ? "none" : domainPlan.reason),
                fallback,
                suppressed,
                debugReason,
                "none");

        return new HookExecutionPlan(
                resolvedFontMode,
                viewportEnabled,
                resourcesHooksEnabled,
                activityThreadFontEnabled,
                textViewHooksEnabled,
                webViewTextZoomEnabled,
                flutterSettingsEnabled,
                hyperOsNativeFlutterEnabledFinal,
                resourcesProbeEnabled,
                viewportProbeEnabled,
                domainPlan,
                reason,
                resolvedViewportMode,
                toFontApplyMode(resolvedFontMode),
                resolvedDebug.forceFlutterSettings,
                resolvedDebug.flutterSettingsOnly,
                resolvedDebug.disableTextViewAbsoluteRewrite,
                probeHooksRequested,
                probeInstallMode);
    }

    static boolean resolveViewportHookEnabled(HookRuntimePolicy policy,
                                              boolean viewportConfigured,
                                              String viewportMode) {
        if (!viewportConfigured) {
            return false;
        }
        boolean systemHooksEnabled = policy == null || policy.systemServerHooksEnabled;
        String normalized = EffectiveModeResolver.resolveViewportMode(viewportMode, systemHooksEnabled);
        return !ViewportApplyMode.OFF.equals(normalized);
    }

    static FontMode resolveFontMode(HookRuntimePolicy policy,
                                    boolean fontScaleActive,
                                    String fontMode) {
        if (!fontScaleActive) {
            return FontMode.OFF;
        }
        boolean systemHooksEnabled = policy == null || policy.systemServerHooksEnabled;
        String normalized = EffectiveModeResolver.resolveFontMode(fontMode, systemHooksEnabled);
        if (FontApplyMode.SYSTEM_EMULATION.equals(normalized)) {
            return FontMode.EMULATION;
        }
        if (FontApplyMode.FIELD_REWRITE.equals(normalized)) {
            return FontMode.FIELD_REWRITE;
        }
        return FontMode.OFF;
    }

    static String resolveProbeInstallMode(HookRuntimePolicy policy) {
        return resolveProbeInstallMode(policy, policy != null && policy.probeHooksEnabled);
    }

    private static String resolveProbeInstallMode(HookRuntimePolicy policy,
                                                  boolean probeHooksRequested) {
        if (probeHooksRequested) {
            return "full";
        }
        return policy != null && policy.systemServerSafeModeEnabled
                ? "safe mode"
                : "probe disabled";
    }

    private static String toFontApplyMode(FontMode mode) {
        if (mode == FontMode.EMULATION) {
            return FontApplyMode.SYSTEM_EMULATION;
        }
        if (mode == FontMode.FIELD_REWRITE) {
            return FontApplyMode.FIELD_REWRITE;
        }
        return FontApplyMode.OFF;
    }

    private static String resolveFallbackReason(boolean viewportConfigured,
                                                String requestedViewportMode,
                                                String resolvedViewportMode,
                                                boolean fontScaleActive,
                                                String requestedFontMode,
                                                FontMode resolvedFontMode,
                                                boolean systemHooksEnabled,
                                                HookRuntimePolicy policy) {
        if (!systemHooksEnabled
                && ViewportApplyMode.SYSTEM_EMULATION.equals(
                ViewportApplyMode.normalize(requestedViewportMode))
                && ViewportApplyMode.OFF.equals(resolvedViewportMode)) {
            return "viewport-system-hooks-off";
        }
        if (!systemHooksEnabled
                && fontScaleActive
                && FontApplyMode.SYSTEM_EMULATION.equals(FontApplyMode.normalize(requestedFontMode))
                && resolvedFontMode == FontMode.OFF) {
            return "font-system-hooks-off";
        }
        if (policy != null && policy.systemServerSafeModeEnabled) {
            return "safe-mode";
        }
        if (!viewportConfigured && !fontScaleActive) {
            return "inactive";
        }
        return "none";
    }

    private static FontHookArbitration.FontDomainPlan createDebugFlutterSettingsPlan(
            FontHookArbitration.FontDomainPlan source,
            boolean flutterSettingsOnly) {
        if (source == null) {
            return new FontHookArbitration.FontDomainPlan(
                    false, false, false, false, false,
                    false, false, true, false, false,
                    "debug-flutter-settings-domain-plan");
        }
        return new FontHookArbitration.FontDomainPlan(
                !flutterSettingsOnly && source.resourcesFontEnabled,
                !flutterSettingsOnly && source.webViewTextZoomEnabled,
                !flutterSettingsOnly && source.textViewHooksEnabled,
                !flutterSettingsOnly && source.textViewSpRewriteEnabled,
                !flutterSettingsOnly && source.textViewAbsoluteRewriteEnabled,
                !flutterSettingsOnly && source.textViewCurrentPxFallbackEnabled,
                !flutterSettingsOnly && source.paintFallbackEnabled,
                true,
                !flutterSettingsOnly && source.hyperOsNativeFlutterEnabled,
                false,
                flutterSettingsOnly
                        ? "debug-flutter-settings-only-domain-plan"
                        : source.reason + "+debug-flutter-settings");
    }

    private static FontHookArbitration.FontDomainPlan createDebugDisableTextViewAbsoluteRewritePlan(
            FontHookArbitration.FontDomainPlan source) {
        if (source == null || !source.textViewAbsoluteRewriteEnabled) {
            return source;
        }
        return new FontHookArbitration.FontDomainPlan(
                source.resourcesFontEnabled,
                source.webViewTextZoomEnabled,
                source.textViewHooksEnabled,
                source.textViewSpRewriteEnabled,
                false,
                source.textViewCurrentPxFallbackEnabled,
                source.paintFallbackEnabled,
                source.flutterSettingsEnabled,
                source.hyperOsNativeFlutterEnabled,
                source.genericNativeFlutterEnabled,
                source.reason + "+debug-disable-textview-absolute");
    }

    private static String resolveDebugReason(DebugFontOverride debugOverride) {
        if (debugOverride.forceFlutterSettings && debugOverride.disableTextViewAbsoluteRewrite) {
            return "force-flutter-settings+disable-textview-absolute";
        }
        if (debugOverride.forceFlutterSettings) {
            return "force-flutter-settings";
        }
        if (debugOverride.disableTextViewAbsoluteRewrite) {
            return "disable-textview-absolute";
        }
        return "none";
    }
}
