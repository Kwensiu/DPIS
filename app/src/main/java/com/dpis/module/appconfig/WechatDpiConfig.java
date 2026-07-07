package com.dpis.module.appconfig;

import com.dpis.module.viewport.DpiConfig;

public final class WechatDpiConfig {
    public static final String PACKAGE_NAME = "com.tencent.mm";
    public static final int MIN_DPI = 200;
    public static final int MAX_DPI = 1000;

    private WechatDpiConfig() {
    }

    public static boolean appliesTo(String packageName) {
        return PACKAGE_NAME.equals(packageName);
    }

    public static Integer parseOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return normalize(Integer.parseInt(raw.trim()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static boolean isInputValid(String raw) {
        return raw == null || raw.isBlank() || parseOrNull(raw) != null;
    }

    public static Integer normalize(Integer value) {
        if (value == null || value < MIN_DPI || value > MAX_DPI) {
            return null;
        }
        return value;
    }
}
