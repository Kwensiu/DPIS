package com.dpis.module;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageButton;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.slider.Slider;
import com.google.android.material.textview.MaterialTextView;

final class SystemFontScaleToolBinder {
    private static final float DISABLED_ACTION_ALPHA = 0.45f;
    private static final int PREVIEW_TITLE_SP = 18;
    private static final int PREVIEW_BODY_SP = 14;

    private final LocalizedActivity activity;
    private final View workspaceView;
    private final SystemFontScaleSettingsGateway settingsGateway;

    private MaterialCardView card;
    private View operationGroup;
    private View permissionOverlay;
    private View unavailableOverlay;
    private MaterialTextView badgeView;
    private MaterialTextView pendingValueView;
    private MaterialTextView previewTitleView;
    private MaterialTextView previewBodyView;
    private AppCompatImageButton applyButton;
    private AppCompatImageButton decrementButton;
    private AppCompatImageButton incrementButton;
    private Slider slider;
    private MaterialButton restoreButton;
    private boolean expanded;
    private boolean updatingSlider;
    private SystemFontScaleToolState state;

    SystemFontScaleToolBinder(LocalizedActivity activity, View workspaceView) {
        this(activity, workspaceView, new SystemFontScaleSettingsGateway());
    }

    SystemFontScaleToolBinder(LocalizedActivity activity,
                              View workspaceView,
                              SystemFontScaleSettingsGateway settingsGateway) {
        this.activity = activity;
        this.workspaceView = workspaceView;
        this.settingsGateway = settingsGateway;
    }

    void bind() {
        card = workspaceView.findViewById(R.id.system_font_scale_card);
        operationGroup = workspaceView.findViewById(R.id.system_font_scale_operation_group);
        permissionOverlay = workspaceView.findViewById(R.id.system_font_scale_permission_overlay);
        unavailableOverlay = workspaceView.findViewById(R.id.system_font_scale_unavailable_overlay);
        badgeView = workspaceView.findViewById(R.id.system_font_scale_badge);
        pendingValueView = workspaceView.findViewById(R.id.system_font_scale_pending_value);
        previewTitleView = workspaceView.findViewById(R.id.system_font_scale_preview_title);
        previewBodyView = workspaceView.findViewById(R.id.system_font_scale_preview_body);
        applyButton = workspaceView.findViewById(R.id.system_font_scale_apply_button);
        decrementButton = workspaceView.findViewById(R.id.system_font_scale_decrement_button);
        incrementButton = workspaceView.findViewById(R.id.system_font_scale_increment_button);
        slider = workspaceView.findViewById(R.id.system_font_scale_slider);
        restoreButton = workspaceView.findViewById(R.id.system_font_scale_restore_button);

        if (slider != null) {
            slider.setValueFrom(SystemFontScaleToolState.MIN_PERCENT);
            slider.setValueTo(SystemFontScaleToolState.MAX_PERCENT);
            slider.setStepSize(1f);
            slider.addOnChangeListener((slider, value, fromUser) -> {
                if (!fromUser || updatingSlider || state == null) {
                    return;
                }
                setPendingPercent(Math.round(value));
            });
        }

        TouchFeedbackBinder.bindPressHaptic(card);
        TouchFeedbackBinder.bindPressHaptic(applyButton);
        TouchFeedbackBinder.bindPressHaptic(decrementButton);
        TouchFeedbackBinder.bindPressHaptic(incrementButton);
        TouchFeedbackBinder.bindPressHaptic(restoreButton);

        if (card != null) {
            card.setOnClickListener(v -> {
                expanded = !expanded;
                render();
            });
        }
        if (applyButton != null) {
            applyButton.setOnClickListener(v -> applyPending());
        }
        if (decrementButton != null) {
            decrementButton.setOnClickListener(v -> {
                if (state != null && state.canDecrement()) {
                    setPendingPercent(state.pendingPercent - 1);
                }
            });
        }
        if (incrementButton != null) {
            incrementButton.setOnClickListener(v -> {
                if (state != null && state.canIncrement()) {
                    setPendingPercent(state.pendingPercent + 1);
                }
            });
        }
        if (restoreButton != null) {
            restoreButton.setOnClickListener(v -> restoreDefault());
        }
        TouchFeedbackBinder.bindPressHaptic(permissionOverlay);
        if (permissionOverlay != null) {
            permissionOverlay.setOnClickListener(v -> openWriteSettingsPermission());
        }

        refreshFromSystem();
    }

    void refreshFromSystem() {
        boolean canWrite = settingsGateway.canWrite(activity);
        Integer currentPercent = settingsGateway.readPercent(activity);
        boolean unavailable = currentPercent == null;
        int pendingPercent = state != null && state.userSelectedPending
                ? state.pendingPercent
                : SystemFontScaleToolState.initialPendingPercent(currentPercent);
        state = new SystemFontScaleToolState(
                canWrite,
                currentPercent,
                pendingPercent,
                state != null && state.userSelectedPending,
                unavailable);
        render();
    }

    void collapseAndRefreshFromSystem() {
        expanded = false;
        refreshFromSystem();
    }

    private void setPendingPercent(int percent) {
        if (state == null) {
            return;
        }
        state = new SystemFontScaleToolState(
                state.canWrite,
                state.currentPercent,
                SystemFontScaleToolState.clampPercent(percent),
                true,
                state.unavailable);
        render();
    }

    private void applyPending() {
        if (state == null || !state.canApply()) {
            return;
        }
        writeScale(state.pendingPercent);
    }

    private void restoreDefault() {
        if (state == null || !state.canRestore()) {
            return;
        }
        if (state.shouldRestorePendingOnly()) {
            setPendingPercent(SystemFontScaleToolState.DEFAULT_PERCENT);
            return;
        }
        writeScale(SystemFontScaleToolState.DEFAULT_PERCENT);
    }

    private void writeScale(int percent) {
        SystemFontScaleWriter.write(new SystemFontScaleWriter.Host() {
            @Override
            public boolean writePercent(int percent) {
                return settingsGateway.writePercent(activity, percent);
            }

            @Override
            public void onWriteSucceeded(int percent) {
                state = new SystemFontScaleToolState(
                        state.canWrite,
                        percent,
                        percent,
                        false,
                        false);
                refreshFromSystem();
            }

            @Override
            public void onWriteFailed() {
                showWriteFailed();
            }
        }, percent);
    }

    private void showWriteFailed() {
        Toast.makeText(
                activity,
                R.string.system_font_scale_write_failed,
                Toast.LENGTH_SHORT).show();
        refreshFromSystem();
    }

    private void openWriteSettingsPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + activity.getPackageName()));
        activity.startActivity(intent);
    }

    @SuppressLint("SetTextI18n")
    private void render() {
        if (state == null) {
            return;
        }
        setVisible(operationGroup, expanded && (state.canWrite || state.unavailable));
        bindBadge();
        bindValues();
        bindControls();
        bindPreview();
        setVisible(permissionOverlay, expanded && !state.canWrite && !state.unavailable);
        setVisible(unavailableOverlay, expanded && state.unavailable);
    }

    private void bindBadge() {
        if (badgeView == null) {
            return;
        }
        int textRes;
        switch (state.badge()) {
            case UNAVAILABLE:
                textRes = R.string.system_font_scale_badge_unavailable;
                break;
            case PERMISSION_REQUIRED:
                textRes = R.string.system_font_scale_badge_permission_required;
                break;
            case OUT_OF_RANGE:
                textRes = R.string.system_font_scale_badge_out_of_range;
                break;
            case MODIFIED:
                textRes = R.string.system_font_scale_badge_modified;
                break;
            case NONE:
            default:
                textRes = 0;
                break;
        }
        if (textRes == 0) {
            badgeView.setVisibility(View.GONE);
            return;
        }
        badgeView.setVisibility(View.VISIBLE);
        badgeView.setText(textRes);
    }

    private void bindValues() {
        if (pendingValueView != null) {
            pendingValueView.setText(activity.getString(
                    R.string.system_font_scale_pending_value,
                    state.pendingPercent));
        }
    }

    private void bindControls() {
        boolean controlsEnabled = state.canWrite && !state.unavailable;
        if (slider != null) {
            updatingSlider = true;
            slider.setValue(SystemFontScaleToolState.clampPercent(state.pendingPercent));
            updatingSlider = false;
            slider.setEnabled(controlsEnabled);
        }
        bindIconButton(applyButton, state.canApply());
        bindIconButton(decrementButton, controlsEnabled && state.canDecrement());
        bindIconButton(incrementButton, controlsEnabled && state.canIncrement());
        if (restoreButton != null) {
            restoreButton.setEnabled(state.canRestore());
        }
    }

    private void bindIconButton(View button, boolean enabled) {
        if (button == null) {
            return;
        }
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : DISABLED_ACTION_ALPHA);
    }

    private void bindPreview() {
        float scale = state.pendingPercent / 100f;
        bindPreviewText(previewTitleView, PREVIEW_TITLE_SP, scale, true);
        bindPreviewText(previewBodyView, PREVIEW_BODY_SP, scale, false);
    }

    private void bindPreviewText(TextView view, int baseSp, float scale, boolean bold) {
        if (view == null) {
            return;
        }
        float density = activity.getResources().getDisplayMetrics().density;
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, baseSp * density * scale);
        view.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
    }

    private static void setVisible(View view, boolean visible) {
        if (view != null) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}
