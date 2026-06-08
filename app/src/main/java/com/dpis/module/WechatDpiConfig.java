package com.dpis.module;

final class WechatDpiConfig {
    static final String PACKAGE_NAME = "com.tencent.mm";
    static final int MIN_DPI = 200;
    static final int MAX_DPI = 1000;

    private WechatDpiConfig() {
    }

    static boolean appliesTo(String packageName) {
        return PACKAGE_NAME.equals(packageName);
    }

    static Integer parseOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return normalize(Integer.parseInt(raw.trim()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static boolean isInputValid(String raw) {
        return raw == null || raw.isBlank() || parseOrNull(raw) != null;
    }

    static Integer normalize(Integer value) {
        if (value == null || value < MIN_DPI || value > MAX_DPI) {
            return null;
        }
        return value;
    }
}
