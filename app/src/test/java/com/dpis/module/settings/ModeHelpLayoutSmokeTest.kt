package com.dpis.module

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeHelpLayoutSmokeTest {
    @Test
    fun helpAndGuidePreserveNavigationAndAllGuideContentInCompose() {
        val helpSource = read("src/main/java/com/dpis/module/home/ModeHelpActivity.java")
        val guideSource = read("src/main/java/com/dpis/module/home/ModeGuideActivity.java")
        val activityContent = read("src/main/java/com/dpis/module/about/presentation/SupportActivityContent.kt")
        val compose = read("src/main/java/com/dpis/module/about/presentation/SupportPages.kt")
        val cards = read("src/main/java/com/dpis/module/about/presentation/SupportCards.kt")
        val manifest = read("src/main/AndroidManifest.xml")
        val mainActivity = read("src/main/java/com/dpis/module/MainActivity.java")

        assertTrue(helpSource.contains("SupportActivityContent.installModeHelp(this);"))
        assertTrue(guideSource.contains("SupportActivityContent.installModeGuide(this);"))
        assertTrue(activityContent.contains("Intent(activity, ModeGuideActivity::class.java)"))
        assertTrue(compose.contains("fun ModeHelpPage(onBack: () -> Unit, onOpenModeGuide: () -> Unit)"))
        assertTrue(compose.contains("fun ModeGuidePage(onBack: () -> Unit)"))
        assertTrue(compose.contains("ModeHelpPage") && compose.contains("ModeGuidePage"))
        assertTrue(compose.contains("Modifier.height(IntrinsicSize.Min)"))
        assertTrue(compose.contains(".fillMaxHeight()"))
        assertTrue(compose.contains("dpisClickable"))
        assertTrue(compose.contains("R.string.mode_help_tip_font_lag_question"))
        assertTrue(compose.contains("R.string.mode_help_tip_font_lag_steps"))
        assertTrue(compose.contains("R.string.mode_help_tip_font_lag_reason"))
        assertTrue(compose.contains("R.string.help_tutorial_system_summary") || cards.contains("R.string.help_tutorial_system_summary"))
        assertTrue(compose.contains("R.string.help_tutorial_compat_summary") || cards.contains("R.string.help_tutorial_compat_summary"))
        assertTrue(compose.contains("R.string.help_tutorial_scale_summary") || cards.contains("R.string.help_tutorial_scale_summary"))
        assertTrue(compose.contains("R.string.help_tutorial_width_summary") || cards.contains("R.string.help_tutorial_width_summary"))
        assertTrue(compose.contains("R.string.help_tutorial_font_hooks_summary") || cards.contains("R.string.help_tutorial_font_hooks_summary"))
        assertTrue(compose.contains("R.string.help_tutorial_typeface_summary") || cards.contains("R.string.help_tutorial_typeface_summary"))
        assertTrue(manifest.contains("android:name=\".home.ModeHelpActivity\""))
        assertTrue(manifest.contains("android:name=\".home.ModeGuideActivity\""))
        assertTrue(mainActivity.contains("startActivity(new Intent(MainActivity.this, ModeHelpActivity.class));"))
        assertFalse(mainActivity.contains("MainStandaloneRoute"))
    }

    private fun read(relativePath: String): String = SourceSmokeTestPaths.read(relativePath)
}
