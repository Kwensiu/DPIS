package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AppListFilterTest {
    @Test
    public void userTabMatchesOnlyNonSystemApps() {
        assertTrue(AppListFilter.matches("",
                AppListFilter.Tab.USER_APPS,
                "Coolapk",
                "com.coolapk.market",
                false,
                false,
                null,
                null));
        assertFalse(AppListFilter.matches("",
                AppListFilter.Tab.USER_APPS,
                "系统相机",
                "com.android.camera",
                true,
                false,
                null,
                null));
    }

    @Test
    public void configuredTabMatchesScopedOrConfiguredApps() {
        assertTrue(AppListFilter.matches("",
                AppListFilter.Tab.CONFIGURED_APPS,
                "Coolapk",
                "com.coolapk.market",
                false,
                true,
                null,
                null));
        assertTrue(AppListFilter.matches("",
                AppListFilter.Tab.CONFIGURED_APPS,
                "Xiaoheihe",
                "com.max.xiaoheihe",
                false,
                false,
                300,
                null));
        assertTrue(AppListFilter.matches("",
                AppListFilter.Tab.CONFIGURED_APPS,
                "Tieba",
                "com.baidu.tieba",
                false,
                false,
                null,
                115));
        assertFalse(AppListFilter.matches("",
                AppListFilter.Tab.CONFIGURED_APPS,
                "AdClose",
                "com.close.hook.ads",
                false,
                false,
                null,
                null));
    }

    @Test
    public void systemTabSupportsQueryFiltering() {
        assertTrue(AppListFilter.matches("android",
                AppListFilter.Tab.SYSTEM_APPS,
                "Android System WebView",
                "com.google.android.webview",
                true,
                false,
                null,
                null));
        assertFalse(AppListFilter.matches("cool",
                AppListFilter.Tab.SYSTEM_APPS,
                "Android System WebView",
                "com.google.android.webview",
                true,
                false,
                null,
                null));
    }
}
