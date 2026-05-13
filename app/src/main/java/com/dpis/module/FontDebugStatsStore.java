package com.dpis.module;

import android.content.Context;
import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;

final class FontDebugStatsStore {
    static final String ACTION_STATS_UPDATE = "io.github.kwensiu.dpis.ACTION_FONT_DEBUG_STATS_UPDATE";

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
    static final String KEY_FONT_DEBUG_OVERLAY_TOP_LIMIT = "font.debug.overlay_top_limit";

    private FontDebugStatsStore() {
    }

    static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(DpiConfigStore.GROUP, Context.MODE_PRIVATE);
    }

    static void clearStats(Context context) {
        if (context == null) {
            return;
        }
        clearStats(getPreferences(context));
    }

    static void clearStats(SharedPreferences preferences) {
        if (preferences == null) {
            return;
        }
        preferences.edit()
                .remove(KEY_CHAIN_5S)
                .remove(KEY_CHAIN_30S)
                .remove(KEY_CHAIN_ALL)
                .remove(KEY_CHAIN_VIEW_5S)
                .remove(KEY_CHAIN_VIEW_30S)
                .remove(KEY_CHAIN_VIEW_ALL)
                .remove(KEY_EVENT_TOTAL)
                .remove(KEY_UPDATED_AT)
                .remove(KEY_UNIT_BREAKDOWN_5S)
                .remove(KEY_VIEWPORT_DEBUG_SUMMARY)
                .apply();
    }

    static long estimateStatsBytes(Context context) {
        if (context == null) {
            return 0L;
        }
        SharedPreferences preferences = getPreferences(context);
        return estimateString(preferences, KEY_CHAIN_5S)
                + estimateString(preferences, KEY_CHAIN_30S)
                + estimateString(preferences, KEY_CHAIN_ALL)
                + estimateString(preferences, KEY_CHAIN_VIEW_5S)
                + estimateString(preferences, KEY_CHAIN_VIEW_30S)
                + estimateString(preferences, KEY_CHAIN_VIEW_ALL)
                + estimateString(preferences, KEY_UNIT_BREAKDOWN_5S)
                + estimateString(preferences, KEY_VIEWPORT_DEBUG_SUMMARY)
                + estimateNumber(preferences, KEY_EVENT_TOTAL)
                + estimateNumber(preferences, KEY_UPDATED_AT);
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
}
