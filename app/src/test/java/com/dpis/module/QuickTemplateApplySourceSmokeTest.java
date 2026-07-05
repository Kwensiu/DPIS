package com.dpis.module;
import com.dpis.module.templates.QuickTemplateApplyAdapters;

import com.dpis.module.templates.TemplateConfigValue;

import com.dpis.module.templates.QuickTemplateApplyConfirmationMessage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;

public class QuickTemplateApplySourceSmokeTest {
    @Test
    public void mainActivityWiresApplyConfirmationAndResultCopy() throws IOException {
        String mainActivity = read("src/main/java/com/dpis/module/MainActivity.java");
        String binder = read("src/main/java/com/dpis/module/templates/TemplateWorkspaceBinder.java");
        String coordinator = read(
                "src/main/java/com/dpis/module/templates/QuickTemplateApplyCoordinator.java");
        String adapters = read("src/main/java/com/dpis/module/templates/QuickTemplateApplyAdapters.java");
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
        assertTrue(mainActivity.contains("QuickTemplateApplyCoordinator<TemplateConfigValue> coordinator"));
        assertTrue(mainActivity.contains("QuickTemplateApplyAdapters.from(getHookConfigStore())"));
        assertTrue(mainActivity.contains("getHookConfigStore()"));
        assertTrue(mainActivity.contains("return DpisApplication.getActiveHookConfigStore(this);"));
        assertTrue(mainActivity.contains("this::isInstalledTemplateTargetPackage"));
        assertTrue(mainActivity.contains("getPackageManager().getApplicationInfo("));
        assertTrue(mainActivity.contains("R.string.quick_template_apply_result_success"));
        assertTrue(mainActivity.contains("R.string.quick_template_apply_result_partial"));
        assertTrue(coordinator.contains("public interface ConfigWriter<T>"));
        assertTrue(coordinator.contains("public interface RuntimePublisher<T>"));
        assertTrue(coordinator.contains("boolean writePackageTemplateConfigValue("));
        assertFalse(coordinator.contains("DpisConfigStore"));
        assertFalse(coordinator.contains("PackageConfigRepository"));
        assertFalse(coordinator.contains("ViewportPropertySyncer"));
        assertTrue(adapters.contains("new PackageConfigRepository(store)"));
        assertTrue(adapters.contains("ViewportPropertySyncer.publishTargetAsync("));
        assertTrue(adapters.contains("ViewportPropertySyncer.clearTargetAsync("));
        assertTrue(adapters.contains("FontRuntimePropertySyncer.publishTargetAsync("));
        assertTrue(adapters.contains("FontRuntimePropertySyncer.clearFontScaleTargetAsync("));
        assertTrue(adapters.contains("FontRuntimePropertySyncer.publishTypefaceTargetAsync("));
        assertTrue(adapters.contains("FontHookDomainPropertySyncer.publishTargetAsync("));
        assertTrue(adapters.contains("FontHookDomainPropertySyncer.clearTargetAsync("));
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
