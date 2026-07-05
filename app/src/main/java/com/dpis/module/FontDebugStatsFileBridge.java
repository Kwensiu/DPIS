package com.dpis.module;

import com.dpis.module.fonts.FontDebugStatsSchema;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

final class FontDebugStatsFileBridge {
    private static final String DIR_NAME = "font_debug_stats";
    private static final String FILE_NAME = "font_debug_stats.properties";

    private FontDebugStatsFileBridge() {
    }

    static void write(Context context, Bundle extras) {
        File file = resolveFile(context);
        if (file == null || extras == null || extras.isEmpty()) {
            return;
        }
        File parent = file.getParentFile();
        if (parent == null || (!parent.exists() && !parent.mkdirs())) {
            return;
        }
        Properties properties = new Properties();
        FontDebugStatsSchema.copyExtrasToProperties(extras, properties);
        try (FileOutputStream output = new FileOutputStream(file)) {
            properties.store(output, null);
        } catch (IOException ignored) {
        }
    }

    static void importIfNewer(Context context) {
        if (context == null) {
            return;
        }
        SharedPreferences preferences = FontDebugStatsStore.getPreferences(context);
        importIfNewer(preferences, resolveFile(context));
        File legacyFile = resolveLegacyPublicFile();
        importIfNewer(preferences, legacyFile);
    }

    static void importIfNewer(SharedPreferences preferences, Properties properties) {
        if (preferences == null || properties == null || properties.isEmpty()) {
            return;
        }
        long incomingUpdatedAt = FontDebugStatsSchema.propertyUpdatedAt(properties);
        long currentUpdatedAt = preferences.getLong(FontDebugStatsStore.KEY_UPDATED_AT, 0L);
        if (incomingUpdatedAt <= 0L || incomingUpdatedAt <= currentUpdatedAt) {
            return;
        }
        SharedPreferences.Editor editor = preferences.edit();
        FontDebugStatsSchema.copyPropertiesToPreferences(properties, editor);
        editor.apply();
    }

    static void importIfNewer(SharedPreferences preferences, File file) {
        Properties properties = loadProperties(file);
        if (properties != null) {
            importIfNewer(preferences, properties);
        }
    }

    static File resolveAppSpecificStatsFile(Context context) {
        return resolveFile(context);
    }

    static File resolveAppSpecificStatsFile(File baseDir) {
        return resolveFile(baseDir);
    }

    static File resolveLegacyPublicStatsFile(File downloads) {
        return resolveLegacyPublicFile(downloads);
    }

    private static File resolveFile(Context context) {
        File baseDir = context != null ? context.getExternalFilesDir(null) : null;
        return resolveFile(baseDir);
    }

    private static File resolveFile(File baseDir) {
        if (baseDir == null) {
            return null;
        }
        return new File(new File(baseDir, DIR_NAME), FILE_NAME);
    }

    private static File resolveLegacyPublicFile() {
        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        return resolveLegacyPublicFile(downloads);
    }

    private static File resolveLegacyPublicFile(File downloads) {
        if (downloads == null) {
            return null;
        }
        File legacyDir = new File(downloads, "DPIS");
        return new File(legacyDir, FILE_NAME);
    }

    private static Properties loadProperties(File file) {
        if (file == null || !file.isFile()) {
            return null;
        }
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(file)) {
            properties.load(input);
        } catch (IOException ignored) {
            return null;
        }
        return properties;
    }

}
