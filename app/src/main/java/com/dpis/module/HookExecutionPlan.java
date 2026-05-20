package com.dpis.module;

final class HookExecutionPlan {
    final FontMode fontMode;
    final boolean viewportEnabled;
    final boolean resourcesHooksEnabled;
    final boolean activityThreadFontEnabled;
    final boolean textViewHooksEnabled;
    final boolean webViewTextZoomEnabled;
    final boolean flutterSettingsEnabled;
    final boolean hyperOsNativeFlutterEnabled;
    final boolean resourcesProbeEnabled;
    final boolean viewportProbeEnabled;
    final FontHookArbitration.FontDomainPlan fontDomainPlan;
    final PlanReason reason;
    final String resolvedViewportMode;
    final String resolvedFontMode;
    final boolean debugForceFlutterSettings;
    final boolean debugFlutterSettingsOnly;
    final boolean debugDisableTextViewAbsoluteRewrite;
    final boolean probeHooksRequested;
    final String probeInstallMode;
    final String hookDomains;
    final String hookDomainSource;
    final String builtinDomains;
    final String unknownCustomDomains;

    HookExecutionPlan(FontMode fontMode,
                      boolean viewportEnabled,
                      boolean resourcesHooksEnabled,
                      boolean activityThreadFontEnabled,
                      boolean textViewHooksEnabled,
                      boolean webViewTextZoomEnabled,
                      boolean flutterSettingsEnabled,
                      boolean hyperOsNativeFlutterEnabled,
                      boolean resourcesProbeEnabled,
                      boolean viewportProbeEnabled,
                      FontHookArbitration.FontDomainPlan fontDomainPlan,
                      PlanReason reason,
                      String resolvedViewportMode,
                      String resolvedFontMode,
                      boolean debugForceFlutterSettings,
                      boolean debugFlutterSettingsOnly,
                      boolean debugDisableTextViewAbsoluteRewrite,
                      boolean probeHooksRequested,
                      String probeInstallMode,
                      String hookDomains,
                      String hookDomainSource,
                      String builtinDomains,
                      String unknownCustomDomains) {
        this.fontMode = fontMode;
        this.viewportEnabled = viewportEnabled;
        this.resourcesHooksEnabled = resourcesHooksEnabled;
        this.activityThreadFontEnabled = activityThreadFontEnabled;
        this.textViewHooksEnabled = textViewHooksEnabled;
        this.webViewTextZoomEnabled = webViewTextZoomEnabled;
        this.flutterSettingsEnabled = flutterSettingsEnabled;
        this.hyperOsNativeFlutterEnabled = hyperOsNativeFlutterEnabled;
        this.resourcesProbeEnabled = resourcesProbeEnabled;
        this.viewportProbeEnabled = viewportProbeEnabled;
        this.fontDomainPlan = fontDomainPlan;
        this.reason = reason;
        this.resolvedViewportMode = resolvedViewportMode;
        this.resolvedFontMode = resolvedFontMode;
        this.debugForceFlutterSettings = debugForceFlutterSettings;
        this.debugFlutterSettingsOnly = debugFlutterSettingsOnly;
        this.debugDisableTextViewAbsoluteRewrite = debugDisableTextViewAbsoluteRewrite;
        this.probeHooksRequested = probeHooksRequested;
        this.probeInstallMode = probeInstallMode;
        this.hookDomains = hookDomains;
        this.hookDomainSource = hookDomainSource;
        this.builtinDomains = builtinDomains;
        this.unknownCustomDomains = unknownCustomDomains;
    }
}
