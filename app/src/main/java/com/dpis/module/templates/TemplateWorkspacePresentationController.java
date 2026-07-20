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
    private TemplateWorkspacePresentation.DetailKind detailKind
            = TemplateWorkspacePresentation.DetailKind.NONE;
    private String detailTemplateId;

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

    public void refresh(String query) {
        refresh(query, detailKind, detailTemplateId);
    }

    public void refresh(
            String query,
            TemplateWorkspacePresentation.DetailKind nextDetailKind,
            String nextDetailTemplateId
    ) {
        detailKind = nextDetailKind != null
                ? nextDetailKind
                : TemplateWorkspacePresentation.DetailKind.NONE;
        detailTemplateId = nextDetailTemplateId;
        state = TemplateWorkspacePresentation.create(
                context,
                query,
                actions,
                detailKind,
                detailTemplateId
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
