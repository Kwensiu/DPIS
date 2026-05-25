package com.dpis.module;

final class ViewportApplyMode {
    static final String OFF = "off";
    static final String AUTO = "auto";
    static final String SYSTEM = "system";
    static final String COMPAT = "compat";
    static final String LEGACY_SYSTEM_EMULATION = "system_emulation";
    static final String LEGACY_FIELD_REWRITE = "field_rewrite";
    // Compatibility aliases for old call sites. Persisted values normalize to
    // SYSTEM / COMPAT.
    @Deprecated
    static final String SYSTEM_EMULATION = SYSTEM;
    @Deprecated
    static final String FIELD_REWRITE = COMPAT;

    private ViewportApplyMode() {
    }

    static String normalize(String mode) {
        if (AUTO.equals(mode)) {
            return AUTO;
        }
        if (COMPAT.equals(mode) || LEGACY_FIELD_REWRITE.equals(mode)) {
            return COMPAT;
        }
        if (SYSTEM.equals(mode) || LEGACY_SYSTEM_EMULATION.equals(mode)) {
            return SYSTEM;
        }
        return OFF;
    }

    static boolean isEnabled(String mode) {
        String normalized = normalize(mode);
        return AUTO.equals(normalized) || SYSTEM.equals(normalized) || COMPAT.equals(normalized);
    }
}

