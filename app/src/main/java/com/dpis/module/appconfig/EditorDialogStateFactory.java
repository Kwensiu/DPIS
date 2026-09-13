package com.dpis.module.appconfig;

import com.dpis.module.appconfig.EditorDraft;
import com.dpis.module.applist.AppListItem;
import com.dpis.module.appconfig.presentation.AppConfigDialogBinder;

/**
 * Projects an immutable Compose editor draft into [AppConfigDialogBinder.AppConfigDialogState].
 */
public final class EditorDialogStateFactory {
    private EditorDialogStateFactory() {
    }

    public static AppConfigDialogBinder.AppConfigDialogState create(
            AppListItem item,
            EditorDraft draft
    ) {
        AppConfigDialogBinder.AppConfigDialogState state
                = AppConfigDialogBinder.AppConfigDialogState.fromItem(item);
        state.selectedTypefaceId = draft.selectedTypefaceId;
        state.draftFontHookDomainsRaw = draft.draftFontHookDomainsRaw;
        state.viewportApplyMode = draft.viewportApplyMode;
        state.fontHookDomainsResetRequested = draft.fontHookDomainsResetRequested;
        state.viewportApplyModeResetRequested = draft.viewportApplyModeResetRequested;
        state.viewportScaleInput = draft.viewportScaleInput;
        state.viewportAbsoluteInput = draft.viewportAbsoluteInput;
        state.scopeSelected = draft.scopeSelected;
        state.dpisEnabled = draft.dpisEnabled;
        return state;
    }
}
