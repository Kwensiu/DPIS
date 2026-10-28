package com.dpis.module.updates

import com.dpis.module.SourceSmokeTestPaths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePromptDialogCoordinatorSourceSmokeTest {
    @Test
    fun coordinatorKeepsUpdatePromptSemantics() {
        val source = read("src/main/java/com/dpis/module/updates/presentation/UpdatePromptDialogCoordinator.kt")
        val dialogSource = read("src/main/java/com/dpis/module/updates/presentation/UpdateAvailableDialog.kt")
        val rendererSource = read("src/main/java/com/dpis/module/updates/ReleaseNotesMarkdownRenderer.kt")
        val markwonPluginSource = read("src/main/java/com/dpis/module/updates/presentation/ReleaseNotesMarkwonPlugin.kt")
        val textInteropSource = read("src/main/java/com/dpis/module/ui/presentation/interop/AndroidTextInterop.kt")
        val modalSource = read("src/main/java/com/dpis/module/ui/dialog/ModalDialog.kt")

        assertTrue(source.contains("fun showUpdateAvailableDialog("))
        assertTrue(modalSource.contains("fun StructuredModalDialog("))
        assertTrue(modalSource.contains("usePlatformDefaultWidth = false"))
        assertTrue(modalSource.contains(".widthIn(max = 560.dp)"))
        assertTrue(modalSource.contains(".heightIn(max = maxHeight)"))
        assertTrue(modalSource.contains(".imeWindowPan(imeFocusBoundary)"))
        assertFalse(modalSource.contains("val minHeight"))
        assertTrue(modalSource.contains(".weight(1f, fill = false)"))
        assertTrue(modalSource.contains(".verticalScroll(rememberScrollState())"))
        assertTrue(source.contains("UpdateAvailableDialog.create("))
        assertFalse(source.contains("fun applyLargeDialogWidth(dialog: AlertDialog)"))
        assertFalse(source.contains("host.applyLargeDialogWidth(dialogHandle.dialog)"))
        assertFalse(source.contains("DialogWindowSizer"))
        assertFalse(dialogSource.contains("MaterialAlertDialogBuilder"))
        assertTrue(dialogSource.contains("ComposeOverlay.show(activity)"))
        assertTrue(dialogSource.contains("ModalDialog("))
        assertTrue(source.contains("dialogHandle.setCancel("))
        assertTrue(source.contains("UpdatePromptPolicy.cancelStopsDownload(host.isDownloadInProgress())"))
        val cancelStart = source.indexOf("dialogHandle.setCancel(")
        val cancelEnd = source.indexOf("        )", cancelStart)
        assertTrue(cancelStart >= 0)
        assertTrue(cancelEnd > cancelStart)
        assertFalse(source.substring(cancelStart, cancelEnd).contains("host.markPromptedVersion(request.versionCode)"))
        assertTrue(source.contains("host.markPromptedVersion(request.versionCode)"))
        assertTrue(source.contains("startStartupUpdateDownload("))
        assertTrue(source.contains("request: UpdatePromptRequest"))
        assertTrue(source.contains("host.onUpdatePromptDismissed()"))
        assertTrue(source.contains("UpdatePromptPolicy.shouldCancelDownloadOnDismiss("))
        assertTrue(source.contains("private val releaseNotesController: ReleaseNotesController"))
        assertTrue(source.contains("releaseNotesController.load("))
        assertTrue(source.contains("dialogHandle.isShowing()"))
        assertTrue(source.contains("R.string.about_update_release_notes_loading"))
        assertTrue(source.contains("ReleaseNotesMarkdownRenderer.render("))
        assertTrue(markwonPluginSource.contains("ReleaseNotesComposeCompatiblePlugin"))
        assertTrue(rendererSource.contains("insertVisibleListMarkers("))
        assertTrue(markwonPluginSource.contains("SoftLineBreak"))
        assertTrue(dialogSource.contains("AnimatedVisibility(expanded)"))
        assertTrue(dialogSource.contains("BoxWithConstraints(Modifier.fillMaxWidth())"))
        assertTrue(dialogSource.contains("val compactActionRow = maxWidth < 420.dp"))
        assertTrue(dialogSource.contains("modifier = Modifier.fillMaxWidth()"))
        assertTrue(dialogSource.contains("Row("))
        assertTrue(dialogSource.contains("RoundedCornerShape(16.dp)"))
        assertFalse(dialogSource.contains("painterResource"))
        assertTrue(source.contains("request.versionName"))
        assertTrue(source.contains("request.versionCode"))
        assertTrue(dialogSource.contains("toReleaseNotesAnnotatedString()"))
        assertFalse(dialogSource.contains("toComposeAnnotatedString()"))
        assertTrue(dialogSource.contains("clickable { expanded = !expanded }"))
        assertTrue(dialogSource.contains(".padding(horizontal = 12.dp, vertical = 10.dp)"))
        assertTrue(dialogSource.contains("drawReleaseNotesQuoteBars("))
        assertTrue(textInteropSource.contains("addLink(LinkAnnotation.Url(span.url)"))
    }

    private fun read(relativePath: String): String = SourceSmokeTestPaths.read(relativePath)
}
