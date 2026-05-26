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
        FontDebugStatsSchema.copyExtrasToPreferences(extras, editor);
        editor.apply();
    }
}
