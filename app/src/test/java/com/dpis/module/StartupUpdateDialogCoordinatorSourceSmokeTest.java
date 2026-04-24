package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class StartupUpdateDialogCoordinatorSourceSmokeTest {
    @Test
    public void coordinatorOwnsStartupDisclaimerAndUpdateDialogComposition() throws IOException {
        String source = read("src/main/java/com/dpis/module/StartupUpdateDialogCoordinator.java");

        assertTrue(source.contains("final class StartupUpdateDialogCoordinator"));
        assertTrue(source.contains("boolean maybeShowStartupDisclaimerDialog("));
        assertTrue(source.contains("new MaterialAlertDialogBuilder(activity)"));
        assertTrue(source.contains("R.layout.dialog_startup_disclaimer"));
        assertTrue(source.contains("setStartupDisclaimerAccepted(true)"));
        assertTrue(source.contains("UpdateAvailableDialog.create("));
        assertTrue(source.contains("dialogHandle.cancelButton.setOnClickListener"));
        assertTrue(source.contains("if (host.isDownloadInProgress()) {"));
        int cancelStart = source.indexOf("dialogHandle.cancelButton.setOnClickListener(v -> {");
        int cancelEnd = source.indexOf("});", cancelStart);
        assertTrue(cancelStart >= 0);
        assertTrue(cancelEnd > cancelStart);
        String cancelBlock = source.substring(cancelStart, cancelEnd);
        assertTrue(!cancelBlock.contains("host.markPromptedVersion(remoteVersionCode)"));
        assertTrue(source.contains("host.markPromptedVersion(remoteVersionCode)"));
        assertTrue(source.contains("startStartupUpdateDownload("));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
