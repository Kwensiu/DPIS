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
        logSuppressed(processName);
        return true;
    }

    static boolean shouldSuppressModuleLoadedGenericHooks(String packageName, String processName) {
        if (!WechatTargetFieldConfig.appliesTo(packageName)) {
            return false;
        }
        logSuppressed(processName);
        return true;
    }

    private static void logSuppressed(String processName) {
        DpisLog.i("modern101 app-specific route suppresses generic hooks: package="
                + WechatTargetFieldConfig.PACKAGE_NAME + ", process=" + processName);
    }
}
