package com.dpis.module;

import io.github.libxposed.api.XposedModule;

final class Modern101AppSpecificRouteInstaller {
    private Modern101AppSpecificRouteInstaller() {
    }

    static boolean handlePackageReady(XposedModule xposed,
            XposedModule.PackageReadyParam param,
            String processName) {
        if (param == null || !WechatTargetFieldConfig.appliesTo(param.getPackageName())) {
            return false;
        }
        if (WechatTargetFieldConfig.appliesTo(processName)) {
            WechatTargetFieldModernHookInstaller.install(
                    xposed,
                    param.getClassLoader(),
                    param.getApplicationInfo(),
                    param.getPackageName());
        }
        DpisLog.i("modern101 app-specific route installed alongside generic hooks: package="
                + WechatTargetFieldConfig.PACKAGE_NAME + ", process=" + processName);
        return false;
    }

    static boolean shouldSuppressModuleLoadedGenericHooks(String packageName, String processName) {
        if (!WechatTargetFieldConfig.appliesTo(packageName)) {
            return false;
        }
        DpisLog.i("modern101 app-specific route allowing generic hooks alongside: package="
                + WechatTargetFieldConfig.PACKAGE_NAME + ", process=" + processName);
        return false;
    }
}
