package com.dpis.module;

final class FontApplyMode {
    static final String OFF = "off";
    static final String SYSTEM_EMULATION = "system_emulation";
    static final String FIELD_REWRITE = "field_rewrite";

    private FontApplyMode() {
    }

    static String normalize(String raw) {
        if (SYSTEM_EMULATION.equals(raw) || FIELD_REWRITE.equals(raw) || OFF.equals(raw)) {
            return raw;
        }
        return OFF;
    }

    static boolean isEnabled(String mode) {
        String normalized = normalize(mode);
        return !OFF.equals(normalized);
    }
}
