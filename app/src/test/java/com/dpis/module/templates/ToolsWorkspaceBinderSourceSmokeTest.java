package com.dpis.module;

import com.dpis.module.diagnostics.presentation.LogGate;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.IOException;

import org.junit.Test;

public class ToolsWorkspaceBinderSourceSmokeTest {
    @Test
    public void mainActivityWiresToolsWorkspaceBinderLifecycle() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");
        String workspace = read("src/main/java/com/dpis/module/settings/presentation/ToolsWorkspace.kt");

        assertTrue(source.contains("private ToolsWorkspace toolsWorkspace;"));
        assertTrue(source.contains("toolsWorkspace = new ToolsWorkspace("));
        assertTrue(workspace.contains("private val binder = ToolsWorkspaceBinder("));
        assertTrue(workspace.contains(
                "WindowInsetsBinder.applySystemBarPadding(toolbar, false, true, false, false)"));
        assertTrue(workspace.contains("TouchFeedbackBinder.bindPressHaptic(view)"));
        assertTrue(workspace.contains("LogGate.ensureEnabled("));
        assertTrue(source.contains("bindToolsWorkspace();"));
        assertTrue(source.contains("toolsWorkspace.onStart();"));
        assertTrue(source.contains("toolsWorkspace.onResume();"));
        assertTrue(source.contains("toolsWorkspace.onStop();"));
        assertTrue(source.contains("toolsWorkspace.onActivityResult(requestCode, resultCode, data);"));
    }

    @Test
    public void toolsWorkspaceBinderOwnsSystemFontScaleToolBinder() throws IOException {
        String source = read("src/main/java/com/dpis/module/settings/presentation/ToolsWorkspaceBinder.kt");

        assertTrue(source.contains("private var fontScaleToolBinder: SystemFontScaleToolBinder? = null"));
        assertTrue(source.contains(
                "fontScaleToolBinder = SystemFontScaleToolBinder(host.activity(), workspaceView, host)"));
        assertTrue(source.contains("fontScaleToolBinder?.refreshFromSystem()"));
        assertTrue(source.contains("fontScaleToolBinder?.collapseAndRefreshFromSystem()"));
    }

    @Test
    public void systemFontScalePermissionPanelOwnsAuthorizationClick() throws IOException {
        String source = read("src/main/java/com/dpis/module/settings/presentation/SystemFontScaleToolBinder.kt");

        assertTrue(source.contains("host.bindPressHaptic(permissionOverlay)"));
        assertTrue(source.contains("permissionOverlay?.setOnClickListener { openWriteSettingsPermission() }"));
        assertTrue(source.contains("setVisible(operationGroup, expanded && (current.canWrite || current.unavailable))"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
