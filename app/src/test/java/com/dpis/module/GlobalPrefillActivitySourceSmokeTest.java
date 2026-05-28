package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class GlobalPrefillActivitySourceSmokeTest {
    @Test
    public void activityAndLayoutExposeDedicatedGlobalPrefillPageWithoutForbiddenControls()
            throws IOException {
        String manifest = read("src/main/AndroidManifest.xml");
        String source = read("src/main/java/com/dpis/module/GlobalPrefillActivity.java");
        String layout = read("src/main/res/layout/activity_global_prefill.xml");

        assertTrue(manifest.contains(".GlobalPrefillActivity"));
        assertTrue(source.contains("setContentView(R.layout.activity_global_prefill);"));
        assertTrue(source.contains("new GlobalPrefillSaveHandler()"));
        assertTrue(source.contains("FontHookDomainDialog.show(this,"));
        assertTrue(source.contains("new GlobalPrefillStore(preferences)"));
        assertTrue(source.contains("startActivity(new Intent(GlobalPrefillActivity.this, FontLibraryActivity.class));"));
        assertTrue(source.contains("finish();"));
        assertTrue(layout.contains("android:id=\"@+id/global_prefill_toolbar\""));
        assertTrue(layout.contains("android:id=\"@+id/global_prefill_viewport_input_layout\""));
        assertTrue(layout.contains("android:id=\"@+id/global_prefill_font_scale_input_layout\""));
        assertTrue(layout.contains("android:id=\"@+id/global_prefill_typeface_selector_button\""));
        assertTrue(layout.contains("android:id=\"@+id/global_prefill_font_hook_domains_button\""));
        assertTrue(layout.contains("android:id=\"@+id/global_prefill_reset_button\""));
        assertTrue(layout.contains("android:id=\"@+id/global_prefill_save_button\""));
        assertTrue(layout.contains("@string/global_prefill_page_title"));
        assertTrue(layout.contains("@string/global_prefill_page_description"));
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
        String binder = read("src/main/java/com/dpis/module/TemplateWorkspaceBinder.java");

        assertTrue(mainActivity.contains("new TemplateWorkspaceBinder(this, createTemplateWorkspaceActions())"));
        assertTrue(mainActivity.contains("new Intent(MainActivity.this, GlobalPrefillActivity.class)"));
        assertTrue(mainActivity.contains("showToast(R.string.global_prefill_reset_success);"));
        assertTrue(mainActivity.contains("bindTemplateWorkspace();"));
        assertTrue(binder.contains("interface GlobalPrefillActions"));
        assertTrue(binder.contains("globalPrefillActions.edit();"));
        assertTrue(binder.contains("globalPrefillActions.reset();"));
        assertFalse(binder.contains("editButton.setOnClickListener(v -> showNotWiredToast())"));
        assertFalse(binder.contains("resetButton.setOnClickListener(v -> showNotWiredToast())"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
