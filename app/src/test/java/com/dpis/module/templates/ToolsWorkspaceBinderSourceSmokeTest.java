package com.dpis.module;

import com.dpis.module.diagnostics.LogGate;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.IOException;

import org.junit.Test;

public class ToolsWorkspaceBinderSourceSmokeTest {
    @Test
    public void mainActivityWiresToolsWorkspaceBinderLifecycle() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");
        String workspace = read("src/main/java/com/dpis/module/settings/ToolsWorkspace.kt");

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
        String source = read("src/main/java/com/dpis/module/settings/ToolsWorkspaceBinder.java");

        assertTrue(source.contains("private SystemFontScaleToolBinder fontScaleToolBinder;"));
        assertTrue(source.contains(
                "fontScaleToolBinder = new SystemFontScaleToolBinder(host.activity(), workspaceView, host);"));
        assertTrue(source.contains("fontScaleToolBinder.refreshFromSystem();"));
        assertTrue(source.contains("fontScaleToolBinder.collapseAndRefreshFromSystem();"));
    }

    @Test
    public void systemFontScalePermissionPanelOwnsAuthorizationClick() throws IOException {
        String source = read("src/main/java/com/dpis/module/settings/SystemFontScaleToolBinder.java");

        assertTrue(source.contains("host.bindPressHaptic(permissionOverlay);"));
        assertTrue(source.contains("permissionOverlay.setOnClickListener(v -> openWriteSettingsPermission());"));
        assertTrue(source.contains("setVisible(operationGroup, expanded && (state.canWrite || state.unavailable));"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
