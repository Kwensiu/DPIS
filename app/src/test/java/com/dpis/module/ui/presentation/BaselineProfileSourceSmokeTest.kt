package com.dpis.module.ui.presentation

import com.dpis.module.SourceSmokeTestPaths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BaselineProfileSourceSmokeTest {
    @Test
    fun workspaceNavigationExposesLocaleIndependentTags() {
        val shell =
            readApp("src/main/java/com/dpis/module/ui/presentation/workspace/WorkspaceShell.kt")
        val design = readApp(
            "src/main/java/com/dpis/module/ui/presentation/design/ComposeDesignSystem.kt",
        )
        val notice = readApp(
            "src/main/java/com/dpis/module/tools/presentation/LocalToolDialogs.kt",
        )

        assertTrue(shell.contains("APP(R.string.workspace_app, R.drawable.ic_apps_24, \"workspace-nav-app\")"))
        assertTrue(shell.contains("TEMPLATE(R.string.workspace_template, R.drawable.ic_template_24, \"workspace-nav-template\")"))
        assertTrue(shell.contains("HOME(R.string.workspace_home, R.drawable.ic_home_24, \"workspace-nav-home\")"))
        assertTrue(shell.contains("TOOLS(R.string.workspace_tools, R.drawable.ic_build_24, \"workspace-nav-tools\")"))
        assertTrue(shell.contains("SETTINGS(R.string.workspace_settings, R.drawable.ic_settings_24, \"workspace-nav-settings\")"))
        assertTrue(shell.contains("val testTag: String"))
        assertTrue(shell.contains("Modifier.testTag(destination.testTag)"))
        assertTrue(shell.contains("testTag(\"workspace-nav-menu\")"))
        assertTrue(shell.contains("testTagsAsResourceId = true"))
        assertTrue(design.contains("testTagsAsResourceId = true"))
        assertTrue(notice.contains("testTag(\"runtime-reload-notice-ack\")"))
    }

    @Test
    fun generatorFollowsOfficialStartupCollect() {
        val generator = SourceSmokeTestPaths.readRepositoryRoot(
            "baselineprofile/src/main/java/com/dpis/module/baselineprofile/DpisBaselineProfileGenerator.kt",
        )
        val moduleGradle =
            SourceSmokeTestPaths.readRepositoryRoot("baselineprofile/build.gradle.kts")
        val appGradle = SourceSmokeTestPaths.readRepositoryRoot("app/build.gradle.kts")
        val catalog = SourceSmokeTestPaths.readRepositoryRoot("gradle/libs.versions.toml")

        assertTrue(generator.contains("baselineProfileRule.collect("))
        assertTrue(generator.contains("includeInStartupProfile = true"))
        assertTrue(generator.contains("startManagerForBaselineProfile()"))
        assertFalse(generator.contains("shortenUiAutomatorIdleTimeouts()"))
        assertTrue(generator.contains("targetPackageName()"))
        assertFalse(generator.contains("startActivityAndWait()"))
        assertFalse(generator.contains("APPS_NAV_X"))
        assertFalse(generator.contains("io.github.kwensiu.dpis"))
        assertTrue(
            generator.indexOf("includeInStartupProfile = true") ==
                    generator.lastIndexOf("includeInStartupProfile = true")
        )
        assertTrue(moduleGradle.contains("animationsDisabled = true"))
        assertTrue(moduleGradle.contains("androidx.benchmark.suppressErrors"))
        assertTrue(appGradle.contains("implementation(libs.androidx.profileinstaller)"))
        assertTrue(catalog.contains("androidx-profileinstaller"))
    }

    private fun readApp(relativePath: String): String = SourceSmokeTestPaths.read(relativePath)
}
