package com.dpis.module;

import java.lang.reflect.Executable;
import java.util.LinkedHashMap;
import java.util.Map;

import io.github.libxposed.api.XposedInterface;

final class ModernHookRegistry {
    private final Map<String, XposedInterface.HookHandle> handlesById = new LinkedHashMap<>();
    private final Map<Executable, XposedInterface.HookHandle> handlesByExecutable =
            new LinkedHashMap<>();

    void register(String hookId, XposedInterface.HookHandle handle) {
        if (handle == null) {
            return;
        }
        if (hookId != null && !hookId.isBlank()) {
            handlesById.put(hookId, handle);
        }
        Executable executable = handle.getExecutable();
        if (executable != null) {
            handlesByExecutable.put(executable, handle);
        }
    }

    XposedInterface.HookHandle findById(String hookId) {
        if (hookId == null || hookId.isBlank()) {
            return null;
        }
        return handlesById.get(hookId);
    }

    XposedInterface.HookHandle findByExecutable(Executable executable) {
        if (executable == null) {
            return null;
        }
        return handlesByExecutable.get(executable);
    }

    void clear() {
        handlesById.clear();
        handlesByExecutable.clear();
    }
}
