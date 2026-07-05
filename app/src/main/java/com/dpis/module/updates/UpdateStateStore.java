package com.dpis.module.updates;

import android.content.Context;
import android.content.SharedPreferences;

public final class UpdateStateStore {
    public static final String PREFS_NAME = "dpis.update_prompt";
    public static final String KEY_LAST_UPDATE_CHECK_TIMESTAMP = "last_update_check_timestamp";
    public static final String KEY_LAST_UPDATE_CHECK_FAILED = "last_update_check_failed";
    public static final String KEY_LAST_PROMPTED_UPDATE_VERSION_CODE = "last_prompted_update_version_code";

    private final SharedPreferences prefs;

    public UpdateStateStore(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public UpdateStateStore(SharedPreferences prefs) {
        if (prefs == null) {
            throw new IllegalArgumentException("prefs == null");
        }
        this.prefs = prefs;
    }

    public long getLastUpdateCheckTimestamp() {
        return prefs.getLong(KEY_LAST_UPDATE_CHECK_TIMESTAMP, 0L);
    }

    public boolean wasLastUpdateCheckFailed() {
        return prefs.getBoolean(KEY_LAST_UPDATE_CHECK_FAILED, false);
    }

    public int getLastPromptedUpdateVersionCode() {
        return prefs.getInt(KEY_LAST_PROMPTED_UPDATE_VERSION_CODE, 0);
    }

    public void setLastUpdateCheckTimestamp(long timestamp) {
        prefs.edit().putLong(KEY_LAST_UPDATE_CHECK_TIMESTAMP, timestamp).apply();
    }

    public void setLastUpdateCheckFailed(boolean failed) {
        prefs.edit().putBoolean(KEY_LAST_UPDATE_CHECK_FAILED, failed).apply();
    }

    public void setLastPromptedUpdateVersionCode(int versionCode) {
        prefs.edit().putInt(KEY_LAST_PROMPTED_UPDATE_VERSION_CODE, versionCode).apply();
    }

    public UpdateCoordinator.State buildCoordinatorState(boolean startupCheckInProgress,
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

    public void applyStartupCheckState(UpdateCoordinator.State state) {
        if (state == null) {
            return;
        }
        setLastUpdateCheckTimestamp(state.lastUpdateCheckTimestampMs);
        setLastUpdateCheckFailed(state.lastUpdateCheckFailed);
    }

    public void applyPromptedVersion(UpdateCoordinator.State state) {
        if (state == null) {
            return;
        }
        setLastPromptedUpdateVersionCode(state.lastPromptedUpdateVersionCode);
    }
}
