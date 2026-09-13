package com.dpis.module.appconfig

import com.dpis.module.applist.AppListItem

enum class AppConfigProcessAction {
    START,
    RESTART,
    STOP,
}

/** Activity-owned capabilities the Compose app editor needs from its host. */
interface AppConfigEditorHost {
    fun toggleScope(
        item: AppListItem?,
        currentlyInScope: Boolean,
        onTurnedInScope: Runnable?,
        onTurnedOutScope: Runnable?,
    )

    fun getFontHookDomainsButtonText(
        item: AppListItem?,
        state: AppConfigDialogState?,
    ): String?

    fun setDpisEnabled(packageName: String?, enabled: Boolean): Boolean
}
