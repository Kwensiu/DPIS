package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;

public class QuickTemplateUiPolishSmokeTest {
    @Test
    public void quickTemplateCopyUsesStringResources() throws IOException {
        String content = read(
                "src/main/java/com/dpis/module/templates/presentation/QuickTemplateTargetsContent.kt");
        String strings = read("src/main/res/values/strings.xml");
        String zhStrings = read("src/main/res/values-zh-rCN/strings.xml");

        assertTrue(content.contains("R.string.quick_template_targets_configured_badge"));
        assertTrue(content.contains("R.string.search_hint"));
        assertTrue(strings.contains("quick_template_apply_confirm_message"));
        assertTrue(strings.contains("quick_template_apply_confirm_message_overwrite"));
        assertTrue(zhStrings.contains("quick_template_apply_confirm_message"));
        assertTrue(zhStrings.contains("quick_template_apply_confirm_message_overwrite"));
        assertTrue(zhStrings.contains("快捷模板"));
        assertFalse(zhStrings.contains("快速模板"));
        assertTrue(strings.contains("Apply to %1$d apps"));
        assertTrue(strings.contains("%2$d existing configs will be overwritten"));
        assertTrue(zhStrings.contains("将应用到 %1$d 个应用"));
        assertTrue(zhStrings.contains("%2$d 个已有配置会被覆盖"));
    }

    @Test
    public void sharedModeToggleLayoutsKeepThumbBehindLabels() throws IOException {
        String strings = read("src/main/res/values/strings.xml");
        String zhStrings = read("src/main/res/values-zh-rCN/strings.xml");

        assertTrue(strings.contains("dialog_viewport_mode_toggle_description"));
        assertTrue(strings.contains("dialog_font_mode_toggle_description"));
        assertTrue(zhStrings.contains("dialog_viewport_mode_toggle_description"));
        assertTrue(zhStrings.contains("dialog_font_mode_toggle_description"));
    }

    @Test
    public void quickTemplateApplyCopyAvoidsFailureZeroMessage() throws IOException {
        String workspace = read("src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceCoordinator.kt");

        assertTrue(workspace.contains("if (result.failureCount() > 0)"));
        assertTrue(workspace.contains("quick_template_apply_result_partial"));
        assertTrue(workspace.contains("quick_template_apply_result_success"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }

}
