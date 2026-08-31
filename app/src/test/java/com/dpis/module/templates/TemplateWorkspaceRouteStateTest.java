package com.dpis.module.templates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dpis.module.ConfigEditorDestination;
import com.dpis.module.TemplateDetailKind;
import com.dpis.module.TemplateDetailSelection;

import org.junit.Test;

/** Locks template route transitions independently from the Activity surfaces that render them. */
public final class TemplateWorkspaceRouteStateTest {
    @Test
    public void openingTargetsClearsEditorDraftsButKeepsTheTargetSelection() {
        TemplateWorkspaceCoordinator.RouteState route
                = new TemplateWorkspaceCoordinator.RouteState(
                TemplateDetailSelection.quickTemplate("template"),
                ConfigEditorDestination.TYPEFACE,
                false,
                new TemplateEditorDraft(false, "", "120", null, null, null, null,
                        null, null, null, null),
                new TemplateEditorDraft(true, "Template", "120", null, null, null, null,
                        null, null, null, null)
        );

        route.openQuickTemplateTargets("template");

        assertEquals(TemplateDetailKind.QUICK_TEMPLATE_TARGETS, route.selection().kind);
        assertEquals("template", route.selection().templateId);
        assertNull(route.globalPrefillDraft());
        assertNull(route.quickTemplateDraft());
    }

    @Test
    public void closingRouteResetsEveryTemplateSessionValue() {
        TemplateWorkspaceCoordinator.RouteState route = new TemplateWorkspaceCoordinator.RouteState();
        route.openQuickTemplateTargets("template");
        route.updateEditorDestination(ConfigEditorDestination.HOOK_CHAIN_FONT);
        route.markTargetSelectionActivityStarted();

        route.clear();

        assertEquals(TemplateDetailKind.NONE, route.selection().kind);
        assertEquals(ConfigEditorDestination.MAIN, route.editorDestination());
        assertFalse(route.targetSelectionActivityStarted());
        assertFalse(route.hasPendingQuickTemplateTargets());
    }

    @Test
    public void configurationResetOnlyReleasesAnInFlightTargetSelection() {
        TemplateWorkspaceCoordinator.RouteState route = new TemplateWorkspaceCoordinator.RouteState();
        route.markTargetSelectionActivityStarted();
        route.resetTargetSelectionActivityForConfiguration();
        assertTrue(route.targetSelectionActivityStarted());

        route.openQuickTemplateTargets("template");
        route.markTargetSelectionActivityStarted();
        route.resetTargetSelectionActivityForConfiguration();

        assertFalse(route.targetSelectionActivityStarted());
    }
}
