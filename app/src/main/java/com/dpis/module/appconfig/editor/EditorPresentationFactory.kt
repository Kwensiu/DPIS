package com.dpis.module.appconfig

import com.dpis.module.ConfigEditorDestination
import com.dpis.module.applist.AppListItem

/** Pure projection boundary for the Compose app editor. */
object EditorPresentationFactory {
    @JvmStatic
    fun create(
        item: AppListItem,
        versionName: String?,
        draft: EditorDraft,
        typefaceSelectorText: String?,
        hookChainText: String?,
        savedDraft: EditorDraft?,
        saveFeedbackVisible: Boolean,
        systemHooksEnabled: Boolean,
        automaticFontHookDomains: Set<String>,
        destination: ConfigEditorDestination?,
        actions: EditorPresentation.Actions,
    ): EditorPresentation.State = EditorPresentation.State(
        item,
        versionName,
        draft,
        typefaceSelectorText,
        hookChainText,
        !draft.hasSameSavedConfig(savedDraft),
        saveFeedbackVisible,
        systemHooksEnabled,
        automaticFontHookDomains,
        destination,
        actions,
    )
}
