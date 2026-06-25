package com.dpis.module;

import io.github.libxposed.api.XposedInterface;

final class ModernApi101Capabilities implements ModernApiCapabilities {
    static final ModernApi101Capabilities INSTANCE = new ModernApi101Capabilities();

    private ModernApi101Capabilities() {
    }

    @Override
    public int apiVersion() {
        return ModernApiCapabilitiesResolver.API_101;
    }

    @Override
    public boolean supportsStableHookIds() {
        return false;
    }

    @Override
    public boolean supportsHotReloadCallbacks() {
        return false;
    }

    @Override
    public <T extends XposedInterface.HookBuilder> T applyStableHookId(T builder, String hookId) {
        return builder;
    }
}
