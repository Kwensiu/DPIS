package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;

public class QuickTemplateEditSheetDialogSourceSmokeTest {
    @Test
    public void activityAndLayoutExposeDedicatedQuickTemplateEditPage() throws IOException {
        String manifest = read("src/main/AndroidManifest.xml");
        String source = read("src/main/java/com/dpis/module/QuickTemplateEditSheetDialog.java");
        String editorBinder = read("src/main/java/com/dpis/module/QuickTemplateEditorBinder.java");
        String saveHandler = read("src/main/java/com/dpis/module/QuickTemplateSaveHandler.java");
        String layout = read("src/main/res/layout/dialog_quick_template_edit_sheet.xml");
        String sharedTemplateFields = read("src/main/res/layout/view_template_config_sheet_fields.xml");

        assertFalse(manifest.contains(".QuickTemplateEditActivity"));
        assertTrue(source.contains("BottomSheetDialog"));
        assertTrue(source.contains("R.layout.dialog_quick_template_edit_sheet"));
        assertTrue(source.contains("dialog.getBehavior().setFitToContents(true)"));
        assertTrue(source.contains("dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED)"));
        assertTrue(source.contains("applyWrapContentSheetHeight();"));
        assertTrue(source.contains("params.height = ViewGroup.LayoutParams.WRAP_CONTENT;"));
        assertTrue(source.contains("QuickTemplateEditorBinder.bind("));
        assertFalse(source.contains("FormInputFocusBinder.bindDismissOnOutsideTouch"));
        assertTrue(editorBinder.contains("FormInputFocusBinder.bindDismissOnOutsideTouch"));
        assertTrue(editorBinder.contains("FormInputFocusBinder.clearFocusAndHideIme"));
        assertTrue(editorBinder.contains("nameInputView"));
        assertFalse(source.contains("setHalfExpandedRatio"));
        assertTrue(source.contains("show(Activity activity, String templateId, Runnable onUpdated)"));
        assertTrue(editorBinder.contains("new QuickTemplateStore(preferences)"));
        assertTrue(editorBinder.contains("new QuickTemplateSaveHandler()"));
        assertTrue(editorBinder.contains("saveHandler.save(quickTemplateStore"));
        assertTrue(editorBinder.contains("quickTemplateStore.delete("));
        assertTrue(editorBinder.contains("quickTemplateStore.read("));
        assertTrue(editorBinder.contains("quickTemplateStore.newTemplateId()"));
        assertTrue(editorBinder.contains("showToast(R.string.quick_template_missing)"));
        assertTrue(editorBinder.contains("MaterialAlertDialogBuilder"));
        assertTrue(editorBinder.contains("R.string.quick_template_delete_title"));
        assertTrue(editorBinder.contains("DialogWindowSizer.applyStandardWidth(dialog, activity)"));
        assertTrue(editorBinder.contains("R.string.quick_template_name_required"));
        assertTrue(editorBinder.contains("R.string.quick_template_name_duplicate"));
        assertTrue(editorBinder.contains("bindNameErrorState(nameValid, R.string.quick_template_name_required)"));
        assertTrue(editorBinder.contains("bindNameErrorState(false, R.string.quick_template_name_required)"));
        assertTrue(editorBinder.contains("bindNameErrorState(false, R.string.quick_template_name_duplicate)"));
        assertTrue(saveHandler.contains("R.string.quick_template_save_success"));
        assertTrue(saveHandler.contains("R.string.quick_template_save_failed"));
        assertTrue(editorBinder.contains("R.string.quick_template_delete_success"));
        assertTrue(editorBinder.contains("R.string.quick_template_delete_failed"));
        assertTrue(editorBinder.contains("resetTemplateConfig()"));
        assertTrue(editorBinder.contains("FontHookDomainRegistry.recommendedTemplateKnownDomains()"));
        assertTrue(editorBinder.contains("normalizeTemplateHookDomainsRaw(state.draftFontHookDomainsRaw)"));
        assertFalse(editorBinder.contains("target_packages"));
        assertTrue(editorBinder.contains("TextUtils.isEmpty(name)"));
        assertTrue(editorBinder.contains("textOf(nameInputView)"));
        assertTrue(editorBinder.contains("FontHookDomainDialog.show(activity,"));
        assertTrue(editorBinder.contains("FontApplyMode.FIELD_REWRITE.equals("));
        assertTrue(editorBinder.contains("AppConfigDialogBinder.resolveFontMode(fontModeToggle)"));
        assertTrue(editorBinder.contains("AppConfigDialogBinder.switchViewportTargetType"));
        assertTrue(editorBinder.contains("AppConfigDialogBinder.resolveViewportMode"));
        assertTrue(editorBinder.contains("AppConfigDialogBinder.resolveFontMode"));
        assertTrue(editorBinder.contains("viewportApplyModeButton.setVisibility(View.GONE);"));
        assertFalse(editorBinder.contains("showViewportApplyModeDialog()"));
        assertFalse(editorBinder.contains("toggleFontMode(fontModeToggle)"));
        assertTrue(editorBinder.contains("activity.startActivity(new Intent(activity, FontLibraryActivity.class));"));
        assertTrue(source.contains("dialog::dismiss"));
        assertTrue(editorBinder.contains("if (onUpdated != null)"));
        assertTrue(layout.contains("@layout/view_sheet_unsaved_badge_handle"));
        assertFalse(layout.contains("android:fillViewport=\"true\""));
        assertTrue(layout.contains("@dimen/template_config_sheet_padding_bottom"));
        assertTrue(layout.contains("@dimen/sheet_unsaved_badge_min_height"));
        assertTrue(layout.contains("@dimen/dialog_app_config_padding_horizontal"));
        assertTrue(layout.contains("android:id=\"@+id/quick_template_edit_subtitle\""));
        assertTrue(layout.contains("@string/quick_template_edit_sheet_subtitle"));
        assertTrue(layout.contains("@dimen/dialog_sheet_header_subtitle_spacing_top"));
        assertTrue(layout.contains("android:id=\"@+id/quick_template_edit_reset_button\""));
        assertTrue(layout.contains("@drawable/ic_reset_settings_24"));
        assertTrue(layout.contains("android:id=\"@+id/quick_template_edit_delete_button\""));
        assertTrue(layout.contains("@drawable/ic_delete_24"));
        assertTrue(layout.contains("@drawable/bg_quick_template_delete_button"));
        assertTrue(layout.contains("android:id=\"@+id/quick_template_edit_name_layout\""));
        assertTrue(layout.contains("android:id=\"@+id/quick_template_edit_name_input\""));
        assertTrue(sharedTemplateFields.contains("android:id=\"@+id/template_config_viewport_input_layout\""));
        assertTrue(sharedTemplateFields.contains("android:id=\"@+id/template_config_viewport_apply_mode_button\""));
        assertTrue(sharedTemplateFields.contains("android:id=\"@+id/template_config_font_scale_input_layout\""));
        assertTrue(sharedTemplateFields.contains("android:id=\"@+id/template_config_typeface_selector_button\""));
        assertTrue(sharedTemplateFields.contains("android:id=\"@+id/template_config_font_hook_domains_button\""));
        assertTrue(sharedTemplateFields.contains("android:id=\"@+id/template_config_save_button\""));
        assertTrue(sharedTemplateFields.contains("android:id=\"@+id/template_config_delete_button\""));
        assertTrue(sharedTemplateFields.contains("android:clickable=\"false\""));
        assertTrue(sharedTemplateFields.contains("android:id=\"@+id/template_config_viewport_mode_system_label\""));
        assertTrue(sharedTemplateFields.contains("android:id=\"@+id/template_config_font_mode_compat_label\""));
        assertTrue(sharedTemplateFields.contains("@dimen/template_config_sheet_save_row_spacing_bottom"));
        assertTrue(editorBinder.contains("footerResetButton.setVisibility(View.GONE);"));
        assertTrue(editorBinder.contains("footerDeleteButton.setVisibility(View.GONE);"));
        assertTrue(layout.contains("@string/quick_template_name_hint"));
        assertTrue(sharedTemplateFields.contains("@string/dialog_viewport_apply_strategy_title"));
        assertTrue(layout.contains("@layout/view_template_config_sheet_fields"));
        assertTrue(sharedTemplateFields.contains("WarnOutline"));
        assertFalse(layout.contains("dialog_disable_button"));
        assertFalse(layout.contains("dialog_scope_button"));
        assertFalse(layout.contains("dialog_start_button"));
        assertFalse(layout.contains("dialog_stop_button"));
        assertFalse(layout.contains("dialog_dpis_enable_button"));
    }

    @Test
    public void mainActivityAndTemplateWorkspaceWireQuickTemplateActions() throws IOException {
        String mainActivity = read("src/main/java/com/dpis/module/MainActivity.java");
        String binder = read("src/main/java/com/dpis/module/TemplateWorkspaceBinder.java");
        String adapter = read("src/main/java/com/dpis/module/QuickTemplateListAdapter.java");
        String layout = read("src/main/res/layout/template_workspace.xml");

        assertTrue(mainActivity.contains("createQuickTemplateActions()"));
        assertTrue(mainActivity.contains("showQuickTemplateEditor(templateId);"));
        assertTrue(mainActivity.contains("showQuickTemplateEditor(null);"));
        assertTrue(mainActivity.contains("QuickTemplateEditSheetDialog.show("));
        assertTrue(mainActivity.contains("QuickTemplateEditorBinder.bind("));
        assertTrue(mainActivity.contains("applyTemplateDetailInsets("));
        assertTrue(mainActivity.contains("templateId"));
        assertTrue(mainActivity.contains("null"));
        assertTrue(mainActivity.contains("this::onTemplateEditorUpdated"));
        assertTrue(binder.contains("interface QuickTemplateActions"));
        assertTrue(binder.contains("void edit(String templateId)"));
        assertTrue(binder.contains("void create()"));
        assertTrue(binder.contains("quickTemplateActions.create()"));
        assertTrue(binder.contains("quickTemplateActions.edit("));
        assertTrue(adapter.contains("interface TemplateAction"));
        assertTrue(adapter.contains("TemplateAction editAction"));
        assertFalse(adapter.contains("Runnable placeholderAction"));
        assertTrue(layout.contains("android:id=\"@+id/quick_template_create_button\""));
        assertTrue(layout.contains("@string/quick_template_create_action"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
