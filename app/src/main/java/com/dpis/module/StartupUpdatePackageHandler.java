package com.dpis.module;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import java.io.File;
import java.util.Locale;

final class StartupUpdatePackageHandler {
    private final Activity activity;

    StartupUpdatePackageHandler(Activity activity) {
        this.activity = activity;
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

    private void showToast(int messageResId) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        Toast.makeText(activity, messageResId, Toast.LENGTH_SHORT).show();
    }
}
