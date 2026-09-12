package com.dpis.module.ui.presentation

import android.content.Intent
import android.view.View
import android.widget.FrameLayout
import com.dpis.module.MainActivity
import com.dpis.module.appconfig.AppConfigSaveHandler
import com.dpis.module.appconfig.presentation.AppConfigDialogActivityHost
import com.dpis.module.applist.AppListFilterState
import com.dpis.module.applist.AppListItem
import com.dpis.module.applist.AppListPage
import com.dpis.module.diagnostics.LogActivity
import com.dpis.module.quirks.presentation.WechatDpiHelp
import com.dpis.module.ui.MainUiAction

/** Wires onCreate host construction to MainActivity platform capabilities. */
class MainHostWiringShell(
    private val activity: MainActivity,
) : MainHostWiringSession.Shell {
    override fun activity(): MainActivity = activity

    override fun requestEditorScope(item: AppListItem, onApproved: Runnable): Boolean =
        activity.requestEditorScope(item, onApproved)

    override fun refreshApps() = activity.refreshComposeApps()

    override fun refreshSettings() = activity.refreshComposeSettings()

    override fun refreshTools() = activity.refreshComposeTools()

    override fun showToast(messageResId: Int) = activity.showToast(messageResId)

    override fun appConfigDialogHost(): AppConfigDialogActivityHost =
        activity.appConfigDialogHost()

    override fun appConfigSaveHandler(): AppConfigSaveHandler = activity.appConfigSaveHandler()

    override fun wechatDpiHelp(): WechatDpiHelp = activity.wechatDpiHelp()

    override fun dispatch(action: MainUiAction) = activity.dispatchMainUiAction(action)

    override fun setCurrentAppListPage(page: AppListPage, submit: Boolean) =
        activity.setCurrentAppListPage(page, submit)

    override fun saveFilterState(filterState: AppListFilterState) =
        activity.applyAppListFilter(filterState)

    override fun onPageRefreshRequested(page: AppListPage) =
        activity.onPageRefreshRequested(page)

    override fun updateScrollPosition(
        page: AppListPage,
        index: Int,
        scrollOffset: Int,
    ) = activity.updateAppListScrollPosition(page, index, scrollOffset)

    override fun attachTemplateLegacyViews(
        workspaceContainer: View?,
        detailEmpty: View?,
        detailContent: FrameLayout?,
    ) = activity.attachTemplateLegacyViews(workspaceContainer, detailEmpty, detailContent)

    override fun openLogs() {
        activity.startActivity(Intent(activity, LogActivity::class.java))
    }
}
