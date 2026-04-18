package com.dpis.module;

import io.github.libxposed.api.XposedInterface;

final class AppProcessHookInstaller {
    private AppProcessHookInstaller() {
    }

    static void install(XposedInterface xposed,
                        String packageName,
                        DpiConfigStore store,
                        HookRuntimePolicy policy,
                        boolean viewportEnabled,
                        String fontMode,
                        boolean fontScaleActive) throws Throwable {
        boolean emulationEnabled =
                fontScaleActive && FontApplyMode.SYSTEM_EMULATION.equals(fontMode);
        boolean fallbackEnabled =
                fontScaleActive && FontApplyMode.FIELD_REWRITE.equals(fontMode);
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
        if (policy.probeHooksEnabled) {
            if (resourcesHooksEnabled) {
                ResourcesProbeHookInstaller.install(xposed, packageName, store);
            }
            if (viewportEnabled) {
                WindowManagerProbeHookInstaller.install(xposed, packageName);
                WindowSessionProbeHookInstaller.install(xposed);
                ViewRootProbeHookInstaller.install(xposed);
            }
            DpisLog.i("hooks installed (full): viewportEnabled=" + viewportEnabled
                    + ", fontMode=" + fontMode + " for " + packageName);
            return;
        }
        String mode = policy.systemServerSafeModeEnabled ? "safe mode" : "probe disabled";
        DpisLog.i("hooks installed (" + mode + "): viewportEnabled=" + viewportEnabled
                + ", fontMode=" + fontMode + " for " + packageName);
    }
}
