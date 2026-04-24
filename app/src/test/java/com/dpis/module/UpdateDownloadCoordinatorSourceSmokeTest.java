package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class UpdateDownloadCoordinatorSourceSmokeTest {
    @Test
    public void coordinatorOwnsDownloadExecutionAndProgressUIContracts() throws IOException {
        String source = read("src/main/java/com/dpis/module/UpdateDownloadCoordinator.java");

        assertTrue(source.contains("final class UpdateDownloadCoordinator"));
        assertTrue(source.contains("interface Host"));
        assertTrue(source.contains("boolean isActivityAlive()"));
        assertTrue(source.contains("void onDownloadSuccess(File targetFile)"));
        assertTrue(source.contains("void startDownload("));
        assertTrue(source.contains("void cancelActiveDownload()"));
        assertTrue(source.contains("void shutdown()"));
        assertTrue(source.contains("boolean isDownloadInProgress()"));
        assertTrue(source.contains("static void showDialogIdleState("));
        assertTrue(source.contains("static void showDownloadingState("));
        assertTrue(source.contains("static void prepareProgressView("));
        assertTrue(source.contains("static void updateProgressView("));
        assertTrue(source.contains("static void updateProgressViewWithoutTotal("));
        assertTrue(source.contains("packageHandler.verifyDownloadedApk("));
        assertTrue(source.contains("UpdatePackageInstaller.persistDownloadedFile("));
        assertTrue(source.contains("StartupUpdatePackageHandler.safeDeleteFile("));
        assertTrue(source.contains("StartupUpdatePackageHandler.formatBytesStatic("));
    }

    @Test
    public void coordinatorReusesUpdateCoordinatorForDownloadDecisions() throws IOException {
        String source = read("src/main/java/com/dpis/module/UpdateDownloadCoordinator.java");

        assertTrue(source.contains("updateCoordinator.requestDownloadStart("));
        assertTrue(source.contains("updateCoordinator.requestDownloadCancel("));
        assertTrue(source.contains("updateCoordinator.markDownloadFinished("));
        assertTrue(source.contains("UpdateCoordinator.State rollbackState = updateCoordinator.markDownloadFinished("));
        assertTrue(source.contains("applyDownloadState(rollbackState);"));
    }

    @Test
    public void coordinatorReusesStartupUpdateDownloadExecutorForHttpDownload() throws IOException {
        String source = read("src/main/java/com/dpis/module/UpdateDownloadCoordinator.java");

        assertTrue(source.contains("downloadExecutor.download("));
        assertTrue(source.contains("StartupUpdateDownloadExecutor.DownloadCanceledException"));
    }

    @Test
    public void coordinatorTreatsInterruptedExceptionPathAsCanceledWhenCancelRequested() throws IOException {
        String source = read("src/main/java/com/dpis/module/UpdateDownloadCoordinator.java");

        assertTrue(source.contains("boolean canceled = downloadCancelRequested"));
        assertTrue(source.contains("? R.string.about_update_download_canceled"));
        assertTrue(source.contains(": R.string.about_update_download_failed"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
