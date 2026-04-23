package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class AboutActivitySourceSmokeTest {
    @Test
    public void aboutActivityWiresOpenSourceLicenseEntryToDedicatedPage() throws IOException {
        String source = read("src/main/java/com/dpis/module/AboutActivity.java");

        assertTrue(source.contains("R.id.row_about_open_source_license"));
        assertTrue(source.contains("R.string.open_source_license"));
        assertTrue(source.contains("R.string.open_source_license_settings_description"));
        assertTrue(source.contains("new Intent(this, OpenSourceLicenseActivity.class)"));
    }

    @Test
    public void aboutActivityUpdateFlowUsesDirectApkUrlAndHttpsOnly() throws IOException {
        String source = read("src/main/java/com/dpis/module/AboutActivity.java");
        String dialogSource = read("src/main/java/com/dpis/module/UpdateAvailableDialog.java");
        String dialogLayout = read("src/main/res/layout/dialog_update_available.xml");

        assertTrue(source.contains("String downloadUrl = manifest.apkUrl;"));
        assertTrue(source.contains("R.string.about_update_action_view_release"));
        assertTrue(source.contains("\"https\".equalsIgnoreCase(scheme)"));
        assertTrue(source.contains("UpdateAvailableDialog.create("));
        assertTrue(dialogSource.contains("R.id.update_dialog_cancel_button"));
        assertTrue(dialogLayout.contains("android:id=\"@+id/update_dialog_cancel_button\""));
    }

    @Test
    public void aboutActivityVerifiesDownloadedApkSignatureBeforeInstall() throws IOException {
        String source = read("src/main/java/com/dpis/module/AboutActivity.java");

        assertTrue(source.contains("verifyDownloadedApk(targetFile);"));
        assertTrue(source.contains("extractSigningFingerprints"));
        assertTrue(source.contains("about_update_download_untrusted"));
    }

    @Test
    public void manifestDeclaresOpenSourceLicenseActivity() throws IOException {
        String manifest = read("src/main/AndroidManifest.xml");

        assertTrue(manifest.contains("android:name=\".OpenSourceLicenseActivity\""));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
