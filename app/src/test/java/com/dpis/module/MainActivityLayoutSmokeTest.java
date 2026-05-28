package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class MainActivityLayoutSmokeTest {
    @Test
    public void activityStatusLayoutKeepsSingleHelpFabWithExpectedIcon() throws IOException {
        String layout = read("src/main/res/layout/activity_status.xml");

        assertTrue(countMatches(layout, "android:id=\"@+id/help_fab\"") == 1);
        assertTrue(layout.contains("android:contentDescription=\"@string/help_button\""));
        assertTrue(layout.contains("app:srcCompat=\"@drawable/ic_info_24\""));
    }

    @Test
    public void activityStatusLayoutContainsSearchFilterSettingsAndPager() throws IOException {
        String layout = read("src/main/res/layout/activity_status.xml");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(layout.contains("android:id=\"@+id/search_input\""));
        assertTrue(layout.contains("android:id=\"@+id/search_filter_button\""));
        assertTrue(layout.contains("android:id=\"@+id/search_focus_fab\""));
        assertTrue(countMatches(layout, "app:elevation=\"@dimen/floating_actions_elevation\"") == 2);
        assertTrue(countMatches(layout,
                "app:hoveredFocusedTranslationZ=\"@dimen/floating_actions_hovered_focused_translation_z\"") == 2);
        assertTrue(countMatches(layout,
                "app:pressedTranslationZ=\"@dimen/floating_actions_pressed_translation_z\"") == 2);
        assertTrue(countMatches(layout, "app:rippleColor=\"@color/dpis_fab_ripple\"") == 2);
        assertTrue(layout.contains("android:id=\"@+id/top_container\""));
        assertTrue(layout.contains("android:paddingStart=\"@dimen/main_toolbar_padding_horizontal\""));
        assertTrue(layout.contains("android:paddingTop=\"@dimen/main_toolbar_padding_top\""));
        assertTrue(layout.contains("@dimen/main_search_card_height"));
        assertTrue(layout.contains("@dimen/main_search_action_button_size"));
        assertTrue(layout.contains("@dimen/main_tabs_indicator_height"));
        assertTrue(layout.contains("android:src=\"@drawable/ic_search_24\""));
        assertTrue(layout.contains("android:src=\"@drawable/ic_tune_24\""));
        assertTrue(layout.contains("android:id=\"@+id/system_settings_button\""));
        assertTrue(layout.contains("android:id=\"@+id/app_pager\""));
        assertTrue(layout.contains("android:id=\"@+id/app_workspace_divider\""));
        assertTrue(layout.contains("<include layout=\"@layout/template_workspace\""));
        assertTrue(layout.contains("android:id=\"@+id/workspace_switch\""));
        assertTrue(layout.contains("android:id=\"@+id/workspace_app_button\""));
        assertTrue(layout.contains("android:id=\"@+id/workspace_template_button\""));
        assertTrue(layout.contains("app:selectionRequired=\"true\""));
        assertTrue(layout.contains("app:singleSelection=\"true\""));
        assertTrue(layout.contains("android:visibility=\"gone\""));
        assertTrue(strings.contains("tab_all_apps"));
        assertTrue(strings.contains("workspace_app"));
        assertTrue(strings.contains("workspace_template"));
        assertTrue(strings.contains("template_workspace_global_prefill_title"));
        assertTrue(strings.contains("template_workspace_quick_templates_title"));
        assertTrue(strings.contains("quick_search_button"));
        assertTrue(Files.exists(Path.of("src/main/res/drawable/ic_tune_24.xml")));
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
