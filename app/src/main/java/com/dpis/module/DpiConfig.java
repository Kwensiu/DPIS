package com.dpis.module;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class DpiConfig {
    static final int SEED_TARGET_VIEWPORT_WIDTH_DP = 360;
    static final String[] TARGET_PACKAGES = {
            "bin.mt.plus.canary",
            "com.max.xiaoheihe"
    };

    private DpiConfig() {
    }

    static boolean shouldHandlePackage(String packageName) {
        for (String targetPackage : TARGET_PACKAGES) {
            if (targetPackage.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    static Set<String> getSeedTargetPackages() {
        return new LinkedHashSet<>(Arrays.asList(TARGET_PACKAGES));
    }

    static Map<String, Integer> getSeedViewportWidthDps() {
        LinkedHashMap<String, Integer> values = new LinkedHashMap<>();
        for (String packageName : TARGET_PACKAGES) {
            values.put(packageName, SEED_TARGET_VIEWPORT_WIDTH_DP);
        }
        return values;
    }

    static Integer getSeedViewportWidthDp(String packageName) {
        return shouldHandlePackage(packageName) ? SEED_TARGET_VIEWPORT_WIDTH_DP : null;
    }

    static String getTargetPackagesText() {
        return String.join(", ", TARGET_PACKAGES);
    }
}
