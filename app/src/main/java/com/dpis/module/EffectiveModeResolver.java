package com.dpis.module;

final class EffectiveModeResolver {
    private EffectiveModeResolver() {
    }

    static String resolveViewportMode(String requestedMode, boolean systemHooksEnabled) {
        String normalized = ViewportApplyMode.normalize(requestedMode);
        if (ViewportApplyMode.AUTO.equals(normalized)) {
            // Auto is system-first only. Compat must be selected explicitly or
            // reached by guarded runtime evidence, not by a missing system route.
            return systemHooksEnabled ? ViewportApplyMode.SYSTEM : ViewportApplyMode.OFF;
        }
        if (ViewportApplyMode.SYSTEM_EMULATION.equals(normalized) && !systemHooksEnabled) {
            return ViewportApplyMode.OFF;
        }
        return normalized;
    }

    static String resolveFontMode(String requestedMode, boolean systemHooksEnabled) {
        String normalized = FontApplyMode.normalize(requestedMode);
        if (FontApplyMode.SYSTEM_EMULATION.equals(normalized) && !systemHooksEnabled) {
            return FontApplyMode.OFF;
        }
        return normalized;
    }
}
