package com.dpis.module;

import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;

final class ModernApi102Capabilities implements ModernApiCapabilities {
    static final ModernApi102Capabilities INSTANCE = new ModernApi102Capabilities();
    private final Method setIdMethod;

    private ModernApi102Capabilities() {
        setIdMethod = findSetIdMethod();
    }

    @Override
    public int apiVersion() {
        return ModernApiCapabilitiesResolver.API_102;
    }

    @Override
    public boolean supportsStableHookIds() {
        return setIdMethod != null;
    }

    @Override
    public boolean supportsHotReloadCallbacks() {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends XposedInterface.HookBuilder> T applyStableHookId(T builder, String hookId) {
        if (builder == null || hookId == null || hookId.isBlank() || setIdMethod == null) {
            return builder;
        }
        try {
            Object result = setIdMethod.invoke(builder, hookId);
            return result instanceof XposedInterface.HookBuilder
                    ? (T) result
                    : builder;
        } catch (ReflectiveOperationException | RuntimeException throwable) {
            DpisLog.e("modern api 102 setId failed: hookId=" + hookId, throwable);
            return builder;
        }
    }

    private static Method findSetIdMethod() {
        try {
            return XposedInterface.HookBuilder.class.getMethod("setId", String.class);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }
}
