package com.dpis.module;

final class ViewportApplyMode {
    static final String OFF = "off";
    static final String SYSTEM_EMULATION = "system_emulation";
    static final String FIELD_REWRITE = "field_rewrite";

    private ViewportApplyMode() {
    }

    static String normalize(String mode) {
        if (FIELD_REWRITE.equals(mode)) {
            return FIELD_REWRITE;
        }
        if (SYSTEM_EMULATION.equals(mode)) {
            return SYSTEM_EMULATION;
        }
        return OFF;
    }

    static boolean isEnabled(String mode) {
        return !OFF.equals(normalize(mode));
    }
}

