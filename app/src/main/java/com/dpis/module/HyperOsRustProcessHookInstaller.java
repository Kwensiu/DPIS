package com.dpis.module;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.io.File;
import java.util.List;
import java.util.Locale;

import io.github.libxposed.api.XposedInterface;

final class HyperOsRustProcessHookInstaller {
    private static final String RUST_PROCESS_IMPL = "android.os.RustProcessImpl";
    private static final String START_RUST_PROCESS = "startRustProcess";
    private static final int ARG_PACKAGE_NAME = 1;
    private static final int ARG_ENVIRONMENTS = 21;
    private static final int ARG_BINARY_PATH = 20;
    private static final String MODULE_PACKAGE = "io.github.kwensiu.dpis";
    private static final String NATIVE_LIBRARY_NAME = "libdpis_native.so";

    private HyperOsRustProcessHookInstaller() {
    }

    static boolean install(XposedInterface xposed, PerAppDisplayConfigSource source) {
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

    static String appendEnvironmentForTest(String existing,
                                           String packageName,
                                           int targetFontScalePercent,
                                           String binaryPath) {
        return appendEnvironment(existing, packageName, targetFontScalePercent, binaryPath);
    }

    private static Object[] applyEnvironmentArgs(PerAppDisplayConfigSource source, List<Object> args) {
        if (source == null || args == null || args.size() <= ARG_ENVIRONMENTS) {
            return null;
        }
        Object packageValue = args.get(ARG_PACKAGE_NAME);
        if (!(packageValue instanceof String)) {
            return null;
        }
        String packageName = (String) packageValue;
        PerAppDisplayConfig config = source.get(packageName);
        if (config == null
                || config.targetFontScalePercent == null
                || config.targetFontScalePercent <= 0) {
            HyperOsFlutterFontBridge.clearTarget(packageName);
            return null;
        }
        Object existingValue = args.get(ARG_ENVIRONMENTS);
        String existing = existingValue instanceof String ? (String) existingValue : "";
        Object binaryValue = args.size() > ARG_BINARY_PATH ? args.get(ARG_BINARY_PATH) : null;
        String binaryPath = binaryValue instanceof String ? (String) binaryValue : "";
        String updated = appendEnvironment(existing, packageName,
                config.targetFontScalePercent, binaryPath);
        HyperOsFlutterFontBridge.publishRustBinaryPath(packageName, binaryPath);
        HyperOsFlutterFontBridge.publishRustProxyTarget(packageName, config);
        String proxyLibraryPath = resolveProxyLibraryPath(binaryPath);
        if (proxyLibraryPath == null || proxyLibraryPath.isEmpty()) {
            DpisLog.i("DPIS_FONT HyperOS Rust process proxy missing: package=" + packageName);
            return null;
        }
        DpisLog.i("DPIS_FONT HyperOS Rust process env apply: package=" + packageName
                + ", binary=" + binaryPath
                + ", proxy=" + proxyLibraryPath
                + ", envs=" + updated);
        Object[] updatedArgs = args.toArray();
        updatedArgs[ARG_BINARY_PATH] = proxyLibraryPath;
        updatedArgs[ARG_ENVIRONMENTS] = updated;
        return updatedArgs;
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
        return proxy.isFile() ? proxy.getAbsolutePath() : null;
    }

    private static String appendEnvironment(String existing,
                                            String packageName,
                                            int targetFontScalePercent,
                                            String binaryPath) {
        StringBuilder builder = new StringBuilder();
        if (existing != null && !existing.trim().isEmpty()) {
            builder.append(existing.trim());
            if (builder.charAt(builder.length() - 1) != ',') {
                builder.append(',');
            }
        }
        appendPair(builder, "DPIS_PACKAGE", packageName);
        appendPair(builder, "DPIS_FONT_SCALE_PERCENT",
                String.format(Locale.US, "%d", targetFontScalePercent));
        appendPair(builder, "DPIS_RUST_BINARY", binaryPath == null ? "" : binaryPath);
        builder.append(" --cold-boot-speed");
        return builder.toString();
    }

    private static void appendPair(StringBuilder builder, String key, String value) {
        if (builder.length() > 0) {
            builder.append(" --envs=");
        }
        builder.append(key).append('=').append(sanitize(value));
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(',', '_')
                .replace('\n', '_')
                .replace('\r', '_')
                .replace(' ', '_');
    }

    private static Class<?> resolveClass(String className) {
        try {
            return Class.forName(className);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
