package com.dpis.module.updates

import android.content.Context
import android.content.SharedPreferences

class UpdateStateStore {
    companion object {
        const val PREFS_NAME = "dpis.update_prompt"
        const val KEY_LAST_UPDATE_CHECK_TIMESTAMP = "last_update_check_timestamp"
        const val KEY_LAST_UPDATE_CHECK_FAILED = "last_update_check_failed"
        const val KEY_LAST_PROMPTED_UPDATE_VERSION_CODE = "last_prompted_update_version_code"
    }

    private val prefs: SharedPreferences

    constructor(context: Context) {
        requireNotNull(context) { "context == null" }
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    constructor(prefs: SharedPreferences) {
        this.prefs = requireNotNull(prefs) { "prefs == null" }
    }

    fun getLastUpdateCheckTimestamp(): Long = prefs.getLong(KEY_LAST_UPDATE_CHECK_TIMESTAMP, 0L)
    fun wasLastUpdateCheckFailed(): Boolean = prefs.getBoolean(KEY_LAST_UPDATE_CHECK_FAILED, false)
    fun getLastPromptedUpdateVersionCode(): Int = prefs.getInt(KEY_LAST_PROMPTED_UPDATE_VERSION_CODE, 0)

    fun setLastUpdateCheckTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_UPDATE_CHECK_TIMESTAMP, timestamp).apply()
    }

    fun setLastUpdateCheckFailed(failed: Boolean) {
        prefs.edit().putBoolean(KEY_LAST_UPDATE_CHECK_FAILED, failed).apply()
    }

    fun setLastPromptedUpdateVersionCode(versionCode: Int) {
        prefs.edit().putInt(KEY_LAST_PROMPTED_UPDATE_VERSION_CODE, versionCode).apply()
    }

    fun buildCoordinatorState(
        startupCheckInProgress: Boolean,
        downloadInProgress: Boolean,
        downloadCancelRequested: Boolean
    ): UpdateCoordinator.State = UpdateCoordinator.State(
        getLastUpdateCheckTimestamp(),
        wasLastUpdateCheckFailed(),
        getLastPromptedUpdateVersionCode(),
        startupCheckInProgress,
        downloadInProgress,
        downloadCancelRequested
    )

    fun applyStartupCheckState(state: UpdateCoordinator.State?) {
        if (state == null) return
        setLastUpdateCheckTimestamp(state.lastUpdateCheckTimestampMs)
        setLastUpdateCheckFailed(state.lastUpdateCheckFailed)
    }

    fun applyPromptedVersion(state: UpdateCoordinator.State?) {
        if (state == null) return
        setLastPromptedUpdateVersionCode(state.lastPromptedUpdateVersionCode)
    }
}
