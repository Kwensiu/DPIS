package com.dpis.module

import org.junit.Assert.assertTrue
import org.junit.Test

class AboutActivitySourceSmokeTest {
    @Test
    fun aboutActivityWiresOpenSourceLicenseEntryToDedicatedPage() {
        val source = read("src/main/java/com/dpis/module/about/AboutActivity.kt")
        val content = read("src/main/java/com/dpis/module/about/presentation/AboutContent.kt")

        source.assertContainsAll(
            "SupportActivityContent.installAbout(",
            "Intent(this, OpenSourceLicenseActivity::class.java)",
        )
        content.assertContainsAll(
            "R.string.open_source_license",
            "R.string.open_source_license_settings_description",
        )
    }

    @Test
    fun aboutActivityUpdateFlowUsesSharedDownloadCoordinatorAndHttpsOnly() {
        val source = read("src/main/java/com/dpis/module/about/AboutActivity.kt")
        val dialogSource = read("src/main/java/com/dpis/module/updates/UpdateAvailableDialog.kt")
        val textInteropSource = read("src/main/java/com/dpis/module/ui/presentation/AndroidTextInterop.kt")
        val manifestFetcherSource = read("src/main/java/com/dpis/module/updates/UpdateManifestFetcher.java")

        source.assertContainsAll(
            "UpdateManifestFetcher.fetch(",
            "updatePromptDialogCoordinator::showUpdateAvailableDialog",
            "updatePromptDialogCoordinator = UpdatePromptDialogCoordinator(",
            "UpdatePromptRequest.from(manifest)",
            "ViewModelProvider(this)[AboutUpdatePromptState::class.java]",
            "showPendingUpdatePrompt()",
        )
        source.assertNotContainsAll(
            "ReleaseNotesMarkdownLite.format(",
            "UpdateAvailableDialog.create(",
            "private fun loadReleaseNotes(",
            "private fun executeApkDownload(",
            "private fun verifyDownloadedApk(",
            "private fun fetchUpdateManifest(",
            "private fun formatBytes(",
            "private fun compareSemVer(",
        )
        manifestFetcherSource.assertContainsAll("final class UpdateManifestFetcher")
        dialogSource.assertContainsAll(
            "class DialogHandle",
            "toComposeAnnotatedString()",
            "AnimatedVisibility(expanded)",
            "verticalScroll(rememberScrollState())",
            "R.dimen.dialog_surface_padding_horizontal",
            "R.dimen.update_dialog_primary_button_spacing_top",
            "RoundedCornerShape(16.dp)",
        )
        textInteropSource.assertContainsAll("LinkAnnotation.Url(span.url)")
    }

    @Test
    fun aboutActivityDoesNotApplyLocalApkSignatureGate() {
        val source = read("src/main/java/com/dpis/module/about/AboutActivity.kt")
        val coordinatorSource = read("src/main/java/com/dpis/module/updates/UpdateDownloadCoordinator.java")
        val packageHandlerSource = read("src/main/java/com/dpis/module/updates/StartupUpdatePackageHandler.java")

        source.assertNotContainsAll("extractSigningFingerprints", "about_update_download_untrusted")
        coordinatorSource.assertNotContainsAll("verifyDownloadedApk(", "UntrustedUpdateException")
        packageHandlerSource.assertNotContainsAll("verifyDownloadedApk(", "extractSigningFingerprints")
    }

    @Test
    fun aboutActivityTracksDownloadStateForCoordinatorCancelFlow() {
        read("src/main/java/com/dpis/module/about/AboutActivity.kt").apply {
            assertContainsAll(
                "@Volatile private var updateDownloadInProgress = false",
                "@Volatile private var updateDownloadCancelRequested = false",
                "UpdateCoordinator.State(",
                "updateDownloadInProgress,",
                "updateDownloadCancelRequested,",
            )
            assertNotContainsAll("UpdateCoordinator.State.empty()")
        }
    }

    @Test
    fun manifestDeclaresOpenSourceLicenseActivity() {
        read("src/main/AndroidManifest.xml").assertContainsAll(
            "android:name=\".about.OpenSourceLicenseActivity\"",
        )
    }

    @Test
    fun aboutComposePageUsesSharedThemeAndSemanticActions() {
        val content = read("src/main/java/com/dpis/module/about/presentation/AboutContent.kt")
        val source = read("src/main/java/com/dpis/module/about/AboutActivity.kt")

        content.assertContainsAll(
            "fun AboutContent(", "SecondaryPageScaffold(", "SegmentedListItem(",
            "verticalAlignment = Alignment.CenterVertically", "dpisSegmentedShapes(index, total)",
            "LazyColumn(", "rememberConfirmAction", "showDebugUpdateEntry", "AboutContentPreview",
        )
        source.assertContainsAll("BuildConfig.DEBUG")
        source.assertNotContainsAll("setContentView(R.layout.activity_about)")
    }

    private fun read(relativePath: String) = SourceSmokeTestPaths.read(relativePath)

    private fun String.assertContainsAll(vararg needles: String) {
        needles.forEach { assertTrue("Missing $it", contains(it)) }
    }

    private fun String.assertNotContainsAll(vararg needles: String) {
        needles.forEach { assertTrue("Unexpected $it", !contains(it)) }
    }
}
