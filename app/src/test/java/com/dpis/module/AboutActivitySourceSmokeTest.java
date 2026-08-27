package com.dpis.module;

import com.dpis.module.updates.UpdateManifestFetcher;

import com.dpis.module.updates.UpdateDownloadCoordinator;

import com.dpis.module.updates.UpdateCoordinator;

import com.dpis.module.updates.UpdateAvailableDialog;

import com.dpis.module.updates.StartupUpdatePackageHandler;

import com.dpis.module.updates.StartupUpdateManifest;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class AboutActivitySourceSmokeTest {
    @Test
    public void aboutActivityWiresOpenSourceLicenseEntryToDedicatedPage() throws IOException {
        String source = read("src/main/java/com/dpis/module/about/AboutActivity.java");
        String content = read("src/main/java/com/dpis/module/ui/compose/AboutContent.kt");

        assertTrue(source.contains("SupportActivityContent.installAbout("));
        assertTrue(content.contains("R.string.open_source_license"));
        assertTrue(content.contains("R.string.open_source_license_settings_description"));
        assertTrue(source.contains("new Intent(this, OpenSourceLicenseActivity.class)"));
    }

    @Test
    public void aboutActivityUpdateFlowUsesSharedDownloadCoordinatorAndHttpsOnly() throws IOException {
        String source = read("src/main/java/com/dpis/module/about/AboutActivity.java");
        String dialogSource = read("src/main/java/com/dpis/module/updates/UpdateAvailableDialog.kt");
        String textInteropSource = read(
                "src/main/java/com/dpis/module/ui/compose/AndroidTextInterop.kt");
        String manifestFetcherSource = read("src/main/java/com/dpis/module/updates/UpdateManifestFetcher.java");

        assertTrue(source.contains("UpdateManifestFetcher.fetch("));
        assertTrue(source.contains("updatePromptDialogCoordinator.showUpdateAvailableDialog("));
        assertTrue(source.contains("new UpdatePromptDialogCoordinator("));
        assertTrue(!source.contains("ReleaseNotesMarkdownLite.format("));
        assertTrue(!source.contains("UpdateAvailableDialog.create("));
        assertTrue(!source.contains("private void loadReleaseNotes("));
        assertTrue(manifestFetcherSource.contains("final class UpdateManifestFetcher"));
        assertTrue(dialogSource.contains("class DialogHandle"));
        assertTrue(dialogSource.contains("toComposeAnnotatedString()"));
        assertTrue(textInteropSource.contains("LinkAnnotation.Url(span.url)"));
        assertTrue(dialogSource.contains("AnimatedVisibility(expanded)"));
        assertTrue(dialogSource.contains("verticalScroll(rememberScrollState())"));
        assertTrue(dialogSource.contains("R.dimen.dialog_surface_padding_horizontal"));
        assertTrue(dialogSource.contains("R.dimen.update_dialog_primary_button_spacing_top"));
        assertTrue(dialogSource.contains("RoundedCornerShape(16.dp)"));
        assertTrue(!source.contains("private void executeApkDownload("));
        assertTrue(!source.contains("private void verifyDownloadedApk("));
        assertTrue(!source.contains("private static StartupUpdateManifest fetchUpdateManifest("));
        assertTrue(!source.contains("private static String formatBytes("));
        assertTrue(!source.contains("private static int compareSemVer("));
    }

    @Test
    public void aboutActivityDoesNotApplyLocalApkSignatureGate() throws IOException {
        String source = read("src/main/java/com/dpis/module/about/AboutActivity.java");
        String coordinatorSource = read("src/main/java/com/dpis/module/updates/UpdateDownloadCoordinator.java");
        String packageHandlerSource = read("src/main/java/com/dpis/module/updates/StartupUpdatePackageHandler.java");

        assertTrue(!source.contains("extractSigningFingerprints"));
        assertTrue(!source.contains("about_update_download_untrusted"));
        assertTrue(!coordinatorSource.contains("verifyDownloadedApk("));
        assertTrue(!coordinatorSource.contains("UntrustedUpdateException"));
        assertTrue(!packageHandlerSource.contains("verifyDownloadedApk("));
        assertTrue(!packageHandlerSource.contains("extractSigningFingerprints"));
    }

    @Test
    public void aboutActivityTracksDownloadStateForCoordinatorCancelFlow() throws IOException {
        String source = read("src/main/java/com/dpis/module/about/AboutActivity.java");

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

        assertTrue(manifest.contains("android:name=\".about.OpenSourceLicenseActivity\""));
    }

    @Test
    public void aboutComposePageUsesSharedThemeAndSemanticActions() throws IOException {
        String content = read("src/main/java/com/dpis/module/ui/compose/AboutContent.kt");
        String source = read("src/main/java/com/dpis/module/about/AboutActivity.java");

        assertTrue(content.contains("fun AboutContent("));
        assertTrue(content.contains("SecondaryPageScaffold("));
        assertTrue(content.contains("SegmentedListItem("));
        assertTrue(content.contains("verticalAlignment = Alignment.CenterVertically"));
        assertTrue(content.contains("dpisSegmentedShapes(index, total)"));
        assertTrue(content.contains("LazyColumn("));
        assertTrue(content.contains("rememberDpisConfirmAction"));
        assertTrue(content.contains("showDebugUpdateEntry"));
        assertTrue(content.contains("AboutContentPreview"));
        assertTrue(source.contains("BuildConfig.DEBUG"));
        assertTrue(!source.contains("setContentView(R.layout.activity_about)"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
