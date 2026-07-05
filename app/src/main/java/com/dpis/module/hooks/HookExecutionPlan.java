package com.dpis.module.hooks;

import com.dpis.module.runtime.appprocess.ResourcesReadHookPolicy;

import com.dpis.module.fonts.hookdomain.FontHookArbitration;

public final class HookExecutionPlan {
    public final FontMode fontMode;
    public final boolean viewportEnabled;
    public final boolean resourcesHooksEnabled;
    public final boolean resourcesWriteHooksEnabled;
    public final boolean resourcesImplHookEnabled;
    public final boolean resourcesReadHooksEnabled;
    public final ResourcesReadHookPolicy resourcesReadPolicy;
    public final boolean activityThreadFontEnabled;
    public final boolean textViewHooksEnabled;
    public final boolean webViewTextZoomEnabled;
    public final boolean flutterSettingsEnabled;
    public final boolean hyperOsNativeFlutterEnabled;
    public final boolean resourcesProbeEnabled;
    public final boolean viewportProbeEnabled;
    public final FontHookArbitration.FontDomainPlan fontDomainPlan;
    public final HookDomainPlan domainPlan;
    public final PlanReason reason;
    public final String resolvedViewportMode;
    public final String resolvedFontMode;
    public final boolean debugForceFlutterSettings;
    public final boolean debugFlutterSettingsOnly;
    public final boolean debugDisableTextViewAbsoluteRewrite;
    public final boolean debugDisableActivityThreadFont;
    public final boolean probeHooksRequested;
    public final String probeInstallMode;
    public final String hookDomains;
    public final String hookDomainSource;
    public final String builtinDomains;
    public final String unknownCustomDomains;

    public HookExecutionPlan(FontMode fontMode,
                      boolean viewportEnabled,
                      boolean resourcesHooksEnabled,
                      boolean resourcesWriteHooksEnabled,
                      boolean resourcesImplHookEnabled,
                      boolean resourcesReadHooksEnabled,
                      ResourcesReadHookPolicy resourcesReadPolicy,
                      boolean activityThreadFontEnabled,
                      boolean textViewHooksEnabled,
                      boolean webViewTextZoomEnabled,
                      boolean flutterSettingsEnabled,
                      boolean hyperOsNativeFlutterEnabled,
                      boolean resourcesProbeEnabled,
                      boolean viewportProbeEnabled,
                      FontHookArbitration.FontDomainPlan fontDomainPlan,
                      HookDomainPlan domainPlan,
                      PlanReason reason,
                      String resolvedViewportMode,
                      String resolvedFontMode,
                      boolean debugForceFlutterSettings,
                      boolean debugFlutterSettingsOnly,
                      boolean debugDisableTextViewAbsoluteRewrite,
                      boolean debugDisableActivityThreadFont,
                      boolean probeHooksRequested,
                      String probeInstallMode,
                      String hookDomains,
                      String hookDomainSource,
                      String builtinDomains,
                      String unknownCustomDomains) {
        this.fontMode = fontMode;
        this.viewportEnabled = viewportEnabled;
        this.resourcesHooksEnabled = resourcesHooksEnabled;
        this.resourcesWriteHooksEnabled = resourcesWriteHooksEnabled;
        this.resourcesImplHookEnabled = resourcesImplHookEnabled;
        this.resourcesReadHooksEnabled = resourcesReadHooksEnabled;
        this.resourcesReadPolicy = resourcesReadPolicy != null
                ? resourcesReadPolicy
                : ResourcesReadHookPolicy.FULL;
        this.activityThreadFontEnabled = activityThreadFontEnabled;
        this.textViewHooksEnabled = textViewHooksEnabled;
        this.webViewTextZoomEnabled = webViewTextZoomEnabled;
        this.flutterSettingsEnabled = flutterSettingsEnabled;
        this.hyperOsNativeFlutterEnabled = hyperOsNativeFlutterEnabled;
        this.resourcesProbeEnabled = resourcesProbeEnabled;
        this.viewportProbeEnabled = viewportProbeEnabled;
        this.fontDomainPlan = fontDomainPlan;
        this.domainPlan = domainPlan;
        this.reason = reason;
        this.resolvedViewportMode = resolvedViewportMode;
        this.resolvedFontMode = resolvedFontMode;
        this.debugForceFlutterSettings = debugForceFlutterSettings;
        this.debugFlutterSettingsOnly = debugFlutterSettingsOnly;
        this.debugDisableTextViewAbsoluteRewrite = debugDisableTextViewAbsoluteRewrite;
        this.debugDisableActivityThreadFont = debugDisableActivityThreadFont;
        this.probeHooksRequested = probeHooksRequested;
        this.probeInstallMode = probeInstallMode;
        this.hookDomains = hookDomains;
        this.hookDomainSource = hookDomainSource;
        this.builtinDomains = builtinDomains;
        this.unknownCustomDomains = unknownCustomDomains;
    }
}
