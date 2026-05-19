package com.dpis.module;

import java.lang.reflect.Method;

final class RuntimeDebugPropertyBridge {
    private static final String PROP_GLOBAL_LOG_ENABLED = "debug.dpis.global_log_enabled";
    private static final String PERSIST_PROP_GLOBAL_LOG_ENABLED =
            "persist.debug.dpis.global_log_enabled";
    private static final String PROP_FONT_DEBUG_OVERLAY_ENABLED =
            "debug.dpis.font_debug_overlay_enabled";
    private static final String PERSIST_PROP_FONT_DEBUG_OVERLAY_ENABLED =
            "persist.debug.dpis.font_debug_overlay_enabled";

    private RuntimeDebugPropertyBridge() {
    }

    static String globalLogPropertyName() {
        return PROP_GLOBAL_LOG_ENABLED;
    }

    static String persistentGlobalLogPropertyName() {
        return PERSIST_PROP_GLOBAL_LOG_ENABLED;
    }

    static String fontDebugOverlayPropertyName() {
        return PROP_FONT_DEBUG_OVERLAY_ENABLED;
    }

    static String persistentFontDebugOverlayPropertyName() {
        return PERSIST_PROP_FONT_DEBUG_OVERLAY_ENABLED;
    }

    static boolean readGlobalLogEnabled() {
        return readBooleanWithPersistentFallback(
                PROP_GLOBAL_LOG_ENABLED,
                PERSIST_PROP_GLOBAL_LOG_ENABLED,
                false);
    }

    static boolean readFontDebugOverlayEnabled() {
        return readBooleanWithPersistentFallback(
                PROP_FONT_DEBUG_OVERLAY_ENABLED,
                PERSIST_PROP_FONT_DEBUG_OVERLAY_ENABLED,
                false);
    }

    private static boolean readBooleanWithPersistentFallback(String key,
                                                            String persistentKey,
                                                            boolean defaultValue) {
        String value = readSystemProperty(key);
        if (value == null || value.trim().isEmpty()) {
            value = readSystemProperty(persistentKey);
        }
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        String normalized = value.trim();
        return "1".equals(normalized) || "true".equalsIgnoreCase(normalized);
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
