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
        return FontDebugStatsSchema.hasAnyFontEventSignal(preferences);
    }

    private static boolean hasViewportSignal(SharedPreferences preferences) {
        return FontDebugStatsSchema.hasViewportSignal(preferences);
    }
}
