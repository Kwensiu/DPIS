package com.dpis.module;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

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
        return buildPlan(
                policy,
                null,
                viewportConfigured,
                viewportMode,
                fontScaleActive,
                fontMode,
                flutterSettingsFontEnabled,
                hyperOsNativeFlutterEnabled,
                HookDomainOverride.automatic(),
                debugOverride);
    }

    static HookExecutionPlan buildPlan(HookRuntimePolicy policy,
                                       String packageName,
                                       boolean viewportConfigured,
                                       String viewportMode,
                                       boolean fontScaleActive,
                                       String fontMode,
                                       boolean flutterSettingsFontEnabled,
                                       boolean hyperOsNativeFlutterEnabled,
                                       HookDomainOverride hookDomainOverride,
                                       DebugFontOverride debugOverride) {
        return buildPlan(
                policy,
                packageName,
                viewportConfigured,
                viewportMode,
                fontScaleActive,
                fontMode,
                flutterSettingsFontEnabled,
                hyperOsNativeFlutterEnabled,
                hookDomainOverride,
                debugOverride,
                PackageFontHookDomainDefaults.resolveExactDefaults(packageName));
    }

    static HookExecutionPlan buildPlanWithBuiltinDomainsForTest(
            HookRuntimePolicy policy,
            String packageName,
            boolean viewportConfigured,
            String viewportMode,
            boolean fontScaleActive,
            String fontMode,
            boolean flutterSettingsFontEnabled,
            boolean hyperOsNativeFlutterEnabled,
            HookDomainOverride hookDomainOverride,
            DebugFontOverride debugOverride,
            Set<String> builtinDomainsForTest) {
        return buildPlan(
                policy,
                packageName,
                viewportConfigured,
                viewportMode,
                fontScaleActive,
                fontMode,
                flutterSettingsFontEnabled,
                hyperOsNativeFlutterEnabled,
                hookDomainOverride,
                debugOverride,
                builtinDomainsForTest);
    }

    private static HookExecutionPlan buildPlan(HookRuntimePolicy policy,
                                               String packageName,
                                               boolean viewportConfigured,
                                               String viewportMode,
                                               boolean fontScaleActive,
                                               String fontMode,
                                               boolean flutterSettingsFontEnabled,
                                               boolean hyperOsNativeFlutterEnabled,
                                               HookDomainOverride hookDomainOverride,
                                               DebugFontOverride debugOverride,
                                               Set<String> packageBuiltinDomains) {
        DebugFontOverride resolvedDebug = debugOverride == null
                ? DebugFontOverride.none()
                : debugOverride;
        boolean systemHooksEnabled = policy == null || policy.systemServerHooksEnabled;
        String resolvedViewportMode = viewportConfigured
                ? EffectiveModeResolver.resolveViewportMode(viewportMode, systemHooksEnabled)
                : ViewportApplyMode.OFF;
        boolean viewportEnabledBase = viewportConfigured
                && ViewportApplyMode.COMPAT.equals(resolvedViewportMode);

        FontMode resolvedFontMode = resolveFontMode(policy, fontScaleActive, fontMode);
        boolean fontRouteEnabled = resolvedFontMode != FontMode.OFF;
        boolean fieldRewriteEnabled = resolvedFontMode == FontMode.FIELD_REWRITE;
        boolean emulationEnabled = resolvedFontMode == FontMode.EMULATION;

        FontHookArbitration.FontDomainPlan automaticDomainPlan = FontHookArbitration.resolveDomainPlan(
                fontRouteEnabled,
                fieldRewriteEnabled,
                flutterSettingsFontEnabled,
                hyperOsNativeFlutterEnabled);
        Set<String> automaticDomains = toDomainSet(automaticDomainPlan);
        if (emulationEnabled) {
            automaticDomains = mergeDomains(
                    automaticDomains,
                    Collections.singleton(FontHookDomainRegistry.ID_ACTIVITY_THREAD_FONT));
        }
        Set<String> builtinDomains = FontHookDomainRegistry.orderedKnownSubset(
                packageBuiltinDomains);
        HookDomainOverride resolvedOverride = hookDomainOverride != null
                ? hookDomainOverride
                : HookDomainOverride.automatic();
        boolean customPathEffective = resolvedOverride.customPathEnabled
                && resolvedFontMode == FontMode.FIELD_REWRITE;
        if (!customPathEffective) {
            automaticDomains = mergeDomains(automaticDomains, builtinDomains);
        } else {
            builtinDomains = Collections.emptySet();
        }
        Set<String> shapedDomains = new LinkedHashSet<>(automaticDomains);
        if (resolvedDebug.forceFlutterSettings) {
            if (resolvedDebug.flutterSettingsOnly) {
                shapedDomains.clear();
            }
            shapedDomains.add(FontHookDomainRegistry.ID_FLUTTER_SETTINGS);
        }
        if (resolvedDebug.disableTextViewAbsoluteRewrite) {
            shapedDomains.remove(FontHookDomainRegistry.ID_TEXTVIEW_ABSOLUTE_REWRITE);
        }
        String hookDomainSource = "auto";
        Set<String> finalDomains = shapedDomains;
        if (customPathEffective) {
            finalDomains = filterDomainsForResolvedMode(
                    resolvedOverride.enabledKnownDomains,
                    resolvedFontMode);
            hookDomainSource = "custom";
        }
        String domainReason = automaticDomainPlan == null ? "none" : automaticDomainPlan.reason;

        Set<String> unknownDomains = resolvedOverride.unknownDomains != null
                ? resolvedOverride.unknownDomains : Collections.emptySet();
        HookDomainPlan hookDomainPlan = new HookDomainPlan(
                finalDomains, builtinDomains, unknownDomains, hookDomainSource,
                domainReason);
        FontHookArbitration.FontDomainPlan domainPlan = hookDomainPlan.toFontDomainPlan();

        boolean viewportEnabled = viewportEnabledBase;
        boolean resourcesHooksEnabled = !resolvedDebug.flutterSettingsOnly
                && (viewportEnabled || emulationEnabled
                || hookDomainPlan.hasResourcesFont());
        boolean activityThreadFontEnabled = hookDomainPlan.hasActivityThreadFont()
                && !resolvedDebug.flutterSettingsOnly;
        boolean textViewHooksEnabled = hookDomainPlan.hasTextViewHooks()
                && !resolvedDebug.flutterSettingsOnly;
        boolean webViewTextZoomEnabled = hookDomainPlan.hasWebViewTextZoom()
                && !resolvedDebug.flutterSettingsOnly;
        boolean flutterSettingsEnabled = hookDomainPlan.hasFlutterSettings();
        boolean hyperOsNativeFlutterEnabledFinal = hookDomainPlan.hasHyperOsNativeFlutter()
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
                        + ", domain=" + hookDomainPlan.reason,
                fallback,
                suppressed,
                debugReason);

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
                hookDomainPlan,
                reason,
                resolvedViewportMode,
                toFontApplyMode(resolvedFontMode),
                resolvedDebug.forceFlutterSettings,
                resolvedDebug.flutterSettingsOnly,
                resolvedDebug.disableTextViewAbsoluteRewrite,
                probeHooksRequested,
                probeInstallMode,
                hookDomainPlan.enabledDomainsCsv(),
                hookDomainPlan.source,
                hookDomainPlan.builtinDomainsCsv(),
                hookDomainPlan.unknownDomainsCsv());
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

    private static Set<String> toDomainSet(FontHookArbitration.FontDomainPlan domainPlan) {
        LinkedHashSet<String> domains = new LinkedHashSet<>();
        if (domainPlan == null) {
            return domains;
        }
        if (domainPlan.resourcesFontEnabled) {
            domains.add(FontHookDomainRegistry.ID_RESOURCES_FONT);
        }
        if (domainPlan.textViewSpRewriteEnabled) {
            domains.add(FontHookDomainRegistry.ID_TEXTVIEW_SP_REWRITE);
        }
        if (domainPlan.textViewAbsoluteRewriteEnabled) {
            domains.add(FontHookDomainRegistry.ID_TEXTVIEW_ABSOLUTE_REWRITE);
        }
        if (domainPlan.textViewCurrentPxFallbackEnabled) {
            domains.add(FontHookDomainRegistry.ID_TEXTVIEW_CURRENT_PX_FALLBACK);
        }
        if (domainPlan.paintFallbackEnabled) {
            domains.add(FontHookDomainRegistry.ID_PAINT_TEXT_SIZE_FALLBACK);
        }
        if (domainPlan.webViewTextZoomEnabled) {
            domains.add(FontHookDomainRegistry.ID_WEBVIEW_TEXT_ZOOM);
        }
        if (domainPlan.flutterSettingsEnabled) {
            domains.add(FontHookDomainRegistry.ID_FLUTTER_SETTINGS);
        }
        if (domainPlan.hyperOsNativeFlutterEnabled) {
            domains.add(FontHookDomainRegistry.ID_HYPEROS_NATIVE_FLUTTER);
        }
        return FontHookDomainRegistry.orderedKnownSubset(domains);
    }

    private static Set<String> mergeDomains(Set<String> left, Set<String> right) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (left != null) {
            merged.addAll(left);
        }
        if (right != null) {
            merged.addAll(right);
        }
        return FontHookDomainRegistry.orderedKnownSubset(merged);
    }

    private static Set<String> filterDomainsForResolvedMode(Set<String> domains, FontMode mode) {
        LinkedHashSet<String> filtered = new LinkedHashSet<>(
                FontHookDomainRegistry.orderedKnownSubset(domains));
        if (mode != FontMode.EMULATION) {
            filtered.remove(FontHookDomainRegistry.ID_ACTIVITY_THREAD_FONT);
        }
        return filtered;
    }
}
