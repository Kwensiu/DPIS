package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class AppListTabsChromeControllerSourceSmokeTest {
    @Test
    public void controllerKeepsPortraitOverlaySeparateFromLandscapeListLayout()
            throws IOException {
        String source = SourceSmokeTestPaths.read(
                "src/main/java/com/dpis/module/applist/AppListTabsChromeController.java"
        );

        assertTrue(source.contains("public boolean onPageListScrolled(int dy)"));
        assertTrue(source.contains("return WatchUiMode.shouldUseCompactUi(context) && pagerAdapter != null;"));
        assertTrue(source.contains("pagerAdapter.setTopContentInset(tabHeight);"));
        assertTrue(source.contains("landscapeListController.setTopContentInset(0);"));
        assertTrue(source.contains("tabs.setTranslationY(-scrollOffset);"));
    }
}
