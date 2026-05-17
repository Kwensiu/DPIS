package com.dpis.module;

import io.github.libxposed.api.XposedInterface;

final class AppProcessHookInstaller {
    private static final String PROP_FORCE_FLUTTER_SETTINGS_PACKAGE =
            "debug.dpis.font.force_flutter_settings_package";
    private static final String PROP_FLUTTER_SETTINGS_ONLY_PACKAGE =
            "debug.dpis.font.flutter_settings_only_package";

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
        boolean viewportEnabled = resolveViewportHookEnabled(policy, viewportConfigured, viewportMode);
        FontHookPlan fontHookPlan = resolveFontHookPlan(policy, fontScaleActive, fontMode);
        boolean emulationEnabled = fontHookPlan.emulationEnabled;
        FontHookArbitration.FontDomainPlan fontDomainPlan = resolveFontDomainPlan(
                fontHookPlan, flutterSettingsFontEnabled, hyperOsNativeFlutterEnabled);
        boolean debugFlutterSettingsOnly = isDebugPropertyPackageMatch(
                PROP_FLUTTER_SETTINGS_ONLY_PACKAGE, packageName);
        boolean debugForceFlutterSettings = debugFlutterSettingsOnly
                || isDebugPropertyPackageMatch(PROP_FORCE_FLUTTER_SETTINGS_PACKAGE, packageName);
        if (debugForceFlutterSettings) {
            fontDomainPlan = createDebugFlutterSettingsPlan(fontDomainPlan, debugFlutterSettingsOnly);
        }
        DpisLog.i("DPIS_FONT app hook plan: package=" + packageName
                + ", fontScaleActive=" + fontScaleActive
                + ", fontMode=" + fontMode
                + ", emulation=" + fontHookPlan.emulationEnabled
                + ", fieldRewrite=" + fontHookPlan.fieldRewriteEnabled
                + ", domain=" + fontDomainPlan.reason
                + ", flutterSettings=" + fontDomainPlan.flutterSettingsEnabled
                + ", hyperOsNativeFlutter=" + fontDomainPlan.hyperOsNativeFlutterEnabled
                + ", genericNativeFlutter=" + fontDomainPlan.genericNativeFlutterEnabled
                + ", debugForceFlutterSettings=" + debugForceFlutterSettings
                + ", debugFlutterSettingsOnly=" + debugFlutterSettingsOnly);
        boolean resourcesHooksEnabled =
                !debugFlutterSettingsOnly
                        && resolveResourcesHooksEnabled(viewportEnabled, fontHookPlan, fontDomainPlan);
        if (resourcesHooksEnabled) {
            ResourcesManagerHookInstaller.install(xposed, packageName, store);
        }
        if (resourcesHooksEnabled) {
            ResourcesImplHookInstaller.install(xposed, packageName, store);
        }
        if (resourcesHooksEnabled) {
            ResourcesReadHookInstaller.install(xposed, packageName, store);
        }
        if (emulationEnabled && !debugFlutterSettingsOnly) {
            ActivityThreadFontHookInstaller.install(xposed, packageName, store);
        }
        if (fontDomainPlan.textViewHooksEnabled && !debugFlutterSettingsOnly) {
            ForceTextSizeHookInstaller.install(xposed, packageName, store, fontDomainPlan);
        }
        if (fontDomainPlan.flutterSettingsEnabled) {
            DpisLog.i("DPIS_FONT installing Flutter settings font hooks for " + packageName);
            FlutterSettingsFontHookInstaller.install(xposed, packageName, store, fontDomainPlan);
        }
        if (fontDomainPlan.hyperOsNativeFlutterEnabled && !debugFlutterSettingsOnly) {
            DpisLog.i("DPIS_FONT installing HyperOS native Flutter font hooks for " + packageName);
            HyperOsFlutterFontHookInstaller.install(xposed, packageName, store);
        }
        if (fontDomainPlan.webViewTextZoomEnabled && !debugFlutterSettingsOnly) {
            WebViewFontHookInstaller.install(xposed, packageName, store);
        }
        if (viewportEnabled) {
            WindowMetricsHookInstaller.install(xposed);
            DisplayHookInstaller.install(xposed, packageName);
        }
        if (shouldInstallProbeHooks(policy)) {
            if (resourcesHooksEnabled) {
                ResourcesProbeHookInstaller.install(xposed, packageName, store);
            }
            if (viewportEnabled) {
                WindowManagerProbeHookInstaller.install(xposed, packageName);
                WindowSessionProbeHookInstaller.install(xposed);
                ViewRootProbeHookInstaller.install(xposed);
            }
            DpisLog.i("hooks installed (full): viewportEnabled=" + viewportEnabled
                    + ", viewportMode=" + viewportMode
                    + ", fontMode=" + fontMode + " for " + packageName);
            return;
        }
        String mode = resolveProbeInstallMode(policy);
        DpisLog.i("hooks installed (" + mode + "): viewportEnabled=" + viewportEnabled
                + ", viewportMode=" + viewportMode
                + ", fontMode=" + fontMode
                + ", fontDomainPlan=" + fontDomainPlan.reason
                + ", resourcesFont=" + fontDomainPlan.resourcesFontEnabled
                + ", textViewSpRewrite=" + fontDomainPlan.textViewSpRewriteEnabled
                + ", textViewCurrentPxFallback="
                + fontDomainPlan.textViewCurrentPxFallbackEnabled
                + ", paintFallback=" + fontDomainPlan.paintFallbackEnabled
                + ", flutterSettings=" + fontDomainPlan.flutterSettingsEnabled
                + ", hyperOsNativeFlutter=" + fontDomainPlan.hyperOsNativeFlutterEnabled
                + ", genericNativeFlutter=" + fontDomainPlan.genericNativeFlutterEnabled
                + ", debugForceFlutterSettings=" + debugForceFlutterSettings
                + ", debugFlutterSettingsOnly=" + debugFlutterSettingsOnly
                + " for " + packageName);
        if (fontHookPlan.downgradedToEmulation) {
            DpisLog.i("safe mode downgraded font apply mode to emulation for " + packageName);
        }
    }

    static boolean shouldInstallProbeHooks(HookRuntimePolicy policy) {
        return policy != null && policy.probeHooksEnabled;
    }

    static String resolveProbeInstallMode(HookRuntimePolicy policy) {
        return policy != null && policy.systemServerSafeModeEnabled
                ? "safe mode"
                : "probe disabled";
    }

    static boolean resolveViewportHookEnabled(HookRuntimePolicy policy,
                                              boolean viewportConfigured,
                                              String viewportMode) {
        if (!viewportConfigured) {
            return false;
        }
        boolean systemHooksEnabled = policy == null || policy.systemServerHooksEnabled;
        String normalized = EffectiveModeResolver.resolveViewportMode(viewportMode, systemHooksEnabled);
        if (ViewportApplyMode.OFF.equals(normalized)) {
            return false;
        }
        return true;
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
        if (!fontScaleActive) {
            return new FontHookPlan(false, false, false);
        }
        boolean systemHooksEnabled = policy == null || policy.systemServerHooksEnabled;
        String normalized = EffectiveModeResolver.resolveFontMode(fontMode, systemHooksEnabled);
        if (FontApplyMode.OFF.equals(normalized)) {
            return new FontHookPlan(false, false, false);
        }
        boolean fieldRewriteRequested = FontApplyMode.FIELD_REWRITE.equals(normalized);
        boolean emulationEnabled = FontApplyMode.SYSTEM_EMULATION.equals(normalized);
        boolean fieldRewriteEnabled = fieldRewriteRequested;
        return new FontHookPlan(emulationEnabled, fieldRewriteEnabled, false);
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
