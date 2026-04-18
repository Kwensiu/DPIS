package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class MainActivityLayoutSmokeTest {
    @Test
    public void activityStatusLayoutContainsSearchFilterSettingsAndPager() throws IOException {
        String layout = read("src/main/res/layout/activity_status.xml");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(layout.contains("android:id=\"@+id/search_input\""));
        assertTrue(layout.contains("android:id=\"@+id/search_filter_button\""));
        assertTrue(layout.contains("android:src=\"@drawable/ic_search_24\""));
        assertTrue(layout.contains("android:src=\"@drawable/baseline_tune_24\""));
        assertTrue(layout.contains("android:id=\"@+id/system_settings_button\""));
        assertTrue(layout.contains("android:id=\"@+id/app_pager\""));
        assertTrue(strings.contains("tab_all_apps"));
        assertTrue(Files.exists(Path.of("src/main/res/drawable/baseline_tune_24.xml")));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
