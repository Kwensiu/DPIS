package com.dpis.module;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class MainActivityFilterStateSourceSmokeTest {
    @Test
    public void mainActivityLoadsAndSavesPersistedFilterState() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(source.contains("private AppListFilterStateStore appListFilterStateStore;"));
        assertTrue(source.contains("appListFilterStateStore = new AppListFilterStateStore(this);"));
        assertTrue(source.contains("AppListFilterState initialFilterState = appListFilterStateStore.load();"));
        assertTrue(source.contains("initialFilterState = retainedState.filterState;"));
        assertTrue(source.contains("initialFilterState = new AppListFilterState("));
        assertTrue(source.contains("appListFilterStateStore.save(filterState);"));
        assertTrue(source.contains("dispatchMainUiAction(MainUiAction.filterChanged(filterState));"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
