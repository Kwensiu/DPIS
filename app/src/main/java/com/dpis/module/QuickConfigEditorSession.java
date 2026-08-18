package com.dpis.module;
import com.dpis.module.appconfig.EditorDraft;

import com.dpis.module.applist.AppListItem;

/**
 * In-memory Quick Config editing session retained only across Activity configuration changes.
 * Closing the Activity still discards the draft instead of turning it into persisted config.
 */
final class QuickConfigEditorSession {

    final AppListItem item;
    final EditorDraft draft;
    final EditorDraft savedDraft;
    final ConfigEditorDestination destination;

    QuickConfigEditorSession(
            AppListItem item,
            EditorDraft draft,
            EditorDraft savedDraft,
            ConfigEditorDestination destination
    ) {
        this.item = item;
        this.draft = draft;
        this.savedDraft = savedDraft != null ? savedDraft : draft;
        this.destination = destination != null ? destination : ConfigEditorDestination.MAIN;
    }
}
