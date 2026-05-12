package com.dpis.module;

import java.lang.reflect.Method;
import java.util.Locale;

final class ViewportPropertyBridge {
    private static final String PROPERTY_PREFIX = "debug.dpis.vp.";
    // compat100 needs the requested value even for field_rewrite, while vp.* must
    // stay 0 unless system emulation is active.
    private static final String COMPAT_CONFIG_PROPERTY_PREFIX = "debug.dpis.vpcfg.";
    private static final String COMPAT_MODE_PROPERTY_PREFIX = "debug.dpis.vpmode.";
    private static final String PERSIST_PROPERTY_PREFIX = "persist.debug.dpis.vp.";
    private static final String PERSIST_COMPAT_CONFIG_PROPERTY_PREFIX = "persist.debug.dpis.vpcfg.";
    private static final String PERSIST_COMPAT_MODE_PROPERTY_PREFIX = "persist.debug.dpis.vpmode.";

    private ViewportPropertyBridge() {
    }

    static String propertyNameForPackage(String packageName) {
        return PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    static String compatConfigPropertyNameForPackage(String packageName) {
        return COMPAT_CONFIG_PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    static String compatModePropertyNameForPackage(String packageName) {
        return COMPAT_MODE_PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    static String persistentPropertyNameForPackage(String packageName) {
        return PERSIST_PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    static String persistentCompatConfigPropertyNameForPackage(String packageName) {
        return PERSIST_COMPAT_CONFIG_PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    static String persistentCompatModePropertyNameForPackage(String packageName) {
        return PERSIST_COMPAT_MODE_PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    static Integer readTargetWidthDp(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return null;
        }
        return readOverrideValue(propertyNameForPackage(packageName),
                persistentPropertyNameForPackage(packageName));
    }

    static Integer readCompatConfigWidthDp(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return null;
        }
        return readOverrideValue(compatConfigPropertyNameForPackage(packageName),
                persistentCompatConfigPropertyNameForPackage(packageName));
    }

    static String readCompatMode(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return ViewportApplyMode.OFF;
        }
        return ViewportApplyMode.normalize(readPropertyWithPersistentFallback(
                compatModePropertyNameForPackage(packageName),
                persistentCompatModePropertyNameForPackage(packageName)));
    }

    static Integer parseOverrideValueForTest(String value) {
        return parseOverrideValue(value);
    }

    private static Integer parseOverrideValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed >= 0 ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Integer readOverrideValue(String propertyName, String persistentPropertyName) {
        String value = readPropertyWithPersistentFallback(propertyName, persistentPropertyName);
        return parseOverrideValue(value);
    }

    private static String readPropertyWithPersistentFallback(String propertyName,
                                                             String persistentPropertyName) {
        String value = readSystemProperty(propertyName);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }
        return readSystemProperty(persistentPropertyName);
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
}
