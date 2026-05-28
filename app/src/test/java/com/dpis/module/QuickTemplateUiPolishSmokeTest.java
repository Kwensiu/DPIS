package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class QuickTemplateUiPolishSmokeTest {
    @Test
    public void quickTemplateLayoutsUseStringAndSemanticDimensionResources() throws IOException {
        String editLayout = read("src/main/res/layout/activity_quick_template_edit.xml");
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
        assertTrue(editLayout.contains("@dimen/quick_template_edit_content_padding_horizontal"));
        assertTrue(editLayout.contains("@string/dialog_viewport_mode_toggle_description"));
        assertTrue(editLayout.contains("@string/dialog_font_mode_toggle_description"));
        assertTrue(editLayout.contains("android:foreground=\"?attr/selectableItemBackground\""));
        assertFalse(editLayout.contains("@dimen/global_prefill_content_padding_horizontal"));
        assertTrue(dimens.contains("template_target_content_padding_horizontal"));
        assertTrue(dimens.contains("template_target_save_button_height"));
        assertTrue(dimens.contains("template_target_save_button_margin_bottom"));
        assertTrue(dimens.contains("template_target_row_padding_vertical"));
        assertTrue(dimens.contains("template_target_badge_padding_horizontal"));
        assertTrue(dimens.contains("quick_template_edit_content_padding_horizontal"));
        assertTrue(strings.contains("quick_template_apply_confirm_message"));
        assertTrue(zhStrings.contains("quick_template_apply_confirm_message"));
        assertTrue(zhStrings.contains("快捷模板"));
        assertFalse(zhStrings.contains("快速模板"));
        assertTrue(strings.contains("No app process or HyperOS proxy action"));
        assertTrue(zhStrings.contains("不会执行进程操作或 HyperOS 代理操作"));
    }

    @Test
    public void sharedModeToggleLayoutsKeepThumbBehindLabels() throws IOException {
        String appConfigLayout = read("src/main/res/layout/dialog_app_config.xml");
        String prefillLayout = read("src/main/res/layout/activity_global_prefill.xml");
        String editLayout = read("src/main/res/layout/activity_quick_template_edit.xml");
        String strings = read("src/main/res/values/strings.xml");
        String zhStrings = read("src/main/res/values-zh-rCN/strings.xml");

        assertTrue(appConfigLayout.contains("@string/dialog_viewport_mode_toggle_description"));
        assertTrue(appConfigLayout.contains("@string/dialog_font_mode_toggle_description"));
        assertThumbUsesFrameLayout(prefillLayout, "global_prefill_viewport_mode_toggle_thumb");
        assertThumbUsesFrameLayout(prefillLayout, "global_prefill_font_mode_toggle_thumb");
        assertThumbUsesFrameLayout(editLayout, "quick_template_edit_viewport_mode_toggle_thumb");
        assertThumbUsesFrameLayout(editLayout, "quick_template_edit_font_mode_toggle_thumb");
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
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }

    private static void assertThumbUsesFrameLayout(String layout, String thumbId) {
        int thumbIndex = layout.indexOf("android:id=\"@+id/" + thumbId + "\"");
        assertTrue(thumbIndex >= 0);
        int frameBeforeThumb = layout.lastIndexOf("<FrameLayout", thumbIndex);
        int linearBeforeThumb = layout.lastIndexOf("<LinearLayout", thumbIndex);
        assertTrue(frameBeforeThumb > linearBeforeThumb);
    }
}
