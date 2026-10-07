package com.dpis.module.appconfig

import com.dpis.module.appconfig.editor.EditorDraft
import com.dpis.module.applist.AppListItem

/** Resolves the app-editor item and draft baseline used by a presentation surface. */
object EditorSessionResolver {
    @JvmStatic
    fun findItem(apps: List<AppListItem>?, packageName: String?): AppListItem? {
        if (packageName == null) {
            return null
        }
        return apps?.firstOrNull { it.packageName == packageName }
    }

    @JvmStatic
    fun resolve(
        item: AppListItem?,
        currentDraft: EditorDraft?,
        savedDraft: EditorDraft?
    ): Session? {
        item ?: return null
        if (currentDraft == null || item.packageName != currentDraft.packageName) {
            val initial = EditorDraft.fromItem(item)
            return Session(initial, initial, initialized = true)
        }
        val baseline = savedDraft?.takeIf { it.packageName == item.packageName } ?: currentDraft
        return Session(currentDraft, baseline, initialized = false)
    }

    data class Session(
        @JvmField val draft: EditorDraft,
        @JvmField val savedDraft: EditorDraft,
        @JvmField val initialized: Boolean
    )
}
