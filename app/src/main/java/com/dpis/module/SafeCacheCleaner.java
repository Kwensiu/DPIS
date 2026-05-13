package com.dpis.module;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.util.Locale;

final class SafeCacheCleaner {
    private SafeCacheCleaner() {
    }

    static String formatCacheUsage(Context context) {
        long bytes = calculateCacheBytes(context);
        return formatBytes(bytes);
    }

    static void clearAll(Context context) {
        if (context == null) {
            return;
        }
        deleteChildren(context.getCacheDir());
        deleteChildren(context.getExternalCacheDir());
        clearFontDebugCaches(context);
        FontDebugStatsStore.clearStats(context);
        clearUpdateCaches(context);
        clearReleaseNotesCache(context);
    }

    static boolean hasLegacyPublicFontDebugCache() {
        File legacyFile = legacyFontDebugFile();
        return legacyFile != null && legacyFile.isFile();
    }

    private static void clearFontDebugCaches(Context context) {
        File appSpecificDir = FontDebugStatsFileBridge.resolveAppSpecificStatsFile(
                context.getExternalFilesDir(null));
        deleteRecursively(appSpecificDir == null ? null : appSpecificDir.getParentFile());
        File legacyFile = legacyFontDebugFile();
        File legacyDir = legacyFile == null ? null : legacyFile.getParentFile();
        if (legacyDir != null && legacyDir.exists() && legacyDir.list() != null
                && legacyDir.list().length == 0) {
            //noinspection ResultOfMethodCallIgnored
            legacyDir.delete();
        }
    }

    private static void clearUpdateCaches(Context context) {
        UpdatePackageInstaller.clearUpdateCache(context);
    }

    private static void clearReleaseNotesCache(Context context) {
        new ReleaseNotesCacheStore(context).clear();
    }

    private static long calculateCacheBytes(Context context) {
        long total = 0L;
        total += folderSize(context.getCacheDir());
        total += folderSize(context.getExternalCacheDir());
        total += folderSize(appSpecificFontDebugDir(context));
        total += FontDebugStatsStore.estimateStatsBytes(context);
        total += fileSize(legacyFontDebugFile());
        total += folderSize(updateCacheDir(context));
        total += new ReleaseNotesCacheStore(context).estimateCacheBytes();
        return total;
    }

    private static File appSpecificFontDebugDir(Context context) {
        File file = FontDebugStatsFileBridge.resolveAppSpecificStatsFile(
                context.getExternalFilesDir(null));
        return file == null ? null : file.getParentFile();
    }

    private static File legacyFontDebugFile() {
        // Historical builds wrote this debug cache into public Downloads. Current
        // builds only report it and ask the user to delete it manually because
        // scoped storage makes silent removal unreliable across install UIDs.
        return FontDebugStatsFileBridge.resolveLegacyPublicStatsFile(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
    }

    private static File updateCacheDir(Context context) {
        File externalDownloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (externalDownloadsDir == null) {
            return new File(context.getFilesDir(), "updates");
        }
        return new File(externalDownloadsDir, "updates");
    }

    private static long folderSize(File dir) {
        if (dir == null || !dir.exists()) {
            return 0L;
        }
        if (dir.isFile()) {
            return dir.length();
        }
        long total = 0L;
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                total += folderSize(child);
            }
        }
        return total;
    }

    private static long fileSize(File file) {
        return file != null && file.isFile() ? file.length() : 0L;
    }

    private static void deleteChildren(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            deleteRecursively(child);
        }
    }

    private static void deleteRecursively(File target) {
        if (target == null || !target.exists()) {
            return;
        }
        if (target.isDirectory()) {
            File[] children = target.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        //noinspection ResultOfMethodCallIgnored
        target.delete();
    }

    static String formatBytes(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        double kb = bytes / 1024d;
        if (kb < 1024d) {
            return String.format(Locale.US, "%.1f KB", kb);
        }
        return String.format(Locale.US, "%.1f MB", kb / 1024d);
    }
}
