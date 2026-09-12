package com.dpis.module.templates.presentation

import android.content.Context
import com.dpis.module.templates.TemplateEditorDraft
import com.dpis.module.ui.ConfigEditorDestination
import java.util.LinkedHashSet

/** Owns template-list refresh timing so Compose never reads template stores during recomposition. */
class TemplateWorkspacePresentationController(
    private val context: Context,
    private val actions: TemplateWorkspacePresentation.Actions,
    initialQuery: String,
) {
    fun interface Listener {
        fun onStateChanged(state: TemplateWorkspacePresentation.State)
    }

    private val listeners = LinkedHashSet<Listener>()
    private var snapshot: TemplateWorkspacePresentation.State =
        TemplateWorkspacePresentation.create(context, initialQuery, actions)

    fun state(): TemplateWorkspacePresentation.State = snapshot

    /** Republishes one complete editor session snapshot; callers may not drop route or draft state. */
    fun refresh(
        query: String?,
        nextDetailKind: TemplateWorkspacePresentation.DetailKind?,
        nextDetailTemplateId: String?,
        editorDestination: ConfigEditorDestination?,
        globalPrefillDraft: TemplateEditorDraft?,
        quickTemplateDraft: TemplateEditorDraft?,
        applyConfirmation: TemplateWorkspacePresentation.ApplyConfirmation?,
    ) {
        val detailKind = nextDetailKind ?: TemplateWorkspacePresentation.DetailKind.NONE
        snapshot = TemplateWorkspacePresentation.create(
            context,
            query,
            actions,
            detailKind,
            nextDetailTemplateId,
            editorDestination ?: ConfigEditorDestination.MAIN,
            globalPrefillDraft,
            quickTemplateDraft,
            applyConfirmation,
        )
        for (listener in LinkedHashSet(listeners)) {
            listener.onStateChanged(snapshot)
        }
    }

    fun addListener(listener: Listener?) {
        if (listener == null) return
        listeners.add(listener)
        listener.onStateChanged(snapshot)
    }

    fun removeListener(listener: Listener?) {
        listeners.remove(listener)
    }
}
