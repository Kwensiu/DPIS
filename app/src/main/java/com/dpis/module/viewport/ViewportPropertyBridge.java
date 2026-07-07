package com.dpis.module.viewport;

import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;

import java.lang.reflect.Method;
import java.util.Locale;

public final class ViewportPropertyBridge {
    private static final String PROPERTY_PREFIX = "debug.dpis.vp.";
    private static final String TARGET_TYPE_PROPERTY_PREFIX = "debug.dpis.vptype.";
    private static final String SCALE_PROPERTY_PREFIX = "debug.dpis.vpscale.";
    // legacy needs the requested value even for field_rewrite, while vp.* must
    // stay 0 unless system emulation is active.
    private static final String COMPAT_CONFIG_PROPERTY_PREFIX = "debug.dpis.vpcfg.";
    private static final String COMPAT_MODE_PROPERTY_PREFIX = "debug.dpis.vpmode.";
    private static final String PERSIST_PROPERTY_PREFIX = "persist.debug.dpis.vp.";
    private static final String PERSIST_TARGET_TYPE_PROPERTY_PREFIX = "persist.debug.dpis.vptype.";
    private static final String PERSIST_SCALE_PROPERTY_PREFIX = "persist.debug.dpis.vpscale.";
    private static final String PERSIST_COMPAT_CONFIG_PROPERTY_PREFIX = "persist.debug.dpis.vpcfg.";
    private static final String PERSIST_COMPAT_MODE_PROPERTY_PREFIX = "persist.debug.dpis.vpmode.";

    private ViewportPropertyBridge() {
    }

    public static String propertyNameForPackage(String packageName) {
        return PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    public static String compatConfigPropertyNameForPackage(String packageName) {
        return COMPAT_CONFIG_PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    public static String targetTypePropertyNameForPackage(String packageName) {
        return TARGET_TYPE_PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    public static String scalePropertyNameForPackage(String packageName) {
        return SCALE_PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    public static String compatModePropertyNameForPackage(String packageName) {
        return COMPAT_MODE_PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    public static String persistentPropertyNameForPackage(String packageName) {
        return PERSIST_PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    public static String persistentCompatConfigPropertyNameForPackage(String packageName) {
        return PERSIST_COMPAT_CONFIG_PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    public static String persistentTargetTypePropertyNameForPackage(String packageName) {
        return PERSIST_TARGET_TYPE_PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    public static String persistentScalePropertyNameForPackage(String packageName) {
        return PERSIST_SCALE_PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    public static String persistentCompatModePropertyNameForPackage(String packageName) {
        return PERSIST_COMPAT_MODE_PROPERTY_PREFIX + String.format(Locale.US, "%08x", packageName.hashCode());
    }

    public static Integer readTargetWidthDp(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return null;
        }
        return readOverrideValue(propertyNameForPackage(packageName),
                persistentPropertyNameForPackage(packageName));
    }

    public static Integer readCompatConfigWidthDp(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return null;
        }
        return readOverrideValue(compatConfigPropertyNameForPackage(packageName),
                persistentCompatConfigPropertyNameForPackage(packageName));
    }

    public static ViewportTargetSpec readTargetSpec(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return ViewportTargetSpec.off();
        }
        String type = readPropertyWithPersistentFallback(
                targetTypePropertyNameForPackage(packageName),
                persistentTargetTypePropertyNameForPackage(packageName));
        Integer widthDp = readTargetWidthDp(packageName);
        Integer compatConfigWidthDp = readCompatConfigWidthDp(packageName);
        Integer scaleMilliPercent = readOverrideValue(
                scalePropertyNameForPackage(packageName),
                persistentScalePropertyNameForPackage(packageName));
        ViewportPropertyProjection.Decoded decoded = ViewportPropertyProjection.decode(
                widthDp, type, scaleMilliPercent, compatConfigWidthDp, null);
        return decoded.targetSpec;
    }

    public static String readCompatMode(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return ViewportApplyMode.OFF;
        }
        return ViewportApplyMode.normalize(readPropertyWithPersistentFallback(
                compatModePropertyNameForPackage(packageName),
                persistentCompatModePropertyNameForPackage(packageName)));
    }

    public static Integer parseOverrideValueForTest(String value) {
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
