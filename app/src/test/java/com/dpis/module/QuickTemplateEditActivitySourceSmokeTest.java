package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class QuickTemplateEditActivitySourceSmokeTest {
    @Test
    public void activityAndLayoutExposeDedicatedQuickTemplateEditPage() throws IOException {
        String manifest = read("src/main/AndroidManifest.xml");
        String source = read("src/main/java/com/dpis/module/QuickTemplateEditActivity.java");
        String saveHandler = read("src/main/java/com/dpis/module/QuickTemplateSaveHandler.java");
        String layout = read("src/main/res/layout/activity_quick_template_edit.xml");

        assertTrue(manifest.contains(".QuickTemplateEditActivity"));
        assertTrue(source.contains("extends LocalizedActivity"));
        assertTrue(source.contains("setContentView(R.layout.activity_quick_template_edit);"));
        assertTrue(source.contains("EXTRA_TEMPLATE_ID = \"quick_template_edit.template_id\""));
        assertTrue(source.contains("new QuickTemplateStore(preferences)"));
        assertTrue(source.contains("new QuickTemplateSaveHandler()"));
        assertTrue(source.contains("saveHandler.save(quickTemplateStore"));
        assertTrue(source.contains("quickTemplateStore.delete("));
        assertTrue(source.contains("quickTemplateStore.read("));
        assertTrue(source.contains("quickTemplateStore.newTemplateId()"));
        assertTrue(source.contains("MaterialAlertDialogBuilder"));
        assertTrue(source.contains("R.string.quick_template_delete_title"));
        assertTrue(source.contains("R.string.quick_template_name_required"));
        assertTrue(saveHandler.contains("R.string.quick_template_save_success"));
        assertTrue(saveHandler.contains("R.string.quick_template_save_failed"));
        assertTrue(source.contains("R.string.quick_template_delete_success"));
        assertTrue(source.contains("R.string.quick_template_delete_failed"));
        assertFalse(source.contains("target_packages"));
        assertTrue(source.contains("TextUtils.isEmpty(name)"));
        assertTrue(source.contains("textOf(nameInputView)"));
        assertTrue(source.contains("FontHookDomainDialog.show(this,"));
        assertTrue(source.contains("AppConfigDialogBinder.toggleViewportMode"));
        assertTrue(source.contains("AppConfigDialogBinder.toggleFontMode"));
        assertTrue(source.contains("AppConfigDialogBinder.resolveViewportMode"));
        assertTrue(source.contains("AppConfigDialogBinder.resolveFontMode"));
        assertTrue(source.contains("protected void onSaveInstanceState(Bundle outState)"));
        assertTrue(source.contains("STATE_TEMPLATE_ID"));
        assertTrue(source.contains("STATE_NAME_INPUT"));
        assertTrue(source.contains("STATE_VIEWPORT_TARGET_TYPE"));
        assertTrue(source.contains("STATE_TYPEFACE_ID"));
        assertTrue(source.contains("STATE_FONT_HOOK_DOMAINS"));
        assertTrue(source.contains("startActivity(new Intent(QuickTemplateEditActivity.this, FontLibraryActivity.class));"));
        assertTrue(source.contains("finish();"));
        assertTrue(layout.contains("android:id=\"@+id/quick_template_edit_toolbar\""));
        assertTrue(layout.contains("android:id=\"@+id/quick_template_edit_name_layout\""));
        assertTrue(layout.contains("android:id=\"@+id/quick_template_edit_name_input\""));
        assertTrue(layout.contains("android:id=\"@+id/quick_template_edit_viewport_input_layout\""));
        assertTrue(layout.contains("android:id=\"@+id/quick_template_edit_font_scale_input_layout\""));
        assertTrue(layout.contains("android:id=\"@+id/quick_template_edit_typeface_selector_button\""));
        assertTrue(layout.contains("android:id=\"@+id/quick_template_edit_font_hook_domains_button\""));
        assertTrue(layout.contains("android:id=\"@+id/quick_template_edit_save_button\""));
        assertTrue(layout.contains("android:id=\"@+id/quick_template_edit_delete_button\""));
        assertTrue(layout.contains("@string/quick_template_name_hint"));
        assertTrue(layout.contains("WarnOutline"));
        assertFalse(layout.contains("dialog_disable_button"));
    }

    @Test
    public void mainActivityAndTemplateWorkspaceWireQuickTemplateActions() throws IOException {
        String mainActivity = read("src/main/java/com/dpis/module/MainActivity.java");
        String binder = read("src/main/java/com/dpis/module/TemplateWorkspaceBinder.java");
        String adapter = read("src/main/java/com/dpis/module/QuickTemplateListAdapter.java");
        String layout = read("src/main/res/layout/template_workspace.xml");

        assertTrue(mainActivity.contains("createQuickTemplateActions()"));
        assertTrue(mainActivity.contains("new Intent(MainActivity.this, QuickTemplateEditActivity.class)"));
        assertTrue(mainActivity.contains("QuickTemplateEditActivity.EXTRA_TEMPLATE_ID"));
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
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
