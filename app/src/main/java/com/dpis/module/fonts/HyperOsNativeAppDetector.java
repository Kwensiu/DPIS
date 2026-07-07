package com.dpis.module.fonts;

import android.content.pm.ApplicationInfo;
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
}
