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
                        String packageName,
                        DpiConfigStore store,
                        HookRuntimePolicy policy,
                        boolean viewportConfigured,
                        String viewportMode,
                        String fontMode,
                        boolean fontScaleActive,
                        boolean flutterSettingsFontEnabled,
                        boolean hyperOsNativeFlutterEnabled) throws Throwable {
        DebugFontOverride debugOverride = resolveDebugFontOverrideForPackage(packageName);
        HookExecutionPlan plan = HookExecutionPlanner.buildPlan(
                policy,
                viewportConfigured,
                viewportMode,
                fontScaleActive,
                fontMode,
                flutterSettingsFontEnabled,
                hyperOsNativeFlutterEnabled,
                debugOverride);
        DpisLog.i("DPIS_FONT app hook plan: package=" + packageName
                + ", fontScaleActive=" + fontScaleActive
                + ", fontMode=" + fontMode
                + ", resolvedFontMode=" + plan.resolvedFontMode
                + ", resolvedViewportMode=" + plan.resolvedViewportMode
                + ", domain=" + plan.fontDomainPlan.reason
                + ", flutterSettings=" + plan.flutterSettingsEnabled
                + ", hyperOsNativeFlutter=" + plan.hyperOsNativeFlutterEnabled
                + ", debugForceFlutterSettings=" + plan.debugForceFlutterSettings
                + ", debugFlutterSettingsOnly=" + plan.debugFlutterSettingsOnly
                + ", debugDisableTextViewAbsoluteRewrite=" + plan.debugDisableTextViewAbsoluteRewrite
                + ", reason={" + plan.reason.formatForLog() + "}");
        installFromPlan(xposed, packageName, store, plan);
        if (plan.probeHooksRequested) {
            DpisLog.i("hooks installed (full): viewportEnabled=" + plan.viewportEnabled
                    + ", viewportMode=" + viewportMode
                    + ", fontMode=" + fontMode + " for " + packageName);
            return;
        }
        DpisLog.i("hooks installed (" + plan.probeInstallMode + "): viewportEnabled=" + plan.viewportEnabled
                + ", viewportMode=" + viewportMode
                + ", fontMode=" + fontMode
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
                + ", debugForceFlutterSettings=" + plan.debugForceFlutterSettings
                + ", debugFlutterSettingsOnly=" + plan.debugFlutterSettingsOnly
                + ", debugDisableTextViewAbsoluteRewrite=" + plan.debugDisableTextViewAbsoluteRewrite
                + " for " + packageName);
        if (!"none".equals(plan.reason.downgrade)) {
            DpisLog.i("safe mode downgraded font apply mode to emulation for " + packageName);
        }
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
                resolved == FontMode.FIELD_REWRITE,
                false);
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
                                        HookExecutionPlan plan) throws Throwable {
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
        if (plan.viewportEnabled) {
            WindowMetricsHookInstaller.install(xposed);
            DisplayHookInstaller.install(xposed, packageName);
        }
        if (plan.resourcesProbeEnabled) {
            ResourcesProbeHookInstaller.install(xposed, packageName, store);
        }
        if (plan.viewportProbeEnabled) {
            WindowManagerProbeHookInstaller.install(xposed, packageName);
            WindowSessionProbeHookInstaller.install(xposed);
            ViewRootProbeHookInstaller.install(xposed);
        }
    }

    static final class FontHookPlan {
        final boolean emulationEnabled;
        final boolean fieldRewriteEnabled;
        final boolean downgradedToEmulation;

        FontHookPlan(boolean emulationEnabled,
                     boolean fieldRewriteEnabled,
                     boolean downgradedToEmulation) {
            this.emulationEnabled = emulationEnabled;
            this.fieldRewriteEnabled = fieldRewriteEnabled;
            this.downgradedToEmulation = downgradedToEmulation;
        }
    }
}
