package com.dpis.module.about;

import com.dpis.module.BuildConfig;
import com.dpis.module.LocalizedActivity;
import com.dpis.module.R;
import com.dpis.module.ui.compose.SupportActivityContent;

import com.dpis.module.updates.UpdateManifestFetcher;

import com.dpis.module.updates.UpdateDownloadCoordinator;

import com.dpis.module.updates.UpdateCoordinator;

import com.dpis.module.updates.StartupUpdatePackageHandler;

import com.dpis.module.updates.StartupUpdateManifest;

import com.dpis.module.updates.StartupUpdateDownloadExecutor;

import com.dpis.module.updates.UpdatePromptDialogCoordinator;
import com.dpis.module.updates.ReleaseNotesController;
import com.dpis.module.updates.ReleaseNotesCacheStore;
import com.dpis.module.updates.GitHubReleaseNotesFetcher;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AboutActivity extends LocalizedActivity {
    private static final int UPDATE_CONNECT_TIMEOUT_MS = 10_000;
    private static final int UPDATE_READ_TIMEOUT_MS = 10_000;
    private static final int DOWNLOAD_BUFFER_SIZE = 16 * 1024;
    private static final long DOWNLOAD_PROGRESS_UPDATE_INTERVAL_MS = 180L;

    private final UpdateCoordinator updateCoordinator = new UpdateCoordinator();
    private final StartupUpdateDownloadExecutor downloadExecutor = new StartupUpdateDownloadExecutor(
            UPDATE_CONNECT_TIMEOUT_MS,
            UPDATE_READ_TIMEOUT_MS,
            DOWNLOAD_BUFFER_SIZE,
            DOWNLOAD_PROGRESS_UPDATE_INTERVAL_MS);
    private final StartupUpdatePackageHandler packageHandler = new StartupUpdatePackageHandler(this);
    private final ExecutorService updateExecutor = Executors.newSingleThreadExecutor();
    private UpdateDownloadCoordinator updateDownloadCoordinator;
    private UpdatePromptDialogCoordinator updatePromptDialogCoordinator;
    private volatile boolean updateCheckInProgress = false;
    private volatile boolean updateDownloadInProgress = false;
    private volatile boolean updateDownloadCancelRequested = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateDownloadCoordinator = new UpdateDownloadCoordinator(
                createUpdateDownloadHost(),
                updateCoordinator,
                downloadExecutor,
                updateExecutor);
        updatePromptDialogCoordinator = new UpdatePromptDialogCoordinator(
                this,
                createUpdatePromptDialogHost(),
                new ReleaseNotesController(
                        new ReleaseNotesCacheStore(this),
                        updateExecutor,
                        this::runOnUiThread,
                        GitHubReleaseNotesFetcher::fetchByVersionName,
                        System::currentTimeMillis,
                        UPDATE_CONNECT_TIMEOUT_MS,
                        UPDATE_READ_TIMEOUT_MS));

        SupportActivityContent.installAbout(
                this,
                getString(R.string.about_version_format,
                        BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                BuildConfig.DEBUG,
                () -> checkForUpdates(false),
                () -> checkForUpdates(true),
                () -> openUrl(getString(R.string.about_source_url)),
                () -> openUrl(getString(R.string.about_issues_url)),
                () -> startActivity(new Intent(this, OpenSourceLicenseActivity.class)));
    }

    @Override
    protected void onDestroy() {
        if (updateDownloadCoordinator != null) {
            updateDownloadCoordinator.shutdown();
        }
        super.onDestroy();
    }

    private void checkForUpdates(boolean forceShow) {
        if (updateDownloadCoordinator.isDownloadInProgress()) {
            showToast(R.string.about_update_download_in_progress);
            return;
        }
        if (updateCheckInProgress) {
            showToast(R.string.about_update_checking);
            return;
        }
        updateCheckInProgress = true;
        showToast(R.string.about_update_checking);

        final String manifestUrl = getString(R.string.about_update_manifest_url);
        updateExecutor.execute(() -> {
            try {
                StartupUpdateManifest manifest = UpdateManifestFetcher.fetch(
                        manifestUrl,
                        UPDATE_CONNECT_TIMEOUT_MS,
                        UPDATE_READ_TIMEOUT_MS);
                runOnUiThread(() -> onUpdateManifestLoaded(manifest, forceShow));
            } catch (Exception ignored) {
                runOnUiThread(() -> showToast(R.string.about_update_check_failed));
            } finally {
                runOnUiThread(() -> updateCheckInProgress = false);
            }
        });
    }

    private void onUpdateManifestLoaded(StartupUpdateManifest manifest, boolean forceShow) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        boolean hasUpdate = UpdateCoordinator.isRemoteVersionNewer(
                manifest.versionCode,
                manifest.versionName,
                BuildConfig.VERSION_CODE,
                BuildConfig.VERSION_NAME);

        if (!forceShow && !hasUpdate) {
            showToast(R.string.about_update_up_to_date);
            return;
        }
        updatePromptDialogCoordinator.showUpdateAvailableDialog(
                manifest.versionName,
                manifest.versionCode,
                manifest.apkUrl,
                manifest.releasePage,
                manifest.releaseNotes);
    }

    private UpdateDownloadCoordinator.Host createUpdateDownloadHost() {
        return new UpdateDownloadCoordinator.Host() {
            @Override
            public boolean isActivityAlive() {
                return !isFinishing() && !isDestroyed();
            }

            @Override
            public Context getContext() {
                return AboutActivity.this;
            }

            @Override
            public void runOnUiThread(Runnable runnable) {
                AboutActivity.this.runOnUiThread(runnable);
            }

            @Override
            public void showToast(int messageResId) {
                AboutActivity.this.showToast(messageResId);
            }

            @Override
            public void onDownloadSuccess(File targetFile) {
                packageHandler.launchPackageInstaller(targetFile);
            }

            @Override
            public UpdateCoordinator.State buildUpdateCoordinatorState() {
                return new UpdateCoordinator.State(
                        0L,
                        false,
                        0,
                        false,
                        updateDownloadInProgress,
                        updateDownloadCancelRequested);
            }

            @Override
            public void applyDownloadState(UpdateCoordinator.State state) {
                if (state == null) {
                    return;
                }
                updateDownloadInProgress = state.downloadInProgress;
                updateDownloadCancelRequested = state.downloadCancelRequested;
            }
        };
    }

    private UpdatePromptDialogCoordinator.Host createUpdatePromptDialogHost() {
        return new UpdatePromptDialogCoordinator.Host() {
            @Override
            public void markPromptedVersion(int versionCode) {
                // About-triggered prompts do not participate in startup suppression state.
            }

            @Override
            public boolean isDownloadInProgress() {
                return updateDownloadCoordinator.isDownloadInProgress();
            }

            @Override
            public void cancelActiveUpdateDownload() {
                updateDownloadCoordinator.cancelActiveDownload();
            }

            @Override
            public void startStartupUpdateDownload(
                    String targetVersionName,
                    String downloadUrl,
                    com.dpis.module.updates.UpdateAvailableDialog.DialogHandle dialogHandle) {
                updateDownloadCoordinator.startDownload(targetVersionName, downloadUrl, dialogHandle);
            }

            @Override
            public void openUrl(String url) {
                AboutActivity.this.openUrl(url);
            }

            @Override
            public void showToast(int messageResId) {
                AboutActivity.this.showToast(messageResId);
            }

            @Override
            public void applyLargeDialogWidth(androidx.appcompat.app.AlertDialog dialog) {
                com.dpis.module.ui.DialogWindowSizer.applyLargeWidth(dialog, AboutActivity.this);
            }

            @Override
            public void finishActivity() {
                AboutActivity.this.finish();
            }
        };
    }

    private void openUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            showToast(R.string.about_link_open_failed);
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException ignored) {
            showToast(R.string.about_link_open_failed);
        }
    }

    private void showToast(int messageResId) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }
}
