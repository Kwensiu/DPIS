package com.dpis.module.runtime.hookapi;

import io.github.libxposed.api.XposedInterface;

public final class ModernApiCapabilitiesResolver {
    static final int API_101 = 101;
    static final int API_102 = 102;

    private ModernApiCapabilitiesResolver() {
    }

    public static ModernApiCapabilities fromXposed(XposedInterface xposed) {
        // One Modern artifact keeps API 101 as the loading baseline and targets API 102.
        // API 102 hosts may exercise hot reload and stable hook ids; API 101 keeps
        // the normal install path and ignores the unavailable lifecycle enhancement.
        int apiVersion = xposed != null ? xposed.getApiVersion() : API_101;
        return apiVersion >= API_102
                ? ModernApi102Capabilities.INSTANCE
                : ModernApi101Capabilities.INSTANCE;
    }
}
