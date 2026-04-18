package com.dpis.module;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AppListPageTest {
    @Test
    public void fromPosition_mapsTwoPageOrder() {
        assertEquals(AppListPage.ALL_APPS, AppListPage.fromPosition(0));
        assertEquals(AppListPage.CONFIGURED_APPS, AppListPage.fromPosition(1));
    }

    @Test
    public void titleRes_matchesApprovedTabs() {
        assertEquals(R.string.tab_all_apps, AppListPage.ALL_APPS.titleRes());
        assertEquals(R.string.tab_configured_apps, AppListPage.CONFIGURED_APPS.titleRes());
    }
}
