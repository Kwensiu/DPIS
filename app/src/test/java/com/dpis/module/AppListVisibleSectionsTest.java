package com.dpis.module;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public class AppListVisibleSectionsTest {
    @Test
    public void filter_returnsConfiguredSubsetWithoutDroppingSearch() {
        AppListItem configured = new AppListItem(
                "123云盘",
                "com.mfcloudcalculate.networkdisk",
                true,
                null,
                ViewportApplyMode.OFF,
                null,
                FontApplyMode.OFF,
                true,
                false,
                null);
        AppListItem plain = new AppListItem(
                "Android System WebView",
                "com.google.android.webview",
                false,
                null,
                ViewportApplyMode.OFF,
                null,
                FontApplyMode.OFF,
                true,
                true,
                null);

        List<AppListItem> configuredItems = AppListVisibleSections.filter(
                List.of(configured, plain), "", AppListPage.CONFIGURED_APPS);
        List<AppListItem> searchedItems = AppListVisibleSections.filter(
                List.of(configured, plain), "android", AppListPage.ALL_APPS);

        assertEquals(1, configuredItems.size());
        assertEquals("123云盘", configuredItems.get(0).label);
        assertEquals(1, searchedItems.size());
        assertEquals("com.google.android.webview", searchedItems.get(0).packageName);
    }
}
