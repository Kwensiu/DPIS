package com.dpis.module.fonts;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

public final class HyperOsNativeAppDetector {
    private HyperOsNativeAppDetector() {
    }

    public static boolean isNativeProxyCandidate(ApplicationInfo applicationInfo) {
        if (applicationInfo == null) {
            return false;
        }
        Bundle metaData = applicationInfo.metaData;
        if (metaData == null) {
            return false;
        }
        return metaData.getBoolean("hyperos_package", false)
                || metaData.containsKey("hyperos_app_lib_name")
                || metaData.containsKey("hyperos_application_entry");
    }

    /**
     * Resolves metadata only for a package about to use the native proxy. Keeping this out of
     * the app catalogue avoids asking PackageManager to marshal every installed manifest.
     */
    public static boolean isNativeProxyCandidate(PackageManager packageManager, String packageName) {
        if (packageManager == null || packageName == null || packageName.isEmpty()) {
            return false;
        }
        try {
            ApplicationInfo applicationInfo;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                applicationInfo = packageManager.getApplicationInfo(packageName,
                        PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA));
            } else {
                applicationInfo = packageManager.getApplicationInfo(packageName,
                        PackageManager.GET_META_DATA);
            }
            return isNativeProxyCandidate(applicationInfo);
        } catch (PackageManager.NameNotFoundException | RuntimeException ignored) {
            return false;
        }
    }
}
