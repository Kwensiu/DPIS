package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class MainActivityLayoutSmokeTest {
    @Test
    public void mainActivityStartsComposeWorkspaceShell() throws IOException {
        String activity = read("src/main/java/com/dpis/module/MainActivity.kt");
        String workspace = read(
                "src/main/java/com/dpis/module/ui/presentation/MainWorkspaceSession.kt");
        String strings = read("src/main/res/values/strings.xml");

        assertFalse(activity.contains("setContentView(R.layout.activity_status)"));
        assertTrue(workspace.contains("activity.setContentView("));
        assertTrue(workspace.contains("ComposeView(activity)"));
        assertTrue(strings.contains("workspace_app"));
        assertTrue(strings.contains("workspace_template"));
        assertTrue(strings.contains("template_workspace_global_prefill_title"));
        assertTrue(strings.contains("quick_search_button"));
        assertTrue(SourceSmokeTestPaths.exists("src/main/res/drawable/ic_tune_24.xml"));
    }

    @Test
    public void composeWorkspaceDestinationsKeepEstablishedNavigationOrder() throws IOException {
        String shell = read(
                "src/main/java/com/dpis/module/ui/presentation/workspace/WorkspaceShell.kt");

        assertTrue(shell.contains("APP(R.string.workspace_app, R.drawable.ic_apps_24)"));
        assertTrue(shell.contains("TEMPLATE(R.string.workspace_template, R.drawable.ic_template_24)"));
        assertTrue(shell.contains("HOME(R.string.workspace_home, R.drawable.ic_home_24)"));
        assertTrue(shell.contains("TOOLS(R.string.workspace_tools, R.drawable.ic_build_24)"));
        assertTrue(shell.contains("SETTINGS(R.string.workspace_settings, R.drawable.ic_settings_24)"));
        int app = shell.indexOf("APP(R.string.workspace_app");
        int template = shell.indexOf("TEMPLATE(R.string.workspace_template");
        int home = shell.indexOf("HOME(R.string.workspace_home");
        int tools = shell.indexOf("TOOLS(R.string.workspace_tools");
        int settings = shell.indexOf("SETTINGS(R.string.workspace_settings");
        assertTrue(app >= 0 && app < template && template < home && home < tools && tools < settings);
    }

    @Test
    public void toolsWorkspaceComposeOwnsFontScaleAndLogSurfaces() throws IOException {
        String content = read(
                "src/main/java/com/dpis/module/tools/presentation/ToolsWorkspaceContent.kt");
        String workspace = read(
                "src/main/java/com/dpis/module/tools/presentation/ToolsWorkspace.kt");

        assertTrue(content.contains("R.string.system_font_scale_title"));
        assertTrue(content.contains("R.string.workspace_tools"));
        assertTrue(workspace.contains("SystemFontScaleToolPresenter("));
        assertTrue(workspace.contains("presenter.refresh()"));
        assertFalse(workspace.contains("ToolsWorkspaceBinder"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
