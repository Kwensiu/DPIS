package com.dpis.module

import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateWorkspaceLayoutSmokeTest {
    @Test
    fun templateWorkspaceContainsGlobalPrefillCardWithoutApplyAction() {
        val layout = read("src/main/res/layout/template_workspace.xml")
        val strings = read("src/main/res/values/strings.xml")
        layout.assertContainsAll(
            "android:id=\"@+id/template_workspace_container\"", "android:id=\"@+id/global_prefill_card\"",
            "app:cardBackgroundColor=\"?attr/colorSurfaceContainer\"", "app:strokeColor=\"?attr/colorOutlineVariant\"",
            "android:id=\"@+id/global_prefill_header\"", "android:id=\"@+id/global_prefill_title\"",
            "android:id=\"@+id/global_prefill_subtitle\"", "android:id=\"@+id/global_prefill_summary_chips\"",
            "android:id=\"@+id/global_prefill_empty_summary\"", "android:id=\"@+id/global_prefill_edit_button\"",
            "@string/template_workspace_global_prefill_title", "@string/template_workspace_global_prefill_subtitle",
            "@string/template_workspace_action_edit_global_prefill", "androidx.appcompat.widget.AppCompatImageButton",
            "android:scaleType=\"centerInside\"", "@drawable/ic_chevron_right_24",
        )
        layout.assertNotContainsAll("android:id=\"@+id/global_prefill_reset_button\"", "global_prefill_apply_button", "@string/template_workspace_action_reset")
        assertDashedEmptySummaryState(elementWithId(layout, "global_prefill_empty_summary"))
        strings.assertContainsAll("template_workspace_missing_font", "template_workspace_global_prefill_subtitle", "template_workspace_action_edit_global_prefill")
    }

    @Test
    fun quickTemplateCardsExposeListContainerAndRequiredActions() {
        val workspace = read("src/main/res/layout/template_workspace.xml")
        val card = read("src/main/res/layout/item_quick_template_card.xml")
        val strings = read("src/main/res/values/strings.xml")
        workspace.assertContainsAll(
            "android:id=\"@+id/quick_template_list_container\"", "android:id=\"@+id/quick_template_empty_state\"",
            "android:id=\"@+id/quick_template_section_header\"", "android:textAppearance=\"@style/TextAppearance.Material3.TitleLarge\"",
            "android:id=\"@+id/quick_template_sort_button\"", "android:id=\"@+id/quick_template_create_button\"",
            "@drawable/bg_round_button_surface", "@drawable/bg_template_workspace_add_button", "@drawable/ic_add_24", "@drawable/ic_sort_24",
        )
        card.assertContainsAll(
            "android:id=\"@+id/quick_template_card\"", "app:cardBackgroundColor=\"?attr/colorSurfaceContainer\"",
            "app:cardCornerRadius=\"@dimen/template_workspace_card_corner_radius_compact\"", "android:id=\"@+id/quick_template_title\"",
            "android:id=\"@+id/quick_template_summary_chips\"", "android:id=\"@+id/quick_template_empty_summary\"",
            "android:id=\"@+id/quick_template_apply_button\"", "android:id=\"@+id/quick_template_edit_button\"",
            "android:id=\"@+id/quick_template_select_button\"", "@string/template_workspace_action_apply",
            "@string/template_workspace_action_edit_template", "@string/template_workspace_action_select_apps",
            "androidx.appcompat.widget.AppCompatImageButton", "android:scaleType=\"centerInside\"", "@drawable/ic_edit_24",
            "@drawable/ic_checklist_rtl_24", "@drawable/bg_template_workspace_apply_button", "@drawable/ic_done_all_24",
        )
        card.assertNotContainsAll("android:id=\"@+id/quick_template_updated\"", "android:id=\"@+id/quick_template_missing_font\"")
        assertDashedEmptySummaryState(elementWithId(card, "quick_template_empty_summary"))
        read("src/main/java/com/dpis/module/templates/QuickTemplateSortDialog.kt").assertContainsAll("QuickTemplateSortContent(", "ReorderableItem", "longPressDraggableHandle", "R.drawable.ic_drag_indicator_24")
        read("src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceContent.kt").assertContainsAll("var sortDialogVisible by rememberSaveable", "ModalDialog(onDismissRequest", "initialItems = state.sortItems", "onOrderChanged = state.actions::reorderTemplates")
        read("src/main/java/com/dpis/module/templates/TemplateWorkspacePresentation.kt").assertContainsAll("val sortItems: List<QuickTemplateSortItem>", "fun reorderTemplates(orderedIds: List<String>): Boolean")
        read("src/main/java/com/dpis/module/templates/TemplateWorkspaceCoordinator.kt").assertContainsAll(
            "class TemplateWorkspaceCoordinator", "private val presentation = TemplateWorkspacePresentationController",
            "refresh(presentation.state().query)",
            "QuickTemplateStore(activity).reorder(orderedIds)",
            "override fun reorderTemplates", "override fun saveGlobalPrefill", "override fun saveQuickTemplate",
            "override fun deleteQuickTemplate", "override fun selectTypeface", "override fun editHookDomains",
            "host.refreshTemplateWorkspace()",
        )
        strings.assertContainsAll("<string name=\"template_workspace_action_apply\">Apply</string>", "<string name=\"template_workspace_summary_empty\">No custom values</string>", "<string name=\"template_workspace_action_edit_template\">Edit template</string>", "<string name=\"template_workspace_action_select_apps\">Select apps</string>", "template_search_hint", "quick_template_sort_action")
    }

    @Test
    fun binderAndAdapterReadStoresAndBindMissingFontHooks() {
        val binder = read("src/main/java/com/dpis/module/templates/TemplateWorkspaceBinder.kt")
        val adapter = read("src/main/java/com/dpis/module/templates/QuickTemplateListAdapter.java")
        binder.assertContainsAll("GlobalPrefillStore(preferences).read()", "QuickTemplateStore(context).readAll()", "ConfigStoreFactory.createLocalUiFontLibraryStore", "TemplateTypefaceResolver(", "store.resolveFontFile(typefaceId) != null", "R.id.global_prefill_summary_chips", "bindHeaderActions(workspaceView, templates)", "sortButton.isEnabled = enabled", "sortButton.alpha = if (enabled) 1f else DISABLED_ACTION_ALPHA")
        read("src/main/java/com/dpis/module/templates/TemplateTypefaceResolver.java").assertContainsAll("importedTypefaceProvider.resolve(typefaceId)", "SystemFontRegistry.loadTypeface(typefaceId) != null")
        read("src/main/java/com/dpis/module/templates/TemplateSummaryChipBinder.java").assertContainsAll("colorSurfaceContainerHighest", "chip.setChipStrokeWidth(0);")
        adapter.assertContainsAll("R.id.quick_template_summary_chips", "TemplateSummaryChipBinder", "R.id.quick_template_apply_button", "R.id.quick_template_edit_button", "R.id.quick_template_select_button")
        adapter.assertNotContainsAll("quick_template_updated")
        read("src/main/java/com/dpis/module/templates/TemplateWorkspaceBinder.kt").assertContainsAll("quick_template_sort_button")
        read("src/main/java/com/dpis/module/templates/QuickTemplateSortDialog.kt").assertContainsAll("DialogWindowSizer.applyLargeWidth(dialog, activity)")
        read("src/main/java/com/dpis/module/MainActivity.java").apply {
            assertContainsAll("private TemplateWorkspaceActivitySession workspaceSession;", "ensureWorkspaceSession()")
            assertContainsAll("TemplateWorkspaceActivitySession.State", "attachLegacyViews(")
            assertNotContainsAll("ensureComposeTemplateWorkspacePresentation()", "new GlobalPrefillSaveHandler().save(", "new QuickTemplateSaveHandler().save(", "QuickTemplateSortDialog.show(")
        }
    }

    private fun assertDashedEmptySummaryState(element: String) {
        element.assertContainsAll("@string/template_workspace_summary_empty", "android:background=\"@drawable/bg_template_workspace_empty_summary\"", "android:minHeight=\"@dimen/template_workspace_empty_summary_min_height\"", "android:gravity=\"center\"")
    }

    private fun elementWithId(layout: String, id: String): String {
        val expression = Regex("<com\\.google\\.android\\.material\\.textview\\.MaterialTextView\\s+[^>]*android:id=\\\"@\\+id/$id\\\"[^>]*/>", setOf(RegexOption.DOT_MATCHES_ALL))
        return expression.find(layout)?.value ?: error("Missing MaterialTextView #$id")
    }

    private fun read(relativePath: String) = SourceSmokeTestPaths.read(relativePath)
    private fun String.assertContainsAll(vararg needles: String) = needles.forEach { assertTrue("Missing $it", contains(it)) }
    private fun String.assertNotContainsAll(vararg needles: String) = needles.forEach { assertTrue("Unexpected $it", !contains(it)) }
}
