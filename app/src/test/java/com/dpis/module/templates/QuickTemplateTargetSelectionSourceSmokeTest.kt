package com.dpis.module

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickTemplateTargetSelectionSourceSmokeTest {
    @Test
    fun targetSelectionPagePersistsSelectedPackagesAndShowsConfiguredBadge() {
        val manifest = read("src/main/AndroidManifest.xml")
        val activity = read("src/main/java/com/dpis/module/templates/QuickTemplateTargetSelectionActivity.kt")
        val contract = read(
            "src/main/java/com/dpis/module/templates/QuickTemplateTargetSelectionContract.java",
        )
        val routeState = read("src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceCoordinator.kt")
        val filterState = read("src/main/java/com/dpis/module/templates/QuickTemplateTargetFilterState.kt")
        val presentation = read("src/main/java/com/dpis/module/templates/presentation/QuickTemplateTargetsPresentationController.kt")
        val carrierState = read(
            "src/main/java/com/dpis/module/templates/QuickTemplateTargetCarrierState.java",
        )
        val composeContent = read(
            "src/main/java/com/dpis/module/templates/presentation/QuickTemplateTargetsContent.kt",
        )
        val composeHost = read(
            "src/main/java/com/dpis/module/templates/presentation/QuickTemplateTargetActivityContent.kt",
        )
        val mainActivity = read("src/main/java/com/dpis/module/MainActivity.kt")

        assertTrue(manifest.contains("android:name=\".templates.QuickTemplateTargetSelectionActivity\""))
        assertTrue(manifest.contains("android:exported=\"false\""))
        assertTrue(contract.contains("EXTRA_TEMPLATE_ID = \"quick_template_targets.template_id\""))
        assertTrue(contract.contains("EXTRA_CLOSE_REASON"))
        assertTrue(contract.contains("CLOSE_REASON_ORIENTATION_MIGRATION"))
        assertTrue(contract.contains("CLOSE_REASON_USER_BACK"))
        assertTrue(contract.contains("CLOSE_REASON_SAVED"))
        assertTrue(contract.contains("CLOSE_REASON_MISSING_TEMPLATE"))
        assertTrue(activity.contains("Configuration.ORIENTATION_LANDSCAPE"))
        assertTrue(activity.contains("shouldClosePortraitPageInLandscape()"))
        assertTrue(
            activity.contains(
                "finishWithReason(QuickTemplateTargetSelectionContract.CLOSE_REASON_ORIENTATION_MIGRATION)",
            ),
        )
        assertTrue(activity.contains("val controller = QuickTemplateTargetsPresentationController(this)"))
        assertTrue(activity.contains("targetsController?.dispose()"))
        assertTrue(activity.contains("finish()"))
        assertFalse(activity.contains("quickTemplateStore.setSelectedPackages(template.id, selectedPackages)"))
        assertFalse(composeHost.contains("controller::onIconVisible"))
        assertTrue(composeHost.contains("result.messageResId"))
        assertTrue(composeContent.contains("TargetSearchCard("))
        assertTrue(composeContent.contains("R.drawable.ic_search_24"))
        assertTrue(composeContent.contains("R.drawable.ic_close_24"))
        assertTrue(composeContent.contains("R.drawable.ic_tune_24"))
        assertTrue(composeContent.contains("searchVisible = !searchVisible"))
        assertTrue(composeContent.contains("if (!searchVisible) focusManager.clearFocus()"))
        assertTrue(composeContent.contains("onValueChange = onQueryChanged"))
        assertTrue(composeContent.contains("clearTextInputFocusOutside(focusManager, inputFocusBoundary)"))
        assertTrue(composeContent.contains("reportTextInputFocusBounds(inputFocusBoundary, \"target-search\")"))
        assertTrue(composeContent.contains("onValueChange = rememberClickValueAction(onSelected)"))
        assertTrue(composeContent.contains("AndroidView("))
        assertFalse(composeContent.contains("onIconVisible(app.packageName)"))
        assertTrue(composeContent.contains("FilterSheetScaffold("))
        assertTrue(composeContent.contains("FilterSheetUiTokens.PillShape"))
        assertFalse(composeContent.contains("AlertDialog("))
        assertTrue(composeContent.contains("filterSheetVisible"))
        assertFalse(composeContent.contains("filterDialogVisible"))
        assertTrue(composeContent.contains("navigationBarsPadding()"))
        assertTrue(composeContent.contains("SecondaryPageTopBar("))
        assertTrue(composeContent.contains("SplitPaneHeader("))
        assertTrue(composeContent.contains("WindowInsets.statusBars"))
        assertFalse(composeContent.contains("TopAppBar("))
        assertTrue(presentation.contains("templates.setSelectedPackages(id, LinkedHashSet<String?>(selectedPackages))"))
        assertTrue(presentation.contains("QuickTemplateTargetSelectionPolicy.retainInstalled"))
        assertTrue(presentation.contains("catalog.loadInstalledAppCatalogWithIcons(false)"))
        assertTrue(presentation.contains("item.icon"))
        assertTrue(presentation.contains("catch (throwable: Throwable)"))
        assertTrue(
            presentation.contains(
                "quick template target presentation load failed",
            ),
        )
        assertTrue(presentation.contains("loading = false"))
        assertTrue(filterState.contains("KEY_SHOW_SYSTEM_APPS = \"show_system_apps\""))
        assertTrue(filterState.contains("KEY_HIDE_CONFIGURED_APPS = \"hide_configured_apps\""))
        assertTrue(composeContent.contains("if (icon == null)"))
        assertTrue(composeContent.contains("surfaceVariant"))
        val workspaceCoordinator = read("src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceCoordinator.kt")
        assertTrue(workspaceCoordinator.contains("openQuickTemplateTargets(templateId)"))
        assertTrue(workspaceCoordinator.contains("startPortraitTargetSelection(templateId)"))
        assertTrue(workspaceCoordinator.contains("REQUEST_TARGET_SELECTION"))
        assertTrue(workspaceCoordinator.contains("routeState.targetSelectionActivityStarted()"))
        assertTrue(workspaceCoordinator.contains("routeState.markTargetSelectionActivityStarted()"))
        val startup = read(
            "src/main/java/com/dpis/module/ui/presentation/MainStartupSession.kt",
        )
        assertTrue(startup.contains("handleActivityResult(requestCode, data)"))
        assertTrue(workspaceCoordinator.contains("activity.startActivityForResult("))
        val loadSession = read(
            "src/main/java/com/dpis/module/applist/presentation/InstalledAppsLoadSession.kt",
        )
        assertTrue(loadSession.contains("InstalledAppCatalogCoordinator("))
        assertFalse(mainActivity.contains("REQUEST_QUICK_TEMPLATE_TARGETS"))
        assertTrue(routeState.contains("QuickTemplateTargetCarrierState.shouldClearPendingAfterResult("))
        assertTrue(routeState.contains("QuickTemplateTargetSelectionContract.closeReasonFrom("))
        assertTrue(carrierState.contains("enum CloseReason"))
        assertTrue(carrierState.contains("ORIENTATION_MIGRATION"))
        assertTrue(workspaceCoordinator.contains("TemplateDetailKind.QUICK_TEMPLATE_TARGETS"))
        assertFalse(workspaceCoordinator.contains("TemplateDetailPaneController"))
        assertTrue(routeState.contains("class RouteState"))
        assertTrue(routeState.contains("fun resetTargetSelectionActivityForConfiguration()"))
        assertTrue(workspaceCoordinator.contains("QuickTemplateTargetSelectionContract.EXTRA_TEMPLATE_ID"))
        val showTargetsMethod = workspaceCoordinator.substring(
            workspaceCoordinator.indexOf("private fun openQuickTemplateTargets("),
            workspaceCoordinator.indexOf("private fun startPortraitTargetSelection("),
        )
        assertFalse(showTargetsMethod.contains("clearTemplateDetailSelection();"))
        assertFalse(showTargetsMethod.contains("showLegacyDetail"))
    }

    private fun read(relativePath: String): String = SourceSmokeTestPaths.read(relativePath)
}
