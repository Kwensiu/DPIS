package com.dpis.module;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.AppCompatImageView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.textview.MaterialTextView;

final class HomeWorkspaceBinder {
    static final class State {
        final boolean serviceConnected;
        final boolean systemHooksEnabled;
        final boolean modernFlavor;
        final int enabledConfiguredAppCount;
        final int totalConfiguredAppCount;
        final int importedFontCount;
        final int templateCount;
        final RootAccessProbe.Result rootAccess;

        State(boolean serviceConnected,
                boolean systemHooksEnabled,
                boolean modernFlavor,
                int enabledConfiguredAppCount,
                int totalConfiguredAppCount,
                int importedFontCount,
                int templateCount,
                RootAccessProbe.Result rootAccess) {
            this.serviceConnected = serviceConnected;
            this.systemHooksEnabled = systemHooksEnabled;
            this.modernFlavor = modernFlavor;
            this.enabledConfiguredAppCount = Math.max(0, enabledConfiguredAppCount);
            this.totalConfiguredAppCount = Math.max(0, totalConfiguredAppCount);
            this.importedFontCount = Math.max(0, importedFontCount);
            this.templateCount = Math.max(0, templateCount);
            this.rootAccess = rootAccess != null
                    ? rootAccess
                    : RootAccessProbe.Result.unknown();
        }
    }

    private final Context context;

    HomeWorkspaceBinder(Context context) {
        this.context = context;
    }

    void bind(View workspaceView, State state) {
        if (workspaceView == null || state == null) {
            return;
        }
        bindStatus(workspaceView, state);
        bindModeHelpEntry(workspaceView);
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
                primaryStatusSummaryRes(state)
        );
        setText(
                workspaceView.findViewById(R.id.home_configured_apps_value),
                context.getString(
                        R.string.home_workspace_status_configured_apps_value,
                        state.enabledConfiguredAppCount,
                        state.totalConfiguredAppCount
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

    private void bindPrimaryStatusVisuals(View workspaceView, State state) {
        MaterialCardView card = workspaceView.findViewById(
                R.id.home_primary_status_card
        );
        AppCompatImageView icon = workspaceView.findViewById(
                R.id.home_primary_status_icon
        );
        int containerColor = primaryStatusContainerColor(state);
        int contentColor = primaryStatusContentColor(state);
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
        if (state.modernFlavor && !state.serviceConnected) {
            return R.string.home_workspace_status_service_disconnected;
        }
        if (!state.systemHooksEnabled) {
            return R.string.home_workspace_status_hooks_disabled;
        }
        return R.string.home_workspace_status_service_connected;
    }

    private int primaryStatusSummaryRes(State state) {
        if (state.modernFlavor && !state.serviceConnected) {
            return R.string.home_workspace_status_service_disconnected_summary;
        }
        if (!state.systemHooksEnabled) {
            return R.string.home_workspace_status_hooks_disabled_summary;
        }
        if (!state.modernFlavor) {
            return R.string.home_workspace_status_legacy_enabled_summary;
        }
        return R.string.home_workspace_status_hooks_enabled;
    }

    private int primaryStatusIconRes(State state) {
        if ((state.modernFlavor && !state.serviceConnected)
                || !state.systemHooksEnabled) {
            return R.drawable.ic_error_outline_24;
        }
        return R.drawable.ic_check_24;
    }

    private int primaryStatusContainerColor(State state) {
        if (state.modernFlavor && !state.serviceConnected) {
            return MaterialColors.getColor(
                    context,
                    com.google.android.material.R.attr.colorErrorContainer,
                    context.getColor(R.color.dpis_stop_container)
            );
        }
        if (!state.systemHooksEnabled) {
            return context.getColor(R.color.dpis_warn_container);
        }
        return context.getColor(R.color.dpis_success_container);
    }

    private int primaryStatusContentColor(State state) {
        if (state.modernFlavor && !state.serviceConnected) {
            return MaterialColors.getColor(
                    context,
                    com.google.android.material.R.attr.colorOnErrorContainer,
                    context.getColor(R.color.dpis_on_stop_container)
            );
        }
        if (!state.systemHooksEnabled) {
            return context.getColor(R.color.dpis_on_warn_container);
        }
        return context.getColor(R.color.dpis_on_success_container);
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
