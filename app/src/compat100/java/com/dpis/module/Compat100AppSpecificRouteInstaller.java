package com.dpis.module;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

final class Compat100AppSpecificRouteInstaller {
    private Compat100AppSpecificRouteInstaller() {
    }

    static boolean handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam == null || !WechatTargetFieldConfig.appliesTo(lpparam.packageName)) {
            return false;
        }
        if (WechatTargetFieldConfig.appliesTo(lpparam.processName)) {
            WechatTargetFieldCompat100HookInstaller.install(lpparam);
        }
        DpisLog.i("compat100 app-specific route installed alongside generic hooks: package="
                + WechatTargetFieldConfig.PACKAGE_NAME + ", process=" + lpparam.processName);
        return false;
    }
}
