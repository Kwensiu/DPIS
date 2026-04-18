package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class AppListRowIndicatorSmokeTest {
    @Test
    public void itemRowDoesNotRenderTrailingIndicator() throws IOException {
        String layout = read("src/main/res/layout/item_app_entry.xml");
        String adapter = read("src/main/java/com/dpis/module/AppListPagerAdapter.java");

        assertTrue(!layout.contains("@+id/expand_indicator"));
        assertTrue(!layout.contains("@drawable/ic_chevron_right_24"));
        assertTrue(!adapter.contains("expandIndicator"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
