package com.dpis.module.appconfig.editor

import com.dpis.module.appconfig.AppConfigSaveHandler
import com.dpis.module.applist.AppListItem
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.viewport.ViewportTargetSpec

/**
 * Maps a parsed editor draft onto [AppConfigSaveHandler.saveResolved].
 *
 * Store and system-hook availability stay on [PersistContext] so main workspace and
 * Quick Config can share persist without sharing post-save effects.
 */
class AppConfigEditorPersister(
    private val saveHandler: AppConfigSaveHandler,
    private val persistContext: PersistContext,
) : ComposeAppEditorSaveWorkflow.Persister {
    interface PersistContext {
        fun systemHooksEnabled(): Boolean
        fun configStore(): DpisConfigStore?
    }

    override fun persist(
        item: AppListItem,
        draft: EditorDraft,
        viewport: ViewportTargetSpec,
        fontPercent: Int?,
    ): AppConfigSaveHandler.Result = saveHandler.saveResolved(
        item,
        viewport,
        draft.viewportMode,
        draft.viewportApplyMode,
        draft.viewportApplyModeResetRequested,
        fontPercent,
        draft.fontMode,
        draft.selectedTypefaceId,
        draft.draftFontHookDomainsRaw,
        draft.fontHookDomainsResetRequested,
        draft.viewportScaleInput,
        draft.viewportAbsoluteInput,
        persistContext.systemHooksEnabled(),
        persistContext.configStore(),
        null,
    )
}
