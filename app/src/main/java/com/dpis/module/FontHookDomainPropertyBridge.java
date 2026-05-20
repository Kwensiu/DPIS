package com.dpis.module;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class FontHookDomainPropertyBridge {
    private static final String PROPERTY_PREFIX = "debug.dpis.hookdomains.";
    private static final String PERSIST_PROPERTY_PREFIX = "persist.debug.dpis.hookdomains.";
    private static final String VALUE_VERSION_PREFIX = "v2:";
    private static final int PACKAGE_CHECK_HEX_LENGTH = 12;

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
        return parseOverrideValue(packageName, raw);
    }

    static HookDomainOverride parseOverrideValueForTest(String raw) {
        return parseOverrideValue("org.telegram.messenger", raw);
    }

    static String encodeOverrideValue(String packageName, Set<String> domains) {
        int encoded = encodeMask(domains) + 1;
        return VALUE_VERSION_PREFIX + packageCheck(packageName) + ":" + encoded;
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

    private static HookDomainOverride parseOverrideValue(String packageName, String raw) {
        if (raw == null || raw.trim().isEmpty() || "0".equals(raw.trim())) {
            return HookDomainOverride.automatic();
        }
        String normalized = raw.trim();
        if (normalized.startsWith(VALUE_VERSION_PREFIX)) {
            normalized = parseVerifiedV2Value(packageName, normalized);
            if (normalized == null) {
                return HookDomainOverride.automatic();
            }
        }
        try {
            int encoded = Integer.parseInt(normalized);
            if (encoded <= 0) {
                return HookDomainOverride.automatic();
            }
            return new HookDomainOverride(true, decodeMask(encoded - 1), Set.of());
        } catch (NumberFormatException ignored) {
            return HookDomainOverride.automatic();
        }
    }

    private static String parseVerifiedV2Value(String packageName, String raw) {
        String[] parts = raw.split(":", 3);
        if (parts.length != 3 || !"v2".equals(parts[0])) {
            return null;
        }
        if (!packageCheck(packageName).equals(parts[1])) {
            return null;
        }
        return parts[2];
    }

    private static String packageCheck(String packageName) {
        String value = packageName == null ? "" : packageName;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(PACKAGE_CHECK_HEX_LENGTH);
            for (byte b : bytes) {
                if (builder.length() >= PACKAGE_CHECK_HEX_LENGTH) {
                    break;
                }
                builder.append(String.format(Locale.US, "%02x", b));
            }
            return builder.substring(0, PACKAGE_CHECK_HEX_LENGTH);
        } catch (NoSuchAlgorithmException ignored) {
            return String.format(Locale.US, "%08x", value.hashCode());
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
