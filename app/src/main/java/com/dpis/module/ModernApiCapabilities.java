package com.dpis.module;

import io.github.libxposed.api.XposedInterface;

interface ModernApiCapabilities {
    int apiVersion();

    boolean supportsStableHookIds();

    boolean supportsHotReloadCallbacks();

    <T extends XposedInterface.HookBuilder> T applyStableHookId(T builder, String hookId);
}
