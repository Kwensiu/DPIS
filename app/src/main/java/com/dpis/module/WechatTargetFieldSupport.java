package com.dpis.module;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

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
            return resolvePackageVersionCode(packageInfo);
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    private static long resolvePackageVersionCode(PackageInfo packageInfo) {
        if (packageInfo == null) {
            return 0L;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return packageInfo.getLongVersionCode();
        }
        //noinspection deprecation
        return packageInfo.versionCode;
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
