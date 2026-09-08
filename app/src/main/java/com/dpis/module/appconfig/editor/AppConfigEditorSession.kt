package com.dpis.module.appconfig

import com.dpis.module.applist.AppListItem
import com.dpis.module.templates.TemplateConfigValue

/** Exactly one header chip derived from the current editor session. */
enum class AppConfigEditorChip {
    NONE,
    PREFILL,
    UNSAVED,
}

/**
 * Per-opened-sheet comparison baselines plus the mutable draft.
 *
 * `prefillSnapshot` is resolved once when the sheet opens and is never reloaded for this session.
 */
class AppConfigEditorSession(
    @JvmField val persistedBaseline: EditorDraft,
    @JvmField val prefillSnapshot: EditorDraft?,
    @JvmField val draft: EditorDraft,
    @JvmField val prefillInvalidated: Boolean,
) {
    val chip: AppConfigEditorChip
        get() {
            val snapshot = prefillSnapshot
            if (snapshot != null && !prefillInvalidated && draft.hasSamePersistedConfig(snapshot)) {
                return AppConfigEditorChip.PREFILL
            }
            if (!draft.hasSamePersistedConfig(persistedBaseline)) {
                return AppConfigEditorChip.UNSAVED
            }
            return AppConfigEditorChip.NONE
        }

    fun withDraft(next: EditorDraft): AppConfigEditorSession {
        val leftPrefill = prefillSnapshot != null && !next.hasSamePersistedConfig(prefillSnapshot)
        return AppConfigEditorSession(
            persistedBaseline,
            prefillSnapshot,
            next,
            prefillInvalidated || leftPrefill,
        )
    }

    fun withScopeSelected(selected: Boolean): AppConfigEditorSession = AppConfigEditorSession(
        persistedBaseline.withScopeSelected(selected),
        prefillSnapshot?.withScopeSelected(selected),
        draft.withScopeSelected(selected),
        prefillInvalidated,
    )

    fun reset(): AppConfigEditorSession = AppConfigEditorSession(
        persistedBaseline,
        prefillSnapshot,
        draft.cleared(),
        true,
    )

    fun afterSave(): AppConfigEditorSession {
        val saved = draft.afterSuccessfulSave()
        return AppConfigEditorSession(
            saved,
            null,
            saved,
            false,
        )
    }

    companion object {
        @JvmStatic
        fun open(
            item: AppListItem,
            hasSavedPackageConfig: Boolean,
            prefill: TemplateConfigValue?,
        ): AppConfigEditorSession {
            val persistedBaseline = if (hasSavedPackageConfig) {
                EditorDraft.fromItem(item)
            } else {
                EditorDraft.fromItem(item).cleared()
            }
            val prefillSnapshot = if (
                !hasSavedPackageConfig && prefill != null && prefill.hasAnyValue()
            ) {
                EditorDraft.fromItem(item.withGlobalPrefillPreview(prefill))
            } else {
                null
            }
            return AppConfigEditorSession(
                persistedBaseline,
                prefillSnapshot,
                prefillSnapshot ?: persistedBaseline,
                false,
            )
        }

        @JvmStatic
        fun retainOrOpen(
            current: AppConfigEditorSession?,
            item: AppListItem,
            hasSavedPackageConfig: Boolean,
            prefill: TemplateConfigValue?,
        ): AppConfigEditorSession {
            if (current != null && current.draft.packageName == item.packageName) {
                return current
            }
            return open(item, hasSavedPackageConfig, prefill)
        }
    }
}
