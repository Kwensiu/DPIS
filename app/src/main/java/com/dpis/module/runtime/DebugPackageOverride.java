package com.dpis.module.runtime;

import com.dpis.module.BuildConfig;

public final class DebugPackageOverride {
    private DebugPackageOverride() {
    }

    public static boolean matches(String propertyName, String packageName) {
        return matches(propertyName, packageName, () -> readSystemProperty(propertyName));
    }

    public static boolean matchesForTest(String propertyName, String packageName, String propertyValue) {
        return matches(propertyName, packageName, () -> propertyValue);
    }

    private static boolean matches(String propertyName, String packageName, PropertyReader reader) {
        if (!BuildConfig.DEBUG || packageName == null || packageName.isBlank()) {
            return false;
        }
        String value = reader.read();
        if (value == null) {
            return false;
        }
        String normalized = value.trim();
        return "*".equals(normalized) || packageName.equals(normalized);
    }

    private static String readSystemProperty(String key) {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            return (String) systemProperties
                    .getMethod("get", String.class, String.class)
                    .invoke(null, key, "");
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return "";
        }
    }

    private interface PropertyReader {
        String read();
    }
}
