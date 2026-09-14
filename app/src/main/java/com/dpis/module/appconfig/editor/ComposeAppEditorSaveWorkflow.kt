package com.dpis.module.appconfig.editor

import com.dpis.module.appconfig.AppConfigInputValidation
import com.dpis.module.appconfig.AppConfigSaveHandler
import com.dpis.module.applist.AppListItem
import com.dpis.module.viewport.ViewportTargetSpec

/**
 * Parses a Compose editor draft, persists it through [AppConfigSaveHandler], and delegates
 * surface-specific post-save effects.
 *
 * Persist is shared. Main workspace and Quick Config supply different [PostSaveEffects]:
 * the main path finalizes runtime sync and HyperOS proxy work; Quick Config publishes
 * local runtime properties and must not run HyperOS mount.
 */
class ComposeAppEditorSaveWorkflow(
    private val persister: Persister,
    private val effects: PostSaveEffects,
) {
    fun interface Persister {
        fun persist(
            item: AppListItem,
            draft: EditorDraft,
            viewport: ViewportTargetSpec,
            fontPercent: Int?,
        ): AppConfigSaveHandler.Result
    }

    interface PostSaveEffects {
        fun afterPersist(
            result: AppConfigSaveHandler.Result,
            item: AppListItem,
            draft: EditorDraft,
        ): AppConfigSaveHandler.Result

        fun showMessage(messageResId: Int)

        fun afterSuccessfulSave(item: AppListItem, draft: EditorDraft)
    }

    fun save(item: AppListItem?, draft: EditorDraft?): Boolean {
        if (item == null || draft == null) return false
        val viewport = AppConfigInputValidation.parseViewportTargetSpec(
            draft.viewportInputFor(draft.viewportMode),
            draft.viewportMode,
        )
        val fontPercent = AppConfigInputValidation.parseFontScalePercentOrNull(draft.fontInput)
        var result = persister.persist(item, draft, viewport, fontPercent)
        if (result.success) {
            result = effects.afterPersist(result, item, draft)
        }
        if (result.messageResId != 0) effects.showMessage(result.messageResId)
        if (!result.success) return false
        effects.afterSuccessfulSave(item, draft)
        return true
    }
}
