package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;

public class QuickTemplateApplySourceSmokeTest {
    @Test
    public void mainActivityWiresApplyConfirmationAndResultCopy() throws IOException {
        String mainActivity = read("src/main/java/com/dpis/module/MainActivity.java");
        String binder = read("src/main/java/com/dpis/module/TemplateWorkspaceBinder.java");
        String coordinator = read("src/main/java/com/dpis/module/QuickTemplateApplyCoordinator.java");
        String strings = read("src/main/res/values/strings.xml");
        String zhStrings = read("src/main/res/values-zh-rCN/strings.xml");

        assertTrue(binder.contains("void apply(String templateId)"));
        assertTrue(mainActivity.contains("applyQuickTemplate(templateId)"));
        assertTrue(mainActivity.contains("R.string.quick_template_apply_confirm_message"));
        assertTrue(mainActivity.contains("R.string.quick_template_apply_confirm_message_overwrite"));
        assertTrue(mainActivity.contains("QuickTemplateApplyConfirmationMessage.format("));
        assertTrue(mainActivity.contains("R.string.quick_template_apply_scope_note"));
        assertTrue(mainActivity.contains("finishQuickTemplateApply("));
        assertTrue(mainActivity.contains("DialogWindowSizer.applyStandardWidth(dialog, this)"));
        assertTrue(mainActivity.contains("this::isInstalledTemplateTargetPackage"));
        assertTrue(mainActivity.contains("getPackageManager().getApplicationInfo("));
        assertTrue(mainActivity.contains("R.string.quick_template_apply_result_success"));
        assertTrue(mainActivity.contains("R.string.quick_template_apply_result_partial"));
        assertTrue(coordinator.contains("writePackageTemplateConfigValue("));
        assertTrue(coordinator.contains("ViewportPropertySyncer.publishTargetAsync("));
        assertTrue(coordinator.contains("ViewportPropertySyncer.clearTargetAsync("));
        assertTrue(coordinator.contains("FontRuntimePropertySyncer.publishTargetAsync("));
        assertTrue(coordinator.contains("FontRuntimePropertySyncer.clearFontScaleTargetAsync("));
        assertTrue(coordinator.contains("FontRuntimePropertySyncer.publishTypefaceTargetAsync("));
        assertTrue(coordinator.contains("FontHookDomainPropertySyncer.publishTargetAsync("));
        assertTrue(coordinator.contains("FontHookDomainPropertySyncer.clearTargetAsync("));
        assertTrue(strings.contains("quick_template_apply_confirm_message_overwrite"));
        assertTrue(zhStrings.contains("quick_template_apply_confirm_message_overwrite"));
        assertFalse(coordinator.contains("executeProcessAction"));
        assertFalse(coordinator.contains("applyHyperOsNativeProxy"));
        assertFalse(coordinator.contains("unmountHyperOsNativeProxy"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
