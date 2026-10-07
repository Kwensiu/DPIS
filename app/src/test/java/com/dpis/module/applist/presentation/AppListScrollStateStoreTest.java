package com.dpis.module.applist.presentation;

import static org.junit.Assert.assertEquals;

import com.dpis.module.applist.AppListPage;
import com.dpis.module.applist.presentation.AppListPresentation;

import org.junit.Test;
import com.dpis.module.applist.presentation.AppListScrollStateStore;

public final class AppListScrollStateStoreTest {
    @Test
    public void snapshotRestoreKeepsBothPagePositionsIndependent() {
        AppListScrollStateStore source = new AppListScrollStateStore();
        source.update(AppListPage.ALL_APPS, 12, 34);
        source.update(AppListPage.CONFIGURED_APPS, 5, 67);

        AppListScrollStateStore restored = new AppListScrollStateStore();
        restored.restore(source.snapshot());

        assertPosition(restored, AppListPage.ALL_APPS, 12, 34);
        assertPosition(restored, AppListPage.CONFIGURED_APPS, 5, 67);
    }

    @Test
    public void invalidValuesAndSnapshotsCannotProduceNegativePositions() {
        AppListScrollStateStore store = new AppListScrollStateStore();
        store.update(AppListPage.ALL_APPS, -4, -8);
        store.restore(new int[]{9});

        assertPosition(store, AppListPage.ALL_APPS, 0, 0);
        assertPosition(store, AppListPage.CONFIGURED_APPS, 0, 0);
    }

    private static void assertPosition(AppListScrollStateStore store,
            AppListPage page, int index, int offset) {
        AppListPresentation.ScrollPosition position = store.positionFor(page);
        assertEquals(index, position.index);
        assertEquals(offset, position.scrollOffset);
    }
}
