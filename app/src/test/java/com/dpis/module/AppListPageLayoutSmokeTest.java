package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class AppListPageLayoutSmokeTest {
    @Test
    public void pageLayout_wrapsRecyclerViewWithSwipeRefresh() throws IOException {
        String layout = read("src/main/res/layout/item_app_list_page.xml");

        assertTrue(layout.contains("androidx.swiperefreshlayout.widget.SwipeRefreshLayout"));
        assertTrue(layout.contains("android:id=\"@+id/page_swipe_refresh\""));
        assertTrue(layout.contains("android:id=\"@+id/page_list\""));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
