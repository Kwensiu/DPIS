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

import java.util.Locale;

import androidx.appcompat.widget.AppCompatImageView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

public final class HomeWorkspaceBinder {
    private int statusCardEqualizationGeneration = 0;

    public static final class State {
        /** Immutable domain snapshot shared by the legacy binder and Compose presenter. */
        public final boolean xposedModuleActivated;
        public final int configuredAppCount;
        public final int importedFontCount;
        public final int templateCount;
        public final RootAccessProbe.Result rootAccess;
        public final HomeUpdateUiState updateState;
        public final Actions actions;

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
            public void checkForUpdates() {
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

        void checkForUpdates();

        void openConfiguredAppsWorkspace();

        void openFontLibrary();

        void openTemplateWorkspace();

        default void openModeHelp() {
        }

        default void openDonate() {
        }
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
        MaterialTextView statusSummary = workspaceView.findViewById(
                R.id.home_primary_status_summary
        );

        if (statusSummary != null) {
            statusSummary.setVisibility(state.xposedModuleActivated ? View.VISIBLE : View.GONE);
            if (state.xposedModuleActivated) {
                statusSummary.setText(state.updateState.subtitle(context));
            }
        }
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
        return targetHeight;
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
                workspaceView.findViewById(R.id.home_primary_status_card),
                state.xposedModuleActivated ? state.actions::checkForUpdates : null
        );
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
        if (card == null) {
            return;
        }
        if (action == null) {
            card.setOnClickListener(null);
            card.setClickable(false);
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
        if (isPrimaryStatusDisabled(state)) {
            return PrimaryStatusTone.DISABLED;
        }
        return PrimaryStatusTone.ENABLED;
    }

    private boolean isPrimaryStatusDisabled(State state) {
        return !state.xposedModuleActivated;
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
        if (model.toLowerCase(Locale.ROOT).startsWith(manufacturer.toLowerCase(Locale.ROOT))) {
            return model;
        }
        return manufacturer + " " + model;
    }

    private static String normalizeBuildValue(String value) {
        return value != null ? value.trim() : "";
    }
}
