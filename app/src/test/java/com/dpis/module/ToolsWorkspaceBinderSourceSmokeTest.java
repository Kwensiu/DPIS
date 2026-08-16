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

        assertTrue(source.contains("private ToolsWorkspaceBinder toolsWorkspaceBinder;"));
        assertTrue(source.contains(
                "toolsWorkspaceBinder = new ToolsWorkspaceBinder(new ToolsWorkspaceBinder.Host()"));
        assertTrue(source.contains(
                "WindowInsetsBinder.applySystemBarPadding(toolbar, false, true, false, false);"));
        assertTrue(source.contains("TouchFeedbackBinder.bindPressHaptic(view);"));
        assertTrue(source.contains("LogGate.ensureEnabled("));
        assertTrue(source.contains("bindToolsWorkspace();"));
        assertTrue(source.contains("toolsWorkspaceBinder.onStart();"));
        assertTrue(source.contains("toolsWorkspaceBinder.onResume();"));
        assertTrue(source.contains("toolsWorkspaceBinder.onStop();"));
        assertTrue(source.contains("toolsWorkspaceBinder.onActivityResult(requestCode, resultCode, data);"));
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
