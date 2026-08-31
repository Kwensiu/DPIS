package com.dpis.module.templates

import com.dpis.module.ConfigEditorDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Locks template route transitions independently from the Activity surfaces that render them. */
class TemplateWorkspaceRouteStateTest {
    @Test
    fun openingTargetsClearsEditorDraftsButKeepsTheTargetSelection() {
        val route = TemplateWorkspaceCoordinator.RouteState(
            TemplateDetailSelection.quickTemplate("template"),
            ConfigEditorDestination.TYPEFACE,
            false,
            TemplateEditorDraft(false, "", "120", null, null, null, null, null, null, null, null),
            TemplateEditorDraft(true, "Template", "120", null, null, null, null, null, null, null, null),
        )

        route.openQuickTemplateTargets("template")

        assertEquals(TemplateDetailKind.QUICK_TEMPLATE_TARGETS, route.selection().kind)
        assertEquals("template", route.selection().templateId)
        assertNull(route.globalPrefillDraft())
        assertNull(route.quickTemplateDraft())
    }

    @Test
    fun closingRouteResetsEveryTemplateSessionValue() {
        val route = TemplateWorkspaceCoordinator.RouteState()
        route.openQuickTemplateTargets("template")
        route.updateEditorDestination(ConfigEditorDestination.HOOK_CHAIN_FONT)
        route.markTargetSelectionActivityStarted()

        route.clear()

        assertEquals(TemplateDetailKind.NONE, route.selection().kind)
        assertEquals(ConfigEditorDestination.MAIN, route.editorDestination())
        assertFalse(route.targetSelectionActivityStarted())
        assertFalse(route.hasPendingQuickTemplateTargets())
    }

    @Test
    fun configurationResetOnlyReleasesAnInFlightTargetSelection() {
        val route = TemplateWorkspaceCoordinator.RouteState()
        route.markTargetSelectionActivityStarted()
        route.resetTargetSelectionActivityForConfiguration()
        assertTrue(route.targetSelectionActivityStarted())

        route.openQuickTemplateTargets("template")
        route.markTargetSelectionActivityStarted()
        route.resetTargetSelectionActivityForConfiguration()

        assertFalse(route.targetSelectionActivityStarted())
    }
}
