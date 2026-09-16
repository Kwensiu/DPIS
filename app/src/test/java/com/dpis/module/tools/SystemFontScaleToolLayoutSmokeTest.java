package com.dpis.module.tools;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dpis.module.SourceSmokeTestPaths;
import java.io.IOException;
import org.junit.Test;

public class SystemFontScaleToolLayoutSmokeTest {
    @Test
    public void composeToolsWorkspaceOwnsSystemFontScaleToolSurface() throws IOException {
        String content = read(
                "src/main/java/com/dpis/module/tools/presentation/ToolsWorkspaceContent.kt");

        assertTrue(content.contains("R.string.workspace_tools"));
        assertTrue(content.contains("R.string.system_font_scale_title"));
        assertTrue(content.contains("R.string.system_font_scale_subtitle"));
        assertTrue(content.contains("R.string.system_font_scale_apply"));
        assertTrue(content.contains("onRequestPermission"));
        assertTrue(content.contains("onPendingChanged"));
        assertFalse(content.contains("system_font_scale_seek_bar"));
    }

    @Test
    public void toolsWorkspaceRefreshIsOwnedByPresenter() throws IOException {
        String source = read("src/main/java/com/dpis/module/tools/presentation/ToolsWorkspace.kt");

        assertTrue(source.contains("SystemFontScaleToolPresenter("));
        assertTrue(source.contains("fun onStart()"));
        assertTrue(source.contains("fun onResume()"));
        assertTrue(source.contains("presenter.refresh()"));
        assertFalse(source.contains("ToolsWorkspaceBinder"));
        assertFalse(source.contains("WindowInsetsBinder.applySystemBarPadding"));
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
