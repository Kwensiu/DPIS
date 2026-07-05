package com.dpis.module;

import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;
import com.dpis.module.viewport.ViewportTargetType;

import com.dpis.module.applist.AppListItem;
import com.dpis.module.applist.AppListPage;
import com.dpis.module.applist.AppListVisibleSections;

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
                true,
                null,
                null,
                ViewportApplyMode.OFF,
                ViewportTargetType.OFF,
                ViewportTargetSpec.off(),
                null,
                FontApplyMode.OFF,
                null,
                false,
                true,
                true,
                true,
                false,
                false,
                null);
        AppListItem plain = new AppListItem(
                "Android System WebView",
                "com.google.android.webview",
                false,
                true,
                null,
                ViewportApplyMode.OFF,
                null,
                FontApplyMode.OFF,
                true,
                true,
                false,
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

    @Test
    public void configuredButUninstalledAppearsOnlyInConfiguredApps() {
        AppListItem configuredUninstalled = new AppListItem(
                "com.example.removed",
                "com.example.removed",
                false,
                true,
                null,
                null,
                ViewportApplyMode.SYSTEM_EMULATION,
                ViewportTargetType.OFF,
                ViewportTargetSpec.off(),
                null,
                FontApplyMode.SYSTEM_EMULATION,
                null,
                false,
                true,
                true,
                false,
                false,
                false,
                null);

        List<AppListItem> configuredItems = AppListVisibleSections.filter(
                List.of(configuredUninstalled), "", AppListPage.CONFIGURED_APPS);
        List<AppListItem> allAppsItems = AppListVisibleSections.filter(
                List.of(configuredUninstalled), "", AppListPage.ALL_APPS);

        assertEquals(1, configuredItems.size());
        assertEquals("com.example.removed", configuredItems.get(0).packageName);
        assertEquals(0, allAppsItems.size());
    }

    @Test
    public void configuredTabUsesSameConfiguredFlagAsCountSource() {
        AppListItem modeOnly = new AppListItem(
                "Mode Only",
                "com.example.mode",
                false,
                true,
                null,
                null,
                ViewportApplyMode.SYSTEM_EMULATION,
                ViewportTargetType.OFF,
                ViewportTargetSpec.off(),
                null,
                FontApplyMode.OFF,
                null,
                false,
                true,
                true,
                true,
                false,
                false,
                null);
        AppListItem plain = new AppListItem(
                "Plain",
                "com.example.plain",
                false,
                true,
                null,
                null,
                ViewportApplyMode.OFF,
                ViewportTargetType.OFF,
                ViewportTargetSpec.off(),
                null,
                FontApplyMode.OFF,
                null,
                false,
                true,
                false,
                true,
                false,
                false,
                null);

        List<AppListItem> items = List.of(modeOnly, plain);
        long configuredCount = items.stream().filter(item -> item.configured).count();
        List<AppListItem> configuredItems = AppListVisibleSections.filter(
                items, "", AppListPage.CONFIGURED_APPS);

        assertEquals(configuredCount, configuredItems.size());
        assertEquals("com.example.mode", configuredItems.get(0).packageName);
    }
}
