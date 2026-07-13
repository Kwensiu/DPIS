package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class ModeHelpLayoutSmokeTest {
    @Test
    public void helpAndGuidePreserveNavigationAndAllGuideContentInCompose() throws IOException {
        String helpSource = read("src/main/java/com/dpis/module/home/ModeHelpActivity.java");
        String guideSource = read("src/main/java/com/dpis/module/home/ModeGuideActivity.java");
        String activityContent = read("src/main/java/com/dpis/module/ui/compose/SupportActivityContent.kt");
        String compose = read("src/main/java/com/dpis/module/ui/compose/SupportPages.kt");
        String manifest = read("src/main/AndroidManifest.xml");

        assertTrue(helpSource.contains("SupportActivityContent.installModeHelp(this);"));
        assertTrue(guideSource.contains("SupportActivityContent.installModeGuide(this);"));
        assertTrue(activityContent.contains("Intent(activity, ModeGuideActivity::class.java)"));
        assertTrue(compose.contains("fun ModeHelpPage(onBack: () -> Unit, onOpenModeGuide: () -> Unit)"));
        assertTrue(compose.contains("fun ModeGuidePage(onBack: () -> Unit)"));
        assertTrue(compose.contains("Modifier.height(IntrinsicSize.Min)"));
        assertTrue(compose.contains(".fillMaxHeight()"));
        assertTrue(compose.contains("rememberDpisConfirmAction"));
        assertTrue(compose.contains("R.string.mode_help_tip_font_lag_question"));
        assertTrue(compose.contains("R.string.mode_help_tip_font_lag_steps"));
        assertTrue(compose.contains("R.string.mode_help_tip_font_lag_reason"));
        assertTrue(compose.contains("R.string.help_tutorial_system_summary"));
        assertTrue(compose.contains("R.string.help_tutorial_compat_summary"));
        assertTrue(compose.contains("R.string.help_tutorial_scale_summary"));
        assertTrue(compose.contains("R.string.help_tutorial_width_summary"));
        assertTrue(compose.contains("R.string.help_tutorial_font_hooks_summary"));
        assertTrue(compose.contains("R.string.help_tutorial_typeface_summary"));
        assertTrue(manifest.contains("android:name=\".home.ModeHelpActivity\""));
        assertTrue(manifest.contains("android:name=\".home.ModeGuideActivity\""));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
