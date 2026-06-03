package com.dpis.module;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

final class WechatTargetFieldSupport {
    private WechatTargetFieldSupport() {
    }

    static State current(Context context) {
        long versionCode = resolveWechatVersionCode(context);
        if (versionCode <= 0L) {
            return new State(0L, false);
        }
        return new State(versionCode, WechatTargetFieldRoutes.supportsVersionCode(versionCode));
    }

    private static long resolveWechatVersionCode(Context context) {
        if (context == null) {
            return 0L;
        }
        try {
            PackageManager packageManager = context.getPackageManager();
            if (packageManager == null) {
                return 0L;
            }
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    WechatTargetFieldConfig.PACKAGE_NAME, 0);
            return packageInfo != null ? packageInfo.getLongVersionCode() : 0L;
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    static final class State {
        final long versionCode;
        final boolean supported;

        State(long versionCode, boolean supported) {
            this.versionCode = versionCode;
            this.supported = supported;
        }
    }
}
