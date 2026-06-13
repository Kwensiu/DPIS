package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

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
        assertTrue(source.contains("showManualUpdatePromptDialog(manifest);"));
        assertTrue(source.contains("showCenteredManualUpdatePromptDialog("));
        assertTrue(source.contains("ReleaseNotesMarkdownRenderer.render("));
        assertTrue(!source.contains("ReleaseNotesMarkdownLite.format("));
        assertTrue(source.contains("DialogWindowSizer.applyLargeWidth(dialog, this)"));
        assertTrue(manifestFetcherSource.contains("final class UpdateManifestFetcher"));
        assertTrue(source.contains("UpdateAvailableDialog.create("));
        assertTrue(dialogSource.contains("R.id.update_dialog_cancel_button"));
        assertTrue(dialogLayout.contains("android:id=\"@+id/update_dialog_cancel_button\""));
        assertTrue(dialogLayout.contains("com.dpis.module.MaxHeightNestedScrollView"));
        assertTrue(dialogLayout.contains("android:scrollbars=\"vertical\""));
        assertTrue(dialogLayout.contains("android:fadeScrollbars=\"false\""));
        assertTrue(dialogLayout.contains("@dimen/dialog_surface_padding_horizontal"));
        assertTrue(dialogLayout.contains("@dimen/dialog_status_icon_size"));
        assertTrue(dialogLayout.contains("@dimen/update_dialog_primary_button_spacing_top"));
        assertTrue(dialogLayout.contains("@dimen/update_dialog_cancel_button_spacing_top"));
        assertTrue(!source.contains("private void executeApkDownload("));
        assertTrue(!source.contains("private void verifyDownloadedApk("));
        assertTrue(!source.contains("private static StartupUpdateManifest fetchUpdateManifest("));
        assertTrue(!source.contains("private static String formatBytes("));
        assertTrue(!source.contains("private static int compareSemVer("));
    }

    @Test
    public void aboutActivityDoesNotApplyLocalApkSignatureGate() throws IOException {
        String source = read("src/main/java/com/dpis/module/AboutActivity.java");
        String coordinatorSource = read("src/main/java/com/dpis/module/UpdateDownloadCoordinator.java");
        String packageHandlerSource = read("src/main/java/com/dpis/module/StartupUpdatePackageHandler.java");

        assertTrue(!source.contains("extractSigningFingerprints"));
        assertTrue(!source.contains("about_update_download_untrusted"));
        assertTrue(!coordinatorSource.contains("verifyDownloadedApk("));
        assertTrue(!coordinatorSource.contains("UntrustedUpdateException"));
        assertTrue(!packageHandlerSource.contains("verifyDownloadedApk("));
        assertTrue(!packageHandlerSource.contains("extractSigningFingerprints"));
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

    @Test
    public void aboutLayoutUsesNamedDimensions() throws IOException {
        String layout = read("src/main/res/layout/activity_about.xml");
        String source = read("src/main/java/com/dpis/module/AboutActivity.java");

        assertTrue(layout.contains("android:id=\"@+id/about_toolbar\""));
        assertTrue(layout.contains("android:id=\"@+id/about_scroll\""));
        assertTrue(layout.contains("android:layout_height=\"0dp\""));
        assertTrue(layout.contains("android:layout_weight=\"1\""));
        assertTrue(layout.contains("@dimen/page_toolbar_padding_horizontal"));
        assertTrue(layout.contains("@dimen/about_content_padding_horizontal"));
        assertTrue(layout.contains("@dimen/page_card_corner_radius"));
        assertTrue(layout.contains("@dimen/about_app_card_padding"));
        assertTrue(layout.contains("@dimen/about_divider_margin_horizontal"));
        assertTrue(source.contains("R.id.about_toolbar"));
        assertTrue(source.contains("WindowInsetsBinder.applySafeDrawingPadding(toolbar, false, true, false, false);"));
        assertTrue(source.contains("WindowInsetsBinder.applySafeDrawingPadding(content, false, false, false, true);"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
