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
        assertTrue(targetItem.contains("@dimen/template_target_row_min_height"));
        assertTrue(dimens.contains("template_target_content_padding_horizontal"));
        assertTrue(dimens.contains("template_target_row_padding_vertical"));
        assertTrue(strings.contains("quick_template_apply_confirm_message"));
        assertTrue(zhStrings.contains("quick_template_apply_confirm_message"));
        assertTrue(strings.contains("No app process or HyperOS proxy action"));
        assertTrue(zhStrings.contains("不会执行进程操作或 HyperOS 代理操作"));
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
}
