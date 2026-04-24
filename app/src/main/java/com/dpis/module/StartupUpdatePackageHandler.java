package com.dpis.module;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

final class StartupUpdatePackageHandler {
    private final Activity activity;

    StartupUpdatePackageHandler(Activity activity) {
        this.activity = activity;
    }

    void verifyDownloadedApk(File apkFile) throws UntrustedUpdateException {
        PackageManager packageManager = activity.getPackageManager();
        PackageInfo downloadedPackage = readArchivePackageInfo(packageManager, apkFile);
        if (downloadedPackage == null
                || downloadedPackage.packageName == null
                || !activity.getPackageName().equals(downloadedPackage.packageName)) {
            throw new UntrustedUpdateException();
        }

        PackageInfo installedPackage;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                installedPackage = packageManager.getPackageInfo(
                        activity.getPackageName(),
                        PackageManager.GET_SIGNING_CERTIFICATES);
            } else {
                installedPackage = packageManager.getPackageInfo(
                        activity.getPackageName(),
                        PackageManager.GET_SIGNATURES);
            }
        } catch (PackageManager.NameNotFoundException e) {
            throw new UntrustedUpdateException();
        }

        Set<String> downloadedSignatures = extractSigningFingerprints(downloadedPackage);
        Set<String> installedSignatures = extractSigningFingerprints(installedPackage);
        if (downloadedSignatures.isEmpty()
                || installedSignatures.isEmpty()
                || !downloadedSignatures.equals(installedSignatures)) {
            throw new UntrustedUpdateException();
        }
    }

    void launchPackageInstaller(File apkFile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !activity.getPackageManager().canRequestPackageInstalls()) {
            Intent settingsIntent = new Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(settingsIntent);
            showToast(R.string.about_update_install_permission_required);
            return;
        }
        try {
            Uri contentUri = UpdatePackageInstaller.getInstallUri(activity, apkFile);
            Intent installIntent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(contentUri, UpdatePackageInstaller.APK_MIME_TYPE)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(installIntent);
        } catch (ActivityNotFoundException | IllegalArgumentException ignored) {
            showToast(R.string.about_update_install_failed);
        }
    }

    String formatBytes(long bytes) {
        return formatBytesStatic(bytes);
    }

    static String formatBytesStatic(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        double value = bytes;
        String[] units = { "KB", "MB", "GB", "TB" };
        int unitIndex = -1;
        do {
            value /= 1024.0;
            unitIndex++;
        } while (value >= 1024.0 && unitIndex < units.length - 1);
        return String.format(Locale.getDefault(), "%.1f %s", value, units[unitIndex]);
    }

    static void safeDeleteFile(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        // noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    private static PackageInfo readArchivePackageInfo(PackageManager packageManager, File apkFile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return packageManager.getPackageArchiveInfo(
                    apkFile.getAbsolutePath(),
                    PackageManager.GET_SIGNING_CERTIFICATES);
        }
        return packageManager.getPackageArchiveInfo(
                apkFile.getAbsolutePath(),
                PackageManager.GET_SIGNATURES);
    }

    private static Set<String> extractSigningFingerprints(PackageInfo packageInfo) {
        Signature[] signatures;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            SigningInfo signingInfo = packageInfo.signingInfo;
            if (signingInfo == null) {
                return new HashSet<>();
            }
            signatures = signingInfo.hasMultipleSigners()
                    ? signingInfo.getApkContentsSigners()
                    : signingInfo.getSigningCertificateHistory();
        } else {
            signatures = packageInfo.signatures;
        }
        return signaturesToFingerprints(signatures);
    }

    private static Set<String> signaturesToFingerprints(Signature[] signatures) {
        Set<String> fingerprints = new HashSet<>();
        if (signatures == null) {
            return fingerprints;
        }
        for (Signature signature : signatures) {
            if (signature == null) {
                continue;
            }
            fingerprints.add(toSha256Hex(signature.toByteArray()));
        }
        return fingerprints;
    }

    private static String toSha256Hex(byte[] value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(value));
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    private static String toHex(byte[] value) {
        StringBuilder builder = new StringBuilder(value.length * 2);
        for (byte item : value) {
            builder.append(Character.forDigit((item >> 4) & 0xF, 16));
            builder.append(Character.forDigit(item & 0xF, 16));
        }
        return builder.toString();
    }

    private void showToast(int messageResId) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        Toast.makeText(activity, messageResId, Toast.LENGTH_SHORT).show();
    }

    static final class UntrustedUpdateException extends IOException {
    }
}
