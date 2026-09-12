package com.dpis.module.applist.presentation

import android.app.Activity
import com.dpis.module.MainActivity
import com.dpis.module.applist.AppListItem
import com.dpis.module.config.DpisConfigStore

/** Wires installed-app catalog load to MainActivity platform capabilities. */
class InstalledAppsLoadShell(
    private val activity: MainActivity,
) : InstalledAppsLoadSession.Shell {
    override fun activity(): Activity = activity

    override fun hookConfigStore(): DpisConfigStore? = activity.hookConfigStore

    override fun dispatchRequestAppsLoad(forceReload: Boolean) =
        activity.dispatchInstalledAppsLoad(forceReload)

    override fun dispatchAppsLoadFinished(requestId: Int, loaded: List<AppListItem>?) =
        activity.dispatchInstalledAppsLoadFinished(requestId, loaded)
}
