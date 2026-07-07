package com.dpis.module;

import com.dpis.module.runtime.systemserver.HyperOsRustProcessHookInstaller;

import com.dpis.module.config.PerAppDisplayConfigSource;


import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class LegacyRustProcessHookInstaller {
    private static final String RUST_PROCESS_IMPL = "android.os.RustProcessImpl";
    private static final String START_RUST_PROCESS = "startRustProcess";

    private LegacyRustProcessHookInstaller() {
    }

    static boolean install(PerAppDisplayConfigSource source) {
        Class<?> clazz = resolveClass(RUST_PROCESS_IMPL);
        if (clazz == null) {
            DpisLog.i("DPIS_FONT HyperOS Rust process legacy hook missing: class=" + RUST_PROCESS_IMPL);
            return false;
        }
        boolean hooked = false;
        for (Method method : clazz.getDeclaredMethods()) {
            if (!START_RUST_PROCESS.equals(method.getName())
                    || Modifier.isAbstract(method.getModifiers())) {
                continue;
            }
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        List<Object> args = Arrays.asList(param.args);
                        HyperOsRustProcessHookInstaller.logTargetArgumentProbeForLegacy(args);
                        Object[] updatedArgs =
                                HyperOsRustProcessHookInstaller.applyEnvironmentArgsForLegacy(source, args);
                        if (updatedArgs != null) {
                            param.args = updatedArgs;
                        }
                    } catch (Throwable throwable) {
                        DpisLog.e("DPIS_FONT HyperOS Rust process legacy env hook failed", throwable);
                    }
                }
            });
            hooked = true;
        }
        if (hooked) {
            DpisLog.i("DPIS_FONT HyperOS Rust process legacy hook ready: class=" + RUST_PROCESS_IMPL
                    + ", method=" + START_RUST_PROCESS);
        } else {
            DpisLog.i("DPIS_FONT HyperOS Rust process legacy hook missing: method=" + START_RUST_PROCESS);
        }
        return hooked;
    }

    private static Class<?> resolveClass(String className) {
        try {
            return Class.forName(className);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
