package com.dpis.module;

import static org.junit.Assert.assertEquals;

import com.dpis.module.applist.AppListPage;

import org.junit.Test;

public final class AppWorkspaceScrollStateStoreTest {
    @Test
    public void snapshotRestoreKeepsBothPagePositionsIndependent() {
        AppWorkspaceScrollStateStore source = new AppWorkspaceScrollStateStore();
        source.update(AppListPage.ALL_APPS, 12, 34);
        source.update(AppListPage.CONFIGURED_APPS, 5, 67);

        AppWorkspaceScrollStateStore restored = new AppWorkspaceScrollStateStore();
        restored.restore(source.snapshot());

        assertPosition(restored, AppListPage.ALL_APPS, 12, 34);
        assertPosition(restored, AppListPage.CONFIGURED_APPS, 5, 67);
    }

    @Test
    public void invalidValuesAndSnapshotsCannotProduceNegativePositions() {
        AppWorkspaceScrollStateStore store = new AppWorkspaceScrollStateStore();
        store.update(AppListPage.ALL_APPS, -4, -8);
        store.restore(new int[]{9});

        assertPosition(store, AppListPage.ALL_APPS, 0, 0);
        assertPosition(store, AppListPage.CONFIGURED_APPS, 0, 0);
    }

    private static void assertPosition(AppWorkspaceScrollStateStore store,
            AppListPage page, int index, int offset) {
        AppWorkspacePresentation.ScrollPosition position = store.positionFor(page);
        assertEquals(index, position.index);
        assertEquals(offset, position.scrollOffset);
    }
}
