package com.dpis.module;

import io.github.libxposed.api.XposedModule;

final class Modern101AppSpecificRouteInstaller {
    private Modern101AppSpecificRouteInstaller() {
    }

    static boolean handlePackageReady(XposedModule xposed,
            XposedModule.PackageReadyParam param,
            String processName) {
        if (param == null || !WechatDpiConfig.appliesTo(param.getPackageName())) {
            return false;
        }
        if (WechatDpiConfig.appliesTo(processName)) {
            DpisLog.i("modern101 WeChat DPI route enter: package="
                    + param.getPackageName() + ", process=" + processName);
            try {
                WechatDpiModernHookInstaller.install(
                        xposed,
                        param.getClassLoader(),
                        param.getApplicationInfo(),
                        param.getPackageName());
                DpisLog.i("modern101 WeChat DPI route install attempted: package="
                        + param.getPackageName() + ", process=" + processName);
            } catch (Throwable throwable) {
                DpisLog.e("modern101 WeChat DPI route install failed: package="
                        + param.getPackageName() + ", process=" + processName + ", "
                        + throwable.getClass().getName() + ": " + throwable.getMessage(),
                        throwable);
            }
        }
        DpisLog.i("modern101 app-specific route installed alongside generic hooks: package="
                + WechatDpiConfig.PACKAGE_NAME + ", process=" + processName);
        return false;
    }

    static boolean shouldSuppressModuleLoadedGenericHooks(String packageName, String processName) {
        if (!WechatDpiConfig.appliesTo(packageName)) {
            return false;
        }
        DpisLog.i("modern101 app-specific route allowing generic hooks alongside: package="
                + WechatDpiConfig.PACKAGE_NAME + ", process=" + processName);
        return false;
    }
}
