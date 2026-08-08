package com.dpis.module.updates;

import android.content.Context;
import android.net.Uri;
import com.dpis.module.R;

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
            UpdateAvailableDialog.DialogHandle dialogHandle) {
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

        dialogHandle.setCancelable(false);
        showDownloadingState(dialogHandle);

        activeDownloadFuture = executor.submit(() -> executeDownload(
                downloadUri,
                targetFile,
                dialogHandle));
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
            UpdateAvailableDialog.DialogHandle dialogHandle) {
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
                            host.runOnUiThread(() -> prepareProgressView(dialogHandle, totalBytes));
                        }

                        @Override
                        public void onProgress(long downloadedBytes, long totalBytes) {
                            if (totalBytes > 0L) {
                                int progress = (int) Math.min(100L, (downloadedBytes * 100L) / totalBytes);
                                if (progress == lastProgress[0]) {
                                    return;
                                }
                                lastProgress[0] = progress;
                                host.runOnUiThread(() -> updateProgressView(dialogHandle,
                                        progress,
                                        downloadedBytes,
                                        totalBytes));
                                return;
                            }
                            host.runOnUiThread(() -> updateProgressViewWithoutTotal(
                                    dialogHandle, downloadedBytes));
                        }
                    });

            // Product decision: downloaded update files are handed to Android's installer
            // without an app-side package/signature trust gate.
            UpdatePackageInstaller.persistDownloadedFile(host.getContext(), targetFile);
            host.runOnUiThread(() -> {
                if (!host.isActivityAlive()) {
                    return;
                }
                if (dialogHandle.isShowing()) {
                    dialogHandle.dismiss();
                }
                host.onDownloadSuccess(targetFile);
            });
        } catch (StartupUpdateDownloadExecutor.DownloadCanceledException ignored) {
            StartupUpdatePackageHandler.safeDeleteFile(targetFile);
            host.runOnUiThread(() -> {
                if (!host.isActivityAlive()) {
                    return;
                }
                showDialogIdleState(dialogHandle);
                dialogHandle.setCancelable(true);
                host.showToast(R.string.about_update_download_canceled);
            });
        } catch (Exception ignored) {
            boolean canceled = downloadCancelRequested || Thread.currentThread().isInterrupted();
            StartupUpdatePackageHandler.safeDeleteFile(targetFile);
            host.runOnUiThread(() -> {
                if (!host.isActivityAlive()) {
                    return;
                }
                showDialogIdleState(dialogHandle);
                dialogHandle.setCancelable(true);
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

    public static void showDialogIdleState(UpdateAvailableDialog.DialogHandle dialogHandle) {
        dialogHandle.showIdle(
                dialogHandle.getDialog().getContext().getString(R.string.about_update_action_download),
                dialogHandle.getDialog().getContext().getString(R.string.about_update_action_cancel_dialog));
    }

    static void showDownloadingState(UpdateAvailableDialog.DialogHandle dialogHandle) {
        android.content.Context context = dialogHandle.getDialog().getContext();
        dialogHandle.showDownloading(
                context.getString(R.string.about_update_action_cancel_download),
                context.getString(R.string.about_update_download_progress_preparing));
    }

    static void prepareProgressView(UpdateAvailableDialog.DialogHandle dialogHandle, long totalBytes) {
        if (totalBytes > 0L) {
            updateProgressView(dialogHandle, 0, 0L, totalBytes);
            return;
        }
        updateProgressViewWithoutTotal(dialogHandle, 0L);
    }

    static void updateProgressView(UpdateAvailableDialog.DialogHandle dialogHandle,
            int progress,
            long downloadedBytes,
            long totalBytes) {
        dialogHandle.showProgress(false, progress, dialogHandle.getDialog().getContext().getString(
                R.string.about_update_download_progress_with_percent,
                progress,
                StartupUpdatePackageHandler.formatBytesStatic(downloadedBytes),
                StartupUpdatePackageHandler.formatBytesStatic(totalBytes)));
    }

    static void updateProgressViewWithoutTotal(UpdateAvailableDialog.DialogHandle dialogHandle,
            long downloadedBytes) {
        dialogHandle.showProgress(true, 0, dialogHandle.getDialog().getContext().getString(
                R.string.about_update_download_progress_without_total,
                StartupUpdatePackageHandler.formatBytesStatic(downloadedBytes)));
    }
}
