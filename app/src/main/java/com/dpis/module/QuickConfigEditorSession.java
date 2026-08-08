package com.dpis.module;

import com.dpis.module.applist.AppListItem;

/**
 * In-memory Quick Config editing session retained only across Activity configuration changes.
 * Closing the Activity still discards the draft instead of turning it into persisted config.
 */
final class QuickConfigEditorSession {

    final AppListItem item;
    final AppConfigEditorDraft draft;
    final AppConfigEditorDraft savedDraft;
    final ConfigEditorDestination destination;

    QuickConfigEditorSession(
            AppListItem item,
            AppConfigEditorDraft draft,
            AppConfigEditorDraft savedDraft,
            ConfigEditorDestination destination
    ) {
        this.item = item;
        this.draft = draft;
        this.savedDraft = savedDraft != null ? savedDraft : draft;
        this.destination = destination != null ? destination : ConfigEditorDestination.MAIN;
    }
}
