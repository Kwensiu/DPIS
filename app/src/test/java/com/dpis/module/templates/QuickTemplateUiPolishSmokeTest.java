package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;

public class QuickTemplateUiPolishSmokeTest {
    @Test
    public void quickTemplateLayoutsUseStringAndSemanticDimensionResources() throws IOException {
        String targetLayout = read("src/main/res/layout/activity_quick_template_targets.xml");
        String targetItem = read("src/main/res/layout/item_quick_template_target_app.xml");
        String strings = read("src/main/res/values/strings.xml");
        String zhStrings = read("src/main/res/values-zh-rCN/strings.xml");
        String dimens = read("src/main/res/values/dimens.xml");

        assertFalse(targetLayout.contains("android:text=\"Select"));
        assertFalse(targetItem.contains("android:text=\"Configured"));
        assertTrue(targetLayout.contains("@string/system_settings_back"));
        assertTrue(targetLayout.contains("@string/search_hint"));
        assertTrue(targetLayout.contains("@string/status_save_button"));
        assertTrue(targetItem.contains("@string/quick_template_targets_configured_badge"));
        assertTrue(targetLayout.contains("@dimen/template_target_content_padding_horizontal"));
        assertTrue(targetLayout.contains("@dimen/main_search_icon_padding_start"));
        assertTrue(targetLayout.contains("@dimen/main_search_icon_padding_end"));
        assertTrue(targetLayout.contains("@dimen/main_search_action_icon_padding_start"));
        assertTrue(targetLayout.contains("@dimen/main_search_action_icon_padding_end"));
        assertTrue(targetLayout.contains("@dimen/main_search_action_icon_padding_vertical"));
        assertTrue(targetLayout.contains("@dimen/template_target_list_container_spacing_top"));
        assertTrue(targetLayout.contains("@dimen/template_target_save_button_height"));
        assertTrue(targetLayout.contains("@dimen/template_target_save_button_margin_top"));
        assertTrue(targetLayout.contains("@dimen/template_target_save_button_margin_bottom"));
        assertFalse(targetLayout.contains("@dimen/main_workspace_"));
        assertTrue(targetItem.contains("@dimen/template_target_row_min_height"));
        assertTrue(targetItem.contains("@dimen/template_target_badge_padding_horizontal"));
        assertTrue(dimens.contains("template_target_content_padding_horizontal"));
        assertTrue(dimens.contains("template_target_list_container_spacing_top"));
        assertTrue(dimens.contains("template_target_save_button_height"));
        assertTrue(dimens.contains("template_target_save_button_margin_top"));
        assertTrue(dimens.contains("template_target_save_button_margin_bottom"));
        assertTrue(dimens.contains("template_target_row_padding_vertical"));
        assertTrue(dimens.contains("template_target_badge_padding_horizontal"));
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
        String appConfigLayout = read("src/main/res/layout/dialog_app_config.xml");
        String strings = read("src/main/res/values/strings.xml");
        String zhStrings = read("src/main/res/values-zh-rCN/strings.xml");

        assertTrue(appConfigLayout.contains("@string/dialog_viewport_mode_toggle_description"));
        assertTrue(appConfigLayout.contains("@string/dialog_font_mode_toggle_description"));
        assertTrue(strings.contains("dialog_viewport_mode_toggle_description"));
        assertTrue(strings.contains("dialog_font_mode_toggle_description"));
        assertTrue(zhStrings.contains("dialog_viewport_mode_toggle_description"));
        assertTrue(zhStrings.contains("dialog_font_mode_toggle_description"));
    }

    @Test
    public void quickTemplateApplyCopyAvoidsFailureZeroMessage() throws IOException {
        String workspace = read("src/main/java/com/dpis/module/templates/TemplateWorkspaceCoordinator.kt");

        assertTrue(workspace.contains("if (result.failureCount() > 0)"));
        assertTrue(workspace.contains("quick_template_apply_result_partial"));
        assertTrue(workspace.contains("quick_template_apply_result_success"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }

}
