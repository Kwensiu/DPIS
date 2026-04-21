package com.dpis.module;

final class EffectiveModeResolver {
    private EffectiveModeResolver() {
    }

    static String resolveViewportMode(String requestedMode, boolean systemHooksEnabled) {
        String normalized = ViewportApplyMode.normalize(requestedMode);
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
