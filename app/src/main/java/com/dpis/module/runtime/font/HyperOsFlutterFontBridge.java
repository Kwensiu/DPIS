package com.dpis.module.runtime.font;

import com.dpis.module.DpisLog;
import com.dpis.module.fonts.FontApplyMode;

import com.dpis.module.runtime.systemserver.PerAppDisplayConfig;

import java.lang.reflect.Method;
import java.util.Locale;

public final class HyperOsFlutterFontBridge {
    private static final String PROPERTY_PREFIX = "debug.dpis.font.";
    private static final String FORCE_PROPERTY_PREFIX = "debug.dpis.forcefont.";
    private static final String COMPAT_FONT_PROPERTY_PREFIX = "debug.dpis.compatfont.";
    // compatfont.* is non-zero only for system emulation; fontmode.* lets
    // legacy interpret forcefont.* as field_rewrite when needed.
    private static final String COMPAT_FONT_MODE_PROPERTY_PREFIX = "debug.dpis.fontmode.";
    private static final String TYPEFACE_PROPERTY_PREFIX = "debug.dpis.typeface.";
    private static final String RUST_BINARY_PROPERTY_PREFIX = "debug.dpis.rustbin.";
    private static final String PERSIST_FORCE_PROPERTY_PREFIX = "persist.debug.dpis.forcefont.";
    private static final String PERSIST_COMPAT_FONT_PROPERTY_PREFIX = "persist.debug.dpis.compatfont.";
    private static final String PERSIST_COMPAT_FONT_MODE_PROPERTY_PREFIX = "persist.debug.dpis.fontmode.";
    private static final String PERSIST_TYPEFACE_PROPERTY_PREFIX = "persist.debug.dpis.typeface.";

    private HyperOsFlutterFontBridge() {
    }

    public static String propertyNameForPackage(String packageName) {
        return PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    public static String forcePropertyNameForPackage(String packageName) {
        return FORCE_PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    public static String compatFontPropertyNameForPackage(String packageName) {
        return COMPAT_FONT_PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    public static String compatFontModePropertyNameForPackage(String packageName) {
        return COMPAT_FONT_MODE_PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    public static String typefacePropertyNameForPackage(String packageName) {
        return TYPEFACE_PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    public static String persistentForcePropertyNameForPackage(String packageName) {
        return PERSIST_FORCE_PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    public static String persistentCompatFontPropertyNameForPackage(String packageName) {
        return PERSIST_COMPAT_FONT_PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    public static String persistentCompatFontModePropertyNameForPackage(String packageName) {
        return PERSIST_COMPAT_FONT_MODE_PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    public static String persistentTypefacePropertyNameForPackage(String packageName) {
        return PERSIST_TYPEFACE_PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    public static Integer readForceFontScalePercent(String packageName) {
        return readPositiveIntProperty(forcePropertyNameForPackage(packageName),
                persistentForcePropertyNameForPackage(packageName));
    }

    public static Integer readCompatFontScalePercent(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return null;
        }
        return readPositiveIntProperty(compatFontPropertyNameForPackage(packageName),
                persistentCompatFontPropertyNameForPackage(packageName));
    }

    public static String readCompatFontMode(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return FontApplyMode.OFF;
        }
        return FontApplyMode.normalize(readPropertyWithPersistentFallback(
                compatFontModePropertyNameForPackage(packageName),
                persistentCompatFontModePropertyNameForPackage(packageName)));
    }

    public static String readTypefaceId(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return null;
        }
        String value = readPropertyWithPersistentFallback(
                typefacePropertyNameForPackage(packageName),
                persistentTypefacePropertyNameForPackage(packageName));
        if (value == null || value.trim().isEmpty() || "0".equals(value.trim())) {
            return null;
        }
        return value.trim();
    }

    private static Integer readPositiveIntProperty(String key, String persistentKey) {
        String value = readPropertyWithPersistentFallback(key, persistentKey);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static String rustBinaryPropertyNameForPackage(String packageName) {
        return RUST_BINARY_PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    public static void publishTarget(String packageName, PerAppDisplayConfig config) {
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

    public static void publishRustProxyTarget(String packageName, PerAppDisplayConfig config) {
        if (packageName == null || packageName.isEmpty() || config == null
                || config.targetFontScalePercent == null
                || config.targetFontScalePercent <= 0) {
            clearTarget(packageName);
            return;
        }
        setSystemProperty(propertyNameForPackage(packageName),
                String.valueOf(config.targetFontScalePercent));
    }

    public static void clearTarget(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return;
        }
        String[] assignments = clearTargetAssignments(packageName);
        for (int i = 0; i < assignments.length; i += 2) {
            setSystemProperty(assignments[i], assignments[i + 1]);
        }
    }

    public static void clearNativeTarget(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return;
        }
        setSystemProperty(propertyNameForPackage(packageName), "0");
        setSystemProperty(rustBinaryPropertyNameForPackage(packageName), "0");
    }

    public static boolean shouldClearOnPublishTargetSkipForTest(String packageName,
                                                        PerAppDisplayConfig config) {
        return shouldClearOnPublishTargetSkip(packageName, config);
    }

    public static String[] clearTargetAssignmentsForTest(String packageName) {
        return clearTargetAssignments(packageName);
    }

    private static String[] clearTargetAssignments(String packageName) {
        return new String[] {
                propertyNameForPackage(packageName), "0",
                forcePropertyNameForPackage(packageName), "0",
                compatFontPropertyNameForPackage(packageName), "0",
                compatFontModePropertyNameForPackage(packageName), FontApplyMode.OFF,
                persistentForcePropertyNameForPackage(packageName), "0",
                persistentCompatFontPropertyNameForPackage(packageName), "0",
                persistentCompatFontModePropertyNameForPackage(packageName), FontApplyMode.OFF,
                typefacePropertyNameForPackage(packageName), "0",
                persistentTypefacePropertyNameForPackage(packageName), "0",
                rustBinaryPropertyNameForPackage(packageName), "0"
        };
    }

    private static boolean shouldClearOnPublishTargetSkip(String packageName,
                                                         PerAppDisplayConfig config) {
        return packageName == null || packageName.isEmpty() || config == null
                || config.targetFontScalePercent == null
                || config.targetFontScalePercent <= 0;
    }

    public static void publishRustBinaryPath(String packageName, String binaryPath) {
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

    private static String readSystemProperty(String key) {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Method getMethod = systemProperties.getDeclaredMethod("get", String.class, String.class);
            Object value = getMethod.invoke(null, key, "");
            return value instanceof String ? (String) value : "";
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String readPropertyWithPersistentFallback(String key, String persistentKey) {
        String value = readSystemProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }
        return readSystemProperty(persistentKey);
    }
}
