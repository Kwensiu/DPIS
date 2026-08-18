package com.dpis.module.appconfig;

import com.dpis.module.appconfig.EditorDraft;
import com.dpis.module.applist.AppListItem;

import java.util.List;

/** Pure resolution of the app-editor item and draft baseline used by a presentation surface. */
public final class EditorSessionResolver {
    private EditorSessionResolver() {
    }

    public static AppListItem findItem(List<AppListItem> apps, String packageName) {
        if (apps == null || packageName == null) {
            return null;
        }
        for (AppListItem item : apps) {
            if (packageName.equals(item.packageName)) {
                return item;
            }
        }
        return null;
    }

    public static Session resolve(
            AppListItem item,
            EditorDraft currentDraft,
            EditorDraft savedDraft
    ) {
        if (item == null) {
            return null;
        }
        if (currentDraft == null || !item.packageName.equals(currentDraft.packageName)) {
            EditorDraft initial = EditorDraft.fromItem(item);
            return new Session(initial, initial, true);
        }
        return new Session(
                currentDraft,
                savedDraft != null && item.packageName.equals(savedDraft.packageName)
                        ? savedDraft : currentDraft,
                false);
    }

    public static final class Session {
        public final EditorDraft draft;
        public final EditorDraft savedDraft;
        public final boolean initialized;

        private Session(
                EditorDraft draft,
                EditorDraft savedDraft,
                boolean initialized
        ) {
            this.draft = draft;
            this.savedDraft = savedDraft;
            this.initialized = initialized;
        }
    }
}
