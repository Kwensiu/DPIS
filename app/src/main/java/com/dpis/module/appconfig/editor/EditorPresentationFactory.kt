package com.dpis.module.appconfig

import com.dpis.module.ui.ConfigEditorDestination
import com.dpis.module.applist.AppListItem

/** Pure projection boundary for the Compose app editor. */
object EditorPresentationFactory {
    @JvmStatic
    @JvmOverloads
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
        editorSession: AppConfigEditorSession? = null,
    ): EditorPresentation.State {
        val chip = editorSession?.chip ?: if (!draft.hasSamePersistedConfig(savedDraft)) {
            AppConfigEditorChip.UNSAVED
        } else {
            AppConfigEditorChip.NONE
        }
        return EditorPresentation.State(
            item,
            versionName,
            draft,
            typefaceSelectorText,
            hookChainText,
            chip == AppConfigEditorChip.UNSAVED,
            chip,
            saveFeedbackVisible,
            systemHooksEnabled,
            automaticFontHookDomains,
            destination,
            actions,
        )
    }
}
