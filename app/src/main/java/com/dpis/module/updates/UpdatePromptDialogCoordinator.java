package com.dpis.module.updates;

import android.app.Activity;
import androidx.appcompat.app.AlertDialog;

import com.dpis.module.R;
import com.dpis.module.ui.compose.StartupDisclaimerDialog;

import java.util.Locale;

public final class UpdatePromptDialogCoordinator {
    public interface Host {
        void markPromptedVersion(int versionCode);

        boolean isDownloadInProgress();

        void cancelActiveUpdateDownload();

        void startStartupUpdateDownload(String targetVersionName,
                String downloadUrl,
                UpdateAvailableDialog.DialogHandle dialogHandle);

        void openUrl(String url);

        void showToast(int messageResId);

        void applyLargeDialogWidth(AlertDialog dialog);

        void finishActivity();
    }

    public interface StartupDisclaimerAcceptance {
        boolean isAccepted();

        boolean markAccepted();
    }

    private final Activity activity;
    private final Host host;
    private final ReleaseNotesController releaseNotesController;

    public UpdatePromptDialogCoordinator(Activity activity,
            Host host,
            ReleaseNotesController releaseNotesController) {
        this.activity = activity;
        this.host = host;
        this.releaseNotesController = releaseNotesController;
    }

    public boolean maybeShowStartupDisclaimerDialog(
            StartupDisclaimerAcceptance acceptance,
            Runnable onAccepted) {
        if (acceptance == null
                || acceptance.isAccepted()
                || activity.isFinishing()
                || activity.isDestroyed()) {
            return false;
        }
        showComposeStartupDisclaimerDialog(acceptance, onAccepted);
        return true;
    }

    public void showUpdateAvailableDialog(String localVersionName,
            int localVersionCode,
            String remoteVersionName,
            int remoteVersionCode,
            String remoteApkUrl,
            String remoteReleasePage,
            String remoteReleaseNotes) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        UpdateAvailableDialog.DialogHandle dialogHandle = UpdateAvailableDialog.create(
                activity,
                activity.getString(R.string.about_update_available_title),
                activity.getString(
                        R.string.about_update_available_message,
                        localVersionName,
                        localVersionCode,
                        remoteVersionName,
                        remoteVersionCode));
        String embeddedReleaseNotes = remoteReleaseNotes == null ? "" : remoteReleaseNotes.trim();
        if (!embeddedReleaseNotes.isEmpty()) {
            Locale locale = activity.getResources().getConfiguration().getLocales().get(0);
            dialogHandle.setReleaseNotes(ReleaseNotesMarkdownRenderer.render(
                    activity,
                    embeddedReleaseNotes,
                    locale));
        } else {
            dialogHandle.setReleaseNotes(activity.getString(R.string.about_update_release_notes_loading));
        }
        UpdateDownloadCoordinator.showDialogIdleState(dialogHandle);

        dialogHandle.setCancel(activity.getString(R.string.about_update_action_cancel_dialog), () -> {
            if (host.isDownloadInProgress()) {
                host.cancelActiveUpdateDownload();
                return;
            }
            dialogHandle.dismiss();
        });

        String releasePageUrl = remoteReleasePage == null || remoteReleasePage.isEmpty()
                ? activity.getString(R.string.about_releases_url)
                : remoteReleasePage;
        boolean hasDirectDownload = remoteApkUrl != null && !remoteApkUrl.trim().isEmpty();
        if (!hasDirectDownload) {
            dialogHandle.setPrimary(activity.getString(R.string.about_update_action_view_release), () -> {
                host.markPromptedVersion(remoteVersionCode);
                dialogHandle.dismiss();
                host.openUrl(releasePageUrl);
            });
            dialogHandle.show();
            host.applyLargeDialogWidth(dialogHandle.getDialog());
            loadReleaseNotes(
                    dialogHandle,
                    remoteVersionName,
                    !embeddedReleaseNotes.isEmpty());
            return;
        }

        dialogHandle.setPrimary(activity.getString(R.string.about_update_action_download), () -> {
            host.markPromptedVersion(remoteVersionCode);
            host.startStartupUpdateDownload(
                    remoteVersionName,
                    remoteApkUrl,
                    dialogHandle);
        });
        dialogHandle.setOnDismissListener(host::cancelActiveUpdateDownload);
        dialogHandle.show();
        host.applyLargeDialogWidth(dialogHandle.getDialog());
        loadReleaseNotes(
                dialogHandle,
                remoteVersionName,
                !embeddedReleaseNotes.isEmpty());
    }

    private void loadReleaseNotes(UpdateAvailableDialog.DialogHandle dialogHandle,
            String targetVersionName,
            boolean hasEmbeddedReleaseNotes) {
        Locale locale = activity.getResources().getConfiguration().getLocales().get(0);
        releaseNotesController.load(targetVersionName, hasEmbeddedReleaseNotes,
                new ReleaseNotesController.Listener() {
                    @Override
                    public boolean isAlive() {
                        return !activity.isFinishing()
                                && !activity.isDestroyed()
                                && dialogHandle.isShowing();
                    }

                    @Override
                    public void onBody(String body) {
                        dialogHandle.setReleaseNotes(ReleaseNotesMarkdownRenderer.render(
                                activity,
                                body,
                                locale));
                    }

                    @Override
                    public void onEmptyBody() {
                        dialogHandle.setReleaseNotes(activity.getString(R.string.about_update_release_notes_empty));
                    }

                    @Override
                    public void onFailure() {
                        dialogHandle.setReleaseNotes(activity.getString(R.string.about_update_release_notes_failed));
                    }
                });
    }

    private void showComposeStartupDisclaimerDialog(
            StartupDisclaimerAcceptance acceptance,
            Runnable onAccepted) {
        StartupDisclaimerDialog.show(
                activity,
                acceptance::markAccepted,
                () -> host.showToast(R.string.startup_disclaimer_save_failed),
                () -> {
                    if (onAccepted != null) {
                        onAccepted.run();
                    }
                },
                host::finishActivity);
    }
}
