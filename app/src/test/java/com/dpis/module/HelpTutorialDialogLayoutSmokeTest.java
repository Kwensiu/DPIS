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

        assertTrue(layout.contains("android:id=\"@+id/help_tutorial_emulation_card\""));
        assertTrue(layout.contains("android:id=\"@+id/help_tutorial_replace_card\""));
        assertTrue(layout.contains("android:id=\"@+id/help_tutorial_confirm_button\""));
        assertTrue(strings.contains("name=\"help_tutorial_emulation_badge\""));
        assertTrue(strings.contains("name=\"help_tutorial_replace_badge\""));
        assertTrue(!strings.contains("name=\"help_tutorial_message\""));
    }

    @Test
    public void helpTutorialDialogCardsUseBadgeSummaryAndBulletHierarchy() throws IOException {
        String layout = read("src/main/res/layout/dialog_help_tutorial.xml");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(layout.contains("android:id=\"@+id/help_tutorial_emulation_badge\""));
        assertTrue(layout.contains("android:id=\"@+id/help_tutorial_emulation_summary\""));
        assertTrue(layout.contains("android:id=\"@+id/help_tutorial_replace_badge\""));
        assertTrue(layout.contains("android:id=\"@+id/help_tutorial_replace_summary\""));
        assertTrue(strings.contains("通过系统层链路伪装相关参数"));
        assertTrue(strings.contains("通过字段重写直接覆盖缩放"));
    }

    @Test
    public void helpTutorialDialogBackgroundsUseThemeColorResourcesForDayNight() throws IOException {
        String emulationCard = read("src/main/res/drawable/help_tutorial_emulation_card_background.xml");
        String replaceCard = read("src/main/res/drawable/help_tutorial_replace_card_background.xml");
        String emulationBadge = read("src/main/res/drawable/help_tutorial_emulation_badge_background.xml");
        String replaceBadge = read("src/main/res/drawable/help_tutorial_replace_badge_background.xml");
        String dayColors = read("src/main/res/values/colors.xml");
        String nightColors = read("src/main/res/values-night/colors.xml");

        assertTrue(emulationCard.contains("@color/help_tutorial_emulation_card_container"));
        assertTrue(emulationCard.contains("@color/help_tutorial_emulation_card_stroke"));
        assertTrue(replaceCard.contains("@color/help_tutorial_replace_card_container"));
        assertTrue(replaceCard.contains("@color/help_tutorial_replace_card_stroke"));
        assertTrue(emulationBadge.contains("@color/help_tutorial_emulation_badge_container"));
        assertTrue(replaceBadge.contains("@color/help_tutorial_replace_badge_container"));
        assertTrue(dayColors.contains("name=\"help_tutorial_emulation_card_container\""));
        assertTrue(dayColors.contains("name=\"help_tutorial_emulation_card_stroke\""));
        assertTrue(dayColors.contains("name=\"help_tutorial_emulation_badge_container\""));
        assertTrue(dayColors.contains("name=\"help_tutorial_emulation_badge_text\""));
        assertTrue(dayColors.contains("name=\"help_tutorial_replace_card_container\""));
        assertTrue(dayColors.contains("name=\"help_tutorial_replace_card_stroke\""));
        assertTrue(dayColors.contains("name=\"help_tutorial_replace_badge_container\""));
        assertTrue(dayColors.contains("name=\"help_tutorial_replace_badge_text\""));
        assertTrue(nightColors.contains("name=\"help_tutorial_emulation_card_container\""));
        assertTrue(nightColors.contains("name=\"help_tutorial_emulation_card_stroke\""));
        assertTrue(nightColors.contains("name=\"help_tutorial_emulation_badge_container\""));
        assertTrue(nightColors.contains("name=\"help_tutorial_emulation_badge_text\""));
        assertTrue(nightColors.contains("name=\"help_tutorial_replace_card_container\""));
        assertTrue(nightColors.contains("name=\"help_tutorial_replace_card_stroke\""));
        assertTrue(nightColors.contains("name=\"help_tutorial_replace_badge_container\""));
        assertTrue(nightColors.contains("name=\"help_tutorial_replace_badge_text\""));
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
