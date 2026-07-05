package com.dpis.module.fonts;

import com.dpis.module.*;

import android.content.Context;
import android.content.SharedPreferences;

public final class FontDebugStatsStore {
    public static final String ACTION_STATS_UPDATE = "io.github.kwensiu.dpis.ACTION_FONT_DEBUG_STATS_UPDATE";

    public static final String EXTRA_CHAIN_5S = FontDebugStatsSchema.EXTRA_CHAIN_5S;
    public static final String EXTRA_CHAIN_30S = FontDebugStatsSchema.EXTRA_CHAIN_30S;
    public static final String EXTRA_CHAIN_ALL = FontDebugStatsSchema.EXTRA_CHAIN_ALL;
    public static final String EXTRA_CHAIN_VIEW_5S = FontDebugStatsSchema.EXTRA_CHAIN_VIEW_5S;
    public static final String EXTRA_CHAIN_VIEW_30S = FontDebugStatsSchema.EXTRA_CHAIN_VIEW_30S;
    public static final String EXTRA_CHAIN_VIEW_ALL = FontDebugStatsSchema.EXTRA_CHAIN_VIEW_ALL;
    public static final String EXTRA_UNIT_BREAKDOWN_5S = FontDebugStatsSchema.EXTRA_UNIT_BREAKDOWN_5S;
    public static final String EXTRA_VIEWPORT_DEBUG_SUMMARY = FontDebugStatsSchema.EXTRA_VIEWPORT_DEBUG_SUMMARY;
    public static final String EXTRA_EVENT_TOTAL = FontDebugStatsSchema.EXTRA_EVENT_TOTAL;
    public static final String EXTRA_UPDATED_AT = FontDebugStatsSchema.EXTRA_UPDATED_AT;

    public static final String KEY_CHAIN_5S = FontDebugStatsSchema.KEY_CHAIN_5S;
    public static final String KEY_CHAIN_30S = FontDebugStatsSchema.KEY_CHAIN_30S;
    public static final String KEY_CHAIN_ALL = FontDebugStatsSchema.KEY_CHAIN_ALL;
    public static final String KEY_CHAIN_VIEW_5S = FontDebugStatsSchema.KEY_CHAIN_VIEW_5S;
    public static final String KEY_CHAIN_VIEW_30S = FontDebugStatsSchema.KEY_CHAIN_VIEW_30S;
    public static final String KEY_CHAIN_VIEW_ALL = FontDebugStatsSchema.KEY_CHAIN_VIEW_ALL;
    public static final String KEY_EVENT_TOTAL = FontDebugStatsSchema.KEY_EVENT_TOTAL;
    public static final String KEY_UPDATED_AT = FontDebugStatsSchema.KEY_UPDATED_AT;
    public static final String KEY_UNIT_BREAKDOWN_5S = FontDebugStatsSchema.KEY_UNIT_BREAKDOWN_5S;
    public static final String KEY_VIEWPORT_DEBUG_SUMMARY = FontDebugStatsSchema.KEY_VIEWPORT_DEBUG_SUMMARY;

    public static final int MODE_CHAIN = FontDebugStatsSchema.MODE_CHAIN;
    public static final int MODE_CHAIN_VIEW = FontDebugStatsSchema.MODE_CHAIN_VIEW;
    public static final int WINDOW_5S = FontDebugStatsSchema.WINDOW_5S;
    public static final int WINDOW_30S = FontDebugStatsSchema.WINDOW_30S;
    public static final int WINDOW_ALL = FontDebugStatsSchema.WINDOW_ALL;
    public static final String KEY_FONT_DEBUG_OVERLAY_TOP_LIMIT = "font.debug.overlay_top_limit";

    private FontDebugStatsStore() {
    }

    public static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(DpisConfigStore.GROUP, Context.MODE_PRIVATE);
    }

    public static void clearStats(Context context) {
        if (context == null) {
            return;
        }
        clearStats(getPreferences(context));
    }

    public static void clearStats(SharedPreferences preferences) {
        if (preferences == null) {
            return;
        }
        SharedPreferences.Editor editor = preferences.edit();
        FontDebugStatsSchema.removeStats(editor);
        editor.apply();
    }

    public static long estimateStatsBytes(Context context) {
        if (context == null) {
            return 0L;
        }
        SharedPreferences preferences = getPreferences(context);
        return FontDebugStatsSchema.estimateStatsBytes(preferences);
    }
}
