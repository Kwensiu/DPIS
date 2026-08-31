package com.dpis.module.templates

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import com.dpis.module.MainActivity

/**
 * The sole Activity-facing entry point for the template workspace.
 *
 * This keeps retained route state, legacy View attachment, platform-result forwarding, and
 * Compose presentation setup in the template module. MainActivity only supplies its generic
 * shell query and lifecycle callbacks.
 */
class TemplateWorkspaceActivitySession(
    activity: MainActivity,
    initialQuery: String,
    initialRoute: TemplateWorkspaceCoordinator.RouteState,
    refreshPresentation: Runnable,
) {
    private val coordinator = TemplateWorkspaceCoordinator(
        activity,
        TemplateWorkspaceActivityHost(activity, refreshPresentation),
        initialQuery,
        initialRoute,
    )

    fun restore(savedState: Bundle?) = coordinator.restoreRoute(savedState)

    fun attachLegacyViews(
        workspace: View?,
        detailEmpty: View?,
        detailContent: FrameLayout?,
    ) = coordinator.attachLegacyViews(workspace, detailEmpty, detailContent)

    fun present(query: String, compose: Boolean) = coordinator.present(query, compose)

    fun restoreForConfiguration(query: String, compose: Boolean) =
        coordinator.restoreForConfiguration(query, compose)

    fun updateLegacyDetailVisibility(templateWorkspaceVisible: Boolean) =
        coordinator.updateLegacyDetailVisibility(templateWorkspaceVisible)

    fun handleActivityResult(requestCode: Int, data: Intent?) =
        coordinator.handleActivityResult(requestCode, data)

    fun saveState(outState: Bundle) = coordinator.saveRoute(outState)

    fun retainedRoute(): TemplateWorkspaceCoordinator.RouteState = coordinator.route()

    fun presentationSource(onQueryChanged: (String) -> Unit) =
        coordinator.presentationSource(onQueryChanged)

    fun quickItemCount() = coordinator.quickTemplateCount()

    fun onDestroy() = coordinator.onDestroy()
}
