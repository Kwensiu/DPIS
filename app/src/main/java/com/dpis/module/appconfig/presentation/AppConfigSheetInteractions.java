package com.dpis.module.appconfig.presentation;

import com.dpis.module.applist.AppListItem;

import android.view.View;
import com.dpis.module.appconfig.presentation.AppConfigDialogBinder;

public final class AppConfigSheetInteractions {
    private final AppConfigSheetModeValidationBinder modeValidationBinder;
    private final AppConfigSheetActionBinder actionBinder;

    public AppConfigSheetInteractions(AppConfigDialogBinder binder, AppConfigDialogBinder.Host host) {
        this.modeValidationBinder = new AppConfigSheetModeValidationBinder(binder, host);
        this.actionBinder = new AppConfigSheetActionBinder(binder, host);
    }

    public void bind(View dialogView,
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogViews views,
            AppConfigDialogBinder.AppConfigDialogState state,
            AppConfigDialogBinder.AppConfigDialogActionStyle style,
            boolean systemHooksEnabled) {
        modeValidationBinder.bindDialogValidation(dialogView, item, views, state, style, systemHooksEnabled);
        actionBinder.bindDialogActions(dialogView, item, views, state, style, systemHooksEnabled);
        modeValidationBinder.bindModeToggles(dialogView, item, views, state, style, systemHooksEnabled);
        actionBinder.bindTypefaceSelectorAction(dialogView, item, views, state, style, systemHooksEnabled);
    }
}
