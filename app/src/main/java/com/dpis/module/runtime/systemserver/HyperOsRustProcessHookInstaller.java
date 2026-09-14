package com.dpis.module.runtime.systemserver;

import com.dpis.module.hyperos.HyperOsRustProcessArgPolicy;
import com.dpis.module.runtime.font.HyperOsFlutterFontBridge;

import com.dpis.module.diagnostics.DpisLog;
import com.dpis.module.config.PerAppDisplayConfigSource;


import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.io.File;
import java.util.List;

import io.github.libxposed.api.XposedInterface;

public final class HyperOsRustProcessHookInstaller {
    private static final String RUST_PROCESS_IMPL = "android.os.RustProcessImpl";
    private static final String START_RUST_PROCESS = "startRustProcess";
    private static final String NATIVE_LIBRARY_NAME = "libdpis_native.so";
    private HyperOsRustProcessHookInstaller() {
    }

    public static boolean install(XposedInterface xposed, PerAppDisplayConfigSource source) {
        Class<?> clazz = resolveClass(RUST_PROCESS_IMPL);
        if (clazz == null) {
            DpisLog.i("DPIS_FONT HyperOS Rust process hook missing: class=" + RUST_PROCESS_IMPL);
            return false;
        }
        boolean hooked = false;
        for (Method method : clazz.getDeclaredMethods()) {
            if (!START_RUST_PROCESS.equals(method.getName())
                    || Modifier.isAbstract(method.getModifiers())) {
                continue;
            }
            xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        try {
                            List<Object> args = chain.getArgs();
                            logTargetArgumentProbe(args);
                            Object[] updatedArgs = applyEnvironmentArgs(source, args);
                            if (updatedArgs != null) {
                                return chain.proceed(updatedArgs);
                            }
                        } catch (Throwable throwable) {
                            DpisLog.e("DPIS_FONT HyperOS Rust process env hook failed", throwable);
                        }
                        return chain.proceed();
                    });
            hooked = true;
        }
        if (hooked) {
            DpisLog.i("DPIS_FONT HyperOS Rust process hook ready: class=" + RUST_PROCESS_IMPL
                    + ", method=" + START_RUST_PROCESS);
        } else {
            DpisLog.i("DPIS_FONT HyperOS Rust process hook missing: method=" + START_RUST_PROCESS);
        }
        return hooked;
    }

    public static String resolveProxyLibraryPathForTest(String originalBinaryPath) {
        return resolveProxyLibraryPath(originalBinaryPath);
    }

    public static Object[] applyEnvironmentArgsForLegacy(
            PerAppDisplayConfigSource source,
            List<Object> args) {
        return applyEnvironmentArgs(source, args);
    }

    public static void logTargetArgumentProbeForLegacy(List<Object> args) {
        logTargetArgumentProbe(args);
    }

    private static Object[] applyEnvironmentArgs(PerAppDisplayConfigSource source, List<Object> args) {
        if (source == null) {
            return null;
        }
        HyperOsRustProcessArgPolicy.StartArgs parsed = HyperOsRustProcessArgPolicy.parse(args);
        if (parsed == null) {
            return null;
        }
        PerAppDisplayConfig config = source.get(parsed.packageName);
        Integer fontScalePercent = config == null ? null : config.targetFontScalePercent;
        boolean hookEnabled = config != null && config.hyperOsFlutterFontHookEnabled;
        if (!HyperOsRustProcessArgPolicy.shouldRewrite(fontScalePercent, hookEnabled)) {
            return null;
        }
        String updated = HyperOsRustProcessArgPolicy.appendEnvironment(
                parsed.existingEnvironments,
                parsed.packageName,
                fontScalePercent.intValue(),
                parsed.binaryPath);
        // This property is a diagnostic/fallback path only. Full /data/app/... Rust
        // binary paths can exceed Android's system property value limit, while the
        // environment value remains available to the native proxy at process start.
        HyperOsFlutterFontBridge.publishRustBinaryPath(parsed.packageName, parsed.binaryPath);
        HyperOsFlutterFontBridge.publishRustProxyTarget(parsed.packageName, config);
        String proxyLibraryPath = resolveProxyLibraryPath(parsed.binaryPath);
        if (proxyLibraryPath == null || proxyLibraryPath.isEmpty()) {
            DpisLog.i("DPIS_FONT HyperOS Rust process proxy missing: package=" + parsed.packageName);
            return null;
        }
        DpisLog.i("DPIS_FONT HyperOS Rust process env apply: package=" + parsed.packageName
                + ", binary=" + parsed.binaryPath
                + ", proxy=" + proxyLibraryPath
                + ", envs=" + updated);
        return HyperOsRustProcessArgPolicy.withProxyAndEnvironment(
                args, proxyLibraryPath, updated);
    }

    private static void logTargetArgumentProbe(List<Object> args) {
        String summary = HyperOsRustProcessArgPolicy.buildArgumentProbeSummary(args);
        if (summary != null) {
            DpisLog.i(summary);
        }
    }

    private static String resolveProxyLibraryPath(String originalBinaryPath) {
        return resolveSiblingProxyLibraryPath(originalBinaryPath);
    }

    private static String resolveSiblingProxyLibraryPath(String originalBinaryPath) {
        if (originalBinaryPath == null || originalBinaryPath.isEmpty()) {
            return null;
        }
        File parent = new File(originalBinaryPath).getParentFile();
        if (parent == null) {
            return null;
        }
        File proxy = new File(parent, NATIVE_LIBRARY_NAME);
        return proxy.isFile() && proxy.length() > 0 ? proxy.getAbsolutePath() : null;
    }

    private static Class<?> resolveClass(String className) {
        try {
            return Class.forName(className);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
