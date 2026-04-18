package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class FilterSheetLayoutSmokeTest {
    @Test
    public void filterSheetContainsOnlyCompactInteractiveSwitches() throws IOException {
        String layout = read("src/main/res/layout/dialog_list_filters.xml");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(layout.contains("android:id=\"@+id/filter_show_system_switch\""));
        assertTrue(layout.contains("android:id=\"@+id/filter_injected_only_switch\""));
        assertTrue(layout.contains("android:id=\"@+id/filter_width_only_switch\""));
        assertTrue(layout.contains("android:id=\"@+id/filter_font_only_switch\""));
        assertTrue(layout.contains("android:layout_width=\"36dp\""));
        assertTrue(layout.contains("android:layout_height=\"4dp\""));
        assertTrue(layout.contains("android:layout_gravity=\"center_horizontal\""));
        assertTrue(layout.contains("android:background=\"?attr/colorOutlineVariant\""));
        assertTrue(!layout.contains("filter_sheet_subtitle"));
        assertTrue(!layout.contains("android:id=\"@+id/filter_reset_button\""));
        assertTrue(!layout.contains("android:id=\"@+id/filter_apply_button\""));
        assertTrue(layout.contains("android:layout_marginTop=\"12dp\""));
        assertTrue(strings.contains("filter_show_system_apps"));
        assertTrue(strings.contains("filter_injected_only"));
        assertTrue(strings.contains("filter_width_only"));
        assertTrue(strings.contains("filter_font_only"));
        assertTrue(!strings.contains("filter_sheet_subtitle"));
        assertTrue(!strings.contains("filter_reset_button"));
        assertTrue(!strings.contains("filter_apply_button"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
