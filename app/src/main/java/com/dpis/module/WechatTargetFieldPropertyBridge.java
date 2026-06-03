package com.dpis.module;

import java.lang.reflect.Method;
import java.util.Locale;

final class WechatTargetFieldPropertyBridge {

    private static final String PROPERTY_PREFIX =
            "debug.dpis.wechat.targetfield.";
    private static final String PERSIST_PROPERTY_PREFIX =
            "persist.debug.dpis.wechat.targetfield.";

    private WechatTargetFieldPropertyBridge() {
    }

    static String propertyNameForPackage(String packageName) {
        return PROPERTY_PREFIX + suffixForPackage(packageName);
    }

    static String persistentPropertyNameForPackage(String packageName) {
        return PERSIST_PROPERTY_PREFIX + suffixForPackage(packageName);
    }

    static int readTargetField(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return 0;
        }
        String value = readPropertyWithPersistentFallback(
                propertyNameForPackage(packageName),
                persistentPropertyNameForPackage(packageName));
        return parseTargetField(value);
    }

    private static String suffixForPackage(String packageName) {
        return String.format(Locale.US, "%08x", packageName.hashCode());
    }

    private static String readPropertyWithPersistentFallback(String propertyName,
            String persistentPropertyName) {
        String value = readSystemProperty(propertyName);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }
        return readSystemProperty(persistentPropertyName);
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

    private static int parseTargetField(String value) {
        try {
            if (value == null || value.trim().isEmpty()) {
                return 0;
            }
            int parsed = Integer.parseInt(value.trim());
            return WechatTargetFieldConfig.normalize(parsed) != null ? parsed : 0;
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
