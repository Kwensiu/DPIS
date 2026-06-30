package com.dpis.module;

import android.content.Context;
import android.content.SharedPreferences;

final class StartupDisclaimerStore {
    private static final String PREFS_NAME = "dpis.startup_disclaimer";
    private static final String KEY_ACCEPTED = "accepted";

    private final SharedPreferences preferences;
    private final SharedPreferences legacyPreferences;

    StartupDisclaimerStore(Context context) {
        this(
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
                context.getSharedPreferences(DpisConfigStore.GROUP, Context.MODE_PRIVATE));
    }

    StartupDisclaimerStore(
            SharedPreferences preferences,
            SharedPreferences legacyPreferences
    ) {
        this.preferences = preferences;
        this.legacyPreferences = legacyPreferences;
    }

    boolean isAccepted() {
        if (preferences.contains(KEY_ACCEPTED)) {
            return preferences.getBoolean(KEY_ACCEPTED, false);
        }
        return legacyPreferences != null
                && legacyPreferences.getBoolean(
                        DpisConfigStore.KEY_STARTUP_DISCLAIMER_ACCEPTED,
                        false);
    }

    boolean setAccepted(boolean accepted) {
        return preferences.edit()
                .putBoolean(KEY_ACCEPTED, accepted)
                .commit();
    }
}
