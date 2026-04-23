package com.dpis.module;

import io.github.libxposed.api.XposedInterface;

final class AppProcessHookInstaller {
    private AppProcessHookInstaller() {
    }

    static void install(XposedInterface xposed,
                        String packageName,
                        DpiConfigStore store,
                        HookRuntimePolicy policy,
                        boolean viewportConfigured,
                        String viewportMode,
                        String fontMode,
                        boolean fontScaleActive) throws Throwable {
        boolean viewportEnabled = resolveViewportHookEnabled(policy, viewportConfigured, viewportMode);
        FontHookPlan fontHookPlan = resolveFontHookPlan(policy, fontScaleActive, fontMode);
        boolean emulationEnabled = fontHookPlan.emulationEnabled;
        boolean fallbackEnabled = fontHookPlan.fieldRewriteEnabled;
        boolean resourcesHooksEnabled = viewportEnabled || emulationEnabled;
        if (resourcesHooksEnabled) {
            ResourcesManagerHookInstaller.install(xposed, packageName, store);
        }
        if (resourcesHooksEnabled) {
            ResourcesImplHookInstaller.install(xposed, packageName, store);
        }
        if (resourcesHooksEnabled) {
            ResourcesReadHookInstaller.install(xposed, packageName, store);
        }
        if (emulationEnabled) {
            ActivityThreadFontHookInstaller.install(xposed, packageName, store);
        }
        if (fallbackEnabled) {
            ForceTextSizeHookInstaller.install(xposed, packageName, store);
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
                + ", fontMode=" + fontMode + " for " + packageName);
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
