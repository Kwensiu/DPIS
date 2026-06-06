package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class TemplateWorkspaceLayoutSmokeTest {
    @Test
    public void templateWorkspaceContainsGlobalPrefillCardWithoutApplyAction() throws IOException {
        String layout = read("src/main/res/layout/template_workspace.xml");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(layout.contains("android:id=\"@+id/template_workspace_container\""));
        assertTrue(layout.contains("android:id=\"@+id/global_prefill_card\""));
        assertTrue(layout.contains("app:cardBackgroundColor=\"?attr/colorSurfaceContainer\""));
        assertTrue(layout.contains("app:strokeColor=\"?attr/colorOutlineVariant\""));
        assertTrue(layout.contains("android:id=\"@+id/global_prefill_header\""));
        assertTrue(layout.contains("android:id=\"@+id/global_prefill_title\""));
        assertTrue(layout.contains("android:id=\"@+id/global_prefill_subtitle\""));
        assertTrue(layout.contains("android:id=\"@+id/global_prefill_summary_chips\""));
        assertTrue(layout.contains("android:id=\"@+id/global_prefill_empty_summary\""));
        assertTrue(layout.contains("android:id=\"@+id/global_prefill_edit_button\""));
        assertFalse(layout.contains("android:id=\"@+id/global_prefill_reset_button\""));
        assertFalse(layout.contains("global_prefill_apply_button"));
        assertTrue(layout.contains("@string/template_workspace_global_prefill_title"));
        assertTrue(layout.contains("@string/template_workspace_global_prefill_subtitle"));
        assertTrue(layout.contains("@string/template_workspace_action_edit_global_prefill"));
        assertTrue(layout.contains("androidx.appcompat.widget.AppCompatImageButton"));
        assertTrue(layout.contains("android:scaleType=\"centerInside\""));
        assertTrue(layout.contains("@drawable/ic_chevron_right_24"));
        assertFalse(layout.contains("@string/template_workspace_action_reset"));
        assertTrue(strings.contains("template_workspace_missing_font"));
        assertTrue(strings.contains("template_workspace_global_prefill_subtitle"));
        assertTrue(strings.contains("template_workspace_action_edit_global_prefill"));
    }

    @Test
    public void quickTemplateCardsExposeListContainerAndRequiredActions() throws IOException {
        String workspace = read("src/main/res/layout/template_workspace.xml");
        String card = read("src/main/res/layout/item_quick_template_card.xml");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(workspace.contains("android:id=\"@+id/quick_template_list_container\""));
        assertTrue(workspace.contains("android:id=\"@+id/quick_template_empty_state\""));
        assertTrue(workspace.contains("android:id=\"@+id/quick_template_section_header\""));
        assertTrue(workspace.contains("android:textAppearance=\"@style/TextAppearance.Material3.TitleLarge\""));
        assertTrue(workspace.contains("android:id=\"@+id/quick_template_sort_button\""));
        assertTrue(workspace.contains("android:id=\"@+id/quick_template_create_button\""));
        assertTrue(workspace.contains("@drawable/bg_round_button_surface"));
        assertTrue(workspace.contains("@drawable/bg_template_workspace_add_button"));
        assertTrue(workspace.contains("@drawable/ic_add_24"));
        assertTrue(workspace.contains("@drawable/ic_sort_24"));
        assertTrue(card.contains("android:id=\"@+id/quick_template_card\""));
        assertTrue(card.contains("app:cardBackgroundColor=\"?attr/colorSurfaceContainer\""));
        assertTrue(card.contains("app:cardCornerRadius=\"@dimen/template_workspace_card_corner_radius_compact\""));
        assertTrue(card.contains("android:id=\"@+id/quick_template_title\""));
        assertTrue(card.contains("android:id=\"@+id/quick_template_summary_chips\""));
        assertTrue(card.contains("android:id=\"@+id/quick_template_empty_summary\""));
        assertFalse(card.contains("android:id=\"@+id/quick_template_updated\""));
        assertFalse(card.contains("android:id=\"@+id/quick_template_missing_font\""));
        assertTrue(card.contains("android:id=\"@+id/quick_template_apply_button\""));
        assertTrue(card.contains("android:id=\"@+id/quick_template_edit_button\""));
        assertTrue(card.contains("android:id=\"@+id/quick_template_select_button\""));
        assertTrue(card.contains("@string/template_workspace_action_apply"));
        assertTrue(card.contains("@string/template_workspace_action_edit_template"));
        assertTrue(card.contains("@string/template_workspace_action_select_apps"));
        assertTrue(card.contains("androidx.appcompat.widget.AppCompatImageButton"));
        assertTrue(card.contains("android:scaleType=\"centerInside\""));
        assertTrue(card.contains("@drawable/ic_edit_24"));
        assertTrue(card.contains("@drawable/ic_checklist_rtl_24"));
        assertTrue(card.contains("Widget.Dpis.TemplateWorkspace.ApplyButton"));
        assertTrue(read("src/main/res/layout/dialog_quick_template_sort.xml")
                .contains("@string/quick_template_sort_title"));
        assertTrue(read("src/main/res/layout/dialog_quick_template_sort.xml")
                .contains("@dimen/dialog_template_sort_title_inset_start"));
        assertTrue(read("src/main/res/layout/item_quick_template_sort.xml")
                .contains("@drawable/ic_drag_indicator_24"));
        assertTrue(strings.contains("<string name=\"template_workspace_action_apply\">Apply</string>"));
        assertTrue(strings.contains("<string name=\"template_workspace_action_edit_template\">Edit template</string>"));
        assertTrue(strings.contains("<string name=\"template_workspace_action_select_apps\">Select apps</string>"));
        assertTrue(strings.contains("template_search_hint"));
        assertTrue(strings.contains("quick_template_sort_action"));
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
        assertTrue(binder.contains("R.id.global_prefill_summary_chips"));
        assertTrue(binder.contains("bindHeaderActions(workspaceView, templates)"));
        assertTrue(binder.contains("sortButton.setEnabled(sortEnabled);"));
        assertTrue(binder.contains("sortButton.setAlpha(sortEnabled ? 1f : DISABLED_ACTION_ALPHA);"));
        assertTrue(read("src/main/java/com/dpis/module/TemplateSummaryChipBinder.java")
                .contains("colorSurfaceContainerHighest"));
        assertTrue(read("src/main/java/com/dpis/module/TemplateSummaryChipBinder.java")
                .contains("chip.setChipStrokeWidth(0);"));
        assertTrue(adapter.contains("R.id.quick_template_summary_chips"));
        assertTrue(adapter.contains("TemplateSummaryChipBinder"));
        assertTrue(adapter.contains("R.id.quick_template_apply_button"));
        assertTrue(adapter.contains("R.id.quick_template_edit_button"));
        assertTrue(adapter.contains("R.id.quick_template_select_button"));
        assertTrue(read("src/main/java/com/dpis/module/TemplateWorkspaceBinder.java")
                .contains("quick_template_sort_button"));
        assertTrue(read("src/main/java/com/dpis/module/QuickTemplateSortDialog.java")
                .contains("ItemTouchHelper"));
        assertTrue(read("src/main/java/com/dpis/module/QuickTemplateSortDialog.java")
                .contains("DialogWindowSizer.applyLargeWidth(dialog, activity)"));
        assertFalse(adapter.contains("quick_template_updated"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
