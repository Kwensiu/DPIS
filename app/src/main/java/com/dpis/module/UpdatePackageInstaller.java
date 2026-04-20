package com.dpis.module;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import androidx.core.content.FileProvider;

import java.io.File;

final class UpdatePackageInstaller {
    static final String APK_MIME_TYPE = "application/vnd.android.package-archive";

    private static final String PREFS_NAME = "update_download_state";
    private static final String KEY_DOWNLOAD_ID = "download_id";
    private static final String KEY_FILE_PATH = "file_path";
    private static final String KEY_CREATED_AT_MS = "created_at_ms";
    private static final String UPDATES_DIR_NAME = "updates";

    private UpdatePackageInstaller() {
    }

    static File prepareTargetFile(Context context, String versionName) {
        File updatesDir = resolveUpdatesDir(context);
        if (!updatesDir.exists() && !updatesDir.mkdirs()) {
            throw new IllegalStateException("Unable to create update cache directory");
        }
        String safeVersion = sanitize(versionName);
        String fileName = "dpis-" + safeVersion + "-" + System.currentTimeMillis() + ".apk";
        return new File(updatesDir, fileName);
    }

    static void persistDownloadState(Context context, long downloadId, File targetFile) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_DOWNLOAD_ID, downloadId)
                .putString(KEY_FILE_PATH, targetFile.getAbsolutePath())
                .putLong(KEY_CREATED_AT_MS, System.currentTimeMillis())
                .apply();
    }

    static void persistDownloadedFile(Context context, File targetFile) {
        persistDownloadState(context, -1L, targetFile);
    }

    static DownloadState readDownloadState(Context context) {
        long downloadId = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_DOWNLOAD_ID, -1L);
        String filePath = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_FILE_PATH, null);
        long createdAtMs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_CREATED_AT_MS, -1L);
        if (filePath == null || filePath.trim().isEmpty()) {
            return null;
        }
        return new DownloadState(downloadId, filePath, createdAtMs);
    }

    static void clearUpdateCache(Context context) {
        DownloadState state = readDownloadState(context);
        if (state != null) {
            removeDownloadRecord(context, state.downloadId);
        }

        File updatesDir = resolveUpdatesDir(context);
        if (updatesDir.exists()) {
            deleteRecursively(updatesDir);
        }
        clearDownloadState(context);
    }

    static void clearStaleUpdateCache(Context context, long maxAgeMs) {
        if (maxAgeMs <= 0L) {
            clearUpdateCache(context);
            return;
        }

        long nowMs = System.currentTimeMillis();
        DownloadState state = readDownloadState(context);
        if (state != null) {
            File trackedFile = new File(state.filePath);
            boolean missing = !trackedFile.exists();
            boolean stale = isStale(state.createdAtMs, trackedFile.lastModified(), nowMs, maxAgeMs);
            if (missing || stale) {
                removeDownloadRecord(context, state.downloadId);
                if (trackedFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    trackedFile.delete();
                }
                clearDownloadState(context);
            }
        }

        pruneStaleFiles(resolveUpdatesDir(context), nowMs, maxAgeMs);
    }

    static Uri getInstallUri(Context context, File apkFile) {
        return FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                apkFile);
    }

    private static File resolveUpdatesDir(Context context) {
        File externalDownloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (externalDownloadsDir == null) {
            return new File(context.getFilesDir(), UPDATES_DIR_NAME);
        }
        return new File(externalDownloadsDir, UPDATES_DIR_NAME);
    }

    private static void clearDownloadState(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_DOWNLOAD_ID)
                .remove(KEY_FILE_PATH)
                .remove(KEY_CREATED_AT_MS)
                .apply();
    }

    private static boolean isStale(long createdAtMs,
                                   long fileLastModifiedMs,
                                   long nowMs,
                                   long maxAgeMs) {
        long referenceMs = createdAtMs > 0L ? createdAtMs : fileLastModifiedMs;
        if (referenceMs <= 0L || nowMs <= referenceMs) {
            return false;
        }
        return nowMs - referenceMs > maxAgeMs;
    }

    private static void pruneStaleFiles(File dir, long nowMs, long maxAgeMs) {
        if (!dir.exists()) {
            return;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }

        for (File child : children) {
            if (child.isDirectory()) {
                pruneStaleFiles(child, nowMs, maxAgeMs);
                File[] nested = child.listFiles();
                if (nested == null || nested.length == 0) {
                    //noinspection ResultOfMethodCallIgnored
                    child.delete();
                }
                continue;
            }
            if (isStale(-1L, child.lastModified(), nowMs, maxAgeMs)) {
                //noinspection ResultOfMethodCallIgnored
                child.delete();
            }
        }

        File[] remaining = dir.listFiles();
        if (remaining == null || remaining.length == 0) {
            //noinspection ResultOfMethodCallIgnored
            dir.delete();
        }
    }

    private static void removeDownloadRecord(Context context, long downloadId) {
        DownloadManager downloadManager =
                (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager == null || downloadId <= 0L) {
            return;
        }
        try {
            downloadManager.remove(downloadId);
        } catch (RuntimeException ignored) {
            // Ignore and continue local cleanup.
        }
    }

    private static String sanitize(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "update";
        }
        return input.replaceAll("[^0-9A-Za-z._-]", "_");
    }

    private static void deleteRecursively(File target) {
        File[] children = target.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        //noinspection ResultOfMethodCallIgnored
        target.delete();
    }

    static final class DownloadState {
        final long downloadId;
        final String filePath;
        final long createdAtMs;

        DownloadState(long downloadId, String filePath, long createdAtMs) {
            this.downloadId = downloadId;
            this.filePath = filePath;
            this.createdAtMs = createdAtMs;
        }
    }
}
