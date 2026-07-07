package com.dpis.module;

import com.dpis.module.fonts.FontApplyMode;

import com.dpis.module.appconfig.AppConfigDialogBinder;

import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry;

import com.dpis.module.fonts.hookdomain.FontHookDomainDialog;

import com.dpis.module.templates.GlobalPrefillSaveHandler;

import com.dpis.module.templates.GlobalPrefillStore;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;

public class GlobalPrefillSheetDialogSourceSmokeTest {
    @Test
    public void activityAndLayoutExposeDedicatedGlobalPrefillPageWithoutForbiddenControls()
            throws IOException {
        String manifest = read("src/main/AndroidManifest.xml");
        String source = read("src/main/java/com/dpis/module/templates/GlobalPrefillSheetDialog.java");
        String editorBinder = read("src/main/java/com/dpis/module/templates/GlobalPrefillEditorBinder.java");
        String layout = read("src/main/res/layout/dialog_global_prefill_sheet.xml");
        String sharedTemplateFields = read("src/main/res/layout/view_template_config_sheet_fields.xml");

        assertFalse(manifest.contains(".GlobalPrefillActivity"));
        assertTrue(source.contains("R.layout.dialog_global_prefill_sheet"));
        assertTrue(source.contains("BottomSheetDialog"));
        assertTrue(source.contains("dialog.getBehavior().setFitToContents(true)"));
        assertTrue(source.contains("dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED)"));
        assertTrue(source.contains("applyWrapContentSheetHeight();"));
        assertTrue(source.contains("params.height = ViewGroup.LayoutParams.WRAP_CONTENT;"));
        assertTrue(source.contains("GlobalPrefillEditorBinder.bind("));
        assertFalse(source.contains("FormInputFocusBinder.bindDismissOnOutsideTouch"));
        assertTrue(editorBinder.contains("FormInputFocusBinder.bindDismissOnOutsideTouch"));
        assertTrue(editorBinder.contains("FormInputFocusBinder.clearFocusAndHideIme"));
        assertFalse(source.contains("setHalfExpandedRatio"));
        assertTrue(source.contains("show(Activity activity, Runnable onUpdated)"));
        assertTrue(editorBinder.contains("if (onUpdated != null)"));
        assertTrue(editorBinder.contains("new GlobalPrefillSaveHandler()"));
        assertTrue(editorBinder.contains("FontHookDomainDialog.show(activity,"));
        assertTrue(editorBinder.contains("FontApplyMode.FIELD_REWRITE.equals("));
        assertTrue(editorBinder.contains("AppConfigDialogBinder.resolveFontMode(fontModeToggle)"));
        assertTrue(editorBinder.contains("FontHookDomainRegistry.recommendedTemplateKnownDomains()"));
        assertTrue(editorBinder.contains("normalizeTemplateHookDomainsRaw(state.draftFontHookDomainsRaw)"));
        assertFalse(editorBinder.contains("toggleFontMode(fontModeToggle)"));
        assertTrue(editorBinder.contains("new GlobalPrefillStore(preferences)"));
        assertTrue(editorBinder.contains("activity.startActivity(new Intent(activity, FontLibraryActivity.class));"));
        assertTrue(source.contains("dialog::dismiss"));
        assertTrue(layout.contains("@layout/view_sheet_unsaved_badge_handle"));
        assertTrue(layout.contains("android:id=\"@+id/global_prefill_reset_button\""));
        assertTrue(layout.contains("@drawable/ic_reset_settings_24"));
        assertFalse(layout.contains("android:fillViewport=\"true\""));
        assertTrue(layout.contains("@dimen/template_config_sheet_padding_bottom"));
        assertTrue(layout.contains("@dimen/sheet_unsaved_badge_min_height"));
        assertTrue(layout.contains("@dimen/dialog_app_config_padding_horizontal"));
        assertTrue(sharedTemplateFields.contains("android:id=\"@+id/template_config_viewport_input_layout\""));
        assertTrue(sharedTemplateFields.contains("android:id=\"@+id/template_config_viewport_input\""));
        assertTrue(sharedTemplateFields.contains("android:inputType=\"numberDecimal\""));
        assertTrue(sharedTemplateFields.contains("android:id=\"@+id/template_config_font_scale_input_layout\""));
        assertTrue(sharedTemplateFields.contains("android:id=\"@+id/template_config_typeface_selector_button\""));
        assertTrue(sharedTemplateFields.contains("android:id=\"@+id/template_config_font_hook_domains_button\""));
        assertTrue(sharedTemplateFields.contains("android:id=\"@+id/template_config_reset_button\""));
        assertTrue(sharedTemplateFields.contains("android:id=\"@+id/template_config_save_button\""));
        assertTrue(sharedTemplateFields.contains("android:clickable=\"false\""));
        assertTrue(sharedTemplateFields.contains("@dimen/template_config_sheet_save_row_spacing_bottom"));
        assertTrue(layout.contains("@string/template_workspace_global_prefill_title"));
        assertTrue(layout.contains("@string/template_workspace_global_prefill_subtitle"));
        assertTrue(layout.contains("@dimen/dialog_sheet_header_subtitle_spacing_top"));
        assertTrue(layout.contains("@layout/view_template_config_sheet_fields"));
        assertFalse(layout.contains("dialog_scope_button"));
        assertFalse(layout.contains("dialog_start_button"));
        assertFalse(layout.contains("dialog_restart_button"));
        assertFalse(layout.contains("dialog_stop_button"));
        assertFalse(layout.contains("dialog_dpis_toggle_button"));
        assertFalse(layout.contains("dialog_disable_button"));
    }

    @Test
    public void mainActivityAndTemplateWorkspaceWireGlobalPrefillActions() throws IOException {
        String mainActivity = read("src/main/java/com/dpis/module/MainActivity.java");
        String binder = read("src/main/java/com/dpis/module/templates/TemplateWorkspaceBinder.java");

        assertTrue(mainActivity.contains("new TemplateWorkspaceBinder("));
        assertTrue(mainActivity.contains("createTemplateWorkspaceActions()"));
        assertTrue(mainActivity.contains("showGlobalPrefillEditor();"));
        assertTrue(mainActivity.contains("GlobalPrefillSheetDialog.show("));
        assertTrue(mainActivity.contains("GlobalPrefillEditorBinder.bind("));
        assertTrue(mainActivity.contains("applyTemplateDetailInsets("));
        assertTrue(mainActivity.contains("this::onTemplateEditorUpdated"));
        assertTrue(mainActivity.contains("bindTemplateWorkspace();"));
        assertTrue(binder.contains("interface GlobalPrefillActions"));
        assertTrue(binder.contains("globalPrefillActions.edit();"));
        assertFalse(binder.contains("globalPrefillActions.reset();"));
        assertFalse(binder.contains("editButton.setOnClickListener(v -> showNotWiredToast())"));
        assertFalse(binder.contains("resetButton.setOnClickListener(v -> showNotWiredToast())"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
