package com.dpis.module.ui.presentation

import com.dpis.module.appconfig.EditorDraft
import com.dpis.module.applist.AppListFilterState
import com.dpis.module.applist.AppListItem
import com.dpis.module.diagnostics.presentation.FeedbackDiagnosticActivitySession
import com.dpis.module.templates.presentation.TemplateWorkspaceActivitySession
import com.dpis.module.ui.ConfigEditorDestination
import com.dpis.module.ui.MainUiState
import com.dpis.module.updates.UpdatePromptRequest
import java.util.Objects

/** Non-configuration snapshot for MainActivity rotation and process-preserving recreation. */
class MainRetainedState(
    appsSnapshot: List<AppListItem>?,
    query: String?,
    templateQuery: String?,
    filterState: AppListFilterState?,
    workspaceMode: MainUiState.WorkspaceMode?,
    @JvmField val currentPage: Int,
    appListScrollPositions: IntArray?,
    refreshingPagePositions: IntArray?,
    @JvmField val editingPackageName: String?,
    @JvmField val editingDraft: EditorDraft?,
    @JvmField val savedEditingDraft: EditorDraft?,
    @JvmField val prefillSnapshot: EditorDraft?,
    @JvmField val prefillInvalidated: Boolean,
    editingDestination: ConfigEditorDestination?,
    @JvmField val workspaceSessionState: TemplateWorkspaceActivitySession.State?,
    @JvmField val feedbackDiagnostic: FeedbackDiagnosticActivitySession.State?,
    @JvmField val pendingUpdatePrompt: UpdatePromptRequest?,
) {
    @JvmField
    val appsSnapshot: List<AppListItem> = appsSnapshot ?: emptyList()

    @JvmField
    val query: String = query ?: ""

    @JvmField
    val templateQuery: String = templateQuery ?: ""

    @JvmField
    val filterState: AppListFilterState = filterState ?: AppListFilterState.defaultState()

    @JvmField
    val workspaceMode: MainUiState.WorkspaceMode =
        workspaceMode ?: MainUiState.WorkspaceMode.APP

    @JvmField
    val appListScrollPositions: IntArray =
        appListScrollPositions?.clone() ?: IntArray(0)

    @JvmField
    val refreshingPagePositions: IntArray =
        refreshingPagePositions?.clone() ?: IntArray(0)

    @JvmField
    val editingDestination: ConfigEditorDestination =
        editingDestination ?: ConfigEditorDestination.MAIN

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is MainRetainedState) {
            return false
        }
        return currentPage == other.currentPage
            && appsSnapshot == other.appsSnapshot
            && query == other.query
            && templateQuery == other.templateQuery
            && filterState == other.filterState
            && workspaceMode == other.workspaceMode
            && appListScrollPositions.contentEquals(other.appListScrollPositions)
            && refreshingPagePositions.contentEquals(other.refreshingPagePositions)
            && editingPackageName == other.editingPackageName
            && editingDraft == other.editingDraft
            && savedEditingDraft == other.savedEditingDraft
            && prefillSnapshot == other.prefillSnapshot
            && prefillInvalidated == other.prefillInvalidated
            && editingDestination == other.editingDestination
            && workspaceSessionState == other.workspaceSessionState
            && feedbackDiagnostic == other.feedbackDiagnostic
            && pendingUpdatePrompt == other.pendingUpdatePrompt
    }

    override fun hashCode(): Int {
        var result = Objects.hash(
            appsSnapshot,
            query,
            templateQuery,
            filterState,
            workspaceMode,
            currentPage,
            editingPackageName,
            editingDraft,
            savedEditingDraft,
            prefillSnapshot,
            prefillInvalidated,
            editingDestination,
            workspaceSessionState,
            feedbackDiagnostic,
            pendingUpdatePrompt,
        )
        result = 31 * result + appListScrollPositions.contentHashCode()
        return 31 * result + refreshingPagePositions.contentHashCode()
    }

    override fun toString(): String {
        return "RetainedState[appsSnapshot=$appsSnapshot" +
            ", query=$query" +
            ", templateQuery=$templateQuery" +
            ", currentPage=$currentPage" +
            ", appListScrollPositions=${appListScrollPositions.contentToString()}" +
            ", refreshingPagePositions=${refreshingPagePositions.contentToString()}]"
    }
}
