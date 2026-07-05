package com.dpis.module.runtime.hookapi;

import io.github.libxposed.api.XposedInterface;

public interface ModernApiCapabilities {
    int apiVersion();

    boolean supportsStableHookIds();

    boolean supportsHotReloadCallbacks();

    <T extends XposedInterface.HookBuilder> T applyStableHookId(T builder, String hookId);
}
