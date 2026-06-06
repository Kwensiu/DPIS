package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class AppListRowIndicatorSmokeTest {
    @Test
    public void itemRowDoesNotRenderTrailingIndicator() throws IOException {
        String layout = read("src/main/res/layout/item_app_entry.xml");
        String adapter = read("src/main/java/com/dpis/module/AppListPagerAdapter.java");

        assertTrue(!layout.contains("@+id/expand_indicator"));
        assertTrue(!layout.contains("@drawable/ic_chevron_right_24"));
        assertTrue(!adapter.contains("expandIndicator"));
        assertTrue(layout.contains("@+id/app_icon_skeleton"));
        assertTrue(layout.contains("@drawable/bg_app_icon_skeleton_mask"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
