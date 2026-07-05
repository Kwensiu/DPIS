package com.dpis.module;

import com.dpis.module.viewport.DpiConfig;

import com.dpis.module.appconfig.WechatDpiConfig;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

final class LegacyAppSpecificRouteInstaller {
    private LegacyAppSpecificRouteInstaller() {
    }

    static boolean handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam == null || !WechatDpiConfig.appliesTo(lpparam.packageName)) {
            return false;
        }
        if (WechatDpiConfig.appliesTo(lpparam.processName)) {
            DpisLog.i("legacy WeChat DPI route enter: package="
                    + lpparam.packageName + ", process=" + lpparam.processName);
            FeedbackDiagnosticRuntimeHotPathEvents.event(
                    lpparam.packageName,
                    "wechat_dpi",
                    "legacy_load_package",
                    "route_callback_entered",
                    "process=" + lpparam.processName);
            try {
                WechatDpiLegacyHookInstaller.install(lpparam);
                DpisLog.i("legacy WeChat DPI route install attempted: package="
                        + lpparam.packageName + ", process=" + lpparam.processName);
                FeedbackDiagnosticRuntimeHotPathEvents.event(
                        lpparam.packageName,
                        "wechat_dpi",
                        "legacy_load_package",
                        "mutation_candidate",
                        "installAttempted=true, process=" + lpparam.processName);
            } catch (Throwable throwable) {
                DpisLog.e("legacy WeChat DPI route install failed: package="
                        + lpparam.packageName + ", process=" + lpparam.processName + ", "
                        + throwable.getClass().getName() + ": " + throwable.getMessage(),
                        throwable);
                FeedbackDiagnosticRuntimeHotPathEvents.event(
                        lpparam.packageName,
                        "wechat_dpi",
                        "legacy_load_package",
                        "skipped",
                        "installFailed=true, process=" + lpparam.processName
                                + ", error=" + throwable.getClass().getSimpleName());
            }
        }
        DpisLog.i("legacy app-specific route installed alongside generic hooks: package="
                + WechatDpiConfig.PACKAGE_NAME + ", process=" + lpparam.processName);
        return false;
    }
}
