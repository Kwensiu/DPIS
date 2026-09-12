package com.dpis.module.appconfig.presentation

import android.view.View
import com.dpis.module.applist.AppListItem

class AppConfigSheetInteractions(
    binder: AppConfigDialogBinder,
    host: AppConfigDialogBinder.Host,
) {
    private val modeValidationBinder = AppConfigSheetModeValidationBinder(binder, host)
    private val actionBinder = AppConfigSheetActionBinder(binder, host)

    fun bind(
        dialogView: View,
        item: AppListItem,
        views: AppConfigDialogBinder.AppConfigDialogViews,
        state: AppConfigDialogBinder.AppConfigDialogState,
        style: AppConfigDialogBinder.AppConfigDialogActionStyle,
        systemHooksEnabled: Boolean,
    ) {
        modeValidationBinder.bindDialogValidation(dialogView, item, views, state, style, systemHooksEnabled)
        actionBinder.bindDialogActions(dialogView, item, views, state, style, systemHooksEnabled)
        modeValidationBinder.bindModeToggles(dialogView, item, views, state, style, systemHooksEnabled)
        actionBinder.bindTypefaceSelectorAction(dialogView, item, views, state, style, systemHooksEnabled)
    }
}
