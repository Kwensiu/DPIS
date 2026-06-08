package com.dpis.module;

import java.lang.reflect.Method;
import java.util.Locale;

final class WechatDpiPropertyBridge {
    private static final String PROPERTY_PREFIX = "debug.dpis.wechat.dpi.";
    private static final String PERSIST_PROPERTY_PREFIX = "persist.debug.dpis.wechat.dpi.";

    private WechatDpiPropertyBridge() {
    }

    static String propertyNameForPackage(String packageName) {
        return PROPERTY_PREFIX + suffixForPackage(packageName);
    }

    static String persistentPropertyNameForPackage(String packageName) {
        return PERSIST_PROPERTY_PREFIX + suffixForPackage(packageName);
    }

    static int readDpi(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return 0;
        }
        String suffix = suffixForPackage(packageName);
        String value = readFirstProperty(
                PROPERTY_PREFIX + suffix,
                PERSIST_PROPERTY_PREFIX + suffix);
        return parseDpi(value);
    }

    private static String suffixForPackage(String packageName) {
        return String.format(Locale.US, "%08x", packageName.hashCode());
    }

    private static String readFirstProperty(String... propertyNames) {
        for (String propertyName : propertyNames) {
            String value = readSystemProperty(propertyName);
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private static String readSystemProperty(String propertyName) {
        if (propertyName == null || propertyName.isBlank()) {
            return "";
        }
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Method get = systemProperties.getDeclaredMethod("get", String.class, String.class);
            Object value = get.invoke(null, propertyName, "");
            return value instanceof String ? (String) value : "";
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static int parseDpi(String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                return 0;
            }
            int parsed = Integer.parseInt(value.trim());
            return WechatDpiConfig.normalize(parsed) != null ? parsed : 0;
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
