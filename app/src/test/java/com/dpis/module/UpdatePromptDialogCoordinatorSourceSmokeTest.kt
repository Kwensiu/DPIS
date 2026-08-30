package com.dpis.module.updates

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.dpis.module.SourceSmokeTestPaths

class UpdatePromptDialogCoordinatorSourceSmokeTest {
    @Test
    fun coordinatorUsesRootOwnedDisclaimerAndSharedUpdatePrompt() {
        val source = read("src/main/java/com/dpis/module/updates/UpdatePromptDialogCoordinator.kt")
        val dialogSource = read("src/main/java/com/dpis/module/updates/UpdateAvailableDialog.kt")
        val textInteropSource = read("src/main/java/com/dpis/module/ui/compose/AndroidTextInterop.kt")
        val disclaimerSource = read("src/main/java/com/dpis/module/ui/compose/StartupDisclaimerDialog.kt")
        val modalSource = read("src/main/java/com/dpis/module/ui/compose/ModalDialog.kt")
        val shellHostSource = read("src/main/java/com/dpis/module/MainComposeShellHost.kt")

        assertTrue(source.contains("class UpdatePromptDialogCoordinator"))
        assertTrue(source.contains("fun maybeShowStartupDisclaimerDialog("))
        assertTrue(source.contains("fun showUpdateAvailableDialog("))
        assertTrue(source.contains("StartupDisclaimerGate.show("))
        assertFalse(source.contains("StartupDisclaimerDialog.show("))
        assertFalse(source.contains("R.layout.dialog_startup_disclaimer"))
        assertTrue(disclaimerSource.contains("fun StartupDisclaimerDialog("))
        assertTrue(disclaimerSource.contains("rememberSaveable"))
        assertTrue(disclaimerSource.contains("dismissOnClickOutside = false"))
        assertTrue(disclaimerSource.contains("dismissOnBackPress = false"))
        assertTrue(disclaimerSource.contains("BackHandler(onBack = onBack)"))
        assertTrue(disclaimerSource.contains("color = MaterialTheme.colorScheme.onSurface"))
        assertFalse(disclaimerSource.contains("MaterialAlertDialogBuilder"))
        assertFalse(disclaimerSource.contains("ComposeView"))
        assertTrue(modalSource.contains("fun StructuredModalDialog("))
        assertTrue(modalSource.contains(".weight(1f, fill = false)"))
        assertTrue(modalSource.contains(".verticalScroll(rememberScrollState())"))
        assertTrue(shellHostSource.contains("fun showStartupDisclaimer("))
        assertTrue(shellHostSource.contains("startupDisclaimer = null"))
        assertTrue(shellHostSource.contains("StartupDisclaimerDialog("))
        assertTrue(shellHostSource.contains("DisposableEffect(disclaimerPresenter)"))
        assertTrue(disclaimerSource
            .contains("presentedBy"))
        assertTrue(source.contains("interface StartupDisclaimerAcceptance"))
        assertTrue(source.contains("acceptance.isAccepted()"))
        assertTrue(source.contains("acceptance.markAccepted()"))
        assertFalse(source.contains("StartupDisclaimerStore"))
        assertTrue(source.contains("UpdateAvailableDialog.create("))
        assertTrue(source.contains("fun applyLargeDialogWidth(dialog: AlertDialog)"))
        assertTrue(source.contains("host.applyLargeDialogWidth(dialogHandle.dialog)"))
        assertFalse(source.contains("DialogWindowSizer"))
        assertTrue(source.contains("dialogHandle.setCancel("))
        assertTrue(source.contains("if (host.isDownloadInProgress())"))
        val cancelStart = source.indexOf("dialogHandle.setCancel(")
        val cancelEnd = source.indexOf("        )", cancelStart)
        assertTrue(cancelStart >= 0)
        assertTrue(cancelEnd > cancelStart)
        assertFalse(source.substring(cancelStart, cancelEnd).contains("host.markPromptedVersion(remoteVersionCode)"))
        assertTrue(source.contains("host.markPromptedVersion(remoteVersionCode)"))
        assertTrue(source.contains("startStartupUpdateDownload("))
        assertTrue(source.contains("remoteReleaseNotes: String?"))
        assertTrue(source.contains("private val releaseNotesController: ReleaseNotesController"))
        assertTrue(source.contains("releaseNotesController.load("))
        assertTrue(source.contains("dialogHandle.isShowing()"))
        assertTrue(source.contains("R.string.about_update_release_notes_loading"))
        assertTrue(source.contains("ReleaseNotesMarkdownRenderer.render("))
        assertTrue(dialogSource.contains("AnimatedVisibility(expanded)"))
        assertTrue(dialogSource.contains("Row("))
        assertTrue(dialogSource.contains("RoundedCornerShape(16.dp)"))
        assertFalse(dialogSource.contains("painterResource"))
        assertTrue(source.contains("remoteVersionName"))
        assertTrue(source.contains("remoteVersionCode"))
        assertTrue(dialogSource.contains("toComposeAnnotatedString()"))
        assertTrue(textInteropSource.contains("addLink(LinkAnnotation.Url(span.url)"))
    }

    private fun read(relativePath: String): String = SourceSmokeTestPaths.read(relativePath)
}
