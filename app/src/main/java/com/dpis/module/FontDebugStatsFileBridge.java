package com.dpis.module;

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
    private static final String DIR_NAME = "DPIS";
    private static final String FILE_NAME = "font_debug_stats.properties";

    private FontDebugStatsFileBridge() {
    }

    static void write(Bundle extras) {
        File file = resolveFile();
        if (file == null || extras == null || extras.isEmpty()) {
            return;
        }
        File parent = file.getParentFile();
        if (parent == null || (!parent.exists() && !parent.mkdirs())) {
            return;
        }
        Properties properties = new Properties();
        putString(properties, extras, FontDebugStatsStore.EXTRA_CHAIN_5S);
        putString(properties, extras, FontDebugStatsStore.EXTRA_CHAIN_30S);
        putString(properties, extras, FontDebugStatsStore.EXTRA_CHAIN_ALL);
        putString(properties, extras, FontDebugStatsStore.EXTRA_CHAIN_VIEW_5S);
        putString(properties, extras, FontDebugStatsStore.EXTRA_CHAIN_VIEW_30S);
        putString(properties, extras, FontDebugStatsStore.EXTRA_CHAIN_VIEW_ALL);
        putString(properties, extras, FontDebugStatsStore.EXTRA_UNIT_BREAKDOWN_5S);
        putString(properties, extras, FontDebugStatsStore.EXTRA_VIEWPORT_DEBUG_SUMMARY);
        if (extras.containsKey(FontDebugStatsStore.EXTRA_EVENT_TOTAL)) {
            properties.setProperty(FontDebugStatsStore.EXTRA_EVENT_TOTAL,
                    String.valueOf(extras.getInt(FontDebugStatsStore.EXTRA_EVENT_TOTAL, 0)));
        }
        if (extras.containsKey(FontDebugStatsStore.EXTRA_UPDATED_AT)) {
            properties.setProperty(FontDebugStatsStore.EXTRA_UPDATED_AT,
                    String.valueOf(extras.getLong(FontDebugStatsStore.EXTRA_UPDATED_AT, 0L)));
        }
        try (FileOutputStream output = new FileOutputStream(file)) {
            properties.store(output, null);
        } catch (IOException ignored) {
        }
    }

    static void importIfNewer(Context context) {
        File file = resolveFile();
        if (context == null || file == null || !file.isFile()) {
            return;
        }
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(file)) {
            properties.load(input);
        } catch (IOException ignored) {
            return;
        }
        importIfNewer(FontDebugStatsStore.getPreferences(context), properties);
    }

    static void importIfNewer(SharedPreferences preferences, Properties properties) {
        if (preferences == null || properties == null || properties.isEmpty()) {
            return;
        }
        long incomingUpdatedAt = parseLong(properties.getProperty(FontDebugStatsStore.EXTRA_UPDATED_AT), 0L);
        long currentUpdatedAt = preferences.getLong(FontDebugStatsStore.KEY_UPDATED_AT, 0L);
        if (incomingUpdatedAt <= 0L || incomingUpdatedAt <= currentUpdatedAt) {
            return;
        }
        SharedPreferences.Editor editor = preferences.edit();
        copyString(properties, editor, FontDebugStatsStore.EXTRA_CHAIN_5S,
                FontDebugStatsStore.KEY_CHAIN_5S);
        copyString(properties, editor, FontDebugStatsStore.EXTRA_CHAIN_30S,
                FontDebugStatsStore.KEY_CHAIN_30S);
        copyString(properties, editor, FontDebugStatsStore.EXTRA_CHAIN_ALL,
                FontDebugStatsStore.KEY_CHAIN_ALL);
        copyString(properties, editor, FontDebugStatsStore.EXTRA_CHAIN_VIEW_5S,
                FontDebugStatsStore.KEY_CHAIN_VIEW_5S);
        copyString(properties, editor, FontDebugStatsStore.EXTRA_CHAIN_VIEW_30S,
                FontDebugStatsStore.KEY_CHAIN_VIEW_30S);
        copyString(properties, editor, FontDebugStatsStore.EXTRA_CHAIN_VIEW_ALL,
                FontDebugStatsStore.KEY_CHAIN_VIEW_ALL);
        copyString(properties, editor, FontDebugStatsStore.EXTRA_UNIT_BREAKDOWN_5S,
                FontDebugStatsStore.KEY_UNIT_BREAKDOWN_5S);
        copyString(properties, editor, FontDebugStatsStore.EXTRA_VIEWPORT_DEBUG_SUMMARY,
                FontDebugStatsStore.KEY_VIEWPORT_DEBUG_SUMMARY);
        copyInt(properties, editor, FontDebugStatsStore.EXTRA_EVENT_TOTAL,
                FontDebugStatsStore.KEY_EVENT_TOTAL);
        editor.putLong(FontDebugStatsStore.KEY_UPDATED_AT, incomingUpdatedAt);
        editor.apply();
    }

    private static File resolveFile() {
        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (downloads == null) {
            return null;
        }
        return new File(new File(downloads, DIR_NAME), FILE_NAME);
    }

    private static void putString(Properties properties, Bundle extras, String key) {
        if (extras.containsKey(key)) {
            String value = extras.getString(key);
            if (value != null) {
                properties.setProperty(key, value);
            }
        }
    }

    private static void copyString(Properties properties,
                                   SharedPreferences.Editor editor,
                                   String propertyKey,
                                   String preferenceKey) {
        String value = properties.getProperty(propertyKey);
        if (value != null) {
            editor.putString(preferenceKey, value);
        }
    }

    private static void copyInt(Properties properties,
                                SharedPreferences.Editor editor,
                                String propertyKey,
                                String preferenceKey) {
        String value = properties.getProperty(propertyKey);
        if (value == null) {
            return;
        }
        try {
            editor.putInt(preferenceKey, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
        }
    }

    private static long parseLong(String value, long fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
