package com.dpis.module;

import com.dpis.module.updates.UpdatePromptDialogCoordinator;

import com.dpis.module.updates.UpdateAvailableDialog;

import com.dpis.module.updates.ReleaseNotesController;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class UpdatePromptDialogCoordinatorSourceSmokeTest {
    @Test
    public void coordinatorOwnsDisclaimerAndManualUpdatePromptComposition() throws IOException {
        String source = read("src/main/java/com/dpis/module/updates/UpdatePromptDialogCoordinator.kt");
        String dialogSource = read("src/main/java/com/dpis/module/updates/UpdateAvailableDialog.kt");
        String textInteropSource = read(
                "src/main/java/com/dpis/module/ui/compose/AndroidTextInterop.kt");
        String disclaimerSource = read(
                "src/main/java/com/dpis/module/ui/compose/StartupDisclaimerDialog.kt");

        assertTrue(source.contains("class UpdatePromptDialogCoordinator"));
        assertTrue(source.contains("fun maybeShowStartupDisclaimerDialog("));
        assertTrue(source.contains("fun showUpdateAvailableDialog("));
        assertTrue(source.contains("StartupDisclaimerDialog.show("));
        assertTrue(!source.contains("R.layout.dialog_startup_disclaimer"));
        assertTrue(disclaimerSource.contains("fun StartupDisclaimerContent("));
        assertTrue(disclaimerSource.contains("dialog.setCancelable(false)"));
        assertTrue(disclaimerSource.contains("KeyEvent.KEYCODE_BACK"));
        assertTrue(source.contains("interface StartupDisclaimerAcceptance"));
        assertTrue(source.contains("acceptance.isAccepted()"));
        assertTrue(source.contains("acceptance.markAccepted()"));
        assertTrue(!source.contains("StartupDisclaimerStore"));
        assertTrue(source.contains("UpdateAvailableDialog.create("));
        assertTrue(source.contains("fun applyLargeDialogWidth(dialog: AlertDialog)"));
        assertTrue(source.contains("host.applyLargeDialogWidth(dialogHandle.dialog)"));
        assertTrue(!source.contains("DialogWindowSizer"));
        assertTrue(source.contains("dialogHandle.setCancel("));
        assertTrue(source.contains("if (host.isDownloadInProgress())"));
        int cancelStart = source.indexOf("dialogHandle.setCancel(");
        int cancelEnd = source.indexOf("        )", cancelStart);
        assertTrue(cancelStart >= 0);
        assertTrue(cancelEnd > cancelStart);
        String cancelBlock = source.substring(cancelStart, cancelEnd);
        assertTrue(!cancelBlock.contains("host.markPromptedVersion(remoteVersionCode)"));
        assertTrue(source.contains("host.markPromptedVersion(remoteVersionCode)"));
        assertTrue(source.contains("startStartupUpdateDownload("));
        assertTrue(source.contains("remoteReleaseNotes: String?"));
        assertTrue(source.contains("private val releaseNotesController: ReleaseNotesController"));
        assertTrue(source.contains("releaseNotesController.load("));
        assertTrue(source.contains("dialogHandle.isShowing()"));
        assertTrue(source.contains("R.string.about_update_release_notes_loading"));
        assertTrue(source.contains("ReleaseNotesMarkdownRenderer.render("));
        assertTrue(dialogSource.contains("AnimatedVisibility(expanded)"));
        assertTrue(dialogSource.contains("Row("));
        assertTrue(dialogSource.contains("RoundedCornerShape(16.dp)"));
        assertTrue(!dialogSource.contains("painterResource"));
        assertTrue(source.contains("remoteVersionName"));
        assertTrue(source.contains("remoteVersionCode"));
        assertTrue(dialogSource.contains("toComposeAnnotatedString()"));
        assertTrue(textInteropSource.contains("addLink(LinkAnnotation.Url(span.url)"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
