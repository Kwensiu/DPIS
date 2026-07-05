package com.dpis.module;

import com.dpis.module.hooks.HookExecutionPlan;
import com.dpis.module.hooks.HookExecutionPlanner;
import com.dpis.module.hooks.HookRuntimePolicy;
import com.dpis.module.runtime.hookapi.ModernApiCapabilities;
import com.dpis.module.runtime.hookapi.ModernApiCapabilitiesResolver;

import com.dpis.module.runtime.DebugPackageOverride;
import com.dpis.module.runtime.RuntimeDiagnosticLogFingerprint;

import io.github.libxposed.api.XposedInterface;

final class AppProcessHookInstaller {
    private static final String PROP_FORCE_FLUTTER_SETTINGS_PACKAGE =
            "debug.dpis.font.force_flutter_settings_package";
    private static final String PROP_FLUTTER_SETTINGS_ONLY_PACKAGE =
            "debug.dpis.font.flutter_settings_only_package";
    private static final String PROP_DISABLE_TEXTVIEW_ABSOLUTE_REWRITE_PACKAGE =
            "debug.dpis.font.disable_textview_absolute_rewrite_package";
    private static final String PROP_DISABLE_ACTIVITY_THREAD_PACKAGE =
            "debug.dpis.font.disable_activity_thread_package";
    private static final String PROP_DISABLE_VIEWPORT_DISPLAY_SUPPLEMENT_PACKAGE =
            "debug.dpis.viewport.disable_display_supplement_package";
    private static final String PROP_DISABLE_VIEWPORT_RESOURCES_IMPL_PACKAGE =
            "debug.dpis.viewport.disable_resources_impl_package";
    private static final String PROP_DISABLE_VIEWPORT_RESOURCES_READ_PACKAGE =
            "debug.dpis.viewport.disable_resources_read_package";

    private AppProcessHookInstaller() {
    }

    static void install(XposedInterface xposed,
                        DpisConfigStore store,
                        HookRuntimePolicy policy,
                        ModulePackagePlan packagePlan,
                        ModernApiCapabilities apiCapabilities) throws Throwable {
        String packageName = packagePlan.packageName;
        DebugFontOverride debugOverride = resolveDebugFontOverrideForPackage(packageName);
        HookExecutionPlan plan = packagePlan.buildExecutionPlan(policy, debugOverride);
        DpisLog.i("DPIS_FONT app hook plan: package=" + packageName
                + ", " + RuntimeDiagnosticLogFingerprint.field()
                + ", fontScaleActive=" + packagePlan.fontScaleActive
                + ", fontMode=" + packagePlan.targetFontMode
                + ", resolvedFontMode=" + plan.resolvedFontMode
                + ", resolvedViewportMode=" + plan.resolvedViewportMode
                + ", domain=" + plan.fontDomainPlan.reason
                + ", flutterSettings=" + plan.flutterSettingsEnabled
                + ", hyperOsNativeFlutter=" + plan.hyperOsNativeFlutterEnabled
                + ", debugForceFlutterSettings=" + plan.debugForceFlutterSettings
                + ", debugFlutterSettingsOnly=" + plan.debugFlutterSettingsOnly
                + ", debugDisableTextViewAbsoluteRewrite=" + plan.debugDisableTextViewAbsoluteRewrite
                + ", debugDisableActivityThreadFont=" + plan.debugDisableActivityThreadFont
                + ", hookDomains=" + plan.hookDomains
                + ", hookDomainSource=" + plan.hookDomainSource
                + ", builtinDomains=" + plan.builtinDomains
                + ", unknownCustomDomains=" + plan.unknownCustomDomains
                + ", reason={" + plan.reason.formatForLog() + "}");
        if (packagePlan.typefaceActive) {
            installTypefaceHooks(xposed, packageName, store, packagePlan.targetTypefaceId);
        }
        if (plan.viewportEnabled) {
            AppProcessViewportStateSeeder.seedDisplayBaseline(
                    packageName,
                    packagePlan.targetViewportSpec,
                    packagePlan.targetViewportMode,
                    policy == null || policy.systemServerHooksEnabled);
        }
        WebApkRuntimeOwnerBridge.installLifecycleHooks(xposed, packageName, apiCapabilities);
        installFromPlan(
                xposed, packageName, store, policy, plan, packagePlan.targetViewportSpec,
                apiCapabilities);
        if (plan.probeHooksRequested) {
            DpisLog.i("hooks installed (full): viewportEnabled=" + plan.viewportEnabled
                    + ", viewportMode=" + packagePlan.targetViewportMode
                    + ", fontMode=" + packagePlan.targetFontMode + " for " + packageName);
            return;
        }
        DpisLog.i("hooks installed (" + plan.probeInstallMode + "): viewportEnabled=" + plan.viewportEnabled
                + ", viewportMode=" + packagePlan.targetViewportMode
                + ", fontMode=" + packagePlan.targetFontMode
                + ", fontDomainPlan=" + plan.fontDomainPlan.reason
                + ", resourcesFont=" + plan.fontDomainPlan.resourcesFontEnabled
                + ", textViewSpRewrite=" + plan.fontDomainPlan.textViewSpRewriteEnabled
                + ", textViewAbsoluteRewrite="
                + plan.fontDomainPlan.textViewAbsoluteRewriteEnabled
                + ", textViewCurrentPxFallback="
                + plan.fontDomainPlan.textViewCurrentPxFallbackEnabled
                + ", paintFallback=" + plan.fontDomainPlan.paintFallbackEnabled
                + ", flutterSettings=" + plan.fontDomainPlan.flutterSettingsEnabled
                + ", hyperOsNativeFlutter=" + plan.fontDomainPlan.hyperOsNativeFlutterEnabled
                + ", genericNativeFlutter=" + plan.fontDomainPlan.genericNativeFlutterEnabled
                + ", hookDomains=" + plan.hookDomains
                + ", hookDomainSource=" + plan.hookDomainSource
                + ", builtinDomains=" + plan.builtinDomains
                + ", unknownCustomDomains=" + plan.unknownCustomDomains
                + ", debugForceFlutterSettings=" + plan.debugForceFlutterSettings
                + ", debugFlutterSettingsOnly=" + plan.debugFlutterSettingsOnly
                + ", debugDisableTextViewAbsoluteRewrite=" + plan.debugDisableTextViewAbsoluteRewrite
                + ", debugDisableActivityThreadFont=" + plan.debugDisableActivityThreadFont
                + " for " + packageName);
    }

    static boolean shouldInstallProbeHooks(HookRuntimePolicy policy) {
        return policy != null && policy.probeHooksEnabled;
    }

    static String resolveProbeInstallMode(HookRuntimePolicy policy) {
        return HookExecutionPlanner.resolveProbeInstallMode(policy);
    }

    static boolean resolveViewportHookEnabled(HookRuntimePolicy policy,
                                              boolean viewportConfigured,
                                              String viewportMode) {
        return HookExecutionPlanner.resolveViewportHookEnabled(
                policy, viewportConfigured, viewportMode);
    }

    static FontHookArbitration.FontDomainPlan resolveFontDomainPlan(FontHookPlan fontHookPlan) {
        return resolveFontDomainPlan(fontHookPlan, false);
    }

    static FontHookArbitration.FontDomainPlan resolveFontDomainPlan(FontHookPlan fontHookPlan,
                                                                    boolean hyperOsNativeFlutterEnabled) {
        return resolveFontDomainPlan(fontHookPlan, false, hyperOsNativeFlutterEnabled);
    }

    static FontHookArbitration.FontDomainPlan resolveFontDomainPlan(FontHookPlan fontHookPlan,
                                                                    boolean flutterSettingsEnabled,
                                                                    boolean hyperOsNativeFlutterEnabled) {
        return FontHookArbitration.resolveDomainPlan(
                fontHookPlan != null
                        && (fontHookPlan.emulationEnabled || fontHookPlan.fieldRewriteEnabled),
                fontHookPlan != null && fontHookPlan.fieldRewriteEnabled,
                flutterSettingsEnabled,
                hyperOsNativeFlutterEnabled);
    }

    static boolean resolveResourcesHooksEnabled(boolean viewportEnabled,
                                                FontHookPlan fontHookPlan,
                                                FontHookArbitration.FontDomainPlan domainPlan) {
        return viewportEnabled
                || (fontHookPlan != null && fontHookPlan.emulationEnabled)
                || (domainPlan != null && domainPlan.resourcesFontEnabled);
    }

    static boolean isDebugPropertyPackageMatchForTest(String propertyName,
                                                      String packageName,
                                                      String propertyValue) {
        return DebugPackageOverride.matchesForTest(propertyName, packageName, propertyValue);
    }

    private static boolean isDebugPropertyPackageMatch(String propertyName, String packageName) {
        return DebugPackageOverride.matches(propertyName, packageName);
    }

    static FontHookPlan resolveFontHookPlan(HookRuntimePolicy policy,
                                            boolean fontScaleActive,
                                            String fontMode) {
        FontMode resolved = HookExecutionPlanner.resolveFontMode(policy, fontScaleActive, fontMode);
        return new FontHookPlan(
                resolved == FontMode.EMULATION,
                resolved == FontMode.FIELD_REWRITE);
    }

    static DebugFontOverride resolveDebugFontOverrideForPackage(String packageName) {
        boolean debugFlutterSettingsOnly = isDebugPropertyPackageMatch(
                PROP_FLUTTER_SETTINGS_ONLY_PACKAGE, packageName);
        boolean debugForceFlutterSettings = debugFlutterSettingsOnly
                || isDebugPropertyPackageMatch(PROP_FORCE_FLUTTER_SETTINGS_PACKAGE, packageName);
        boolean disableTextViewAbsoluteRewrite = isDebugPropertyPackageMatch(
                PROP_DISABLE_TEXTVIEW_ABSOLUTE_REWRITE_PACKAGE, packageName);
        boolean disableActivityThreadFont = isDebugPropertyPackageMatch(
                PROP_DISABLE_ACTIVITY_THREAD_PACKAGE, packageName);
        return DebugFontOverride.of(
                debugForceFlutterSettings,
                debugFlutterSettingsOnly,
                disableTextViewAbsoluteRewrite,
                disableActivityThreadFont);
    }

    private static void installFromPlan(XposedInterface xposed,
                                        String packageName,
                                        DpisConfigStore store,
                                        HookRuntimePolicy policy,
                                        HookExecutionPlan plan,
                                        ViewportTargetSpec targetViewportSpec,
                                        ModernApiCapabilities apiCapabilities) throws Throwable {
        if (ComposeFontRuntimeDiagnosticsInstaller.shouldInstall(plan)) {
            ComposeFontRuntimeDiagnosticsInstaller.install(
                    xposed,
                    packageName,
                    store,
                    plan.fontDomainPlan,
                    plan.hookDomains,
                    plan.hookDomainSource);
        }
        if (plan.resourcesHooksEnabled) {
            if (plan.resourcesWriteHooksEnabled) {
                ResourcesManagerHookInstaller.install(
                        xposed, packageName, store, policy, apiCapabilities);
            } else {
                DpisLog.i("Resources write hooks skipped: package=" + packageName);
            }
            if (!shouldInstallResourcesImplHook(plan)) {
                DpisLog.i("ResourcesImpl hook skipped: package=" + packageName);
            } else if (isDebugPropertyPackageMatch(
                    PROP_DISABLE_VIEWPORT_RESOURCES_IMPL_PACKAGE, packageName)) {
                DpisLog.i("ResourcesImpl hook skipped by debug property for " + packageName);
            } else {
                ResourcesImplHookInstaller.install(
                        xposed, packageName, store, policy, apiCapabilities);
            }
            if (!plan.resourcesReadHooksEnabled) {
                DpisLog.i("ResourcesRead hook skipped: package=" + packageName);
            } else if (isDebugPropertyPackageMatch(
                    PROP_DISABLE_VIEWPORT_RESOURCES_READ_PACKAGE, packageName)) {
                DpisLog.i("ResourcesRead hook skipped by debug property for " + packageName);
            } else {
                ResourcesReadHookInstaller.install(
                        xposed,
                        packageName,
                        store,
                        policy,
                        plan.resourcesReadPolicy,
                        apiCapabilities);
            }
        }
        if (plan.activityThreadFontEnabled) {
            ActivityThreadFontHookInstaller.install(
                    xposed, packageName, store, apiCapabilities);
        }
        if (plan.textViewHooksEnabled) {
            ForceTextSizeHookInstaller.install(
                    xposed,
                    packageName,
                    store,
                    plan.fontDomainPlan,
                    apiCapabilities);
        }
        if (plan.flutterSettingsEnabled) {
            DpisLog.i("DPIS_FONT installing Flutter settings font hooks for " + packageName);
            FlutterSettingsFontHookInstaller.install(xposed, packageName, store, plan.fontDomainPlan);
        }
        if (plan.hyperOsNativeFlutterEnabled) {
            DpisLog.i("DPIS_FONT installing HyperOS native Flutter font hooks for " + packageName);
            HyperOsFlutterFontHookInstaller.install(xposed, packageName, store);
        }
        if (plan.webViewTextZoomEnabled) {
            WebViewFontHookInstaller.install(
                    xposed, packageName, store, apiCapabilities);
        }
        if (shouldInstallAppProcessViewportSupplementHooks(plan, targetViewportSpec)
                && !isViewportDisplaySupplementDisabled(packageName)) {
            // Keep 102 replaceable anchors for the small viewport supplement hooks.
            WindowMetricsHookInstaller.install(xposed, packageName);
            DisplayHookInstaller.install(xposed, packageName, store);
        } else if (plan.viewportEnabled) {
            DpisLog.i("viewport app-process supplement hooks skipped: package="
                    + packageName
                    + ", resolvedViewportMode=" + plan.resolvedViewportMode
                    + ", targetViewportSpec=" + targetViewportSpec);
        }
        if (plan.resourcesProbeEnabled) {
            ResourcesProbeHookInstaller.install(xposed, packageName, store, policy);
        }
        if (plan.viewportProbeEnabled) {
            WindowManagerProbeHookInstaller.install(xposed, packageName);
            WindowSessionProbeHookInstaller.install(xposed);
            ViewRootProbeHookInstaller.install(xposed, packageName);
        }
    }

    static boolean shouldInstallAppProcessViewportSupplementHooksForTest(
            HookExecutionPlan plan,
            ViewportTargetSpec targetViewportSpec) {
        return shouldInstallAppProcessViewportSupplementHooks(plan, targetViewportSpec);
    }

    static boolean shouldInstallResourcesImplHookForTest(HookExecutionPlan plan) {
        return shouldInstallResourcesImplHook(plan);
    }

    private static boolean shouldInstallAppProcessViewportSupplementHooks(
            HookExecutionPlan plan,
            ViewportTargetSpec targetViewportSpec) {
        if (plan == null || !plan.viewportEnabled) {
            return false;
        }
        return !ViewportApplyMode.SYSTEM.equals(plan.resolvedViewportMode);
    }

    private static boolean shouldInstallResourcesImplHook(HookExecutionPlan plan) {
        return plan != null
                && (plan.resourcesWriteHooksEnabled || plan.resourcesImplHookEnabled);
    }

    private static boolean isViewportDisplaySupplementDisabled(String packageName) {
        return isDebugPropertyPackageMatch(
                PROP_DISABLE_VIEWPORT_DISPLAY_SUPPLEMENT_PACKAGE, packageName);
    }

    static void installTypefaceHooks(XposedInterface xposed,
                                     String packageName,
                                     DpisConfigStore store,
                                     String targetTypefaceId) {
        try {
            DpisLog.i("DPIS_FONT_STYLE install requested: package=" + packageName
                    + ", targetTypefaceId=" + targetTypefaceId);
            TypefaceOverrideHookInstaller.install(
                    xposed,
                    packageName,
                    targetTypefaceId,
                    store,
                    ConfigStoreFactory.createFontLibraryForXposedHost(xposed),
                    ModernApiCapabilitiesResolver.fromXposed(xposed));
        } catch (Throwable throwable) {
            DpisLog.e("failed to install typeface hooks: package=" + packageName, throwable);
        }
    }

    static final class FontHookPlan {
        final boolean emulationEnabled;
        final boolean fieldRewriteEnabled;

        FontHookPlan(boolean emulationEnabled,
                     boolean fieldRewriteEnabled) {
            this.emulationEnabled = emulationEnabled;
            this.fieldRewriteEnabled = fieldRewriteEnabled;
        }
    }

}
