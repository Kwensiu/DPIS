package com.dpis.module;

import io.github.libxposed.api.XposedInterface;

final class AppProcessHookInstaller {
    private static final String PROP_FORCE_FLUTTER_SETTINGS_PACKAGE =
            "debug.dpis.font.force_flutter_settings_package";
    private static final String PROP_FLUTTER_SETTINGS_ONLY_PACKAGE =
            "debug.dpis.font.flutter_settings_only_package";
    private static final String PROP_DISABLE_TEXTVIEW_ABSOLUTE_REWRITE_PACKAGE =
            "debug.dpis.font.disable_textview_absolute_rewrite_package";

    private AppProcessHookInstaller() {
    }

    static void install(XposedInterface xposed,
                        DpiConfigStore store,
                        HookRuntimePolicy policy,
                        ModulePackagePlan packagePlan) throws Throwable {
        String packageName = packagePlan.packageName;
        DebugFontOverride debugOverride = resolveDebugFontOverrideForPackage(packageName);
        HookExecutionPlan plan = packagePlan.buildExecutionPlan(policy, debugOverride);
        DpisLog.i("DPIS_FONT app hook plan: package=" + packageName
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
                + ", hookDomains=" + plan.hookDomains
                + ", hookDomainSource=" + plan.hookDomainSource
                + ", builtinDomains=" + plan.builtinDomains
                + ", unknownCustomDomains=" + plan.unknownCustomDomains
                + ", reason={" + plan.reason.formatForLog() + "}");
        if (packagePlan.typefaceActive) {
            installTypefaceHooks(xposed, packageName, store, packagePlan.targetTypefaceId);
        }
        if (plan.viewportEnabled) {
            AppProcessViewportStateSeeder.seedAbsoluteTarget(
                    packageName,
                    packagePlan.targetViewportSpec,
                    packagePlan.targetViewportMode,
                    policy == null || policy.systemServerHooksEnabled);
        }
        installFromPlan(xposed, packageName, store, plan, packagePlan.targetViewportSpec);
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
        return isDebugPropertyPackageMatch(propertyName, packageName, () -> propertyValue);
    }

    private static boolean isDebugPropertyPackageMatch(String propertyName, String packageName) {
        return isDebugPropertyPackageMatch(propertyName, packageName,
                () -> readSystemProperty(propertyName));
    }

    private static boolean isDebugPropertyPackageMatch(String propertyName,
                                                       String packageName,
                                                       PropertyReader reader) {
        if (!BuildConfig.DEBUG || packageName == null || packageName.isBlank()) {
            return false;
        }
        String value = reader.read();
        if (value == null) {
            return false;
        }
        String normalized = value.trim();
        return "*".equals(normalized) || packageName.equals(normalized);
    }

    private static String readSystemProperty(String key) {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            return (String) systemProperties
                    .getMethod("get", String.class, String.class)
                    .invoke(null, key, "");
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return "";
        }
    }

    private interface PropertyReader {
        String read();
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
        return DebugFontOverride.of(
                debugForceFlutterSettings,
                debugFlutterSettingsOnly,
                disableTextViewAbsoluteRewrite);
    }

    private static void installFromPlan(XposedInterface xposed,
                                        String packageName,
                                        DpiConfigStore store,
                                        HookExecutionPlan plan,
                                        ViewportTargetSpec targetViewportSpec) throws Throwable {
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
            ResourcesManagerHookInstaller.install(xposed, packageName, store);
            ResourcesImplHookInstaller.install(xposed, packageName, store);
            ResourcesReadHookInstaller.install(xposed, packageName, store);
        }
        if (plan.activityThreadFontEnabled) {
            ActivityThreadFontHookInstaller.install(xposed, packageName, store);
        }
        if (plan.textViewHooksEnabled) {
            ForceTextSizeHookInstaller.install(xposed, packageName, store, plan.fontDomainPlan);
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
            WebViewFontHookInstaller.install(xposed, packageName, store);
        }
        if (shouldInstallAppProcessViewportSupplementHooks(plan, targetViewportSpec)) {
            WindowMetricsHookInstaller.install(xposed);
            DisplayHookInstaller.install(xposed, packageName, store);
        } else if (plan.viewportEnabled) {
            DpisLog.i("viewport app-process supplement hooks skipped: package="
                    + packageName
                    + ", resolvedViewportMode=" + plan.resolvedViewportMode
                    + ", targetViewportSpec=" + targetViewportSpec);
        }
        if (plan.resourcesProbeEnabled) {
            ResourcesProbeHookInstaller.install(xposed, packageName, store);
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

    private static boolean shouldInstallAppProcessViewportSupplementHooks(
            HookExecutionPlan plan,
            ViewportTargetSpec targetViewportSpec) {
        if (plan == null || !plan.viewportEnabled) {
            return false;
        }
        return !ViewportApplyMode.SYSTEM.equals(plan.resolvedViewportMode);
    }

    static void installTypefaceHooks(XposedInterface xposed,
                                     String packageName,
                                     DpiConfigStore store,
                                     String targetTypefaceId) {
        try {
            DpisLog.i("DPIS_FONT_STYLE install requested: package=" + packageName
                    + ", targetTypefaceId=" + targetTypefaceId);
            TypefaceOverrideHookInstaller.install(
                    xposed,
                    packageName,
                    targetTypefaceId,
                    store,
                    ConfigStoreFactory.createFontLibraryForXposedHost(xposed));
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
