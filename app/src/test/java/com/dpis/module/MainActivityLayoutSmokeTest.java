package com.dpis.module;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.IOException;

import org.junit.Test;

public class MainActivityLayoutSmokeTest {
    @Test
    public void activityStatusLayoutKeepsQuickSearchFabWithExpectedIcon() throws IOException {
        String layout = read("src/main/res/layout/activity_status.xml");

        assertTrue(countMatches(layout, "android:id=\"@+id/search_focus_fab\"") == 1);
        assertTrue(layout.contains("android:contentDescription=\"@string/quick_search_button\""));
        assertTrue(layout.contains("app:srcCompat=\"@drawable/ic_search_24\""));
    }

    @Test
    public void activityStatusLayoutContainsSearchFilterSettingsAndPager() throws IOException {
        String layout = read("src/main/res/layout/activity_status.xml");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(layout.contains("android:id=\"@+id/search_input\""));
        assertTrue(layout.contains("android:id=\"@+id/search_filter_button\""));
        assertTrue(layout.contains("android:id=\"@+id/search_focus_fab\""));
        assertTrue(countMatches(layout, "app:elevation=\"@dimen/floating_actions_elevation\"") == 1);
        assertTrue(countMatches(layout,
                "app:hoveredFocusedTranslationZ=\"@dimen/floating_actions_hovered_focused_translation_z\"") == 1);
        assertTrue(countMatches(layout,
                "app:pressedTranslationZ=\"@dimen/floating_actions_pressed_translation_z\"") == 1);
        assertTrue(countMatches(layout, "app:rippleColor=\"@color/dpis_fab_ripple\"") == 1);
        assertTrue(layout.contains("android:id=\"@+id/top_container\""));
        assertTrue(layout.contains("@dimen/main_toolbar_padding_horizontal"));
        assertTrue(layout.contains("@dimen/main_toolbar_padding_top"));
        assertTrue(layout.contains("@dimen/main_search_card_height"));
        assertTrue(layout.contains("@dimen/main_search_icon_padding_start"));
        assertTrue(layout.contains("@dimen/main_search_icon_padding_end"));
        assertTrue(layout.contains("@dimen/main_search_action_pair_padding"));
        assertTrue(layout.contains("@dimen/main_search_action_icon_padding_start"));
        assertTrue(layout.contains("@dimen/main_search_action_icon_padding_end"));
        assertTrue(layout.contains("@dimen/main_search_action_icon_padding_vertical"));
        assertTrue(layout.contains("android:src=\"@drawable/ic_search_24\""));
        assertTrue(layout.contains("android:src=\"@drawable/ic_tune_24\""));
        assertTrue(layout.contains("<include layout=\"@layout/settings_workspace\""));
        assertTrue(layout.contains("<include layout=\"@layout/template_workspace\""));
        assertTrue(layout.contains("<include layout=\"@layout/tools_workspace\""));
        assertTrue(layout.contains("android:id=\"@+id/workspace_switch\""));
        assertTrue(layout.contains("com.google.android.material.bottomnavigation.BottomNavigationView"));
        assertTrue(layout.contains("android:layout_height=\"wrap_content\""));
        assertTrue(layout.contains("android:minHeight=\"@dimen/main_workspace_navigation_height\""));
        assertTrue(layout.contains("android:saveEnabled=\"false\""));
        assertTrue(layout.contains("app:menu=\"@menu/main_workspace_navigation\""));
        assertTrue(layout.contains("app:labelVisibilityMode=\"selected\""));
        assertTrue(layout.contains("android:background=\"?attr/colorSurfaceContainer\""));
        assertFalse(layout.contains("@drawable/bg_workspace_navigation"));
        String workspaceMenu = read("src/main/res/menu/main_workspace_navigation.xml");
        assertTrue(workspaceMenu.contains("android:id=\"@+id/workspace_app_button\""));
        assertTrue(workspaceMenu.contains("android:id=\"@+id/workspace_template_button\""));
        assertTrue(workspaceMenu.contains("android:id=\"@+id/workspace_tools_button\""));
        assertTrue(workspaceMenu.contains("android:icon=\"@drawable/ic_apps_24\""));
        assertTrue(workspaceMenu.contains("android:icon=\"@drawable/ic_template_24\""));
        assertTrue(workspaceMenu.contains("android:icon=\"@drawable/ic_build_24\""));
        assertTrue(layout.contains("android:visibility=\"gone\""));
        assertTrue(strings.contains("tab_all_apps"));
        assertTrue(strings.contains("workspace_app"));
        assertTrue(strings.contains("workspace_template"));
        assertTrue(strings.contains("template_workspace_global_prefill_title"));
        assertTrue(strings.contains("template_workspace_quick_templates_title"));
        assertTrue(strings.contains("quick_search_button"));
        assertTrue(SourceSmokeTestPaths.exists("src/main/res/drawable/ic_tune_24.xml"));
    }

    @Test
    public void workspaceSwitchDoesNotRestorePlatformViewStateAcrossOrientation()
            throws IOException {
        String portraitLayout = read("src/main/res/layout/activity_status.xml");
        String landscapeLayout = read("src/main/res/layout-land/activity_status.xml");
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(portraitLayout.contains("android:id=\"@+id/workspace_switch\""));
        assertTrue(landscapeLayout.contains("android:id=\"@+id/workspace_switch\""));
        assertTrue(landscapeLayout.contains("android:id=\"@+id/workspace_switch_scroll\""));
        assertTrue(portraitLayout.contains("android:saveEnabled=\"false\""));
        assertTrue(landscapeLayout.contains("android:saveEnabled=\"false\""));
        assertFalse(source.contains("workspaceSwitch.setSaveFromParentEnabled(false);"));
    }

    @Test
    public void toolsWorkspaceKeepsExpandedCardsInsideScrollableContent()
            throws IOException {
        String layout = read("src/main/res/layout/tools_workspace.xml");
        String dimensions = read("src/main/res/values/dimens.xml");
        String source = read("src/main/java/com/dpis/module/settings/SystemFontScaleToolBinder.java");

        assertTrue(layout.contains("android:id=\"@+id/tools_toolbar\""));
        assertTrue(layout.contains("android:id=\"@+id/tools_workspace_scroll\""));
        assertTrue(layout.contains("android:layout_height=\"0dp\""));
        assertTrue(layout.contains("android:layout_weight=\"1\""));
        assertTrue(layout.contains("android:clipToPadding=\"false\""));
        assertTrue(layout.contains("android:id=\"@+id/tools_workspace_content\""));
        assertTrue(layout.contains("android:paddingStart=\"@dimen/template_workspace_padding_horizontal\""));
        assertTrue(layout.contains("android:paddingBottom=\"@dimen/tools_workspace_content_padding_bottom\""));
        assertTrue(layout.contains("android:id=\"@+id/system_font_scale_card\""));
        assertTrue(layout.contains("android:id=\"@+id/system_font_scale_operation_group\""));
        assertTrue(layout.contains("android:id=\"@+id/system_font_scale_permission_overlay\""));
        assertTrue(dimensions.contains("tools_workspace_content_padding_bottom"));
        assertTrue(source.contains("revealExpandedPanel();"));
        assertTrue(source.contains("requestRectangleOnScreen"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
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
