package com.dpis.module.home;

import com.dpis.module.BuildConfig;
import com.dpis.module.R;
import com.dpis.module.ui.TouchFeedbackBinder;

import com.dpis.module.root.RootAccessProbe;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.appcompat.widget.AppCompatImageView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

public final class HomeWorkspaceBinder {
    private int statusCardEqualizationGeneration = 0;

    public static final class State {
        final boolean xposedModuleActivated;
        final int configuredAppCount;
        final int importedFontCount;
        final int templateCount;
        final RootAccessProbe.Result rootAccess;
        final HomeUpdateUiState updateState;
        final Actions actions;

        public State(boolean xposedModuleActivated,
                int configuredAppCount,
                int importedFontCount,
                int templateCount,
                RootAccessProbe.Result rootAccess,
                HomeUpdateUiState updateState,
                Actions actions) {
            this.xposedModuleActivated = xposedModuleActivated;
            this.configuredAppCount = Math.max(0, configuredAppCount);
            this.importedFontCount = Math.max(0, importedFontCount);
            this.templateCount = Math.max(0, templateCount);
            this.rootAccess = rootAccess != null
                    ? rootAccess
                    : RootAccessProbe.Result.unknown();
            this.updateState = updateState != null
                    ? updateState
                    : HomeUpdateUiState.UP_TO_DATE;
            this.actions = actions != null ? actions : Actions.NO_OP;
        }
    }

    public interface Actions {
        Actions NO_OP = new Actions() {
            @Override
            public void retryUpdateCheck() {
            }

            @Override
            public void showReleaseNotes() {
            }

            @Override
            public void startUpdateDownload() {
            }

            @Override
            public void installDownloadedUpdate() {
            }

            @Override
            public void openConfiguredAppsWorkspace() {
            }

            @Override
            public void openFontLibrary() {
            }

            @Override
            public void openTemplateWorkspace() {
            }
        };

        void retryUpdateCheck();

        void showReleaseNotes();

        void startUpdateDownload();

        void installDownloadedUpdate();

        void openConfiguredAppsWorkspace();

        void openFontLibrary();

        void openTemplateWorkspace();
    }

    private final Context context;

    private enum PrimaryStatusTone {
        DISABLED(
                R.color.home_status_disabled_container,
                R.color.home_status_disabled_content
        ),
        ENABLED(
                R.color.home_status_enabled_container,
                R.color.home_status_enabled_content
        ),
        UPDATE_AVAILABLE(
                R.color.home_status_update_container,
                R.color.home_status_update_content
        );

        final int containerColorRes;
        final int contentColorRes;

        PrimaryStatusTone(int containerColorRes, int contentColorRes) {
            this.containerColorRes = containerColorRes;
            this.contentColorRes = contentColorRes;
        }
    }

    public HomeWorkspaceBinder(Context context) {
        this.context = context;
    }

    public void bind(View workspaceView, State state) {
        if (workspaceView == null || state == null) {
            return;
        }
        bindStatus(workspaceView, state);
        bindModeHelpEntry(workspaceView);
        bindDonateEntry(workspaceView);
        bindFeedbackEntry(workspaceView);
    }

    private void bindStatus(View workspaceView, State state) {
        bindPrimaryStatusVisuals(workspaceView, state);
        setText(
                workspaceView.findViewById(R.id.home_primary_status_title),
                primaryStatusTitleRes(state)
        );
        setText(
                workspaceView.findViewById(R.id.home_primary_status_summary),
                state.updateState.subtitle(context)
        );
        bindUpdateActions(workspaceView, state);
        setText(
                workspaceView.findViewById(R.id.home_configured_apps_value),
                context.getString(
                        R.string.home_workspace_status_configured_apps_value,
                        state.configuredAppCount
                )
        );
        setText(
                workspaceView.findViewById(R.id.home_imported_fonts_value),
                Integer.toString(state.importedFontCount)
        );
        setText(
                workspaceView.findViewById(R.id.home_templates_value),
                Integer.toString(state.templateCount)
        );
        equalizeStatusCardHeights(workspaceView);
        bindStatusCardActions(workspaceView, state);
        bindInfoRow(
                workspaceView.findViewById(R.id.home_info_version),
                R.string.home_workspace_info_version,
                context.getString(
                        R.string.home_workspace_info_version_value,
                        BuildConfig.VERSION_NAME,
                        BuildConfig.VERSION_CODE
                )
        );
        bindInfoRow(
                workspaceView.findViewById(R.id.home_info_system),
                R.string.home_workspace_info_system,
                context.getString(
                        R.string.home_workspace_info_system_value,
                        Build.VERSION.RELEASE,
                        Build.VERSION.SDK_INT
                )
        );
        bindInfoRow(
                workspaceView.findViewById(R.id.home_info_root),
                R.string.home_workspace_info_root,
                rootAccessText(state.rootAccess)
        );
        bindInfoRow(
                workspaceView.findViewById(R.id.home_info_device),
                R.string.home_workspace_info_device,
                buildDeviceName()
        );
        bindInfoRowShape(
                workspaceView.findViewById(R.id.home_info_version),
                R.drawable.bg_home_info_row_top,
                true
        );
        bindInfoRowShape(
                workspaceView.findViewById(R.id.home_info_system),
                R.drawable.bg_home_info_row_middle,
                false
        );
        bindInfoRowShape(
                workspaceView.findViewById(R.id.home_info_root),
                R.drawable.bg_home_info_row_middle,
                false
        );
        bindInfoRowShape(
                workspaceView.findViewById(R.id.home_info_device),
                R.drawable.bg_home_info_row_bottom,
                false
        );
    }

    private void equalizeStatusCardHeights(View workspaceView) {
        int generation = ++statusCardEqualizationGeneration;
        View[] cards = {
                workspaceView.findViewById(R.id.home_configured_apps_card),
                workspaceView.findViewById(R.id.home_imported_fonts_card),
                workspaceView.findViewById(R.id.home_templates_card)
        };
        for (View card : cards) {
            setLayoutHeight(card, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        ViewTreeObserver observer = workspaceView.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                ViewTreeObserver currentObserver = workspaceView.getViewTreeObserver();
                if (currentObserver.isAlive()) {
                    currentObserver.removeOnPreDrawListener(this);
                }
                if (generation != statusCardEqualizationGeneration) {
                    return true;
                }
                applyEqualStatusCardHeight(cards);
                return true;
            }
        });
        workspaceView.requestLayout();
    }

    private static void applyEqualStatusCardHeight(View[] cards) {
        if (cards == null) {
            return;
        }
        int targetHeight = 0;
        for (View card : cards) {
            if (card != null) {
                targetHeight = Math.max(targetHeight, card.getHeight());
            }
        }
        if (targetHeight <= 0) {
            return;
        }
        for (View card : cards) {
            setLayoutHeight(card, targetHeight);
        }
    }

    public static int equalStatusCardHeightForTest(int... measuredHeights) {
        int targetHeight = 0;
        if (measuredHeights != null) {
            for (int measuredHeight : measuredHeights) {
                targetHeight = Math.max(targetHeight, measuredHeight);
            }
        }
        return Math.max(0, targetHeight);
    }

    private static void setLayoutHeight(View view, int height) {
        if (view == null) {
            return;
        }
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params == null || params.height == height) {
            return;
        }
        params.height = height;
        view.setLayoutParams(params);
    }

    private void bindStatusCardActions(View workspaceView, State state) {
        bindStatusCardAction(
                workspaceView.findViewById(R.id.home_configured_apps_card),
                state.actions::openConfiguredAppsWorkspace
        );
        bindStatusCardAction(
                workspaceView.findViewById(R.id.home_imported_fonts_card),
                state.actions::openFontLibrary
        );
        bindStatusCardAction(
                workspaceView.findViewById(R.id.home_templates_card),
                state.actions::openTemplateWorkspace
        );
    }

    private void bindStatusCardAction(View card, Runnable action) {
        if (card == null || action == null) {
            return;
        }
        TouchFeedbackBinder.bindPressHaptic(card);
        card.setOnClickListener(v -> action.run());
    }

    private void bindPrimaryStatusVisuals(View workspaceView, State state) {
        MaterialCardView card = workspaceView.findViewById(
                R.id.home_primary_status_card
        );
        AppCompatImageView icon = workspaceView.findViewById(
                R.id.home_primary_status_icon
        );
        PrimaryStatusTone tone = resolvePrimaryStatusTone(state);
        int containerColor = context.getColor(tone.containerColorRes);
        int contentColor = context.getColor(tone.contentColorRes);
        if (card != null) {
            card.setCardBackgroundColor(containerColor);
        }
        if (icon != null) {
            icon.setImageResource(primaryStatusIconRes(state));
            icon.setImageTintList(ColorStateList.valueOf(contentColor));
        }
        tintText(
                workspaceView.findViewById(R.id.home_primary_status_title),
                contentColor
        );
        tintText(
                workspaceView.findViewById(R.id.home_primary_status_summary),
                contentColor
        );
    }

    private int primaryStatusTitleRes(State state) {
        if (!state.xposedModuleActivated) {
            return R.string.home_workspace_status_enable_in_lsposed;
        }
        return R.string.home_workspace_status_enabled;
    }

    private int primaryStatusIconRes(State state) {
        if (isPrimaryStatusDisabled(state)) {
            return R.drawable.ic_error_outline_24;
        }
        return R.drawable.ic_check_24;
    }

    private PrimaryStatusTone resolvePrimaryStatusTone(State state) {
        // The primary card color represents module availability plus update availability.
        // Update-check progress only changes the subtitle, not the card tone.
        if (isPrimaryStatusDisabled(state)) {
            return PrimaryStatusTone.DISABLED;
        }
        if (shouldShowUpdateActionCard(state)) {
            return PrimaryStatusTone.UPDATE_AVAILABLE;
        }
        return PrimaryStatusTone.ENABLED;
    }

    private boolean isPrimaryStatusDisabled(State state) {
        return !state.xposedModuleActivated;
    }

    private void bindUpdateActions(View workspaceView, State state) {
        bindRetrySummary(workspaceView, state);
        bindUpdateActionCard(workspaceView, state);
        bindReleaseNotesAction(workspaceView, state);
        bindInstallAction(workspaceView, state);
    }

    private void bindRetrySummary(View workspaceView, State state) {
        View summary = workspaceView.findViewById(R.id.home_primary_status_summary);
        if (summary == null) {
            return;
        }
        boolean retry = state.updateState.status == HomeUpdateUiState.Status.FAILED;
        summary.setClickable(retry);
        summary.setFocusable(retry);
        summary.setOnClickListener(retry ? v -> state.actions.retryUpdateCheck() : null);
        if (retry) {
            TouchFeedbackBinder.bindPressHaptic(summary);
        } else {
            summary.setOnTouchListener(null);
        }
    }

    private void bindUpdateActionCard(View workspaceView, State state) {
        View actionCard = workspaceView.findViewById(R.id.home_update_action_card);
        if (actionCard != null) {
            actionCard.setVisibility(shouldShowUpdateActionCard(state) ? View.VISIBLE : View.GONE);
        }
    }

    private void bindReleaseNotesAction(View workspaceView, State state) {
        MaterialButton notesButton = workspaceView.findViewById(
                R.id.home_update_action_release_notes_button);
        if (notesButton != null) {
            TouchFeedbackBinder.bindPressHaptic(notesButton);
            notesButton.setOnClickListener(v -> state.actions.showReleaseNotes());
        }
    }

    private void bindInstallAction(View workspaceView, State state) {
        MaterialButton installButton = workspaceView.findViewById(
                R.id.home_update_action_install_button);
        View installButtonFrame = workspaceView.findViewById(
                R.id.home_update_action_install_frame);
        View progressFill = workspaceView.findViewById(
                R.id.home_update_action_install_progress_fill);
        boolean downloading = state.updateState.status == HomeUpdateUiState.Status.DOWNLOADING;
        boolean installReady = state.updateState.status == HomeUpdateUiState.Status.INSTALL_READY;
        if (installButton != null) {
            TouchFeedbackBinder.bindPressHaptic(installButton);
            installButton.setEnabled(!downloading);
            installButton.setClickable(!downloading);
            installButton.setFocusable(!downloading);
            installButton.setText(resolveInstallActionText(state.updateState.status));
            installButton.setOnClickListener(downloading
                    ? null
                    : v -> {
                        if (installReady) {
                            state.actions.installDownloadedUpdate();
                            return;
                        }
                        state.actions.startUpdateDownload();
                    });
        }
        if (installButtonFrame != null) {
            installButtonFrame.setEnabled(!downloading);
            installButtonFrame.setOnClickListener(null);
        }
        if (progressFill != null) {
            boolean showFill = downloading && state.updateState.downloadProgress > 0;
            progressFill.setVisibility(showFill ? View.VISIBLE : View.GONE);
            if (showFill) {
                View parent = progressFill.getParent() instanceof View
                        ? (View) progressFill.getParent()
                        : null;
                int parentWidth = parent != null ? parent.getWidth() : 0;
                if (parentWidth > 0) {
                    bindProgressFillWidth(
                            progressFill,
                            parentWidth,
                            state.updateState.downloadProgress
                    );
                } else if (parent != null) {
                    parent.post(() -> bindProgressFillWidth(
                            progressFill,
                            parent.getWidth(),
                            state.updateState.downloadProgress
                    ));
                }
            }
        }
    }

    private static boolean shouldShowUpdateActionCard(State state) {
        return state.updateState.showsUpdateActionCard();
    }

    private static int resolveInstallActionText(HomeUpdateUiState.Status status) {
        return switch (status) {
            case DOWNLOADING -> R.string.home_update_action_downloading;
            case INSTALL_READY -> R.string.home_update_action_install_ready;
            default -> R.string.home_update_action_install;
        };
    }

    private static void bindProgressFillWidth(View progressFill,
            int parentWidth,
            int progress) {
        if (progressFill == null || parentWidth <= 0) {
            return;
        }
        int fillWidth = parentWidth * Math.max(0, Math.min(100, progress)) / 100;
        ViewGroup.LayoutParams params = progressFill.getLayoutParams();
        if (params != null && params.width != fillWidth) {
            params.width = fillWidth;
            progressFill.setLayoutParams(params);
        }
    }

    private void bindInfoRow(View rowView, int labelResId, String value) {
        if (rowView == null) {
            return;
        }
        setText(rowView.findViewById(R.id.home_info_row_label), labelResId);
        setText(rowView.findViewById(R.id.home_info_row_value), value);
    }

    private void bindInfoRowShape(View rowView,
            int backgroundResId,
            boolean firstRow) {
        if (rowView == null) {
            return;
        }
        rowView.setBackgroundResource(backgroundResId);
        ViewGroup.MarginLayoutParams params
                = rowView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams
                        ? (ViewGroup.MarginLayoutParams) rowView.getLayoutParams()
                        : null;
        if (params != null) {
            params.topMargin = firstRow
                    ? 0
                    : context.getResources().getDimensionPixelSize(
                            R.dimen.home_workspace_connected_row_gap
                    );
            rowView.setLayoutParams(params);
        }
    }

    private String rootAccessText(RootAccessProbe.Result result) {
        if (result == null || result.status == RootAccessProbe.Status.UNKNOWN) {
            return context.getString(R.string.home_workspace_info_root_checking);
        }
        if (result.status == RootAccessProbe.Status.AVAILABLE) {
            return context.getString(
                    R.string.home_workspace_info_root_available,
                    result.provider
            );
        }
        return context.getString(R.string.home_workspace_info_root_unavailable);
    }

    private void bindModeHelpEntry(View workspaceView) {
        View entry = workspaceView.findViewById(R.id.home_mode_help_entry);
        if (entry == null) {
            return;
        }
        TouchFeedbackBinder.bindPressHaptic(entry);
        entry.setOnClickListener(v ->
                context.startActivity(new Intent(context, ModeHelpActivity.class)));
    }

    private void bindDonateEntry(View workspaceView) {
        View entry = workspaceView.findViewById(R.id.home_donate_entry);
        if (entry == null) {
            return;
        }
        TouchFeedbackBinder.bindPressHaptic(entry);
        entry.setOnClickListener(v ->
                context.startActivity(DonateActivity.createIntent(context)));
    }

    private void bindFeedbackEntry(View workspaceView) {
        bindUrlButton(
                workspaceView.findViewById(R.id.home_feedback_github_button),
                context.getString(R.string.about_issues_url)
        );
        bindUrlButton(
                workspaceView.findViewById(R.id.home_feedback_qq_button),
                context.getString(R.string.home_feedback_qq_url)
        );
        bindUrlButton(
                workspaceView.findViewById(R.id.home_feedback_telegram_button),
                context.getString(R.string.home_feedback_telegram_url)
        );
    }

    private void bindUrlButton(View button, String url) {
        if (button == null || url == null || url.isEmpty()) {
            return;
        }
        TouchFeedbackBinder.bindPressHaptic(button);
        button.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(intent);
        });
    }

    private void setText(MaterialTextView view, int textResId) {
        if (view != null) {
            view.setText(context.getString(textResId));
        }
    }

    private static void setText(MaterialTextView view, String text) {
        if (view != null) {
            view.setText(text);
        }
    }

    private static void tintText(MaterialTextView view, int color) {
        if (view != null) {
            view.setTextColor(color);
        }
    }

    private static String buildDeviceName() {
        String manufacturer = normalizeBuildValue(Build.MANUFACTURER);
        String model = normalizeBuildValue(Build.MODEL);
        if (manufacturer.isEmpty()) {
            return model;
        }
        if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            return model;
        }
        return manufacturer + " " + model;
    }

    private static String normalizeBuildValue(String value) {
        return value != null ? value.trim() : "";
    }
}
