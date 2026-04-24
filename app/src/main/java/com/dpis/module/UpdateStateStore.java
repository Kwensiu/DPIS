package com.dpis.module;

import android.content.Context;
import android.content.SharedPreferences;

final class UpdateStateStore {
    static final String PREFS_NAME = "dpis.update_prompt";
    static final String KEY_LAST_UPDATE_CHECK_TIMESTAMP = "last_update_check_timestamp";
    static final String KEY_LAST_UPDATE_CHECK_FAILED = "last_update_check_failed";
    static final String KEY_LAST_PROMPTED_UPDATE_VERSION_CODE = "last_prompted_update_version_code";

    private final SharedPreferences prefs;

    UpdateStateStore(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    UpdateStateStore(SharedPreferences prefs) {
        if (prefs == null) {
            throw new IllegalArgumentException("prefs == null");
        }
        this.prefs = prefs;
    }

    long getLastUpdateCheckTimestamp() {
        return prefs.getLong(KEY_LAST_UPDATE_CHECK_TIMESTAMP, 0L);
    }

    boolean wasLastUpdateCheckFailed() {
        return prefs.getBoolean(KEY_LAST_UPDATE_CHECK_FAILED, false);
    }

    int getLastPromptedUpdateVersionCode() {
        return prefs.getInt(KEY_LAST_PROMPTED_UPDATE_VERSION_CODE, 0);
    }

    void setLastUpdateCheckTimestamp(long timestamp) {
        prefs.edit().putLong(KEY_LAST_UPDATE_CHECK_TIMESTAMP, timestamp).apply();
    }

    void setLastUpdateCheckFailed(boolean failed) {
        prefs.edit().putBoolean(KEY_LAST_UPDATE_CHECK_FAILED, failed).apply();
    }

    void setLastPromptedUpdateVersionCode(int versionCode) {
        prefs.edit().putInt(KEY_LAST_PROMPTED_UPDATE_VERSION_CODE, versionCode).apply();
    }

    UpdateCoordinator.State buildCoordinatorState(boolean startupCheckInProgress,
            boolean downloadInProgress,
            boolean downloadCancelRequested) {
        return new UpdateCoordinator.State(
                getLastUpdateCheckTimestamp(),
                wasLastUpdateCheckFailed(),
                getLastPromptedUpdateVersionCode(),
                startupCheckInProgress,
                downloadInProgress,
                downloadCancelRequested);
    }

    void applyStartupCheckState(UpdateCoordinator.State state) {
        if (state == null) {
            return;
        }
        setLastUpdateCheckTimestamp(state.lastUpdateCheckTimestampMs);
        setLastUpdateCheckFailed(state.lastUpdateCheckFailed);
    }

    void applyPromptedVersion(UpdateCoordinator.State state) {
        if (state == null) {
            return;
        }
        setLastPromptedUpdateVersionCode(state.lastPromptedUpdateVersionCode);
    }
}
