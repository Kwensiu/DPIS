package com.dpis.module;

import java.lang.reflect.Method;
import java.util.Locale;

final class HyperOsFlutterFontBridge {
    private static final String PROPERTY_PREFIX = "debug.dpis.font.";
    private static final String FORCE_PROPERTY_PREFIX = "debug.dpis.forcefont.";
    private static final String RUST_BINARY_PROPERTY_PREFIX = "debug.dpis.rustbin.";

    private HyperOsFlutterFontBridge() {
    }

    static String propertyNameForPackage(String packageName) {
        return PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    static String forcePropertyNameForPackage(String packageName) {
        return FORCE_PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    static String rustBinaryPropertyNameForPackage(String packageName) {
        return RUST_BINARY_PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    static void publishTarget(String packageName, PerAppDisplayConfig config) {
        if (packageName == null || packageName.isEmpty() || config == null
                || !config.hyperOsFlutterFontHookEnabled
                || config.targetFontScalePercent == null
                || config.targetFontScalePercent <= 0) {
            if (shouldClearOnPublishTargetSkip(packageName, config)) {
                clearTarget(packageName);
            }
            return;
        }
        setSystemProperty(propertyNameForPackage(packageName),
                String.valueOf(config.targetFontScalePercent));
    }

    static void publishRustProxyTarget(String packageName, PerAppDisplayConfig config) {
        if (packageName == null || packageName.isEmpty() || config == null
                || config.targetFontScalePercent == null
                || config.targetFontScalePercent <= 0) {
            clearTarget(packageName);
            return;
        }
        setSystemProperty(propertyNameForPackage(packageName),
                String.valueOf(config.targetFontScalePercent));
    }

    static void clearTarget(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return;
        }
        setSystemProperty(propertyNameForPackage(packageName), "0");
        setSystemProperty(forcePropertyNameForPackage(packageName), "0");
        setSystemProperty(rustBinaryPropertyNameForPackage(packageName), "0");
    }

    static boolean shouldClearOnPublishTargetSkipForTest(String packageName,
                                                        PerAppDisplayConfig config) {
        return shouldClearOnPublishTargetSkip(packageName, config);
    }

    private static boolean shouldClearOnPublishTargetSkip(String packageName,
                                                         PerAppDisplayConfig config) {
        return packageName == null || packageName.isEmpty() || config == null
                || config.targetFontScalePercent == null
                || config.targetFontScalePercent <= 0;
    }

    static void publishRustBinaryPath(String packageName, String binaryPath) {
        if (packageName == null || packageName.isEmpty()
                || binaryPath == null || binaryPath.isEmpty()) {
            return;
        }
        setSystemProperty(rustBinaryPropertyNameForPackage(packageName), binaryPath);
    }

    private static void setSystemProperty(String key, String value) {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Method set = systemProperties.getDeclaredMethod("set", String.class, String.class);
            set.invoke(null, key, value);
        } catch (Throwable throwable) {
            DpisLog.e("DPIS_FONT HyperOS Flutter property publish failed: key=" + key, throwable);
        }
    }
}
