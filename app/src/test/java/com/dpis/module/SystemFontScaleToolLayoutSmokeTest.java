package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class SystemFontScaleToolLayoutSmokeTest {
    @Test
    public void toolsWorkspaceContainsSystemFontScaleToolSurface() throws IOException {
        String layout = read("src/main/res/layout/tools_workspace.xml");

        assertTrue(layout.contains("android:id=\"@+id/system_font_scale_card\""));
        assertTrue(layout.contains("android:id=\"@+id/system_font_scale_apply_button\""));
        assertTrue(layout.contains("android:background=\"@drawable/bg_round_button_surface\""));
        assertTrue(layout.contains("android:id=\"@+id/system_font_scale_permission_overlay\""));
        assertTrue(layout.contains("android:id=\"@+id/system_font_scale_unavailable_overlay\""));
        assertTrue(layout.contains("android:id=\"@+id/system_font_scale_seek_bar\""));
        assertTrue(layout.contains("android:id=\"@+id/system_font_scale_decrement_button\""));
        assertTrue(layout.contains("android:id=\"@+id/system_font_scale_increment_button\""));
        assertTrue(layout.contains("android:id=\"@+id/system_font_scale_preview_title\""));
        assertTrue(layout.contains("android:id=\"@+id/system_font_scale_preview_body\""));
        assertTrue(layout.contains("android:id=\"@+id/system_font_scale_restore_button\""));
    }

    @Test
    public void systemFontScaleStringsAreLocalized() throws IOException {
        String defaultStrings = read("src/main/res/values/strings.xml");
        String chineseStrings = read("src/main/res/values-zh-rCN/strings.xml");

        assertTrue(defaultStrings.contains("system_font_scale_title"));
        assertTrue(defaultStrings.contains("system_font_scale_badge_out_of_range"));
        assertTrue(defaultStrings.contains("system_font_scale_write_failed"));
        assertTrue(chineseStrings.contains("system_font_scale_title"));
        assertTrue(chineseStrings.contains("system_font_scale_badge_out_of_range"));
        assertTrue(chineseStrings.contains("system_font_scale_write_failed"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
