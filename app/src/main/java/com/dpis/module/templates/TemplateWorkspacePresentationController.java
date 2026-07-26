package com.dpis.module.templates;

import android.content.Context;

import java.util.LinkedHashSet;
import java.util.Set;

/** Owns template-list refresh timing so Compose never reads template stores during recomposition. */
public final class TemplateWorkspacePresentationController {
    public interface Listener {
        void onStateChanged(TemplateWorkspacePresentation.State state);
    }

    private final Context context;
    private final TemplateWorkspacePresentation.Actions actions;
    private final Set<Listener> listeners = new LinkedHashSet<>();
    private TemplateWorkspacePresentation.State state;

    public TemplateWorkspacePresentationController(
            Context context,
            TemplateWorkspacePresentation.Actions actions,
            String initialQuery
    ) {
        this.context = context;
        this.actions = actions;
        this.state = TemplateWorkspacePresentation.create(context, initialQuery, actions);
    }

    public TemplateWorkspacePresentation.State state() {
        return state;
    }

    /** Republishes one complete editor session snapshot; callers may not drop route or draft state. */
    public void refresh(
            String query,
            TemplateWorkspacePresentation.DetailKind nextDetailKind,
            String nextDetailTemplateId,
            com.dpis.module.ConfigEditorDestination editorDestination,
            TemplateEditorDraft globalPrefillDraft,
            TemplateEditorDraft quickTemplateDraft
    ) {
        TemplateWorkspacePresentation.DetailKind detailKind = nextDetailKind != null
                ? nextDetailKind
                : TemplateWorkspacePresentation.DetailKind.NONE;
        state = TemplateWorkspacePresentation.create(
                context,
                query,
                actions,
                detailKind,
                nextDetailTemplateId,
                editorDestination,
                globalPrefillDraft,
                quickTemplateDraft
        );
        for (Listener listener : new LinkedHashSet<>(listeners)) {
            listener.onStateChanged(state);
        }
    }

    public void addListener(Listener listener) {
        if (listener == null) return;
        listeners.add(listener);
        listener.onStateChanged(state);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }
}
