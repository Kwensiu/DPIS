package com.dpis.module.settings

import android.net.Uri

/** Immutable presentation snapshot; persistence and workflow execution stay Java-owned. */
class SettingsUiState(
    @JvmField val storeAvailable: Boolean,
    @JvmField val systemHooksEnabled: Boolean,
    @JvmField val safeModeEnabled: Boolean,
    @JvmField val globalLogEnabled: Boolean,
    @JvmField val launcherIconHidden: Boolean,
    @JvmField val interfaceScalePercent: Int,
    @JvmField val cacheClearInProgress: Boolean,
    cacheUsage: String?,
    languageLabel: String?,
    @JvmField val pendingImportUri: Uri?,
) {
    @JvmField val cacheUsage: String = cacheUsage ?: ""
    @JvmField val languageLabel: String = languageLabel ?: ""
}
