package com.dpis.module.updates;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.dpis.module.R;
import com.dpis.module.ui.WatchUiMode;
import com.dpis.module.ui.compose.StartupDisclaimerDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
        if (WatchUiMode.shouldUseCompactUi(activity)) {
            showLegacyStartupDisclaimerDialog(acceptance, onAccepted);
        } else {
            showComposeStartupDisclaimerDialog(acceptance, onAccepted);
        }
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

    /** Compact Wear devices retain the layout-qualified View implementation. */
    private void showLegacyStartupDisclaimerDialog(
            StartupDisclaimerAcceptance acceptance,
            Runnable onAccepted) {
        View dialogView = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_startup_disclaimer, null, false);
        MaterialCheckBox agreementCheckBox = dialogView.findViewById(R.id.startup_disclaimer_checkbox);
        MaterialButton acceptButton = dialogView.findViewById(R.id.startup_disclaimer_accept_button);

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setView(dialogView)
                .create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnKeyListener((unused, keyCode, event) -> {
            if (keyCode != android.view.KeyEvent.KEYCODE_BACK) {
                return false;
            }
            if (event.getAction() == android.view.KeyEvent.ACTION_UP) {
                host.finishActivity();
            }
            return true;
        });

        agreementCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> acceptButton.setEnabled(isChecked));
        acceptButton.setOnClickListener(v -> {
            if (!agreementCheckBox.isChecked()) {
                return;
            }
            if (!acceptance.markAccepted()) {
                host.showToast(R.string.startup_disclaimer_save_failed);
                return;
            }
            dialog.dismiss();
            if (onAccepted != null) {
                onAccepted.run();
            }
        });
        dialog.show();
        host.applyLargeDialogWidth(dialog);
    }
}
