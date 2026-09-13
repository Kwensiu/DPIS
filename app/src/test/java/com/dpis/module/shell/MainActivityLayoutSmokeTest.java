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
        String workspaceMenu = read("src/main/res/menu/main_workspace_navigation.xml");

        assertTrue(shell.contains("APP(R.string.workspace_app, R.drawable.ic_apps_24)"));
        assertTrue(shell.contains("TEMPLATE(R.string.workspace_template, R.drawable.ic_template_24)"));
        assertTrue(shell.contains("HOME(R.string.workspace_home, R.drawable.ic_home_24)"));
        assertTrue(shell.contains("TOOLS(R.string.workspace_tools, R.drawable.ic_build_24)"));
        assertTrue(shell.contains("SETTINGS(R.string.workspace_settings, R.drawable.ic_settings_24)"));
        assertTrue(workspaceMenu.contains("android:id=\"@+id/workspace_app_button\""));
        assertTrue(workspaceMenu.contains("android:id=\"@+id/workspace_template_button\""));
        assertTrue(workspaceMenu.contains("android:icon=\"@drawable/ic_apps_24\""));
        assertTrue(workspaceMenu.contains("android:icon=\"@drawable/ic_template_24\""));
    }

    @Test
    public void toolsWorkspaceKeepsExpandedCardsInsideScrollableContent()
            throws IOException {
        String layout = read("src/main/res/layout/tools_workspace.xml");
        String dimensions = read("src/main/res/values/dimens.xml");
        String source = read("src/main/java/com/dpis/module/settings/presentation/SystemFontScaleToolBinder.kt");

        assertTrue(layout.contains("android:id=\"@+id/tools_toolbar\""));
        assertTrue(layout.contains("android:id=\"@+id/tools_workspace_scroll\""));
        assertTrue(layout.contains("android:id=\"@+id/system_font_scale_card\""));
        assertTrue(dimensions.contains("tools_workspace_content_padding_bottom"));
        assertTrue(source.contains("revealExpandedPanel()"));
        assertTrue(source.contains("requestRectangleOnScreen"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
