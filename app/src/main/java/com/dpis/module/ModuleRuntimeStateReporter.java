package com.dpis.module;

import java.lang.reflect.Method;

final class ModuleRuntimeStateReporter {
    private static final String KEY_SYSTEM_SERVER_LOADED_AT = "debug.dpis.module.system_server_loaded_at";

    private ModuleRuntimeStateReporter() {
    }

    static void reportSystemServerLoaded() {
        setSystemProperty(KEY_SYSTEM_SERVER_LOADED_AT,
                String.valueOf(System.currentTimeMillis()));
    }

    static long getSystemServerLoadedAt() {
        String value = getSystemProperty(KEY_SYSTEM_SERVER_LOADED_AT, "0");
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static void setSystemProperty(String key, String value) {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Method set = systemProperties.getDeclaredMethod("set", String.class, String.class);
            set.invoke(null, key, value);
        } catch (Throwable throwable) {
            DpisLog.e("module runtime state report failed: key=" + key, throwable);
        }
    }

    private static String getSystemProperty(String key, String fallback) {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Method get = systemProperties.getDeclaredMethod("get", String.class, String.class);
            Object value = get.invoke(null, key, fallback);
            return value instanceof String ? (String) value : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }
}
