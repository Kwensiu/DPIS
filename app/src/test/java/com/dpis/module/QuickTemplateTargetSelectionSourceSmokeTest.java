package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class QuickTemplateTargetSelectionSourceSmokeTest {
    @Test
    public void targetSelectionPagePersistsSelectedPackagesAndShowsConfiguredBadge() throws IOException {
        String manifest = read("src/main/AndroidManifest.xml");
        String activity = read("src/main/java/com/dpis/module/QuickTemplateTargetSelectionActivity.java");
        String adapter = read("src/main/java/com/dpis/module/QuickTemplateTargetAdapter.java");
        String layout = read("src/main/res/layout/activity_quick_template_targets.xml");
        String itemLayout = read("src/main/res/layout/item_quick_template_target_app.xml");
        String mainActivity = read("src/main/java/com/dpis/module/MainActivity.java");
        String binder = read("src/main/java/com/dpis/module/TemplateWorkspaceBinder.java");

        assertTrue(manifest.contains(".QuickTemplateTargetSelectionActivity"));
        assertTrue(activity.contains("EXTRA_TEMPLATE_ID = \"quick_template_targets.template_id\""));
        assertTrue(activity.contains("quickTemplateStore.setSelectedPackages(template.id, selectedPackages)"));
        assertTrue(activity.contains("configStore.hasRealPackageConfig(applicationInfo.packageName)"));
        assertTrue(activity.contains("getInstalledApplications("));
        assertTrue(adapter.contains("MaterialCheckBox"));
        assertTrue(adapter.contains("quick_template_target_configured_badge"));
        assertTrue(layout.contains("android:id=\"@+id/quick_template_targets_search_input\""));
        assertTrue(layout.contains("android:id=\"@+id/quick_template_targets_save_button\""));
        assertTrue(itemLayout.contains("@string/quick_template_targets_configured_badge"));
        assertTrue(mainActivity.contains("QuickTemplateTargetSelectionActivity.EXTRA_TEMPLATE_ID"));
        assertTrue(binder.contains("void select(String templateId)"));
        assertFalse(activity.contains("target_packages"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
