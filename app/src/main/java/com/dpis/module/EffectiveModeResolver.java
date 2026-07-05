package com.dpis.module;

public final class EffectiveModeResolver {
    private EffectiveModeResolver() {
    }

    public static String resolveViewportMode(String requestedMode, boolean systemHooksEnabled) {
        String normalized = ViewportApplyMode.normalize(requestedMode);
        if (ViewportApplyMode.AUTO.equals(normalized)) {
            // Auto is system-first, but if the system route is unavailable or
            // ineffective we fall back to compat rather than dropping to off.
            return systemHooksEnabled ? ViewportApplyMode.SYSTEM : ViewportApplyMode.COMPAT;
        }
        if (ViewportApplyMode.SYSTEM_EMULATION.equals(normalized) && !systemHooksEnabled) {
            return ViewportApplyMode.OFF;
        }
        return normalized;
    }

    public static String resolveFontMode(String requestedMode, boolean systemHooksEnabled) {
        String normalized = FontApplyMode.normalize(requestedMode);
        if (FontApplyMode.SYSTEM_EMULATION.equals(normalized) && !systemHooksEnabled) {
            return FontApplyMode.OFF;
        }
        return normalized;
    }
}
