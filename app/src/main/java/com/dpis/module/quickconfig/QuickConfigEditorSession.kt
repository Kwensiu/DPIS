package com.dpis.module.quickconfig

import com.dpis.module.appconfig.AppConfigEditorSession
import com.dpis.module.appconfig.EditorDraft
import com.dpis.module.applist.AppListItem
import com.dpis.module.ui.ConfigEditorDestination

/**
 * In-memory Quick Config editing session retained only across Activity configuration changes.
 * Closing the Activity still discards the draft instead of turning it into persisted config.
 */
internal class QuickConfigEditorSession @JvmOverloads constructor(
    @JvmField val item: AppListItem?,
    @JvmField val draft: EditorDraft,
    savedDraft: EditorDraft?,
    destination: ConfigEditorDestination?,
    @JvmField val editorSession: AppConfigEditorSession? = null,
) {
    @JvmField val savedDraft: EditorDraft = savedDraft ?: draft
    @JvmField val destination: ConfigEditorDestination = destination ?: ConfigEditorDestination.MAIN
}
