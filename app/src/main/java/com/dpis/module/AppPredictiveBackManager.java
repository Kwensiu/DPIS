package com.dpis.module;

import android.content.Context;

final class AppPredictiveBackManager {
    static final boolean DEFAULT_ENABLED = true;

    private AppPredictiveBackManager() {
    }

    static boolean isEnabled(Context context) {
        return context
                .getSharedPreferences(DpiConfigStore.GROUP, Context.MODE_PRIVATE)
                .getBoolean(DpiConfigStore.KEY_PREDICTIVE_BACK_ENABLED, DEFAULT_ENABLED);
    }
}
