package com.dpis.module

import org.junit.Assert.assertTrue
import org.junit.Test

class ToolsWorkspaceBinderSourceSmokeTest {
    @Test
    fun mainActivityWiresToolsWorkspaceBinderLifecycle() {
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
        assertTrue(workspace.contains("private val binder = ToolsWorkspaceBinder("))
        assertTrue(
            workspace.contains(
                "WindowInsetsBinder.applySystemBarPadding(toolbar, false, true, false, false)"
            )
        )
        assertTrue(workspace.contains("TouchFeedbackBinder.bindPressHaptic(view)"))
        assertTrue(workspace.contains("LogGate.ensureEnabled("))
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
    fun toolsWorkspaceBinderOwnsSystemFontScaleToolBinder() {
        val source = read(
            "src/main/java/com/dpis/module/settings/presentation/ToolsWorkspaceBinder.kt"
        )

        assertTrue(
            source.contains("private var fontScaleToolBinder: SystemFontScaleToolBinder? = null")
        )
        assertTrue(
            source.contains(
                "fontScaleToolBinder = SystemFontScaleToolBinder(host.activity(), workspaceView, host)"
            )
        )
        assertTrue(source.contains("fontScaleToolBinder?.refreshFromSystem()"))
        assertTrue(source.contains("fontScaleToolBinder?.collapseAndRefreshFromSystem()"))
    }

    @Test
    fun systemFontScalePermissionPanelOwnsAuthorizationClick() {
        val source = read(
            "src/main/java/com/dpis/module/settings/presentation/SystemFontScaleToolBinder.kt"
        )

        assertTrue(source.contains("host.bindPressHaptic(permissionOverlay)"))
        assertTrue(
            source.contains(
                "permissionOverlay?.setOnClickListener { openWriteSettingsPermission() }"
            )
        )
        assertTrue(
            source.contains(
                "setVisible(operationGroup, expanded && (current.canWrite || current.unavailable))"
            )
        )
    }

    private fun read(relativePath: String): String = SourceSmokeTestPaths.read(relativePath)
}
