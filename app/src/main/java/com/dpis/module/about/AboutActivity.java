package com.dpis.module.about;

import com.dpis.module.BuildConfig;
import com.dpis.module.LocalizedActivity;
import com.dpis.module.R;
import com.dpis.module.ui.compose.SupportActivityContent;

import com.dpis.module.ui.DialogWindowSizer;

import com.dpis.module.updates.UpdateManifestFetcher;

import com.dpis.module.updates.UpdateDownloadCoordinator;

import com.dpis.module.updates.UpdateCoordinator;

import com.dpis.module.updates.UpdateAvailableDialog;

import com.dpis.module.updates.StartupUpdatePackageHandler;

import com.dpis.module.updates.StartupUpdateManifest;

import com.dpis.module.updates.StartupUpdateDownloadExecutor;

import com.dpis.module.updates.ReleaseNotesController;

import com.dpis.module.updates.ReleaseNotesCacheStore;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.dpis.module.updates.GitHubReleaseNotesFetcher;
import com.dpis.module.updates.ReleaseNotesMarkdownRenderer;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;

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
    private ReleaseNotesController releaseNotesController;
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
        releaseNotesController = new ReleaseNotesController(
                new ReleaseNotesCacheStore(this),
                updateExecutor,
                this::runOnUiThread,
                GitHubReleaseNotesFetcher::fetchByVersionName,
                System::currentTimeMillis,
                UPDATE_CONNECT_TIMEOUT_MS,
                UPDATE_READ_TIMEOUT_MS);

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
        showManualUpdatePromptDialog(manifest);
    }

    private void showManualUpdatePromptDialog(StartupUpdateManifest manifest) {
        String releasePageUrl = manifest.releasePage.isEmpty()
                ? getString(R.string.about_releases_url)
                : manifest.releasePage;
        String downloadUrl = manifest.apkUrl;
        showCenteredManualUpdatePromptDialog(manifest, downloadUrl, releasePageUrl);
    }

    private void showCenteredManualUpdatePromptDialog(StartupUpdateManifest manifest,
            String downloadUrl,
            String releasePageUrl) {
        UpdateAvailableDialog.DialogHandle dialogHandle = UpdateAvailableDialog.create(
                this,
                getString(R.string.about_update_available_title),
                getString(
                        R.string.about_update_available_message,
                        BuildConfig.VERSION_NAME,
                        BuildConfig.VERSION_CODE,
                        manifest.versionName,
                        manifest.versionCode));

        AlertDialog dialog = dialogHandle.dialog;
        MaterialButton primaryButton = dialogHandle.primaryButton;
        MaterialButton cancelButton = dialogHandle.cancelButton;
        LinearProgressIndicator progressView = dialogHandle.progressView;
        MaterialTextView progressTextView = dialogHandle.progressTextView;
        MaterialTextView releaseNotesText = dialogHandle.releaseNotesText;

        UpdateDownloadCoordinator.showDialogIdleState(
                primaryButton, cancelButton, progressView, progressTextView);
        bindDialogCancelButton(dialog, cancelButton);
        String embeddedReleaseNotes = manifest.releaseNotes == null ? "" : manifest.releaseNotes.trim();
        Locale locale = getResources().getConfiguration().getLocales().get(0);
        if (!embeddedReleaseNotes.isEmpty()) {
            releaseNotesText.setText(ReleaseNotesMarkdownRenderer.render(
                    this,
                    embeddedReleaseNotes,
                    locale));
        } else {
            releaseNotesText.setText(R.string.about_update_release_notes_loading);
        }
        loadReleaseNotes(releaseNotesText, locale, manifest.versionName, !embeddedReleaseNotes.isEmpty());

        boolean hasDirectDownload = downloadUrl != null && !downloadUrl.trim().isEmpty();
        if (!hasDirectDownload) {
            primaryButton.setText(R.string.about_update_action_view_release);
            primaryButton.setOnClickListener(v -> openUrl(releasePageUrl));
            dialog.show();
            DialogWindowSizer.applyLargeWidth(dialog, this);
            return;
        }

        primaryButton.setOnClickListener(v -> {
            if (updateDownloadCoordinator.isDownloadInProgress()) {
                updateDownloadCoordinator.cancelActiveDownload();
                return;
            }
            updateDownloadCoordinator.startDownload(
                    manifest.versionName,
                    downloadUrl,
                    dialog,
                    primaryButton,
                    cancelButton,
                    progressView,
                    progressTextView);
        });

        dialog.setOnDismissListener(unused -> updateDownloadCoordinator.cancelActiveDownload());
        dialog.show();
        DialogWindowSizer.applyLargeWidth(dialog, this);
    }

    private void loadReleaseNotes(MaterialTextView releaseNotesText,
            Locale locale,
            String targetVersionName,
            boolean hasEmbeddedReleaseNotes) {
        releaseNotesController.load(targetVersionName, hasEmbeddedReleaseNotes,
                new ReleaseNotesController.Listener() {
                    @Override
                    public boolean isAlive() {
                        return !isFinishing() && !isDestroyed();
                    }

                    @Override
                    public void onBody(String body) {
                        releaseNotesText.setText(ReleaseNotesMarkdownRenderer.render(
                                AboutActivity.this,
                                body,
                                locale));
                    }

                    @Override
                    public void onEmptyBody() {
                        releaseNotesText.setText(R.string.about_update_release_notes_empty);
                    }

                    @Override
                    public void onFailure() {
                        releaseNotesText.setText(R.string.about_update_release_notes_failed);
                    }
                });
    }

    private void bindDialogCancelButton(AlertDialog dialog, MaterialButton cancelButton) {
        cancelButton.setOnClickListener(v -> {
            if (updateDownloadCoordinator.isDownloadInProgress()) {
                updateDownloadCoordinator.cancelActiveDownload();
            }
            dialog.dismiss();
        });
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
