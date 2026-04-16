package com.dpis.module;

import io.github.libxposed.api.XposedInterface;

final class AppProcessHookInstaller {
    private AppProcessHookInstaller() {
    }

    static void install(XposedInterface xposed,
                        String packageName,
                        DpiConfigStore store,
                        HookRuntimePolicy policy) throws Throwable {
        ResourcesManagerHookInstaller.install(xposed, packageName, store);
        ResourcesImplHookInstaller.install(xposed, packageName, store);
        WindowMetricsHookInstaller.install(xposed);
        DisplayHookInstaller.install(xposed, packageName);
        if (policy.probeHooksEnabled) {
            ResourcesProbeHookInstaller.install(xposed, packageName, store);
            WindowManagerProbeHookInstaller.install(xposed, packageName);
            WindowSessionProbeHookInstaller.install(xposed);
            ViewRootProbeHookInstaller.install(xposed);
            DpisLog.i("hooks installed (full): ResourcesManager + ResourcesImpl + ResourcesProbe + WindowManagerProbe + WindowSessionProbe + WindowMetrics + Display + ViewRootProbe for "
                    + packageName);
            return;
        }
        String mode = policy.systemServerSafeModeEnabled ? "safe mode" : "probe disabled";
        DpisLog.i("hooks installed (" + mode + "): ResourcesManager + ResourcesImpl + WindowMetrics + Display for "
                + packageName);
    }
}
