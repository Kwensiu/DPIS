package com.dpis.module;

import android.content.SharedPreferences;
import android.os.Bundle;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

final class FontDebugStatsSchema {
    static final String NO_DATA_TEXT = "暂无数据";
    static final String NO_VIEWPORT_TEXT = "视口: 暂无";

    static final String EXTRA_CHAIN_5S = "chain_5s";
    static final String EXTRA_CHAIN_30S = "chain_30s";
    static final String EXTRA_CHAIN_ALL = "chain_all";
    static final String EXTRA_CHAIN_VIEW_5S = "chain_view_5s";
    static final String EXTRA_CHAIN_VIEW_30S = "chain_view_30s";
    static final String EXTRA_CHAIN_VIEW_ALL = "chain_view_all";
    static final String EXTRA_UNIT_BREAKDOWN_5S = "unit_breakdown_5s";
    static final String EXTRA_VIEWPORT_DEBUG_SUMMARY = "viewport_debug_summary";
    static final String EXTRA_EVENT_TOTAL = "event_total";
    static final String EXTRA_UPDATED_AT = "updated_at";

    static final String KEY_CHAIN_5S = "font.debug.chain.5s";
    static final String KEY_CHAIN_30S = "font.debug.chain.30s";
    static final String KEY_CHAIN_ALL = "font.debug.chain.all";
    static final String KEY_CHAIN_VIEW_5S = "font.debug.chain_view.5s";
    static final String KEY_CHAIN_VIEW_30S = "font.debug.chain_view.30s";
    static final String KEY_CHAIN_VIEW_ALL = "font.debug.chain_view.all";
    static final String KEY_EVENT_TOTAL = "font.debug.event_total";
    static final String KEY_UPDATED_AT = "font.debug.updated_at";
    static final String KEY_UNIT_BREAKDOWN_5S = "font.debug.unit_breakdown.5s";
    static final String KEY_VIEWPORT_DEBUG_SUMMARY = "viewport.debug.summary";

    static final int MODE_CHAIN = 0;
    static final int MODE_CHAIN_VIEW = 1;
    static final int WINDOW_5S = 0;
    static final int WINDOW_30S = 1;
    static final int WINDOW_ALL = 2;

    private static final StringField[] STRING_FIELDS = {
            new StringField(EXTRA_CHAIN_5S, KEY_CHAIN_5S),
            new StringField(EXTRA_CHAIN_30S, KEY_CHAIN_30S),
            new StringField(EXTRA_CHAIN_ALL, KEY_CHAIN_ALL),
            new StringField(EXTRA_CHAIN_VIEW_5S, KEY_CHAIN_VIEW_5S),
            new StringField(EXTRA_CHAIN_VIEW_30S, KEY_CHAIN_VIEW_30S),
            new StringField(EXTRA_CHAIN_VIEW_ALL, KEY_CHAIN_VIEW_ALL),
            new StringField(EXTRA_UNIT_BREAKDOWN_5S, KEY_UNIT_BREAKDOWN_5S),
            new StringField(EXTRA_VIEWPORT_DEBUG_SUMMARY, KEY_VIEWPORT_DEBUG_SUMMARY)
    };

    private static final String[] FONT_EVENT_STAT_KEYS = {
            KEY_CHAIN_5S,
            KEY_CHAIN_30S,
            KEY_CHAIN_ALL,
            KEY_CHAIN_VIEW_5S,
            KEY_CHAIN_VIEW_30S,
            KEY_CHAIN_VIEW_ALL
    };

    private FontDebugStatsSchema() {
    }

    static String statsKeyFor(int mode, int window) {
        if (mode == MODE_CHAIN_VIEW) {
            if (window == WINDOW_5S) {
                return KEY_CHAIN_VIEW_5S;
            }
            if (window == WINDOW_30S) {
                return KEY_CHAIN_VIEW_30S;
            }
            return KEY_CHAIN_VIEW_ALL;
        }
        if (window == WINDOW_5S) {
            return KEY_CHAIN_5S;
        }
        if (window == WINDOW_30S) {
            return KEY_CHAIN_30S;
        }
        return KEY_CHAIN_ALL;
    }

    static void copyExtrasToPreferences(Bundle extras, SharedPreferences.Editor editor) {
        if (extras == null || editor == null) {
            return;
        }
        for (StringField field : STRING_FIELDS) {
            if (extras.containsKey(field.extraKey)) {
                editor.putString(field.preferenceKey, extras.getString(field.extraKey));
            }
        }
        if (extras.containsKey(EXTRA_EVENT_TOTAL)) {
            editor.putInt(KEY_EVENT_TOTAL, extras.getInt(EXTRA_EVENT_TOTAL, 0));
        }
        if (extras.containsKey(EXTRA_UPDATED_AT)) {
            editor.putLong(KEY_UPDATED_AT, extras.getLong(EXTRA_UPDATED_AT, 0L));
        }
    }

    static void copyExtrasToProperties(Bundle extras, Properties properties) {
        if (extras == null || properties == null) {
            return;
        }
        for (StringField field : STRING_FIELDS) {
            if (!extras.containsKey(field.extraKey)) {
                continue;
            }
            String value = extras.getString(field.extraKey);
            if (value != null) {
                properties.setProperty(field.extraKey, value);
            }
        }
        if (extras.containsKey(EXTRA_EVENT_TOTAL)) {
            properties.setProperty(EXTRA_EVENT_TOTAL,
                    String.valueOf(extras.getInt(EXTRA_EVENT_TOTAL, 0)));
        }
        if (extras.containsKey(EXTRA_UPDATED_AT)) {
            properties.setProperty(EXTRA_UPDATED_AT,
                    String.valueOf(extras.getLong(EXTRA_UPDATED_AT, 0L)));
        }
    }

    static long propertyUpdatedAt(Properties properties) {
        return parseLong(properties != null ? properties.getProperty(EXTRA_UPDATED_AT) : null, 0L);
    }

    static void copyPropertiesToPreferences(Properties properties, SharedPreferences.Editor editor) {
        if (properties == null || editor == null) {
            return;
        }
        for (StringField field : STRING_FIELDS) {
            String value = properties.getProperty(field.extraKey);
            if (value != null) {
                editor.putString(field.preferenceKey, value);
            }
        }
        copyInt(properties, editor, EXTRA_EVENT_TOTAL, KEY_EVENT_TOTAL);
        long updatedAt = propertyUpdatedAt(properties);
        if (updatedAt > 0L) {
            editor.putLong(KEY_UPDATED_AT, updatedAt);
        }
    }

    static void removeStats(SharedPreferences.Editor editor) {
        if (editor == null) {
            return;
        }
        for (StringField field : STRING_FIELDS) {
            editor.remove(field.preferenceKey);
        }
        editor.remove(KEY_EVENT_TOTAL)
                .remove(KEY_UPDATED_AT);
    }

    static long estimateStatsBytes(SharedPreferences preferences) {
        if (preferences == null) {
            return 0L;
        }
        long total = 0L;
        for (StringField field : STRING_FIELDS) {
            total += estimateString(preferences, field.preferenceKey);
        }
        total += estimateNumber(preferences, KEY_EVENT_TOTAL);
        total += estimateNumber(preferences, KEY_UPDATED_AT);
        return total;
    }

    static boolean hasAnyFontEventSignal(SharedPreferences preferences) {
        if (preferences == null) {
            return false;
        }
        if (preferences.getInt(KEY_EVENT_TOTAL, 0) > 0) {
            return true;
        }
        for (String key : FONT_EVENT_STAT_KEYS) {
            if (hasNonEmptyStatsText(preferences, key)) {
                return true;
            }
        }
        return false;
    }

    static boolean hasViewportSignal(SharedPreferences preferences) {
        if (preferences == null || !preferences.contains(KEY_VIEWPORT_DEBUG_SUMMARY)) {
            return false;
        }
        return isViewportSignal(preferences.getString(KEY_VIEWPORT_DEBUG_SUMMARY, ""));
    }

    static boolean hasNonEmptyStatsText(SharedPreferences preferences, String key) {
        if (preferences == null || key == null || !preferences.contains(key)) {
            return false;
        }
        return isNonEmptyStatsText(preferences.getString(key, ""));
    }

    static boolean isNonEmptyStatsText(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim();
        return !normalized.isEmpty() && !NO_DATA_TEXT.equals(normalized);
    }

    static boolean isViewportSignal(String summary) {
        if (summary == null) {
            return false;
        }
        String normalized = summary.trim();
        return !normalized.isEmpty() && !NO_VIEWPORT_TEXT.equals(normalized);
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

    private static long estimateString(SharedPreferences preferences, String key) {
        if (!preferences.contains(key)) {
            return 0L;
        }
        String value = preferences.getString(key, "");
        return utf8Bytes(key) + utf8Bytes(value);
    }

    private static long estimateNumber(SharedPreferences preferences, String key) {
        if (!preferences.contains(key)) {
            return 0L;
        }
        return utf8Bytes(key) + Long.BYTES;
    }

    private static int utf8Bytes(String value) {
        return value == null ? 0 : value.getBytes(StandardCharsets.UTF_8).length;
    }

    private static final class StringField {
        final String extraKey;
        final String preferenceKey;

        StringField(String extraKey, String preferenceKey) {
            this.extraKey = extraKey;
            this.preferenceKey = preferenceKey;
        }
    }
}
