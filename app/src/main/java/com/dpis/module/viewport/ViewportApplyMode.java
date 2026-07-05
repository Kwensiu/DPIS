package com.dpis.module.viewport;

public final class ViewportApplyMode {
    public static final String OFF = "off";
    public static final String AUTO = "auto";
    public static final String SYSTEM = "system";
    public static final String COMPAT = "compat";
    public static final String LEGACY_SYSTEM_EMULATION = "system_emulation";
    public static final String LEGACY_FIELD_REWRITE = "field_rewrite";
    // Compatibility aliases for old call sites. Persisted values normalize to
    // SYSTEM / COMPAT.
    @Deprecated
    public static final String SYSTEM_EMULATION = SYSTEM;
    @Deprecated
    public static final String FIELD_REWRITE = COMPAT;

    private ViewportApplyMode() {
    }

    public static String normalize(String mode) {
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

    public static boolean isEnabled(String mode) {
        String normalized = normalize(mode);
        return AUTO.equals(normalized) || SYSTEM.equals(normalized) || COMPAT.equals(normalized);
    }
}

