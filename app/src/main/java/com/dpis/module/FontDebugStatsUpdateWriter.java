package com.dpis.module;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

final class FontDebugStatsUpdateWriter {
    private FontDebugStatsUpdateWriter() {
    }

    static void applyExtras(Context context, Bundle extras) {
        if (context == null || extras == null || extras.isEmpty()) {
            return;
        }
        SharedPreferences preferences = FontDebugStatsStore.getPreferences(context);
        applyExtras(preferences, extras);
    }

    static void applyExtras(SharedPreferences preferences, Bundle extras) {
        if (preferences == null || extras == null || extras.isEmpty()) {
            return;
        }
        SharedPreferences.Editor editor = preferences.edit();
        putStringIfPresent(editor, extras, FontDebugStatsStore.EXTRA_CHAIN_5S,
                FontDebugStatsStore.KEY_CHAIN_5S);
        putStringIfPresent(editor, extras, FontDebugStatsStore.EXTRA_CHAIN_30S,
                FontDebugStatsStore.KEY_CHAIN_30S);
        putStringIfPresent(editor, extras, FontDebugStatsStore.EXTRA_CHAIN_ALL,
                FontDebugStatsStore.KEY_CHAIN_ALL);
        putStringIfPresent(editor, extras, FontDebugStatsStore.EXTRA_CHAIN_VIEW_5S,
                FontDebugStatsStore.KEY_CHAIN_VIEW_5S);
        putStringIfPresent(editor, extras, FontDebugStatsStore.EXTRA_CHAIN_VIEW_30S,
                FontDebugStatsStore.KEY_CHAIN_VIEW_30S);
        putStringIfPresent(editor, extras, FontDebugStatsStore.EXTRA_CHAIN_VIEW_ALL,
                FontDebugStatsStore.KEY_CHAIN_VIEW_ALL);
        putStringIfPresent(editor, extras, FontDebugStatsStore.EXTRA_UNIT_BREAKDOWN_5S,
                FontDebugStatsStore.KEY_UNIT_BREAKDOWN_5S);
        putStringIfPresent(editor, extras, FontDebugStatsStore.EXTRA_VIEWPORT_DEBUG_SUMMARY,
                FontDebugStatsStore.KEY_VIEWPORT_DEBUG_SUMMARY);
        if (extras.containsKey(FontDebugStatsStore.EXTRA_EVENT_TOTAL)) {
            editor.putInt(FontDebugStatsStore.KEY_EVENT_TOTAL,
                    extras.getInt(FontDebugStatsStore.EXTRA_EVENT_TOTAL, 0));
        }
        if (extras.containsKey(FontDebugStatsStore.EXTRA_UPDATED_AT)) {
            editor.putLong(FontDebugStatsStore.KEY_UPDATED_AT,
                    extras.getLong(FontDebugStatsStore.EXTRA_UPDATED_AT, 0L));
        }
        editor.apply();
    }

    private static void putStringIfPresent(SharedPreferences.Editor editor,
                                           Bundle extras,
                                           String extraKey,
                                           String preferenceKey) {
        if (extras.containsKey(extraKey)) {
            editor.putString(preferenceKey, extras.getString(extraKey));
        }
    }
}
