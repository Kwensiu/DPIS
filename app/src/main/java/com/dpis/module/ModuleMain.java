package com.dpis.module;

import io.github.libxposed.api.XposedModule;

public final class ModuleMain extends XposedModule {
    private volatile DpiConfigStore configStore;

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        DpisLog.i("module loaded: process=" + param.getProcessName());
        configStore = new DpiConfigStore(getRemotePreferences(DpiConfigStore.GROUP));
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        DpiConfigStore store = getOrCreateConfigStore();
        String packageName = param.getPackageName();
        if (!store.getConfiguredPackages().contains(packageName)) {
            DpisLog.i("package not configured: package=" + packageName);
            return;
        }
        Integer targetViewportWidthDp = store.getTargetViewportWidthDp(packageName);
        if (targetViewportWidthDp == null) {
            DpisLog.i("target package disabled: package=" + packageName);
            return;
        }
        DpisLog.i("target package matched: package=" + packageName
                + ", targetViewportWidthDp=" + targetViewportWidthDp);
        try {
            ResourcesManagerHookInstaller.install(this, packageName, store);
            ResourcesImplHookInstaller.install(this, packageName, store);
            ResourcesProbeHookInstaller.install(this);
            WindowManagerProbeHookInstaller.install(this);
            WindowMetricsHookInstaller.install(this);
            DisplayHookInstaller.install(this);
            ViewRootProbeHookInstaller.install(this);
            DpisLog.i("hooks installed: ResourcesManager + ResourcesImpl + ResourcesProbe + WindowManagerProbe + WindowMetrics + Display + ViewRootProbe for "
                    + packageName);
        } catch (Throwable throwable) {
            DpisLog.e("failed to install Resources hooks", throwable);
        }
    }

    private DpiConfigStore getOrCreateConfigStore() {
        DpiConfigStore local = configStore;
        if (local == null) {
            local = new DpiConfigStore(getRemotePreferences(DpiConfigStore.GROUP));
            configStore = local;
        }
        return local;
    }
}
