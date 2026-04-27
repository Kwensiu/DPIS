package com.dpis.module;

import java.lang.reflect.Method;
import java.util.Locale;

final class ViewportPropertyBridge {
    private static final String PROPERTY_PREFIX = "debug.dpis.vp.";

    private ViewportPropertyBridge() {
    }

    static String propertyNameForPackage(String packageName) {
        return PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    static Integer readTargetWidthDp(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return null;
        }
        return parseOverrideValue(readSystemProperty(propertyNameForPackage(packageName)));
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
