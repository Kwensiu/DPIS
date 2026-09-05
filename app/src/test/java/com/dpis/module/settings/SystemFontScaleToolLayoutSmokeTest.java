package com.dpis.module;

import com.dpis.module.diagnostics.LogGate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.IOException;

import org.junit.Test;

public class SystemFontScaleToolLayoutSmokeTest {
    @Test
    public void toolsWorkspaceContainsSystemFontScaleToolSurface() throws IOException {
        String layout = read("src/main/res/layout/tools_workspace.xml");
        int fontCardIndex = layout.indexOf("android:id=\"@+id/system_font_scale_card\"");
        int logCardIndex = layout.indexOf("android:id=\"@+id/tools_log_card\"");

        assertTrue(layout.contains("android:id=\"@+id/tools_toolbar\""));
        assertTrue(layout.contains("android:text=\"@string/workspace_tools\""));
        assertTrue(layout.contains("android:id=\"@+id/system_font_scale_card\""));
        assertTrue(layout.contains("android:id=\"@+id/tools_log_card\""));
        assertTrue(fontCardIndex >= 0 && logCardIndex > fontCardIndex);
        assertTrue(layout.contains("android:id=\"@+id/system_font_scale_apply_button\""));
        assertTrue(layout.contains("android:background=\"@drawable/bg_round_button_surface\""));
        assertTrue(layout.contains("android:id=\"@+id/system_font_scale_permission_overlay\""));
        assertTrue(layout.contains("android:minHeight=\"@dimen/system_font_scale_permission_panel_height\""));
        assertTrue(layout.contains("android:background=\"@drawable/bg_system_font_scale_permission_panel\""));
        assertTrue(layout.contains("android:clickable=\"true\""));
        assertTrue(layout.contains("android:id=\"@+id/system_font_scale_unavailable_overlay\""));
        assertTrue(layout.contains("com.google.android.material.slider.Slider"));
        assertTrue(layout.contains("android:id=\"@+id/system_font_scale_slider\""));
        assertTrue(layout.contains("android:stepSize=\"1\""));
        assertTrue(layout.contains("app:tickVisible=\"false\""));
        assertTrue(!layout.contains("android:id=\"@+id/system_font_scale_seek_bar\""));
        assertTrue(!layout.contains("bg_system_font_scale_seekbar"));
        assertTrue(layout.contains("android:id=\"@+id/system_font_scale_decrement_button\""));
        assertTrue(layout.contains("android:id=\"@+id/system_font_scale_pending_value\""));
        assertTrue(layout.contains("android:minWidth=\"@dimen/system_font_scale_value_min_width\""));
        assertTrue(layout.contains("android:id=\"@+id/system_font_scale_increment_button\""));
        assertTrue(layout.contains("android:id=\"@+id/system_font_scale_preview_title\""));
        assertTrue(layout.contains("android:id=\"@+id/system_font_scale_preview_body\""));
        assertTrue(layout.contains("android:id=\"@+id/system_font_scale_restore_button\""));
        assertTrue(layout.contains("android:minHeight=\"@dimen/template_workspace_action_button_height\""));
        assertFalse(layout.contains("@drawable/ic_notes_24"));
        assertTrue(!layout.contains(
                "android:layout_height=\"@dimen/template_workspace_action_button_height\""));
        assertTrue(!layout.contains(
                "android:layout_height=\"@dimen/system_font_scale_permission_panel_height\""));
        assertTrue(!layout.contains("android:id=\"@+id/system_font_scale_current_value\""));
    }

    @Test
    public void toolsWorkspaceToolbarUsesSafeDrawingInsetsLikeSettingsPage()
            throws IOException {
        String source = read("src/main/java/com/dpis/module/settings/ToolsWorkspaceBinder.java");
        String mainActivity = read("src/main/java/com/dpis/module/MainActivity.java");
        String settingsController = read(
                "src/main/java/com/dpis/module/SystemServerSettingsPageController.kt");

        assertTrue(source.contains(
                "View toolsToolbar = workspaceView.findViewById(R.id.tools_toolbar);"));
        assertTrue(source.contains("WatchUiMode.shouldUseCompactUi(host.activity())"));
        assertTrue(source.contains("((LinearLayout) toolsToolbar).setGravity(Gravity.CENTER);"));
        assertTrue(source.contains("host.openLogsWhenDiagnosticLogsEnabled()"));
        assertTrue(mainActivity.contains(
                "WindowInsetsBinder.applySystemBarPadding(toolbar, false, true, false, false);"));
        assertTrue(mainActivity.contains("LogGate.ensureEnabled("));
        assertTrue(mainActivity.contains("new Intent(MainActivity.this, LogActivity.class)"));
        assertTrue(settingsController.contains("val toolbar = findViewById<View?>(R.id.settings_toolbar)"));
        assertTrue(settingsController.contains("baseTopPadding + safeDrawing.top"));
    }

    @Test
    public void systemFontScaleStringsAreLocalized() throws IOException {
        String defaultStrings = read("src/main/res/values/strings.xml");
        String chineseStrings = read("src/main/res/values-zh-rCN/strings.xml");

        assertTrue(defaultStrings.contains("system_font_scale_title"));
        assertTrue(defaultStrings.contains("system_font_scale_badge_out_of_range"));
        assertTrue(defaultStrings.contains("system_font_scale_write_failed"));
        assertTrue(chineseStrings.contains("system_font_scale_title"));
        assertTrue(chineseStrings.contains("system_font_scale_badge_out_of_range"));
        assertTrue(chineseStrings.contains("system_font_scale_write_failed"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
