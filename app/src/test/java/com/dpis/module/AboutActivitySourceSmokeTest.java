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
    public void aboutActivityUpdateFlowUsesSharedDownloadCoordinatorAndHttpsOnly() throws IOException {
        String source = read("src/main/java/com/dpis/module/AboutActivity.java");
        String dialogSource = read("src/main/java/com/dpis/module/UpdateAvailableDialog.java");
        String manifestFetcherSource = read("src/main/java/com/dpis/module/UpdateManifestFetcher.java");
        String dialogLayout = read("src/main/res/layout/dialog_update_available.xml");

        assertTrue(source.contains("String downloadUrl = manifest.apkUrl;"));
        assertTrue(source.contains("UpdateManifestFetcher.fetch("));
        assertTrue(source.contains("R.string.about_update_action_view_release"));
        assertTrue(source.contains("UpdateDownloadCoordinator.showDialogIdleState("));
        assertTrue(source.contains("updateDownloadCoordinator.startDownload("));
        assertTrue(manifestFetcherSource.contains("final class UpdateManifestFetcher"));
        assertTrue(source.contains("UpdateAvailableDialog.create("));
        assertTrue(dialogSource.contains("R.id.update_dialog_cancel_button"));
        assertTrue(dialogLayout.contains("android:id=\"@+id/update_dialog_cancel_button\""));
        assertTrue(!source.contains("private void executeApkDownload("));
        assertTrue(!source.contains("private void verifyDownloadedApk("));
        assertTrue(!source.contains("private static StartupUpdateManifest fetchUpdateManifest("));
        assertTrue(!source.contains("private static String formatBytes("));
        assertTrue(!source.contains("private static int compareSemVer("));
    }

    @Test
    public void aboutActivityDelegatesSignatureVerificationToSharedHandler() throws IOException {
        String source = read("src/main/java/com/dpis/module/AboutActivity.java");
        String coordinatorSource = read("src/main/java/com/dpis/module/UpdateDownloadCoordinator.java");

        assertTrue(!source.contains("extractSigningFingerprints"));
        assertTrue(!source.contains("about_update_download_untrusted"));
        assertTrue(coordinatorSource.contains("packageHandler.verifyDownloadedApk("));
        assertTrue(coordinatorSource.contains("StartupUpdatePackageHandler.UntrustedUpdateException"));
    }

    @Test
    public void aboutActivityTracksDownloadStateForCoordinatorCancelFlow() throws IOException {
        String source = read("src/main/java/com/dpis/module/AboutActivity.java");

        assertTrue(source.contains("private volatile boolean updateDownloadInProgress = false;"));
        assertTrue(source.contains("private volatile boolean updateDownloadCancelRequested = false;"));
        assertTrue(source.contains("new UpdateCoordinator.State("));
        assertTrue(source.contains("updateDownloadInProgress,"));
        assertTrue(source.contains("updateDownloadCancelRequested);"));
        assertTrue(!source.contains("return UpdateCoordinator.State.empty();"));
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
