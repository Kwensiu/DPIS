package com.dpis.module

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolsWorkspaceBinderSourceSmokeTest {
    @Test
    fun mainActivityWiresToolsWorkspaceLifecycle() {
        val startup = read(
            "src/main/java/com/dpis/module/ui/presentation/MainStartupSession.kt"
        )
        val workspaceSession = read(
            "src/main/java/com/dpis/module/ui/presentation/MainWorkspaceSession.kt"
        )
        val hostWiring = read(
            "src/main/java/com/dpis/module/ui/presentation/MainHostWiringSession.kt"
        )
        val workspace = read("src/main/java/com/dpis/module/settings/presentation/ToolsWorkspace.kt")

        assertTrue(hostWiring.contains("var toolsWorkspace: ToolsWorkspace?"))
        assertTrue(hostWiring.contains("toolsWorkspace = ToolsWorkspace("))
        assertTrue(workspace.contains("SystemFontScaleToolPresenter("))
        assertFalse(workspace.contains("ToolsWorkspaceBinder"))
        assertTrue(workspaceSession.contains("fun bindToolsWorkspace("))
        assertTrue(startup.contains("mainWorkspaceSession.bindForLifecycle("))
        assertTrue(startup.contains("hostWiringSession.toolsWorkspace?.onStart()"))
        assertTrue(startup.contains("hostWiringSession.toolsWorkspace?.onResume()"))
        assertTrue(startup.contains("hostWiringSession.toolsWorkspace?.onStop()"))
        assertTrue(
            startup.contains(
                "hostWiringSession.toolsWorkspace?.onActivityResult(requestCode, resultCode, data)",
            )
        )
    }

    @Test
    fun composeToolsWorkspaceOwnsFontScalePermissionAndApply() {
        val content = read(
            "src/main/java/com/dpis/module/tools/presentation/ToolsWorkspaceContent.kt"
        )
        val workspace = read("src/main/java/com/dpis/module/settings/presentation/ToolsWorkspace.kt")

        assertTrue(content.contains("onRequestPermission"))
        assertTrue(content.contains("R.string.system_font_scale_apply"))
        assertTrue(workspace.contains("fun requestPermission()"))
        assertTrue(workspace.contains("Settings.ACTION_MANAGE_WRITE_SETTINGS"))
        assertTrue(workspace.contains("presenter.apply()"))
    }

    private fun read(relativePath: String): String = SourceSmokeTestPaths.read(relativePath)
}
