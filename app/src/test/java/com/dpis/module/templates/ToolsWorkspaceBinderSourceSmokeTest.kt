package com.dpis.module

import org.junit.Assert.assertTrue
import org.junit.Test

class ToolsWorkspaceBinderSourceSmokeTest {
    @Test
    fun mainActivityWiresToolsWorkspaceBinderLifecycle() {
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
        val workspaceSession = read(
            "src/main/java/com/dpis/module/ui/presentation/MainWorkspaceSession.kt"
        )
        val workspace = read("src/main/java/com/dpis/module/settings/presentation/ToolsWorkspace.kt")

        assertTrue(source.contains("private ToolsWorkspace toolsWorkspace;"))
        assertTrue(source.contains("toolsWorkspace = new ToolsWorkspace("))
        assertTrue(workspace.contains("private val binder = ToolsWorkspaceBinder("))
        assertTrue(
            workspace.contains(
                "WindowInsetsBinder.applySystemBarPadding(toolbar, false, true, false, false)"
            )
        )
        assertTrue(workspace.contains("TouchFeedbackBinder.bindPressHaptic(view)"))
        assertTrue(workspace.contains("LogGate.ensureEnabled("))
        assertTrue(workspaceSession.contains("fun bindToolsWorkspace("))
        assertTrue(source.contains("mainWorkspaceSession.bindForLifecycle("))
        assertTrue(source.contains("toolsWorkspace.onStart();"))
        assertTrue(source.contains("toolsWorkspace.onResume();"))
        assertTrue(source.contains("toolsWorkspace.onStop();"))
        assertTrue(
            source.contains("toolsWorkspace.onActivityResult(requestCode, resultCode, data);")
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
