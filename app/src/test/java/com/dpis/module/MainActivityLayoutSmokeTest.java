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
        assertTrue(layout.contains("android:id=\"@+id/help_fab\""));
        assertTrue(layout.contains("android:id=\"@+id/search_focus_fab\""));
        assertTrue(countMatches(layout, "app:elevation=\"6dp\"") == 2);
        assertTrue(countMatches(layout, "app:hoveredFocusedTranslationZ=\"8dp\"") == 2);
        assertTrue(countMatches(layout, "app:pressedTranslationZ=\"10dp\"") == 2);
        assertTrue(countMatches(layout, "app:rippleColor=\"@color/dpis_fab_ripple\"") == 2);
        assertTrue(layout.contains("android:id=\"@+id/top_container\""));
        assertTrue(layout.contains("android:paddingStart=\"16dp\""));
        assertTrue(layout.contains("android:paddingTop=\"16dp\""));
        assertTrue(layout.contains("android:src=\"@drawable/ic_search_24\""));
        assertTrue(layout.contains("app:srcCompat=\"@drawable/ic_info_outline_24\""));
        assertTrue(layout.contains("android:src=\"@drawable/baseline_tune_24\""));
        assertTrue(layout.contains("android:id=\"@+id/system_settings_button\""));
        assertTrue(layout.contains("android:id=\"@+id/app_pager\""));
        assertTrue(strings.contains("tab_all_apps"));
        assertTrue(strings.contains("quick_search_button"));
        assertTrue(strings.contains("help_button"));
        assertTrue(Files.exists(Path.of("src/main/res/drawable/baseline_tune_24.xml")));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }

    private static int countMatches(String text, String target) {
        int count = 0;
        int index = 0;
        while (true) {
            int found = text.indexOf(target, index);
            if (found < 0) {
                return count;
            }
            count++;
            index = found + target.length();
        }
    }
}
