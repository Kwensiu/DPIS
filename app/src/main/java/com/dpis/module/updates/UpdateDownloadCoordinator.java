package com.dpis.module.updates;

import android.content.Context;
import android.net.Uri;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.dpis.module.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.net.HttpURLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public final class UpdateDownloadCoordinator {
    public interface HomeDownloadListener {
        void onStarted();

        void onProgress(int progress);

        void onSucceeded(File targetFile);

        void onFinished();
    }

    public interface Host {
        boolean isActivityAlive();

        Context getContext();

        void runOnUiThread(Runnable runnable);

        void showToast(int messageResId);

        void onDownloadSuccess(File targetFile);

        UpdateCoordinator.State buildUpdateCoordinatorState();

        void applyDownloadState(UpdateCoordinator.State state);
    }

    private final Host host;
    private final UpdateCoordinator updateCoordinator;
    private final StartupUpdateDownloadExecutor downloadExecutor;
    private final ExecutorService executor;

    private volatile boolean downloadInProgress;
    private volatile boolean downloadCancelRequested;
    private volatile Future<?> activeDownloadFuture;
    private volatile HttpURLConnection activeDownloadConnection;

    public UpdateDownloadCoordinator(Host host,
            UpdateCoordinator updateCoordinator,
            StartupUpdateDownloadExecutor downloadExecutor,
            ExecutorService executor) {
        if (host == null || updateCoordinator == null || downloadExecutor == null
                || executor == null) {
            throw new IllegalArgumentException("all arguments must be non-null");
        }
        this.host = host;
        this.updateCoordinator = updateCoordinator;
        this.downloadExecutor = downloadExecutor;
        this.executor = executor;
    }

    public boolean isDownloadInProgress() {
        return downloadInProgress;
    }

    public void startDownload(String targetVersionName,
            String downloadUrl,
            AlertDialog dialog,
            MaterialButton primaryButton,
            MaterialButton cancelButton,
            LinearProgressIndicator progressView,
            MaterialTextView progressTextView) {
        UpdateCoordinator.DownloadDecision downloadDecision = updateCoordinator.requestDownloadStart(
                host.buildUpdateCoordinatorState(),
                downloadUrl);
        if (!downloadDecision.started) {
            switch (downloadDecision.reason) {
                case ALREADY_IN_PROGRESS -> host.showToast(R.string.about_update_download_in_progress);
                case HTTPS_REQUIRED -> host.showToast(R.string.about_update_download_https_required);
                case EMPTY_URL, INVALID_URL -> host.showToast(R.string.about_update_download_failed);
                default -> host.showToast(R.string.about_update_download_failed);
            }
            return;
        }

        applyDownloadState(downloadDecision.nextState);
        Uri downloadUri = Uri.parse(downloadDecision.normalizedUrl);

        final File targetFile;
        try {
            UpdatePackageInstaller.clearUpdateCache(host.getContext());
            targetFile = UpdatePackageInstaller.prepareTargetFile(host.getContext(), targetVersionName);
        } catch (RuntimeException ignored) {
            UpdateCoordinator.State rollbackState = updateCoordinator.markDownloadFinished(
                    host.buildUpdateCoordinatorState());
            applyDownloadState(rollbackState);
            host.showToast(R.string.about_update_download_failed);
            return;
        }

        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        showDownloadingState(primaryButton, cancelButton, progressView, progressTextView);

        activeDownloadFuture = executor.submit(() -> executeDownload(
                downloadUri,
                targetFile,
                dialog,
                primaryButton,
                cancelButton,
                progressView,
                progressTextView));
    }

    public void startHomeDownload(String targetVersionName,
            String downloadUrl,
            HomeDownloadListener listener) {
        UpdateCoordinator.DownloadDecision downloadDecision = updateCoordinator.requestDownloadStart(
                host.buildUpdateCoordinatorState(),
                downloadUrl);
        if (!downloadDecision.started) {
            switch (downloadDecision.reason) {
                case ALREADY_IN_PROGRESS -> host.showToast(R.string.about_update_download_in_progress);
                case HTTPS_REQUIRED -> host.showToast(R.string.about_update_download_https_required);
                case EMPTY_URL, INVALID_URL -> host.showToast(R.string.about_update_download_failed);
                default -> host.showToast(R.string.about_update_download_failed);
            }
            return;
        }

        applyDownloadState(downloadDecision.nextState);
        if (listener != null) {
            listener.onStarted();
        }
        Uri downloadUri = Uri.parse(downloadDecision.normalizedUrl);

        final File targetFile;
        try {
            UpdatePackageInstaller.clearUpdateCache(host.getContext());
            targetFile = UpdatePackageInstaller.prepareTargetFile(host.getContext(), targetVersionName);
        } catch (RuntimeException ignored) {
            UpdateCoordinator.State rollbackState = updateCoordinator.markDownloadFinished(
                    host.buildUpdateCoordinatorState());
            applyDownloadState(rollbackState);
            if (listener != null) {
                listener.onFinished();
            }
            host.showToast(R.string.about_update_download_failed);
            return;
        }

        activeDownloadFuture = executor.submit(() -> executeHomeDownload(
                downloadUri,
                targetFile,
                listener));
    }

    public void cancelActiveDownload() {
        UpdateCoordinator.State nextState = updateCoordinator.requestDownloadCancel(
                host.buildUpdateCoordinatorState());
        applyDownloadState(nextState);
        if (!nextState.downloadInProgress) {
            return;
        }
        HttpURLConnection connection = activeDownloadConnection;
        if (connection != null) {
            connection.disconnect();
        }
        Future<?> future = activeDownloadFuture;
        if (future != null) {
            future.cancel(true);
        }
    }

    public void shutdown() {
        cancelActiveDownload();
        executor.shutdownNow();
    }

    private void executeDownload(Uri downloadUri,
            File targetFile,
            AlertDialog dialog,
            MaterialButton primaryButton,
            MaterialButton cancelButton,
            LinearProgressIndicator progressView,
            MaterialTextView progressTextView) {
        try {
            final int[] lastProgress = new int[] { -1 };
            downloadExecutor.download(
                    downloadUri,
                    targetFile,
                    () -> downloadCancelRequested || Thread.currentThread().isInterrupted(),
                    new StartupUpdateDownloadExecutor.Listener() {
                        @Override
                        public void onConnectionOpened(HttpURLConnection connection, long totalBytes) {
                            activeDownloadConnection = connection;
                            host.runOnUiThread(() -> prepareProgressView(
                                    progressView,
                                    progressTextView,
                                    totalBytes));
                        }

                        @Override
                        public void onProgress(long downloadedBytes, long totalBytes) {
                            if (totalBytes > 0L) {
                                int progress = (int) Math.min(100L, (downloadedBytes * 100L) / totalBytes);
                                if (progress == lastProgress[0]) {
                                    return;
                                }
                                lastProgress[0] = progress;
                                host.runOnUiThread(() -> updateProgressView(
                                        progressView,
                                        progressTextView,
                                        progress,
                                        downloadedBytes,
                                        totalBytes));
                                return;
                            }
                            host.runOnUiThread(() -> updateProgressViewWithoutTotal(
                                    progressView,
                                    progressTextView,
                                    downloadedBytes));
                        }
                    });

            // Product decision: downloaded update files are handed to Android's installer
            // without an app-side package/signature trust gate.
            UpdatePackageInstaller.persistDownloadedFile(host.getContext(), targetFile);
            host.runOnUiThread(() -> {
                if (!host.isActivityAlive()) {
                    return;
                }
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                host.onDownloadSuccess(targetFile);
            });
        } catch (StartupUpdateDownloadExecutor.DownloadCanceledException ignored) {
            StartupUpdatePackageHandler.safeDeleteFile(targetFile);
            host.runOnUiThread(() -> {
                if (!host.isActivityAlive()) {
                    return;
                }
                showDialogIdleState(primaryButton, cancelButton, progressView, progressTextView);
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(true);
                host.showToast(R.string.about_update_download_canceled);
            });
        } catch (Exception ignored) {
            boolean canceled = downloadCancelRequested || Thread.currentThread().isInterrupted();
            StartupUpdatePackageHandler.safeDeleteFile(targetFile);
            host.runOnUiThread(() -> {
                if (!host.isActivityAlive()) {
                    return;
                }
                showDialogIdleState(primaryButton, cancelButton, progressView, progressTextView);
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(true);
                host.showToast(
                        canceled
                                ? R.string.about_update_download_canceled
                                : R.string.about_update_download_failed);
            });
        } finally {
            activeDownloadConnection = null;
            activeDownloadFuture = null;
            UpdateCoordinator.State nextState = updateCoordinator.markDownloadFinished(
                    host.buildUpdateCoordinatorState());
            applyDownloadState(nextState);
        }
    }

    private void executeHomeDownload(Uri downloadUri,
            File targetFile,
            HomeDownloadListener listener) {
        try {
            final int[] lastProgress = new int[] { -1 };
            downloadExecutor.download(
                    downloadUri,
                    targetFile,
                    () -> downloadCancelRequested || Thread.currentThread().isInterrupted(),
                    new StartupUpdateDownloadExecutor.Listener() {
                        @Override
                        public void onConnectionOpened(HttpURLConnection connection, long totalBytes) {
                            activeDownloadConnection = connection;
                        }

                        @Override
                        public void onProgress(long downloadedBytes, long totalBytes) {
                            if (totalBytes <= 0L || listener == null) {
                                return;
                            }
                            int progress = (int) Math.min(100L, (downloadedBytes * 100L) / totalBytes);
                            if (progress == lastProgress[0]) {
                                return;
                            }
                            lastProgress[0] = progress;
                            host.runOnUiThread(() -> listener.onProgress(progress));
                        }
                    });

            // Product decision: downloaded update files are handed to Android's installer
            // without an app-side package/signature trust gate.
            UpdatePackageInstaller.persistDownloadedFile(host.getContext(), targetFile);
            host.runOnUiThread(() -> {
                if (host.isActivityAlive()) {
                    if (listener != null) {
                        listener.onSucceeded(targetFile);
                    }
                    host.onDownloadSuccess(targetFile);
                }
            });
        } catch (StartupUpdateDownloadExecutor.DownloadCanceledException ignored) {
            StartupUpdatePackageHandler.safeDeleteFile(targetFile);
            host.runOnUiThread(() -> {
                if (host.isActivityAlive()) {
                    host.showToast(R.string.about_update_download_canceled);
                }
            });
        } catch (Exception ignored) {
            boolean canceled = downloadCancelRequested || Thread.currentThread().isInterrupted();
            StartupUpdatePackageHandler.safeDeleteFile(targetFile);
            host.runOnUiThread(() -> {
                if (host.isActivityAlive()) {
                    host.showToast(canceled
                            ? R.string.about_update_download_canceled
                            : R.string.about_update_download_failed);
                }
            });
        } finally {
            activeDownloadConnection = null;
            activeDownloadFuture = null;
            UpdateCoordinator.State nextState = updateCoordinator.markDownloadFinished(
                    host.buildUpdateCoordinatorState());
            applyDownloadState(nextState);
            if (listener != null) {
                host.runOnUiThread(listener::onFinished);
            }
        }
    }

    private void applyDownloadState(UpdateCoordinator.State state) {
        if (state == null) {
            return;
        }
        downloadInProgress = state.downloadInProgress;
        downloadCancelRequested = state.downloadCancelRequested;
        host.applyDownloadState(state);
    }

    public static void showDialogIdleState(MaterialButton primaryButton,
            MaterialButton cancelButton,
            LinearProgressIndicator progressView,
            MaterialTextView progressTextView) {
        primaryButton.setEnabled(true);
        primaryButton.setText(R.string.about_update_action_download);
        cancelButton.setText(R.string.about_update_action_cancel_dialog);
        progressView.setVisibility(View.GONE);
        progressTextView.setVisibility(View.GONE);
    }

    static void showDownloadingState(MaterialButton primaryButton,
            MaterialButton cancelButton,
            LinearProgressIndicator progressView,
            MaterialTextView progressTextView) {
        primaryButton.setEnabled(false);
        cancelButton.setText(R.string.about_update_action_cancel_download);
        progressView.setVisibility(View.VISIBLE);
        progressTextView.setVisibility(View.VISIBLE);
        progressView.setIndeterminate(true);
        progressTextView.setText(R.string.about_update_download_progress_preparing);
    }

    static void prepareProgressView(LinearProgressIndicator progressView,
            MaterialTextView progressTextView,
            long totalBytes) {
        if (totalBytes > 0L) {
            progressView.setIndeterminate(false);
            progressView.setProgress(0);
            updateProgressView(progressView, progressTextView, 0, 0L, totalBytes);
            return;
        }
        progressView.setIndeterminate(true);
        updateProgressViewWithoutTotal(progressView, progressTextView, 0L);
    }

    static void updateProgressView(LinearProgressIndicator progressView,
            MaterialTextView progressTextView,
            int progress,
            long downloadedBytes,
            long totalBytes) {
        if (progressView.isIndeterminate()) {
            progressView.setIndeterminate(false);
        }
        progressView.setProgress(progress);
        progressTextView.setText(progressView.getContext().getString(
                R.string.about_update_download_progress_with_percent,
                progress,
                StartupUpdatePackageHandler.formatBytesStatic(downloadedBytes),
                StartupUpdatePackageHandler.formatBytesStatic(totalBytes)));
    }

    static void updateProgressViewWithoutTotal(LinearProgressIndicator progressView,
            MaterialTextView progressTextView,
            long downloadedBytes) {
        progressView.setIndeterminate(true);
        progressTextView.setText(progressView.getContext().getString(
                R.string.about_update_download_progress_without_total,
                StartupUpdatePackageHandler.formatBytesStatic(downloadedBytes)));
    }
}
