package com.dpis.module

import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateWorkspaceLayoutSmokeTest {
    @Test
    fun composeTemplateWorkspaceContainsGlobalPrefillWithoutApplyAction() {
        val list = read("src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceList.kt")
        val strings = read("src/main/res/values/strings.xml")
        list.assertContainsAll(
            "R.string.template_workspace_global_prefill_title",
            "R.string.template_workspace_global_prefill_subtitle",
        )
        list.assertNotContainsAll("R.string.template_workspace_action_reset")
        strings.assertContainsAll(
            "template_workspace_missing_font",
            "template_workspace_global_prefill_subtitle",
            "template_workspace_action_edit_global_prefill",
        )
    }

    @Test
    fun composeQuickTemplateCardsExposeRequiredActions() {
        val list = read("src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceList.kt")
        val strings = read("src/main/res/values/strings.xml")
        list.assertContainsAll(
            "R.string.quick_template_sort_action",
            "R.string.template_workspace_action_apply",
        )
        read("src/main/java/com/dpis/module/templates/presentation/QuickTemplateSortDialog.kt")
            .assertContainsAll(
                "QuickTemplateSortContent(",
                "ReorderableItem",
                "longPressDraggableHandle",
                "R.drawable.ic_drag_indicator_24",
            )
        read("src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceContent.kt")
            .assertContainsAll(
                "var sortDialogVisible by rememberSaveable",
                "QuickTemplateSortDialog(",
                "items = state.sortItems",
                "onOrderChanged = state.actions::reorderTemplates",
            )
        read("src/main/java/com/dpis/module/templates/presentation/TemplateWorkspacePresentation.kt")
            .assertContainsAll(
                "val sortItems: List<QuickTemplateSortItem>",
                "fun reorderTemplates(orderedIds: List<String>): Boolean",
            )
        read("src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceCoordinator.kt")
            .assertContainsAll(
                "class TemplateWorkspaceCoordinator",
                "private val presentation = TemplateWorkspacePresentationController",
                "refresh(presentation.state().query)",
                "QuickTemplateStore(activity).reorder(orderedIds)",
                "override fun reorderTemplates",
                "override fun saveGlobalPrefill",
                "override fun saveQuickTemplate",
                "override fun deleteQuickTemplate",
                "host.refreshTemplateWorkspace()",
            )
        strings.assertContainsAll(
            "<string name=\"template_workspace_action_apply\">Apply</string>",
            "<string name=\"template_workspace_summary_empty\">No custom values</string>",
            "<string name=\"template_workspace_action_edit_template\">Edit template</string>",
            "<string name=\"template_workspace_action_select_apps\">Select apps</string>",
            "template_search_hint",
            "quick_template_sort_action",
        )
    }

    @Test
    fun composeTemplateWorkspaceReadsStoresWithoutXmlBinders() {
        read("src/main/java/com/dpis/module/templates/TemplateTypefaceResolver.kt").assertContainsAll(
            "importedTypefaceProvider.resolve(typefaceId)",
            "SystemFontRegistry.loadTypeface(typefaceId) != null",
            "fun importedFrom(store: FontLibraryStore)",
            "store.resolveFontFile(typefaceId) != null",
        )
        read("src/main/java/com/dpis/module/templates/presentation/QuickTemplateSortDialog.kt")
            .assertContainsAll("ModalDialog(onDismissRequest = onDismiss)", "fun QuickTemplateSortDialog(")
        read("src/main/java/com/dpis/module/ui/presentation/wear/WearWorkspaceContent.kt")
            .assertContainsAll(
                "var sortDialogVisible by rememberSaveable",
                "QuickTemplateSortDialog(",
                "enabled = state.sortItems.isNotEmpty()",
            )
        read("src/main/java/com/dpis/module/ui/presentation/MainStartupSession.kt").apply {
            assertContainsAll("var workspaceSession: TemplateWorkspaceActivitySession?", "fun ensureWorkspaceSession()")
            assertContainsAll("TemplateWorkspaceActivitySession.State")
        }
        read("src/main/java/com/dpis/module/MainActivity.kt").apply {
            assertNotContainsAll(
                "ensureComposeTemplateWorkspacePresentation()",
                "new GlobalPrefillSaveHandler().save(",
                "new QuickTemplateSaveHandler().save(",
                "QuickTemplateSortDialog.show(",
            )
        }
        read("src/main/java/com/dpis/module/ui/presentation/MainWorkspaceSession.kt")
            .assertContainsAll("present(")
        read("src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceActivitySession.kt")
            .assertNotContainsAll("attachLegacyViews", "compose: Boolean")
    }

    private fun read(relativePath: String) = SourceSmokeTestPaths.read(relativePath)
    private fun String.assertContainsAll(vararg needles: String) = needles.forEach { assertTrue("Missing $it", contains(it)) }
    private fun String.assertNotContainsAll(vararg needles: String) = needles.forEach { assertTrue("Unexpected $it", !contains(it)) }
}
