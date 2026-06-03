package com.dpis.module;

final class WechatTargetFieldConfig {
    static final String PACKAGE_NAME = "com.tencent.mm";
    static final int MIN_TARGET_FIELD = 300;
    static final int MAX_TARGET_FIELD = 1200;

    private WechatTargetFieldConfig() {
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
        if (value == null || value < MIN_TARGET_FIELD || value > MAX_TARGET_FIELD) {
            return null;
        }
        return value;
    }
}
