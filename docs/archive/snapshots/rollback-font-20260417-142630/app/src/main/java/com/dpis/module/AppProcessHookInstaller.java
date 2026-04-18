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
                        boolean fontEnabled) throws Throwable {
        if (viewportEnabled || fontEnabled) {
            ResourcesManagerHookInstaller.install(xposed, packageName, store);
        }
        if (viewportEnabled || fontEnabled) {
            ResourcesImplHookInstaller.install(xposed, packageName, store);
        }
        if (fontEnabled) {
            ForceTextSizeHookInstaller.install(xposed, packageName, store);
        }
        if (viewportEnabled) {
            WindowMetricsHookInstaller.install(xposed);
            DisplayHookInstaller.install(xposed, packageName);
        }
        if (policy.probeHooksEnabled) {
            if (viewportEnabled || fontEnabled) {
                ResourcesProbeHookInstaller.install(xposed, packageName, store);
            }
            if (viewportEnabled) {
                WindowManagerProbeHookInstaller.install(xposed, packageName);
                WindowSessionProbeHookInstaller.install(xposed);
                ViewRootProbeHookInstaller.install(xposed);
            }
            DpisLog.i("hooks installed (full): viewportEnabled=" + viewportEnabled
                    + ", fontEnabled=" + fontEnabled + " for " + packageName);
            return;
        }
        String mode = policy.systemServerSafeModeEnabled ? "safe mode" : "probe disabled";
        DpisLog.i("hooks installed (" + mode + "): viewportEnabled=" + viewportEnabled
                + ", fontEnabled=" + fontEnabled + " for " + packageName);
    }
}
