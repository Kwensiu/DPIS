package com.dpis.module.fonts;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public final class HyperOsNativeProxyBindMounter {
    private static final String NATIVE_PROXY_LIBRARY_NAME = "libdpis_native.so";

    private HyperOsNativeProxyBindMounter() {
    }

    public static MountPlan createPlan(Context context, String targetPackageName) {
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
        File targetParent = target.getParentFile();
        if (targetParent == null || !targetParent.isDirectory()) {
            return MountPlan.invalid("target native library directory missing: " + targetNativeLibraryDir);
        }
        if (target.exists() && !target.isFile()) {
            return MountPlan.invalid("target proxy mount point is not a file: " + target.getAbsolutePath());
        }
        return new MountPlan(source.getAbsolutePath(), target.getAbsolutePath(), true, "");
    }

    public static MountResult apply(MountPlan plan) {
        if (plan == null || !plan.valid()) {
            return new MountResult(false, plan == null ? "invalid mount plan" : plan.reason());
        }
        return runRootCommand(buildApplyCommand(plan.sourcePath(), plan.targetPath()));
    }

    public static MountResult unmount(MountPlan plan) {
        if (plan == null || plan.sourcePath() == null || plan.sourcePath().isBlank()
                || plan.targetPath() == null || plan.targetPath().isBlank()) {
            return new MountResult(false, "invalid mount target");
        }
        return runRootCommand(buildUnmountCommand(plan.sourcePath(), plan.targetPath()));
    }

    static String buildApplyCommand(String sourcePath, String targetPath) {
        String source = shellQuote(sourcePath);
        String target = shellQuote(targetPath);
        return lazyUnmount(target)
                + ensureTargetFile(target)
                + restoreTargetMetadata(target)
                // App-initiated su may run in a mount namespace that is not visible
                // to the target app's later process, so use a real file copy.
                + copyProxy(source, target)
                + restoreTargetMetadata(target)
                + "cmp -s " + source + " " + target + " || exit 1; "
                + "md5sum " + source + " " + target
                + " 2>/dev/null || true";
    }

    static String buildUnmountCommand(String sourcePath, String targetPath) {
        String source = shellQuote(sourcePath);
        String target = shellQuote(targetPath);
        return "umount -l " + target + " 2>/dev/null || true; "
                + "test ! -e " + target
                + " || cmp -s " + source + " " + target
                + " || exit 1; "
                + "rm -f " + target + " 2>/dev/null || true; "
                + "test ! -e " + target + " || test ! -s " + target;
    }

    private static String lazyUnmount(String target) {
        return "umount -l " + target + " 2>/dev/null || true; ";
    }

    private static String ensureTargetFile(String target) {
        return "if ! test -e " + target + "; then "
                + "touch " + target + " || exit 1; "
                + "fi; ";
    }

    private static String restoreTargetMetadata(String target) {
        return "chown system:system " + target + " 2>/dev/null || true; "
                + "chmod 755 " + target + " 2>/dev/null || true; "
                + "chcon u:object_r:apk_data_file:s0 " + target + " 2>/dev/null || true; ";
    }

    private static String copyProxy(String source, String target) {
        return "echo dpis_proxy_apply=copy; "
                + "cp -f " + source + " " + target
                + " || cat " + source + " > " + target
                + " || exit 1; ";
    }

    private static MountResult runRootCommand(String command) {
        Process process = null;
        StringBuilder output = new StringBuilder();
        try {
            process = com.dpis.module.runtime.SecureProcessLauncher.startMerged("su", "-c", command);
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

    public static final class MountPlan {
        private final String sourcePath;
        private final String targetPath;
        private final boolean valid;
        private final String reason;

        private MountPlan(String sourcePath, String targetPath, boolean valid, String reason) {
            this.sourcePath = sourcePath;
            this.targetPath = targetPath;
            this.valid = valid;
            this.reason = reason;
        }

        static MountPlan invalid(String reason) {
            return new MountPlan(null, null, false, reason == null ? "invalid" : reason);
        }

        public String sourcePath() {
            return sourcePath;
        }

        public String targetPath() {
            return targetPath;
        }

        public boolean valid() {
            return valid;
        }

        public String reason() {
            return reason;
        }
    }

    public static final class MountResult {
        private final boolean success;
        private final String output;

        MountResult(boolean success, String output) {
            this.success = success;
            this.output = output == null ? "" : output;
        }

        public boolean success() {
            return success;
        }

        public String output() {
            return output;
        }
    }
}
