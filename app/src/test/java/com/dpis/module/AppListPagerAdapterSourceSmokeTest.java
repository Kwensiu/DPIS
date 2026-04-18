package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class AppListPagerAdapterSourceSmokeTest {
    @Test
    public void pageListAdapter_usesListAdapterAndDiffUtil() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppListPagerAdapter.java");

        assertTrue(source.contains("extends ListAdapter<AppListItem, RowHolder>"));
        assertTrue(source.contains("DiffUtil.ItemCallback<AppListItem>"));
        assertTrue(source.contains("submitList(new ArrayList<>(newItems));"));
        assertTrue(!source.contains("notifyDataSetChanged()"));
        assertTrue(source.contains("capturePageScrollStates()"));
        assertTrue(source.contains("restorePageScrollStates("));
        assertTrue(source.contains("setRefreshing(AppListPage page, boolean refreshing)"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
