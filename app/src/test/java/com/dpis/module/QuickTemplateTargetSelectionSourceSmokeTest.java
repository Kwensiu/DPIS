package com.dpis.module;

import com.dpis.module.applist.AppListFilter;
import com.dpis.module.applist.AppListFilterState;
import com.dpis.module.applist.AppListFilterStateStore;
import com.dpis.module.templates.QuickTemplateTargetsBinder;

import com.dpis.module.templates.QuickTemplateTargetCarrierState;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;

public class QuickTemplateTargetSelectionSourceSmokeTest {
    @Test
    public void targetSelectionPagePersistsSelectedPackagesAndShowsConfiguredBadge() throws IOException {
        String manifest = read("src/main/AndroidManifest.xml");
        String activity = read("src/main/java/com/dpis/module/templates/QuickTemplateTargetSelectionActivity.java");
        String contract = read(
                "src/main/java/com/dpis/module/templates/QuickTemplateTargetSelectionContract.java");
        String targetsBinder = read("src/main/java/com/dpis/module/templates/QuickTemplateTargetsBinder.java");
        String presentation = read("src/main/java/com/dpis/module/templates/QuickTemplateTargetsPresentationController.java");
        String carrierState = read(
                "src/main/java/com/dpis/module/templates/QuickTemplateTargetCarrierState.java");
        String adapter = read("src/main/java/com/dpis/module/templates/QuickTemplateTargetAdapter.java");
        String layout = read("src/main/res/layout/activity_quick_template_targets.xml");
        String landLayout = read("src/main/res/layout/view_land_quick_template_targets_detail.xml");
        String filterLayout = read("src/main/res/layout/dialog_quick_template_target_filters.xml");
        String itemLayout = read("src/main/res/layout/item_quick_template_target_app.xml");
        String composeContent = read(
                "src/main/java/com/dpis/module/ui/compose/QuickTemplateTargetsContent.kt");
        String composeHost = read(
                "src/main/java/com/dpis/module/ui/compose/QuickTemplateTargetActivityContent.kt");
        String mainActivity = read("src/main/java/com/dpis/module/MainActivity.java");
        String binder = read("src/main/java/com/dpis/module/templates/TemplateWorkspaceBinder.java");

        assertTrue(manifest.contains("android:name=\".templates.QuickTemplateTargetSelectionActivity\""));
        assertTrue(manifest.contains("android:exported=\"false\""));
        assertTrue(contract.contains("EXTRA_TEMPLATE_ID = \"quick_template_targets.template_id\""));
        assertTrue(contract.contains("EXTRA_CLOSE_REASON"));
        assertTrue(contract.contains("CLOSE_REASON_ORIENTATION_MIGRATION"));
        assertTrue(contract.contains("CLOSE_REASON_USER_BACK"));
        assertTrue(contract.contains("CLOSE_REASON_SAVED"));
        assertTrue(contract.contains("CLOSE_REASON_MISSING_TEMPLATE"));
        assertTrue(activity.contains("Configuration.ORIENTATION_LANDSCAPE"));
        assertTrue(activity.contains("shouldClosePortraitPageInLandscape()"));
        assertTrue(activity.contains(
                "finishWithReason(QuickTemplateTargetSelectionContract.CLOSE_REASON_ORIENTATION_MIGRATION)"));
        assertTrue(activity.contains("new QuickTemplateTargetsPresentationController(this)"));
        assertTrue(activity.contains("targetsController.dispose();"));
        assertTrue(activity.contains("finish();"));
        assertFalse(activity.contains("quickTemplateStore.setSelectedPackages(template.id, selectedPackages)"));
        assertFalse(composeHost.contains("controller::onIconVisible"));
        assertTrue(composeHost.contains("result.messageResId"));
        assertTrue(composeContent.contains("TargetSearchCard("));
        assertTrue(composeContent.contains("R.drawable.ic_search_24"));
        assertTrue(composeContent.contains("R.drawable.ic_close_24"));
        assertTrue(composeContent.contains("R.drawable.ic_tune_24"));
        assertTrue(composeContent.contains("onValueChange = onQueryChanged"));
        assertTrue(composeContent.contains("onValueChange = onSelected"));
        assertTrue(composeContent.contains("AndroidView("));
        assertFalse(composeContent.contains("onIconVisible(app.packageName)"));
        assertTrue(composeContent.contains("rememberBottomSheetState("));
        assertTrue(composeContent.contains("enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)"));
        assertTrue(composeContent.contains("navigationBarsPadding()"));
        assertTrue(composeContent.contains("SecondaryPageTopBar("));
        assertFalse(composeContent.contains("TopAppBar("));
        assertTrue(presentation.contains("templates.setSelectedPackages(templateId, selectedPackages)"));
        assertTrue(presentation.contains("pruneSelection()"));
        assertTrue(presentation.contains("catalog.loadInstalledAppCatalogWithIcons(false)"));
        assertTrue(presentation.contains("item.icon"));
        assertTrue(presentation.contains("catch (Throwable throwable)"));
        assertTrue(presentation.contains(
                "quick template target presentation load failed"));
        assertTrue(presentation.contains("loading = false;"));
        assertTrue(targetsBinder.contains("quickTemplateStore.setSelectedPackages(template.id, selectedPackages)"));
        assertTrue(targetsBinder.contains("pruneSelectedPackagesToInstalledApps(selectedPackages, allTargetItems)"));
        assertTrue(targetsBinder.contains("DpisApplication.getActiveHookConfigStore(activity)"));
        assertTrue(targetsBinder.contains("new PackageConfigRepository("));
        assertTrue(targetsBinder.contains("installedAppCatalogCoordinator.loadInstalledAppCatalogWithIcons(false)"));
        assertTrue(targetsBinder.contains(
                "packageConfigRepository.hasRealPackageConfig(item.packageName)"));
        assertTrue(targetsBinder.contains("FILTER_PREFS_NAME = \"quick_template_target_filters\""));
        assertTrue(targetsBinder.contains("KEY_FILTER_SHOW_SYSTEM_APPS"));
        assertTrue(targetsBinder.contains("KEY_FILTER_HIDE_CONFIGURED_APPS"));
        assertFalse(targetsBinder.contains("AppListFilterStateStore"));
        assertTrue(targetsBinder.contains("hideConfiguredApps && item.configured && !selected"));
        assertTrue(targetsBinder.contains("matchesTargetFilters("));
        assertTrue(targetsBinder.contains("R.layout.dialog_quick_template_target_filters"));
        assertTrue(targetsBinder.contains("loadTargetApps()"));
        assertTrue(targetsBinder.contains("buildTargetItems()"));
        assertTrue(targetsBinder.contains("onTargetAppsLoaded("));
        assertTrue(targetsBinder.contains("applyTargetFilters()"));
        assertFalse(targetsBinder.contains("private void filterApps("));
        assertTrue(targetsBinder.contains("appLoadExecutor.execute("));
        assertTrue(targetsBinder.contains("item.icon"));
        assertTrue(targetsBinder.contains("quick template target list load failed"));
        assertTrue(targetsBinder.contains("if (disposed)"));
        assertFalse(targetsBinder.contains("applicationInfo.loadIcon(packageManager)"));
        assertFalse(targetsBinder.contains("getInstalledApplications("));
        assertTrue(adapter.contains("MaterialCheckBox"));
        assertTrue(adapter.contains("quick_template_target_icon"));
        assertTrue(adapter.contains("holder.icon.setImageDrawable(item.icon);"));
        assertTrue(adapter.contains("holder.iconSkeleton.setVisibility(View.VISIBLE);"));
        assertTrue(adapter.contains("iconResolveRequestListener.onIconResolveRequested(item.packageName);"));
        assertTrue(composeContent.contains("if (icon == null)"));
        assertTrue(composeContent.contains("surfaceVariant"));
        assertTrue(adapter.contains("quick_template_target_configured_badge"));
        assertFalse(adapter.contains("selectionListener.onSelectionChanged(item.packageName, selected);"));
        assertTrue(layout.contains("android:id=\"@+id/quick_template_targets_search_input\""));
        assertTrue(layout.contains("android:id=\"@+id/quick_template_targets_search_clear_button\""));
        assertTrue(layout.contains("android:id=\"@+id/quick_template_targets_filter_button\""));
        assertTrue(layout.contains("@dimen/main_search_card_height"));
        assertTrue(layout.contains("android:layout_marginTop=\"@dimen/template_target_list_container_spacing_top\""));
        assertTrue(layout.contains("@drawable/ic_search_24"));
        assertTrue(layout.contains("@drawable/ic_tune_24"));
        assertTrue(layout.contains("android:id=\"@+id/quick_template_targets_save_button\""));
        assertTrue(layout.contains("android:layout_marginTop=\"@dimen/template_target_save_button_margin_top\""));
        assertTrue(layout.contains("app:tint=\"?attr/colorOnSurface\""));
        assertTrue(landLayout.contains("android:id=\"@+id/quick_template_targets_detail_root\""));
        assertTrue(landLayout.contains("android:id=\"@+id/quick_template_targets_save_button\""));
        assertTrue(landLayout.contains("android:id=\"@+id/quick_template_targets_filter_button\""));
        assertTrue(landLayout.contains("android:id=\"@+id/quick_template_targets_list\""));
        assertFalse(landLayout.contains("quick_template_targets_back_button"));
        assertFalse(landLayout.contains("@layout/activity_quick_template_targets"));
        assertTrue(filterLayout.contains("@+id/quick_template_targets_filter_show_system_switch"));
        assertTrue(filterLayout.contains("@+id/quick_template_targets_filter_hide_configured_switch"));
        assertTrue(filterLayout.contains("@string/quick_template_targets_filter_hide_configured"));
        assertFalse(filterLayout.contains("filter_width_only_switch"));
        assertFalse(filterLayout.contains("filter_font_only_switch"));
        assertTrue(itemLayout.contains("android:id=\"@+id/quick_template_target_icon\""));
        assertTrue(itemLayout.contains("android:id=\"@+id/quick_template_target_icon_skeleton\""));
        assertTrue(itemLayout.contains("@drawable/bg_app_icon_skeleton_mask"));
        assertTrue(itemLayout.contains("@dimen/template_target_icon_size"));
        assertTrue(itemLayout.contains("?attr/textAppearanceTitleSmall"));
        assertTrue(itemLayout.contains("@string/quick_template_targets_configured_badge"));
        assertTrue(mainActivity.contains("showQuickTemplateTargets(templateId);"));
        assertTrue(mainActivity.contains("startQuickTemplateTargetSelectionActivity(selection.templateId);"));
        assertTrue(mainActivity.contains("startQuickTemplateTargetSelectionActivity(templateId);"));
        assertTrue(mainActivity.contains("REQUEST_QUICK_TEMPLATE_TARGETS"));
        assertTrue(mainActivity.contains("quickTemplateTargetSelectionActivityStarted"));
        assertTrue(mainActivity.contains("quickTemplateTargetSelectionActivityStarted = true;"));
        assertTrue(mainActivity.contains("quickTemplateTargetSelectionActivityStarted = false;"));
        assertTrue(mainActivity.contains("startActivityForResult(intent, REQUEST_QUICK_TEMPLATE_TARGETS);"));
        assertTrue(mainActivity.contains("new InstalledAppCatalogCoordinator("));
        assertTrue(mainActivity.contains("requestCode == REQUEST_QUICK_TEMPLATE_TARGETS"));
        assertTrue(mainActivity.contains("QuickTemplateTargetCarrierState.shouldStartPortraitActivity("));
        assertTrue(mainActivity.contains("QuickTemplateTargetCarrierState.shouldClearPendingAfterResult("));
        assertTrue(mainActivity.contains("quickTemplateTargetCloseReason(data)"));
        assertTrue(mainActivity.contains("QuickTemplateTargetSelectionContract.closeReasonFrom(reason)"));
        assertTrue(carrierState.contains("enum CloseReason"));
        assertTrue(carrierState.contains("ORIENTATION_MIGRATION"));
        assertTrue(mainActivity.contains("TemplateDetailKind.QUICK_TEMPLATE_TARGETS"));
        assertTrue(mainActivity.contains("R.layout.view_land_quick_template_targets_detail"));
        assertTrue(mainActivity.contains("new QuickTemplateTargetsBinder("));
        assertTrue(mainActivity.contains("QuickTemplateTargetSelectionContract.EXTRA_TEMPLATE_ID"));
        String showTargetsMethod = mainActivity.substring(
                mainActivity.indexOf("private void showQuickTemplateTargets("),
                mainActivity.indexOf("private void startQuickTemplateTargetSelectionActivity("));
        assertFalse(showTargetsMethod.contains("clearTemplateDetailSelection();"));
        assertTrue(binder.contains("void select(String templateId)"));
        assertFalse(targetsBinder.contains("target_packages"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
