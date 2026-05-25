package com.dpis.module;

import android.content.Context;
import android.content.SharedPreferences;

final class AppConfigSheetWizardStore {
    private static final String PREFS_NAME = "dpis.app_config_sheet_wizard";
    private static final String KEY_ADVANCED_HINT_DISMISSED = "advanced_hint_dismissed";

    private AppConfigSheetWizardStore() {
    }

    static boolean shouldShowAdvancedHint(Context context) {
        return !getPreferences(context).getBoolean(KEY_ADVANCED_HINT_DISMISSED, false);
    }

    static void markAdvancedHintDismissed(Context context) {
        getPreferences(context).edit()
                .putBoolean(KEY_ADVANCED_HINT_DISMISSED, true)
                .apply();
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
