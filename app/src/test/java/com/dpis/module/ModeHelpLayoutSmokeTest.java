package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class ModeHelpLayoutSmokeTest {
    @Test
    public void modeHelpLayoutContainsRouteCardsAndToolbar() throws IOException {
        String source = read("src/main/java/com/dpis/module/ModeHelpActivity.java");
        String layout = read("src/main/res/layout/activity_mode_help.xml");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(layout.contains("android:id=\"@+id/mode_help_toolbar\""));
        assertTrue(layout.contains("android:id=\"@+id/mode_help_back_button\""));
        assertTrue(layout.contains("@layout/item_mode_help_system"));
        assertTrue(layout.contains("@layout/item_mode_help_compat"));
        assertTrue(source.contains("setContentView(R.layout.activity_mode_help)"));
        assertTrue(strings.contains("name=\"help_tutorial_system_badge\""));
        assertTrue(strings.contains("name=\"help_tutorial_compat_badge\""));
        assertTrue(!strings.contains("name=\"help_tutorial_message\""));
    }

    @Test
    public void modeHelpCardsUseBadgeSummaryAndBulletHierarchy() throws IOException {
        String system = read("src/main/res/layout/item_mode_help_system.xml");
        String compat = read("src/main/res/layout/item_mode_help_compat.xml");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(system.contains("@layout/item_mode_help_system_header"));
        assertTrue(system.contains("@string/help_tutorial_system_summary"));
        assertTrue(system.contains("@string/help_tutorial_system_points"));
        assertTrue(compat.contains("@layout/item_mode_help_compat_header"));
        assertTrue(compat.contains("@string/help_tutorial_compat_summary"));
        assertTrue(compat.contains("@string/help_tutorial_compat_points"));
        assertTrue(strings.contains("name=\"help_tutorial_system_summary\""));
        assertTrue(strings.contains("name=\"help_tutorial_compat_summary\""));
    }

    @Test
    public void modeHelpBackgroundsUseThemeColorResourcesForDayNight() throws IOException {
        String systemCard = read("src/main/res/drawable/help_tutorial_system_card_background.xml");
        String compatCard = read("src/main/res/drawable/help_tutorial_compat_card_background.xml");
        String systemBadge = read("src/main/res/drawable/help_tutorial_system_badge_background.xml");
        String compatBadge = read("src/main/res/drawable/help_tutorial_compat_badge_background.xml");
        String dayColors = read("src/main/res/values/colors.xml");
        String nightColors = read("src/main/res/values-night/colors.xml");

        assertTrue(systemCard.contains("@color/help_tutorial_system_card_container"));
        assertTrue(systemCard.contains("@color/help_tutorial_system_card_stroke"));
        assertTrue(compatCard.contains("@color/help_tutorial_compat_card_container"));
        assertTrue(compatCard.contains("@color/help_tutorial_compat_card_stroke"));
        assertTrue(systemBadge.contains("@color/help_tutorial_system_badge_container"));
        assertTrue(compatBadge.contains("@color/help_tutorial_compat_badge_container"));
        assertTrue(dayColors.contains("name=\"help_tutorial_system_card_container\""));
        assertTrue(dayColors.contains("name=\"help_tutorial_system_card_stroke\""));
        assertTrue(dayColors.contains("name=\"help_tutorial_system_badge_container\""));
        assertTrue(dayColors.contains("name=\"help_tutorial_system_badge_text\""));
        assertTrue(dayColors.contains("name=\"help_tutorial_compat_card_container\""));
        assertTrue(dayColors.contains("name=\"help_tutorial_compat_card_stroke\""));
        assertTrue(dayColors.contains("name=\"help_tutorial_compat_badge_container\""));
        assertTrue(dayColors.contains("name=\"help_tutorial_compat_badge_text\""));
        assertTrue(nightColors.contains("name=\"help_tutorial_system_card_container\""));
        assertTrue(nightColors.contains("name=\"help_tutorial_system_card_stroke\""));
        assertTrue(nightColors.contains("name=\"help_tutorial_system_badge_container\""));
        assertTrue(nightColors.contains("name=\"help_tutorial_system_badge_text\""));
        assertTrue(nightColors.contains("name=\"help_tutorial_compat_card_container\""));
        assertTrue(nightColors.contains("name=\"help_tutorial_compat_card_stroke\""));
        assertTrue(nightColors.contains("name=\"help_tutorial_compat_badge_container\""));
        assertTrue(nightColors.contains("name=\"help_tutorial_compat_badge_text\""));
    }

    @Test
    public void modeHelpUsesSharedInsetsForCardContent() throws IOException {
        String layout = read("src/main/res/layout/activity_mode_help.xml");
        String system = read("src/main/res/layout/item_mode_help_system.xml");

        assertTrue(layout.contains("@dimen/home_workspace_padding_top"));
        assertTrue(layout.contains("@dimen/home_workspace_padding_horizontal"));
        assertTrue(system.contains("@dimen/help_tutorial_card_padding"));
        assertTrue(system.contains("@dimen/help_tutorial_body_spacing_top"));
        assertTrue(layout.contains("@dimen/home_workspace_padding_bottom"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
