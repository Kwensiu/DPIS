package com.dpis.module;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

final class HyperOsNativeProxyBindMounter {
    private static final String NATIVE_PROXY_LIBRARY_NAME = "libdpis_native.so";

    private HyperOsNativeProxyBindMounter() {
    }

    static MountPlan createPlan(Context context, String targetPackageName) {
        if (context == null || targetPackageName == null || targetPackageName.isBlank()) {
            return MountPlan.invalid("invalid context or target package");
        }
        try {
            ApplicationInfo moduleInfo = context.getApplicationInfo();
            ApplicationInfo targetInfo = context.getPackageManager()
                    .getApplicationInfo(targetPackageName, 0);
            return createPlan(moduleInfo.nativeLibraryDir, targetInfo.nativeLibraryDir);
        } catch (PackageManager.NameNotFoundException | RuntimeException exception) {
            return MountPlan.invalid("target package not found: " + targetPackageName);
        }
    }

    static MountPlan createPlan(String moduleNativeLibraryDir, String targetNativeLibraryDir) {
        if (moduleNativeLibraryDir == null || moduleNativeLibraryDir.isBlank()
                || targetNativeLibraryDir == null || targetNativeLibraryDir.isBlank()) {
            return MountPlan.invalid("native library directory missing");
        }
        File source = new File(moduleNativeLibraryDir, NATIVE_PROXY_LIBRARY_NAME);
        File target = new File(targetNativeLibraryDir, NATIVE_PROXY_LIBRARY_NAME);
        if (!source.isFile()) {
            return MountPlan.invalid("module proxy library missing: " + source.getAbsolutePath());
        }
        if (!target.isFile()) {
            return MountPlan.invalid("target proxy mount point missing: " + target.getAbsolutePath());
        }
        return new MountPlan(source.getAbsolutePath(), target.getAbsolutePath(), true, "");
    }

    static MountResult apply(MountPlan plan) {
        if (plan == null || !plan.valid) {
            return new MountResult(false, plan == null ? "invalid mount plan" : plan.reason);
        }
        return runRootCommand(buildApplyCommand(plan.sourcePath, plan.targetPath));
    }

    static MountResult unmount(MountPlan plan) {
        if (plan == null || plan.targetPath == null || plan.targetPath.isBlank()) {
            return new MountResult(false, "invalid mount target");
        }
        return runRootCommand(buildUnmountCommand(plan.targetPath));
    }

    static String buildApplyCommand(String sourcePath, String targetPath) {
        String source = shellQuote(sourcePath);
        String target = shellQuote(targetPath);
        return "(umount -l " + target + " 2>/dev/null || true)"
                + " && mount -o bind " + source + " " + target
                + " && mount | grep -F -- " + shellQuote(targetPath) + " >/dev/null"
                + " && md5sum " + source + " " + target;
    }

    static String buildUnmountCommand(String targetPath) {
        String target = shellQuote(targetPath);
        return "(umount -l " + target + " 2>/dev/null || true)"
                + " && (mount | grep -F -- " + shellQuote(targetPath)
                + " >/dev/null && exit 1 || exit 0)";
    }

    private static MountResult runRootCommand(String command) {
        Process process = null;
        StringBuilder output = new StringBuilder();
        try {
            process = new ProcessBuilder("su", "-c", command)
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.length() > 0) {
                        output.append('\n');
                    }
                    output.append(line);
                }
            }
            int exitCode = process.waitFor();
            return new MountResult(exitCode == 0, output.toString());
        } catch (IOException exception) {
            return new MountResult(false, exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new MountResult(false, exception.getMessage());
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static String shellQuote(String value) {
        if (value == null) {
            return "''";
        }
        return "'" + value.replace("'", "'\''") + "'";
    }

    static final class MountPlan {
        final String sourcePath;
        final String targetPath;
        final boolean valid;
        final String reason;

        private MountPlan(String sourcePath, String targetPath, boolean valid, String reason) {
            this.sourcePath = sourcePath;
            this.targetPath = targetPath;
            this.valid = valid;
            this.reason = reason;
        }

        static MountPlan invalid(String reason) {
            return new MountPlan(null, null, false, reason == null ? "invalid" : reason);
        }
    }

    static final class MountResult {
        final boolean success;
        final String output;

        MountResult(boolean success, String output) {
            this.success = success;
            this.output = output == null ? "" : output;
        }
    }
}
