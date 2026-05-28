package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class TemplateWorkspaceLayoutSmokeTest {
    @Test
    public void templateWorkspaceContainsGlobalPrefillCardWithoutApplyAction() throws IOException {
        String layout = read("src/main/res/layout/template_workspace.xml");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(layout.contains("android:id=\"@+id/template_workspace_container\""));
        assertTrue(layout.contains("android:id=\"@+id/global_prefill_card\""));
        assertTrue(layout.contains("android:id=\"@+id/global_prefill_title\""));
        assertTrue(layout.contains("android:id=\"@+id/global_prefill_summary\""));
        assertTrue(layout.contains("android:id=\"@+id/global_prefill_missing_font\""));
        assertTrue(layout.contains("android:id=\"@+id/global_prefill_edit_button\""));
        assertTrue(layout.contains("android:id=\"@+id/global_prefill_reset_button\""));
        assertFalse(layout.contains("global_prefill_apply_button"));
        assertTrue(layout.contains("@string/template_workspace_global_prefill_title"));
        assertTrue(layout.contains("@string/template_workspace_action_edit"));
        assertTrue(layout.contains("@string/template_workspace_action_reset"));
        assertTrue(strings.contains("template_workspace_missing_font"));
    }

    @Test
    public void quickTemplateCardsExposeListContainerAndRequiredActions() throws IOException {
        String workspace = read("src/main/res/layout/template_workspace.xml");
        String card = read("src/main/res/layout/item_quick_template_card.xml");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(workspace.contains("android:id=\"@+id/quick_template_list_container\""));
        assertTrue(workspace.contains("android:id=\"@+id/quick_template_empty_state\""));
        assertTrue(card.contains("android:id=\"@+id/quick_template_card\""));
        assertTrue(card.contains("android:id=\"@+id/quick_template_title\""));
        assertTrue(card.contains("android:id=\"@+id/quick_template_summary\""));
        assertTrue(card.contains("android:id=\"@+id/quick_template_updated\""));
        assertTrue(card.contains("android:id=\"@+id/quick_template_missing_font\""));
        assertTrue(card.contains("android:id=\"@+id/quick_template_apply_button\""));
        assertTrue(card.contains("android:id=\"@+id/quick_template_edit_button\""));
        assertTrue(card.contains("android:id=\"@+id/quick_template_select_button\""));
        assertTrue(card.contains("@string/template_workspace_action_apply"));
        assertTrue(card.contains("@string/template_workspace_action_edit"));
        assertTrue(card.contains("@string/template_workspace_action_select"));
        assertTrue(strings.contains("<string name=\"template_workspace_action_apply\">Apply</string>"));
        assertTrue(strings.contains("<string name=\"template_workspace_action_edit\">Edit</string>"));
        assertTrue(strings.contains("<string name=\"template_workspace_action_select\">Select</string>"));
    }

    @Test
    public void binderAndAdapterReadStoresAndBindMissingFontHooks() throws IOException {
        String binder = read("src/main/java/com/dpis/module/TemplateWorkspaceBinder.java");
        String adapter = read("src/main/java/com/dpis/module/QuickTemplateListAdapter.java");

        assertTrue(binder.contains("new GlobalPrefillStore(preferences).read()"));
        assertTrue(binder.contains("new QuickTemplateStore(preferences).readAll()"));
        assertTrue(binder.contains("ConfigStoreFactory.createFontLibraryForModuleApp"));
        assertTrue(binder.contains("new TemplateTypefaceResolver("));
        assertTrue(read("src/main/java/com/dpis/module/TemplateTypefaceResolver.java")
                .contains("fontLibraryStore.resolveFontFile(typefaceId) != null"));
        assertTrue(read("src/main/java/com/dpis/module/TemplateTypefaceResolver.java")
                .contains("SystemFontRegistry.loadTypeface(typefaceId) != null"));
        assertTrue(binder.contains("R.id.global_prefill_missing_font"));
        assertTrue(adapter.contains("R.id.quick_template_missing_font"));
        assertTrue(adapter.contains("R.id.quick_template_apply_button"));
        assertTrue(adapter.contains("R.id.quick_template_edit_button"));
        assertTrue(adapter.contains("R.id.quick_template_select_button"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
