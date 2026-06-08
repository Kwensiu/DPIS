package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class ToolsWorkspaceBinderSourceSmokeTest {
    @Test
    public void mainActivityWiresToolsWorkspaceBinderLifecycle() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(source.contains("private ToolsWorkspaceBinder toolsWorkspaceBinder;"));
        assertTrue(source.contains("toolsWorkspaceBinder = new ToolsWorkspaceBinder(this);"));
        assertTrue(source.contains("bindToolsWorkspace();"));
        assertTrue(source.contains("toolsWorkspaceBinder.onStart();"));
        assertTrue(source.contains("toolsWorkspaceBinder.onResume();"));
        assertTrue(source.contains("toolsWorkspaceBinder.onStop();"));
        assertTrue(source.contains("toolsWorkspaceBinder.onActivityResult(requestCode, resultCode, data);"));
    }

    @Test
    public void toolsWorkspaceBinderOwnsSystemFontScaleToolBinder() throws IOException {
        String source = read("src/main/java/com/dpis/module/ToolsWorkspaceBinder.java");

        assertTrue(source.contains("private SystemFontScaleToolBinder fontScaleToolBinder;"));
        assertTrue(source.contains("fontScaleToolBinder = new SystemFontScaleToolBinder(activity, workspaceView);"));
        assertTrue(source.contains("fontScaleToolBinder.refreshFromSystem();"));
        assertTrue(source.contains("fontScaleToolBinder.collapseAndRefreshFromSystem();"));
    }

    @Test
    public void systemFontScalePermissionPanelOwnsAuthorizationClick() throws IOException {
        String source = read("src/main/java/com/dpis/module/SystemFontScaleToolBinder.java");

        assertTrue(source.contains("TouchFeedbackBinder.bindPressHaptic(permissionOverlay);"));
        assertTrue(source.contains("permissionOverlay.setOnClickListener(v -> openWriteSettingsPermission());"));
        assertTrue(source.contains("setVisible(operationGroup, expanded && (state.canWrite || state.unavailable));"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
