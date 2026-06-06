package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;

public class QuickTemplateUiPolishSmokeTest {
    @Test
    public void quickTemplateLayoutsUseStringAndSemanticDimensionResources() throws IOException {
        String editLayout = read("src/main/res/layout/dialog_quick_template_edit_sheet.xml");
        String globalPrefillLayout = read("src/main/res/layout/dialog_global_prefill_sheet.xml");
        String sharedTemplateFields = read("src/main/res/layout/view_template_config_sheet_fields.xml");
        String targetLayout = read("src/main/res/layout/activity_quick_template_targets.xml");
        String targetItem = read("src/main/res/layout/item_quick_template_target_app.xml");
        String strings = read("src/main/res/values/strings.xml");
        String zhStrings = read("src/main/res/values-zh-rCN/strings.xml");
        String dimens = read("src/main/res/values/dimens.xml");

        assertFalse(editLayout.contains("android:text=\"New"));
        assertFalse(targetLayout.contains("android:text=\"Select"));
        assertFalse(targetItem.contains("android:text=\"Configured"));
        assertTrue(targetLayout.contains("@string/system_settings_back"));
        assertTrue(targetLayout.contains("@string/search_hint"));
        assertTrue(targetLayout.contains("@string/status_save_button"));
        assertTrue(targetItem.contains("@string/quick_template_targets_configured_badge"));
        assertTrue(targetLayout.contains("@dimen/template_target_content_padding_horizontal"));
        assertTrue(targetLayout.contains("@dimen/template_target_save_button_height"));
        assertTrue(targetLayout.contains("@dimen/template_target_save_button_margin_bottom"));
        assertFalse(targetLayout.contains("@dimen/main_workspace_"));
        assertTrue(targetItem.contains("@dimen/template_target_row_min_height"));
        assertTrue(targetItem.contains("@dimen/template_target_badge_padding_horizontal"));
        assertTrue(editLayout.contains("@dimen/dialog_app_config_padding_horizontal"));
        assertTrue(editLayout.contains("@dimen/template_config_sheet_padding_bottom"));
        assertTrue(editLayout.contains("@layout/view_sheet_unsaved_badge_handle"));
        assertTrue(globalPrefillLayout.contains("@dimen/dialog_app_config_padding_horizontal"));
        assertTrue(globalPrefillLayout.contains("@dimen/template_config_sheet_padding_bottom"));
        assertTrue(globalPrefillLayout.contains("@layout/view_sheet_unsaved_badge_handle"));
        assertTrue(editLayout.contains("@layout/view_template_config_sheet_fields"));
        assertTrue(sharedTemplateFields.contains("@string/dialog_viewport_mode_toggle_description"));
        assertTrue(sharedTemplateFields.contains("@string/dialog_font_mode_toggle_description"));
        assertTrue(sharedTemplateFields.contains("@dimen/dialog_app_config_input_row_spacing_top"));
        assertTrue(sharedTemplateFields.contains("@dimen/template_config_sheet_save_row_spacing_bottom"));
        assertFalse(editLayout.contains("@dimen/global_prefill_content_padding_"));
        assertFalse(editLayout.contains("@dimen/quick_template_edit_content_padding_"));
        assertTrue(dimens.contains("template_target_content_padding_horizontal"));
        assertTrue(dimens.contains("template_target_save_button_height"));
        assertTrue(dimens.contains("template_target_save_button_margin_bottom"));
        assertTrue(dimens.contains("template_target_row_padding_vertical"));
        assertTrue(dimens.contains("template_target_badge_padding_horizontal"));
        assertTrue(dimens.contains("dialog_app_config_padding_bottom"));
        assertTrue(dimens.contains("template_config_sheet_padding_bottom"));
        assertTrue(dimens.contains("template_config_sheet_save_row_spacing_bottom"));
        assertTrue(strings.contains("quick_template_apply_confirm_message"));
        assertTrue(strings.contains("quick_template_apply_confirm_message_overwrite"));
        assertTrue(zhStrings.contains("quick_template_apply_confirm_message"));
        assertTrue(zhStrings.contains("quick_template_apply_confirm_message_overwrite"));
        assertTrue(zhStrings.contains("快捷模板"));
        assertFalse(zhStrings.contains("快速模板"));
        assertTrue(strings.contains("Apply to %1$d apps."));
        assertTrue(strings.contains("%2$d existing configs will be overwritten."));
        assertTrue(zhStrings.contains("将应用到 %1$d 个应用。"));
        assertTrue(zhStrings.contains("%2$d 个已有配置会被覆盖"));
    }

    @Test
    public void sharedModeToggleLayoutsKeepThumbBehindLabels() throws IOException {
        String appConfigLayout = read("src/main/res/layout/dialog_app_config.xml");
        String sharedTemplateFields = read("src/main/res/layout/view_template_config_sheet_fields.xml");
        String strings = read("src/main/res/values/strings.xml");
        String zhStrings = read("src/main/res/values-zh-rCN/strings.xml");

        assertTrue(appConfigLayout.contains("@string/dialog_viewport_mode_toggle_description"));
        assertTrue(appConfigLayout.contains("@string/dialog_font_mode_toggle_description"));
        assertThumbUsesFrameLayout(sharedTemplateFields, "template_config_viewport_mode_toggle_thumb");
        assertThumbUsesFrameLayout(sharedTemplateFields, "template_config_font_mode_toggle_thumb");
        assertTrue(strings.contains("dialog_viewport_mode_toggle_description"));
        assertTrue(strings.contains("dialog_font_mode_toggle_description"));
        assertTrue(zhStrings.contains("dialog_viewport_mode_toggle_description"));
        assertTrue(zhStrings.contains("dialog_font_mode_toggle_description"));
    }

    @Test
    public void quickTemplateApplyCopyAvoidsFailureZeroMessage() throws IOException {
        String mainActivity = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(mainActivity.contains("if (result.failureCount() > 0)"));
        assertTrue(mainActivity.contains("quick_template_apply_result_partial"));
        assertTrue(mainActivity.contains("quick_template_apply_result_success"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }

    private static void assertThumbUsesFrameLayout(String layout, String thumbId) {
        int thumbIndex = layout.indexOf("android:id=\"@+id/" + thumbId + "\"");
        assertTrue(thumbIndex >= 0);
        int frameBeforeThumb = layout.lastIndexOf("<FrameLayout", thumbIndex);
        int linearBeforeThumb = layout.lastIndexOf("<LinearLayout", thumbIndex);
        assertTrue(frameBeforeThumb > linearBeforeThumb);
    }
}
