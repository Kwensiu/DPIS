package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class MainActivitySourceSmokeTest {
    @Test
    public void mainActivityWiresPagerMediatorAndFilterEntry() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(source.contains("R.id.app_pager"));
        assertTrue(source.contains("new TabLayoutMediator("));
        assertTrue(source.contains("searchFilterButton.setOnClickListener"));
        assertTrue(source.contains("showFilterDialog()"));
        assertTrue(source.contains("new AppListFilterState("));
        assertTrue(source.contains("setOnCheckedChangeListener"));
        assertTrue(!source.contains("R.id.filter_apply_button"));
        assertTrue(!source.contains("R.id.filter_reset_button"));
    }

    @Test
    public void restoreSnapshot_isNotBlockedBySavedStateBranch() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        int restoreSnapshotLine = source.indexOf("allApps.addAll(retainedState.appsSnapshot);");
        assertTrue(restoreSnapshotLine > 0);
        String beforeRestoreSnapshot = source.substring(0, restoreSnapshotLine);

        assertTrue(beforeRestoreSnapshot.contains("if (retainedState != null) {"));
        assertTrue(!beforeRestoreSnapshot.contains("else if (retainedState != null)"));
    }

    @Test
    public void loadInstalledApps_usesIconCacheEntryPoint() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(source.contains("AppIconMemoryCache"));
        assertTrue(source.contains("loadAppIcon(packageManager, applicationInfo)"));
        assertTrue(!source.contains("Drawable icon = applicationInfo.loadIcon(packageManager);"));
    }

    @Test
    public void savesAndRestoresPageScrollStatesForRotation() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(source.contains("STATE_PAGE_SCROLL_STATES"));
        assertTrue(source.contains("putSparseParcelableArray("));
        assertTrue(source.contains("capturePageScrollStates()"));
        assertTrue(source.contains("restorePageScrollStates("));
        assertTrue(source.contains("restoredPageScrollStates"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
