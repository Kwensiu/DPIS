package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class ModeHelpLayoutSmokeTest {
    @Test
    public void modeHelpLayoutContainsRouteCardsAndToolbar() throws IOException {
        String source = read("src/main/java/com/dpis/module/home/ModeHelpActivity.java");
        String layout = read("src/main/res/layout/activity_mode_help.xml");
        String guideSource = read("src/main/java/com/dpis/module/home/ModeGuideActivity.java");
        String guideLayout = read("src/main/res/layout/activity_mode_guide.xml");
        String manifest = read("src/main/AndroidManifest.xml");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(layout.contains("android:id=\"@+id/mode_help_toolbar\""));
        assertTrue(layout.contains("android:id=\"@+id/mode_help_back_button\""));
        assertTrue(layout.contains("@drawable/bg_round_button_surface"));
        assertTrue(layout.contains("@style/TextAppearance.Material3.HeadlineLarge"));
        assertTrue(layout.contains("android:id=\"@+id/mode_help_mode_guide_card\""));
        assertTrue(layout.contains("@string/mode_help_tip_font_lag_question"));
        assertTrue(layout.contains("@string/mode_help_tip_font_lag_steps"));
        assertTrue(layout.contains("@string/mode_help_tip_font_lag_reason"));
        assertTrue(!layout.contains("@string/mode_help_tip_resources_font_title"));
        assertTrue(layout.contains("@string/mode_help_mode_guide_entry_title"));
        assertTrue(!layout.contains("@string/mode_help_subtitle"));
        assertTrue(!layout.contains("@layout/item_mode_help_system"));
        assertTrue(!layout.contains("@layout/item_mode_help_compat"));
        assertTrue(source.contains("setContentView(R.layout.activity_mode_help)"));
        assertTrue(source.contains("new Intent(this, ModeGuideActivity.class)"));
        assertTrue(source.contains("WindowInsetsBinder.applySafeDrawingPadding(toolbar, false, true, false, false);"));
        assertTrue(source.contains("WindowInsetsBinder.applySafeDrawingPadding(content, false, false, false, true);"));
        assertTrue(guideSource.contains("setContentView(R.layout.activity_mode_guide)"));
        assertTrue(guideLayout.contains("android:id=\"@+id/mode_guide_toolbar\""));
        assertTrue(guideLayout.contains("android:id=\"@+id/mode_guide_back_button\""));
        assertTrue(guideLayout.contains("@layout/item_mode_help_system"));
        assertTrue(guideLayout.contains("@layout/item_mode_help_compat"));
        assertTrue(manifest.contains("android:name=\".home.ModeGuideActivity\""));
        assertTrue(strings.contains("name=\"help_tutorial_system_badge\""));
        assertTrue(strings.contains("name=\"help_tutorial_compat_badge\""));
        assertTrue(strings.contains("name=\"mode_help_tip_font_lag_reason\""));
        assertTrue(strings.contains("resources_font"));
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
    public void modeHelpUsesCompactPageSpacingForQuickAdvice() throws IOException {
        String layout = read("src/main/res/layout/activity_mode_help.xml");
        String guideLayout = read("src/main/res/layout/activity_mode_guide.xml");
        String system = read("src/main/res/layout/item_mode_help_system.xml");

        assertTrue(layout.contains("android:id=\"@+id/mode_help_tip_group\""));
        assertTrue(layout.contains("android:id=\"@+id/mode_help_tip_item_font_lag\""));
        assertTrue(!layout.contains("android:id=\"@+id/mode_help_tip_card\""));
        assertTrue(layout.contains("@drawable/bg_mode_help_tip_marker"));
        assertTrue(layout.contains("android:id=\"@+id/mode_help_mode_guide_card\""));
        assertTrue(layout.contains("com.google.android.material.card.MaterialCardView"));
        assertTrue(layout.contains("@dimen/page_toolbar_padding_top"));
        assertTrue(layout.contains("@dimen/page_back_button_size"));
        assertTrue(layout.contains("@dimen/page_title_spacing_start"));
        assertTrue(layout.contains("@dimen/mode_help_content_spacing_top"));
        assertTrue(layout.contains("@dimen/mode_help_tip_item_padding_vertical"));
        assertTrue(layout.contains("@dimen/mode_help_tip_marker_spacing_end"));
        assertTrue(layout.contains("@dimen/mode_help_text_spacing_top"));
        assertTrue(layout.contains("@dimen/mode_help_reason_spacing_top"));
        assertTrue(layout.contains("@dimen/mode_help_card_padding_vertical"));
        assertTrue(layout.contains("@dimen/mode_help_entry_min_height"));
        assertTrue(layout.contains("@dimen/home_workspace_padding_horizontal"));
        assertTrue(guideLayout.contains("@dimen/home_workspace_padding_top"));
        assertTrue(guideLayout.contains("@dimen/home_workspace_padding_horizontal"));
        assertTrue(system.contains("@dimen/help_tutorial_card_padding"));
        assertTrue(system.contains("@dimen/help_tutorial_body_spacing_top"));
        assertTrue(layout.contains("@dimen/home_workspace_padding_bottom"));
        assertTrue(guideLayout.contains("@dimen/home_workspace_padding_bottom"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
