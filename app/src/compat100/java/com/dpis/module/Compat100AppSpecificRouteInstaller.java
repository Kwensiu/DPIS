package com.dpis.module;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

final class Compat100AppSpecificRouteInstaller {
    private Compat100AppSpecificRouteInstaller() {
    }

    static boolean handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam == null || !WechatDpiConfig.appliesTo(lpparam.packageName)) {
            return false;
        }
        if (WechatDpiConfig.appliesTo(lpparam.processName)) {
            DpisLog.i("compat100 WeChat DPI route enter: package="
                    + lpparam.packageName + ", process=" + lpparam.processName);
            try {
                WechatDpiCompat100HookInstaller.install(lpparam);
                DpisLog.i("compat100 WeChat DPI route install attempted: package="
                        + lpparam.packageName + ", process=" + lpparam.processName);
            } catch (Throwable throwable) {
                DpisLog.e("compat100 WeChat DPI route install failed: package="
                        + lpparam.packageName + ", process=" + lpparam.processName + ", "
                        + throwable.getClass().getName() + ": " + throwable.getMessage(),
                        throwable);
            }
        }
        DpisLog.i("compat100 app-specific route installed alongside generic hooks: package="
                + WechatDpiConfig.PACKAGE_NAME + ", process=" + lpparam.processName);
        return false;
    }
}
