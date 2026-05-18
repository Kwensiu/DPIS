package com.dpis.module;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class FontHookDomainPropertyBridge {
    private static final String PROPERTY_PREFIX = "debug.dpis.hookdomains.";
    private static final String PERSIST_PROPERTY_PREFIX = "persist.debug.dpis.hookdomains.";

    private FontHookDomainPropertyBridge() {
    }

    static String propertyNameForPackage(String packageName) {
        return PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    static String persistentPropertyNameForPackage(String packageName) {
        return PERSIST_PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    static HookDomainOverride readOverride(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return HookDomainOverride.automatic();
        }
        String raw = readPropertyWithPersistentFallback(
                propertyNameForPackage(packageName),
                persistentPropertyNameForPackage(packageName));
        return parseOverrideValue(raw);
    }

    static HookDomainOverride parseOverrideValueForTest(String raw) {
        return parseOverrideValue(raw);
    }

    static int encodeMask(Set<String> domains) {
        List<String> ids = FontHookDomainRegistry.orderedCustomizableIdsList();
        Set<String> normalized = FontHookDomainRegistry.orderedCustomizableSubset(domains);
        int mask = 0;
        for (int i = 0; i < ids.size(); i++) {
            if (normalized.contains(ids.get(i))) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    static Set<String> decodeMask(int mask) {
        LinkedHashSet<String> domains = new LinkedHashSet<>();
        List<String> ids = FontHookDomainRegistry.orderedCustomizableIdsList();
        for (int i = 0; i < ids.size(); i++) {
            if ((mask & (1 << i)) != 0) {
                domains.add(ids.get(i));
            }
        }
        return domains;
    }

    private static HookDomainOverride parseOverrideValue(String raw) {
        if (raw == null || raw.trim().isEmpty() || "0".equals(raw.trim())) {
            return HookDomainOverride.automatic();
        }
        try {
            int encoded = Integer.parseInt(raw.trim());
            if (encoded <= 0) {
                return HookDomainOverride.automatic();
            }
            return new HookDomainOverride(true, decodeMask(encoded - 1), Set.of());
        } catch (NumberFormatException ignored) {
            return HookDomainOverride.automatic();
        }
    }

    private static String readPropertyWithPersistentFallback(String key, String persistentKey) {
        String value = readSystemProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }
        return readSystemProperty(persistentKey);
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
