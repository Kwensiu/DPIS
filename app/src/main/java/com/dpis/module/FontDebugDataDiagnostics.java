package com.dpis.module;

import android.content.SharedPreferences;

import java.util.Set;

final class FontDebugDataDiagnostics {
    enum NoDataReason {
        NONE,
        SCOPE_MISSING,
        NOT_INJECTED,
        NO_EVENTS
    }

    private FontDebugDataDiagnostics() {
    }

    static NoDataReason resolveNoDataReason(DpiConfigStore store, SharedPreferences preferences) {
        if (preferences == null) {
            return NoDataReason.NOT_INJECTED;
        }
        if (!hasConfiguredTargets(store)) {
            return NoDataReason.SCOPE_MISSING;
        }
        if (hasAnyFontEventSignal(preferences)) {
            return NoDataReason.NONE;
        }
        if (hasViewportSignal(preferences)) {
            return NoDataReason.NO_EVENTS;
        }
        return NoDataReason.NOT_INJECTED;
    }

    private static boolean hasConfiguredTargets(DpiConfigStore store) {
        if (store == null) {
            return false;
        }
        Set<String> targets = store.getConfiguredPackages();
        return targets != null && !targets.isEmpty();
    }

    private static boolean hasAnyFontEventSignal(SharedPreferences preferences) {
        if (preferences.getInt(FontDebugStatsStore.KEY_EVENT_TOTAL, 0) > 0) {
            return true;
        }
        return hasNonEmptyValue(preferences, FontDebugStatsStore.KEY_CHAIN_5S)
                || hasNonEmptyValue(preferences, FontDebugStatsStore.KEY_CHAIN_30S)
                || hasNonEmptyValue(preferences, FontDebugStatsStore.KEY_CHAIN_ALL)
                || hasNonEmptyValue(preferences, FontDebugStatsStore.KEY_CHAIN_VIEW_5S)
                || hasNonEmptyValue(preferences, FontDebugStatsStore.KEY_CHAIN_VIEW_30S)
                || hasNonEmptyValue(preferences, FontDebugStatsStore.KEY_CHAIN_VIEW_ALL);
    }

    private static boolean hasViewportSignal(SharedPreferences preferences) {
        if (!preferences.contains(FontDebugStatsStore.KEY_VIEWPORT_DEBUG_SUMMARY)) {
            return false;
        }
        String summary = preferences.getString(FontDebugStatsStore.KEY_VIEWPORT_DEBUG_SUMMARY, "");
        if (summary == null) {
            return false;
        }
        String normalized = summary.trim();
        return !normalized.isEmpty() && !"视口: 暂无".equals(normalized);
    }

    private static boolean hasNonEmptyValue(SharedPreferences preferences, String key) {
        if (!preferences.contains(key)) {
            return false;
        }
        String value = preferences.getString(key, "");
        return value != null && !value.trim().isEmpty() && !"暂无数据".equals(value.trim());
    }
}
