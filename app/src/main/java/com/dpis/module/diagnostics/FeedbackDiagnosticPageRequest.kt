package com.dpis.module.diagnostics

import com.dpis.module.appconfig.EditorDraft
import com.dpis.module.applist.AppListItem

/** Inputs needed to rebuild the diagnostic page after a configuration change. */
data class FeedbackDiagnosticPageRequest(
    val item: AppListItem,
    val draft: EditorDraft,
    val versionName: String,
)
