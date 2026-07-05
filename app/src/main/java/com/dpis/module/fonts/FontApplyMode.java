package com.dpis.module.fonts;

public final class FontApplyMode {
    public static final String OFF = "off";
    // Persisted/runtime value. UI labels this as "System mode".
    public static final String SYSTEM_EMULATION = "system_emulation";
    // Persisted/runtime value. UI labels this as "Compat mode" to avoid
    // confusion with future font family/style replacement features.
    public static final String FIELD_REWRITE = "field_rewrite";

    private FontApplyMode() {
    }

    public static String normalize(String raw) {
        if (SYSTEM_EMULATION.equals(raw) || FIELD_REWRITE.equals(raw) || OFF.equals(raw)) {
            return raw;
        }
        return OFF;
    }

    public static boolean isEnabled(String mode) {
        String normalized = normalize(mode);
        return !OFF.equals(normalized);
    }
}
