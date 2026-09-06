package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;

public class UpdateDownloadCoordinatorSourceSmokeTest {
    @Test
    public void coordinatorOwnsDownloadExecutionAndProgressUIContracts() throws IOException {
        String source = read("src/main/java/com/dpis/module/updates/UpdateDownloadCoordinator.kt");

        assertTrue(source.contains("class UpdateDownloadCoordinator("));
        assertTrue(source.contains("interface Host"));
        assertTrue(source.contains("val isActivityAlive: Boolean"));
        assertTrue(source.contains("fun onDownloadSuccess(targetFile: File?)"));
        assertTrue(source.contains("fun startDownload("));
        assertTrue(source.contains("fun cancelActiveDownload()"));
        assertTrue(source.contains("fun shutdown()"));
        assertTrue(source.contains("var isDownloadInProgress: Boolean"));
        assertTrue(source.contains("fun showDialogIdleState("));
        assertTrue(source.contains("fun showDownloadingState("));
        assertTrue(source.contains("fun prepareProgressView("));
        assertTrue(source.contains("fun updateProgressView("));
        assertTrue(source.contains("fun updateProgressViewWithoutTotal("));
        assertTrue(source.contains("dialogHandle: DialogHandle"));
        assertFalse(source.contains("MaterialButton"));
        assertFalse(source.contains("LinearProgressIndicator"));
        assertFalse(source.contains("MaterialTextView"));
        assertFalse(source.contains("verifyDownloadedApk("));
        assertFalse(source.contains("UntrustedUpdateException"));
        assertFalse(source.contains("about_update_download_untrusted"));
        assertTrue(source.contains("UpdatePackageInstaller.persistDownloadedFile("));
        assertTrue(source.contains("StartupUpdatePackageHandler.safeDeleteFile("));
        assertTrue(source.contains("StartupUpdatePackageHandler.formatBytesStatic("));
    }

    @Test
    public void coordinatorReusesUpdateCoordinatorForDownloadDecisions() throws IOException {
        String source = read("src/main/java/com/dpis/module/updates/UpdateDownloadCoordinator.kt");

        assertTrue(source.contains("updateCoordinator.requestDownloadStart("));
        assertTrue(source.contains("updateCoordinator.requestDownloadCancel("));
        assertTrue(source.contains("updateCoordinator.markDownloadFinished("));
        assertTrue(source.contains("val rollbackState = updateCoordinator.markDownloadFinished("));
        assertTrue(source.contains("applyDownloadState(rollbackState)"));
    }

    @Test
    public void coordinatorReusesStartupUpdateDownloadExecutorForHttpDownload() throws IOException {
        String source = read("src/main/java/com/dpis/module/updates/UpdateDownloadCoordinator.kt");

        assertTrue(source.contains("downloadExecutor.download("));
        assertTrue(source.contains("StartupUpdateDownloadExecutor.DownloadCanceledException"));
    }

    @Test
    public void coordinatorTreatsInterruptedExceptionPathAsCanceledWhenCancelRequested() throws IOException {
        String source = read("src/main/java/com/dpis/module/updates/UpdateDownloadCoordinator.kt");

        assertTrue(source.contains("val canceled = downloadCancelRequested"));
        assertTrue(source.contains("R.string.about_update_download_canceled"));
        assertTrue(source.contains("R.string.about_update_download_failed"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
