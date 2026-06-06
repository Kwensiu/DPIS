package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;

public class QuickTemplateTargetSelectionSourceSmokeTest {
    @Test
    public void targetSelectionPagePersistsSelectedPackagesAndShowsConfiguredBadge() throws IOException {
        String manifest = read("src/main/AndroidManifest.xml");
        String activity = read("src/main/java/com/dpis/module/QuickTemplateTargetSelectionActivity.java");
        String adapter = read("src/main/java/com/dpis/module/QuickTemplateTargetAdapter.java");
        String layout = read("src/main/res/layout/activity_quick_template_targets.xml");
        String filterLayout = read("src/main/res/layout/dialog_quick_template_target_filters.xml");
        String itemLayout = read("src/main/res/layout/item_quick_template_target_app.xml");
        String mainActivity = read("src/main/java/com/dpis/module/MainActivity.java");
        String binder = read("src/main/java/com/dpis/module/TemplateWorkspaceBinder.java");

        assertTrue(manifest.contains(".QuickTemplateTargetSelectionActivity"));
        assertTrue(activity.contains("EXTRA_TEMPLATE_ID = \"quick_template_targets.template_id\""));
        assertTrue(activity.contains("quickTemplateStore.setSelectedPackages(template.id, selectedPackages)"));
        assertTrue(activity.contains("pruneSelectedPackagesToInstalledApps(selectedPackages, allTargetItems)"));
        assertTrue(activity.contains("configStore = getHookConfigStore();"));
        assertTrue(activity.contains("return DpisApplication.getActiveHookConfigStore(this);"));
        assertTrue(activity.contains("installedAppCatalogCoordinator.loadInstalledAppCatalog(false)"));
        assertTrue(activity.contains("configStore.hasRealPackageConfig(item.packageName)"));
        assertTrue(activity.contains("FILTER_PREFS_NAME = \"quick_template_target_filters\""));
        assertTrue(activity.contains("KEY_FILTER_SHOW_SYSTEM_APPS"));
        assertTrue(activity.contains("KEY_FILTER_HIDE_CONFIGURED_APPS"));
        assertFalse(activity.contains("AppListFilterStateStore"));
        assertTrue(activity.contains("if (hideConfiguredApps && item.configured)"));
        assertTrue(activity.contains("matchesTargetFilters("));
        assertTrue(activity.contains("R.layout.dialog_quick_template_target_filters"));
        assertTrue(activity.contains("loadTargetApps()"));
        assertTrue(activity.contains("buildTargetItems()"));
        assertTrue(activity.contains("onTargetAppsLoaded("));
        assertTrue(activity.contains("applyTargetFilters()"));
        assertFalse(activity.contains("private void filterApps("));
        assertTrue(activity.contains("appLoadExecutor.execute("));
        assertTrue(activity.contains("this::onIconLoadRequested"));
        assertTrue(activity.contains("quick template target list load failed"));
        assertTrue(activity.contains("if (destroyed)"));
        assertFalse(activity.contains("applicationInfo.loadIcon(packageManager)"));
        assertFalse(activity.contains("getInstalledApplications("));
        assertTrue(adapter.contains("MaterialCheckBox"));
        assertTrue(adapter.contains("quick_template_target_icon"));
        assertTrue(adapter.contains("holder.icon.setImageDrawable(item.icon);"));
        assertTrue(adapter.contains("holder.iconSkeleton.setVisibility(View.VISIBLE);"));
        assertTrue(adapter.contains("iconResolveRequestListener.onIconResolveRequested(item.packageName);"));
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
        assertTrue(mainActivity.contains("QuickTemplateTargetSelectionActivity.EXTRA_TEMPLATE_ID"));
        assertTrue(binder.contains("void select(String templateId)"));
        assertFalse(activity.contains("target_packages"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
