package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class QuickTemplateApplySourceSmokeTest {
    @Test
    public void mainActivityWiresApplyConfirmationAndResultCopy() throws IOException {
        String mainActivity = read("src/main/java/com/dpis/module/MainActivity.java");
        String binder = read("src/main/java/com/dpis/module/TemplateWorkspaceBinder.java");
        String coordinator = read("src/main/java/com/dpis/module/QuickTemplateApplyCoordinator.java");

        assertTrue(binder.contains("void apply(String templateId)"));
        assertTrue(mainActivity.contains("applyQuickTemplate(templateId)"));
        assertTrue(mainActivity.contains("R.string.quick_template_apply_confirm_message"));
        assertTrue(mainActivity.contains("finishQuickTemplateApply("));
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
        assertFalse(coordinator.contains("executeProcessAction"));
        assertFalse(coordinator.contains("applyHyperOsNativeProxy"));
        assertFalse(coordinator.contains("unmountHyperOsNativeProxy"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
