package com.dpis.module;

import android.content.Context;

final class AppPredictiveBackManager {
    static final boolean DEFAULT_ENABLED = true;

    private AppPredictiveBackManager() {
    }

    static boolean isEnabled(Context context) {
        return ConfigStoreFactory.createForModuleApp(context).isPredictiveBackEnabled();
    }
}
