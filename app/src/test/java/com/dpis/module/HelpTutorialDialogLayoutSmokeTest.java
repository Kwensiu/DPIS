package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class HelpTutorialDialogLayoutSmokeTest {
    @Test
    public void helpTutorialDialogLayoutContainsTwoCardsAndConfirmButton() throws IOException {
        String layout = read("src/main/res/layout/dialog_help_tutorial.xml");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(layout.contains("android:id=\"@+id/help_tutorial_system_card\""));
        assertTrue(layout.contains("android:id=\"@+id/help_tutorial_compat_card\""));
        assertTrue(layout.contains("android:id=\"@+id/help_tutorial_confirm_button\""));
        assertTrue(strings.contains("name=\"help_tutorial_system_badge\""));
        assertTrue(strings.contains("name=\"help_tutorial_compat_badge\""));
        assertTrue(!strings.contains("name=\"help_tutorial_message\""));
    }

    @Test
    public void helpTutorialDialogCardsUseBadgeSummaryAndBulletHierarchy() throws IOException {
        String layout = read("src/main/res/layout/dialog_help_tutorial.xml");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(layout.contains("android:id=\"@+id/help_tutorial_system_badge\""));
        assertTrue(layout.contains("android:id=\"@+id/help_tutorial_system_summary\""));
        assertTrue(layout.contains("android:id=\"@+id/help_tutorial_compat_badge\""));
        assertTrue(layout.contains("android:id=\"@+id/help_tutorial_compat_summary\""));
        assertTrue(strings.contains("name=\"help_tutorial_system_summary\""));
        assertTrue(strings.contains("name=\"help_tutorial_compat_summary\""));
    }

    @Test
    public void helpTutorialDialogBackgroundsUseThemeColorResourcesForDayNight() throws IOException {
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
    public void helpTutorialDialogUsesSameTopAndHorizontalInsetsForCardContent() throws IOException {
        String layout = read("src/main/res/layout/dialog_help_tutorial.xml");

        assertTrue(layout.contains("android:paddingTop=\"20dp\""));
        assertTrue(layout.contains("android:paddingStart=\"20dp\""));
        assertTrue(layout.contains("android:paddingEnd=\"20dp\""));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
