package com.dpis.module;

import io.github.libxposed.api.XposedInterface;

final class ModernApiCapabilitiesResolver {
    static final int API_101 = 101;
    static final int API_102 = 102;

    private ModernApiCapabilitiesResolver() {
    }

    static ModernApiCapabilities fromXposed(XposedInterface xposed) {
        int apiVersion = xposed != null ? xposed.getApiVersion() : API_101;
        return apiVersion >= API_102
                ? ModernApi102Capabilities.INSTANCE
                : ModernApi101Capabilities.INSTANCE;
    }
}
