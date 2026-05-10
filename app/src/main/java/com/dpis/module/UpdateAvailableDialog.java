package com.dpis.module;

import android.app.Activity;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;

final class UpdateAvailableDialog {
    private static final long RELEASE_NOTES_TOGGLE_ANIMATION_MS = 120L;
    private static final int TAG_RELEASE_NOTES_EXPANDED = R.id.update_dialog_release_notes_card;

    private UpdateAvailableDialog() {
    }

    static DialogHandle create(Activity activity, CharSequence title, CharSequence message) {
        View dialogView = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_update_available, null, false);
        MaterialTextView titleView = dialogView.findViewById(R.id.update_dialog_title);
        MaterialTextView messageView = dialogView.findViewById(R.id.update_dialog_message);
        LinearProgressIndicator progressView =
                dialogView.findViewById(R.id.update_dialog_progress);
        MaterialTextView progressTextView =
                dialogView.findViewById(R.id.update_dialog_progress_text);
        ConstraintLayout releaseNotesHost =
                dialogView.findViewById(R.id.update_dialog_release_notes_host);
        View releaseNotesCard = dialogView.findViewById(R.id.update_dialog_release_notes_card);
        View releaseNotesContainer =
                dialogView.findViewById(R.id.update_dialog_release_notes_container);
        MaterialTextView releaseNotesText =
                dialogView.findViewById(R.id.update_dialog_release_notes_text);
        MaterialButton primaryButton = dialogView.findViewById(R.id.update_dialog_primary_button);
        MaterialButton cancelButton = dialogView.findViewById(R.id.update_dialog_cancel_button);

        titleView.setText(title);
        messageView.setText(message);

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setView(dialogView)
                .create();
        dialog.setCanceledOnTouchOutside(true);
        bindReleaseNotesToggle(releaseNotesHost, releaseNotesCard, releaseNotesContainer);
        releaseNotesText.setMovementMethod(LinkMovementMethod.getInstance());

        return new DialogHandle(
                dialog,
                primaryButton,
                cancelButton,
                progressView,
                progressTextView,
                releaseNotesHost,
                releaseNotesCard,
                releaseNotesContainer,
                releaseNotesText);
    }

    private static void bindReleaseNotesToggle(ConstraintLayout releaseNotesHost,
            View releaseNotesCard,
            View releaseNotesContainer) {
        releaseNotesContainer.setVisibility(View.GONE);
        releaseNotesContainer.setAlpha(0f);
        releaseNotesCard.setTag(TAG_RELEASE_NOTES_EXPANDED, Boolean.FALSE);
        applyReleaseNotesCardWidth(releaseNotesHost, releaseNotesCard, false);
        releaseNotesCard.setOnClickListener(v -> {
            boolean expanded = Boolean.TRUE.equals(
                    releaseNotesCard.getTag(TAG_RELEASE_NOTES_EXPANDED));
            boolean nextExpanded = !expanded;
            releaseNotesCard.setTag(TAG_RELEASE_NOTES_EXPANDED, nextExpanded);
            releaseNotesContainer.animate().cancel();
            if (nextExpanded) {
                applyReleaseNotesCardWidth(releaseNotesHost, releaseNotesCard, true);
                releaseNotesContainer.setVisibility(View.VISIBLE);
                releaseNotesContainer.setAlpha(0f);
                releaseNotesContainer.animate()
                        .alpha(1f)
                        .setDuration(RELEASE_NOTES_TOGGLE_ANIMATION_MS)
                        .start();
            } else {
                releaseNotesContainer.animate()
                        .alpha(0f)
                        .setDuration(RELEASE_NOTES_TOGGLE_ANIMATION_MS)
                        .withEndAction(() -> {
                            boolean stillCollapsed = !Boolean.TRUE.equals(
                                    releaseNotesCard.getTag(TAG_RELEASE_NOTES_EXPANDED));
                            if (!stillCollapsed) {
                                return;
                            }
                            releaseNotesContainer.setVisibility(View.GONE);
                            applyReleaseNotesCardWidth(releaseNotesHost, releaseNotesCard, false);
                        })
                        .start();
            }
        });
    }

    private static void applyReleaseNotesCardWidth(ConstraintLayout host,
            View card,
            boolean expanded) {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(host);
        int cardId = card.getId();
        if (expanded) {
            constraintSet.constrainWidth(cardId, ConstraintSet.MATCH_CONSTRAINT);
        } else {
            constraintSet.constrainWidth(cardId, ConstraintSet.WRAP_CONTENT);
        }
        constraintSet.connect(cardId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
        constraintSet.connect(cardId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
        constraintSet.setHorizontalBias(cardId, 0.5f);
        constraintSet.applyTo(host);
    }

    static final class DialogHandle {
        final AlertDialog dialog;
        final MaterialButton primaryButton;
        final MaterialButton cancelButton;
        final LinearProgressIndicator progressView;
        final MaterialTextView progressTextView;
        final ConstraintLayout releaseNotesHost;
        final View releaseNotesCard;
        final View releaseNotesContainer;
        final MaterialTextView releaseNotesText;

        DialogHandle(AlertDialog dialog,
                     MaterialButton primaryButton,
                     MaterialButton cancelButton,
                     LinearProgressIndicator progressView,
                     MaterialTextView progressTextView,
                     ConstraintLayout releaseNotesHost,
                     View releaseNotesCard,
                     View releaseNotesContainer,
                     MaterialTextView releaseNotesText) {
            this.dialog = dialog;
            this.primaryButton = primaryButton;
            this.cancelButton = cancelButton;
            this.progressView = progressView;
            this.progressTextView = progressTextView;
            this.releaseNotesHost = releaseNotesHost;
            this.releaseNotesCard = releaseNotesCard;
            this.releaseNotesContainer = releaseNotesContainer;
            this.releaseNotesText = releaseNotesText;
        }
    }
}
