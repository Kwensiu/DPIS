package com.dpis.module;

import com.dpis.module.applist.AppListPage;

/** Session-owned scroll positions for the two app catalogue pages. */
final class AppWorkspaceScrollStateStore {
    private static final int VALUES_PER_PAGE = 2;
    private static final int SNAPSHOT_SIZE = AppListPage.values().length * VALUES_PER_PAGE;

    private final int[] positions = new int[SNAPSHOT_SIZE];

    AppWorkspacePresentation.ScrollPosition positionFor(AppListPage page) {
        int offset = pageOffset(page);
        return new AppWorkspacePresentation.ScrollPosition(
                positions[offset],
                positions[offset + 1]
        );
    }

    void update(AppListPage page, int index, int scrollOffset) {
        int offset = pageOffset(page);
        positions[offset] = Math.max(0, index);
        positions[offset + 1] = Math.max(0, scrollOffset);
    }

    int[] snapshot() {
        return positions.clone();
    }

    void restore(int[] snapshot) {
        if (snapshot == null || snapshot.length < SNAPSHOT_SIZE) {
            return;
        }
        for (AppListPage page : AppListPage.values()) {
            int offset = pageOffset(page);
            update(page, snapshot[offset], snapshot[offset + 1]);
        }
    }

    private static int pageOffset(AppListPage page) {
        AppListPage safePage = page != null ? page : AppListPage.ALL_APPS;
        return safePage.position() * VALUES_PER_PAGE;
    }
}
