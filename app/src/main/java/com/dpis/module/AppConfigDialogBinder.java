package com.dpis.module;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

final class AppConfigDialogBinder {
    private static final long MODE_TOGGLE_ANIM_DURATION_MS = 200L;

    enum ProcessAction {
        START,
        RESTART,
        STOP
    }

    interface Host {
        void clearDialogInputFocus(View fallbackFocusView,
                TextInputEditText viewportInputView,
                TextInputEditText fontInputView);

        void toggleScope(AppListItem item,
                boolean currentlyInScope,
                Runnable onTurnedInScope,
                Runnable onTurnedOutScope);

        void executeProcessAction(AppListItem item, ProcessAction action);

        boolean setDpisEnabled(String packageName, boolean enabled);

        int[] saveAppConfig(AppListItem item,
                TextInputEditText viewportInput,
                TextInputEditText fontScaleInput,
                String viewportMode,
                String fontMode);

        void showToast(int messageResId);
    }

    private final Activity activity;
    private final Host host;

    AppConfigDialogBinder(Activity activity, Host host) {
        this.activity = activity;
        this.host = host;
    }

    void bind(View dialogView, AppListItem item, boolean systemHooksEnabled) {
        AppConfigDialogViews views = initDialogViews(dialogView);
        AppConfigDialogState state = bindDialogInitialState(item, views);
        AppConfigDialogActionStyle style = resolveDialogActionStyle(views.scopeButton);
        refreshDialogState(views, state, style, systemHooksEnabled);
        bindDialogValidation(dialogView, views, state, style, systemHooksEnabled);
        bindDialogActions(dialogView, item, views, state, style, systemHooksEnabled);
    }

    private AppConfigDialogViews initDialogViews(View dialogView) {
        return new AppConfigDialogViews(
                dialogView.findViewById(R.id.dialog_app_icon),
                dialogView.findViewById(R.id.dialog_title),
                dialogView.findViewById(R.id.dialog_package),
                dialogView.findViewById(R.id.dialog_status),
                dialogView.findViewById(R.id.dialog_hyperos_native_warning),
                dialogView.findViewById(R.id.dialog_viewport_input_layout),
                dialogView.findViewById(R.id.dialog_viewport_input),
                dialogView.findViewById(R.id.dialog_font_scale_input_layout),
                dialogView.findViewById(R.id.dialog_font_scale_input),
                new ModeToggle(
                        dialogView.findViewById(R.id.dialog_viewport_mode_toggle_button),
                        dialogView.findViewById(R.id.dialog_viewport_mode_toggle_thumb),
                        dialogView.findViewById(R.id.dialog_viewport_mode_emulation_label),
                        dialogView.findViewById(R.id.dialog_viewport_mode_replace_label)),
                new ModeToggle(
                        dialogView.findViewById(R.id.dialog_font_mode_toggle_button),
                        dialogView.findViewById(R.id.dialog_font_mode_toggle_thumb),
                        dialogView.findViewById(R.id.dialog_font_mode_emulation_label),
                        dialogView.findViewById(R.id.dialog_font_mode_replace_label)),
                dialogView.findViewById(R.id.dialog_scope_button),
                dialogView.findViewById(R.id.dialog_start_button),
                dialogView.findViewById(R.id.dialog_restart_button),
                dialogView.findViewById(R.id.dialog_stop_button),
                dialogView.findViewById(R.id.dialog_dpis_toggle_button),
                dialogView.findViewById(R.id.dialog_disable_button),
                dialogView.findViewById(R.id.dialog_save_button));
    }

    private AppConfigDialogState bindDialogInitialState(AppListItem item, AppConfigDialogViews views) {
        views.iconView.setImageDrawable(item.icon);
        views.titleView.setText(item.label);
        views.packageView.setText(item.packageName);
        bindHyperOsNativeWarning(views.hyperOsNativeWarningView, item);
        views.viewportInputView.setText(item.viewportWidthDp != null
                ? String.valueOf(item.viewportWidthDp)
                : "");
        views.fontInputView.setText(item.fontScalePercent != null
                ? String.valueOf(item.fontScalePercent)
                : "");
        bindViewportModeToggle(views.viewportModeToggle, item.viewportMode, false);
        bindFontModeToggle(views.fontModeToggle, item.fontMode, false);
        updateSaveButtonState(views.viewportInputLayout, views.viewportInputView,
                views.fontInputLayout, views.fontInputView, views.saveButton);
        return new AppConfigDialogState(item.inScope, item.dpisEnabled);
    }

    private AppConfigDialogActionStyle resolveDialogActionStyle(MaterialButton baseButton) {
        ColorStateList defaultActionBgTint = baseButton.getBackgroundTintList();
        int defaultActionStrokeWidth = baseButton.getStrokeWidth();
        int defaultActionTextColor = MaterialColors.getColor(
                baseButton, androidx.appcompat.R.attr.colorPrimary);
        return new AppConfigDialogActionStyle(defaultActionBgTint,
                defaultActionStrokeWidth, defaultActionTextColor);
    }

    private void refreshDialogState(AppConfigDialogViews views,
            AppConfigDialogState state,
            AppConfigDialogActionStyle style,
            boolean systemHooksEnabled) {
        updateDialogStatus(
                views.statusView,
                state.scopeSelected,
                state.dpisEnabled,
                views.viewportInputView,
                views.viewportModeToggle,
                views.fontInputView,
                views.fontModeToggle,
                systemHooksEnabled);
        bindScopeButton(views.scopeButton, state.scopeSelected,
                style.defaultActionBgTint, style.defaultActionStrokeWidth, style.defaultActionTextColor);
        bindDpisToggleButton(views.dpisToggleButton, state.dpisEnabled,
                style.defaultActionBgTint, style.defaultActionStrokeWidth, style.defaultActionTextColor);
    }

    private void bindDialogValidation(View dialogView,
            AppConfigDialogViews views,
            AppConfigDialogState state,
            AppConfigDialogActionStyle style,
            boolean systemHooksEnabled) {
        android.widget.TextView.OnEditorActionListener doneListener = (v, actionId, event) -> {
            boolean isDoneAction = actionId == EditorInfo.IME_ACTION_DONE;
            boolean isEnterDown = event != null
                    && event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
            if (!isDoneAction && !isEnterDown) {
                return false;
            }
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            return true;
        };
        views.viewportInputView.setOnEditorActionListener(doneListener);
        views.fontInputView.setOnEditorActionListener(doneListener);
        TextWatcher validationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSaveButtonState(views.viewportInputLayout, views.viewportInputView,
                        views.fontInputLayout, views.fontInputView, views.saveButton);
                refreshDialogState(views, state, style, systemHooksEnabled);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        views.viewportInputView.addTextChangedListener(validationWatcher);
        views.fontInputView.addTextChangedListener(validationWatcher);
    }

    private void bindDialogActions(View dialogView,
            AppListItem item,
            AppConfigDialogViews views,
            AppConfigDialogState state,
            AppConfigDialogActionStyle style,
            boolean systemHooksEnabled) {
        dialogView.setFocusable(true);
        dialogView.setFocusableInTouchMode(true);
        dialogView.setClickable(true);
        dialogView.setOnClickListener(
                v -> host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView));
        views.scopeButton.setOnClickListener(v -> {
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            host.toggleScope(item, state.scopeSelected,
                    () -> {
                        state.scopeSelected = true;
                        refreshDialogState(views, state, style, systemHooksEnabled);
                    },
                    () -> {
                        state.scopeSelected = false;
                        refreshDialogState(views, state, style, systemHooksEnabled);
                    });
        });
        views.startButton.setOnClickListener(v -> {
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            host.executeProcessAction(item, ProcessAction.START);
        });
        views.restartButton.setOnClickListener(v -> {
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            host.executeProcessAction(item, ProcessAction.RESTART);
        });
        views.stopButton.setOnClickListener(v -> {
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            host.executeProcessAction(item, ProcessAction.STOP);
        });
        views.dpisToggleButton.setOnClickListener(v -> {
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            boolean nextEnabled = !state.dpisEnabled;
            if (host.setDpisEnabled(item.packageName, nextEnabled)) {
                state.dpisEnabled = nextEnabled;
                refreshDialogState(views, state, style, systemHooksEnabled);
            }
        });
        views.disableButton.setOnClickListener(v -> {
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            views.viewportInputView.setText("");
            views.fontInputView.setText("");
            bindViewportModeToggle(views.viewportModeToggle, ViewportApplyMode.FIELD_REWRITE, true);
            bindFontModeToggle(views.fontModeToggle, FontApplyMode.FIELD_REWRITE, true);
            updateSaveButtonState(views.viewportInputLayout, views.viewportInputView,
                    views.fontInputLayout, views.fontInputView, views.saveButton);
            refreshDialogState(views, state, style, systemHooksEnabled);
        });
        views.saveButton.setOnClickListener(v -> {
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            int[] result = host.saveAppConfig(
                    item,
                    views.viewportInputView,
                    views.fontInputView,
                    resolveViewportMode(views.viewportModeToggle),
                    resolveFontMode(views.fontModeToggle));
            if (result[0] == 1) {
                showSaveButtonFeedback(views.saveButton);
            }
            if (result[1] != 0) {
                host.showToast(result[1]);
            }
        });
        views.viewportModeToggle.container.setOnClickListener(v -> {
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            toggleViewportMode(views.viewportModeToggle);
            refreshDialogState(views, state, style, systemHooksEnabled);
        });
        views.viewportModeToggle.emulationLabel.setOnClickListener(v -> {
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            bindViewportModeToggle(
                    views.viewportModeToggle, ViewportApplyMode.SYSTEM_EMULATION, true);
            refreshDialogState(views, state, style, systemHooksEnabled);
        });
        views.viewportModeToggle.replaceLabel.setOnClickListener(v -> {
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            bindViewportModeToggle(
                    views.viewportModeToggle, ViewportApplyMode.FIELD_REWRITE, true);
            refreshDialogState(views, state, style, systemHooksEnabled);
        });
        views.fontModeToggle.container.setOnClickListener(v -> {
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            toggleFontMode(views.fontModeToggle);
            refreshDialogState(views, state, style, systemHooksEnabled);
        });
        views.fontModeToggle.emulationLabel.setOnClickListener(v -> {
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            bindFontModeToggle(views.fontModeToggle, FontApplyMode.SYSTEM_EMULATION, true);
            refreshDialogState(views, state, style, systemHooksEnabled);
        });
        views.fontModeToggle.replaceLabel.setOnClickListener(v -> {
            host.clearDialogInputFocus(dialogView, views.viewportInputView, views.fontInputView);
            bindFontModeToggle(views.fontModeToggle, FontApplyMode.FIELD_REWRITE, true);
            refreshDialogState(views, state, style, systemHooksEnabled);
        });
    }

    private static void showSaveButtonFeedback(MaterialButton saveButton) {
        if (saveButton == null) {
            return;
        }
        CharSequence restoreText;
        Object[] tag = saveButton.getTag() instanceof Object[] ? (Object[]) saveButton.getTag() : null;
        if (tag != null && tag[0] instanceof CharSequence) {
            restoreText = (CharSequence) tag[0];
            if (tag[1] instanceof Runnable) {
                saveButton.removeCallbacks((Runnable) tag[1]);
            }
        } else {
            restoreText = saveButton.getText();
        }
        saveButton.setText(R.string.status_save_success_inline);
        Runnable restore = () -> {
            if (saveButton.isAttachedToWindow()) {
                saveButton.setText(restoreText);
            }
        };
        saveButton.setTag(new Object[] { restoreText, restore });
        saveButton.postDelayed(restore, 1500);
    }

    private static boolean updateSaveButtonState(TextInputLayout viewportInputLayout,
            TextInputEditText viewportInputView,
            TextInputLayout fontInputLayout,
            TextInputEditText fontInputView,
            MaterialButton saveButton) {
        boolean viewportValid = isPositiveIntOrEmpty(viewportInputView);
        boolean fontValid = isFontPercentOrEmpty(fontInputView);
        int defaultStrokeColor = MaterialColors.getColor(
                viewportInputLayout, com.google.android.material.R.attr.colorOutline);
        int errorStrokeColor = MaterialColors.getColor(
                viewportInputLayout, androidx.appcompat.R.attr.colorError);
        viewportInputLayout.setError(null);
        fontInputLayout.setError(null);
        viewportInputLayout.setErrorEnabled(false);
        fontInputLayout.setErrorEnabled(false);
        viewportInputLayout.setBoxStrokeColor(viewportValid ? defaultStrokeColor : errorStrokeColor);
        fontInputLayout.setBoxStrokeColor(fontValid ? defaultStrokeColor : errorStrokeColor);
        boolean valid = viewportValid && fontValid;
        saveButton.setEnabled(valid);
        return valid;
    }

    private static boolean isPositiveIntOrEmpty(TextInputEditText inputView) {
        String raw = inputView.getText() != null ? inputView.getText().toString().trim() : "";
        if (raw.isEmpty()) {
            return true;
        }
        try {
            return Integer.parseInt(raw) > 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean isFontPercentOrEmpty(TextInputEditText inputView) {
        String raw = inputView.getText() != null ? inputView.getText().toString().trim() : "";
        if (raw.isEmpty()) {
            return true;
        }
        try {
            int value = Integer.parseInt(raw);
            return value >= 50 && value <= 300;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private void updateDialogStatus(MaterialTextView statusView,
            boolean inScope,
            boolean dpisEnabled,
            TextInputEditText viewportInputView,
            ModeToggle viewportModeToggle,
            TextInputEditText fontInputView,
            ModeToggle fontModeToggle,
            boolean systemHooksEnabled) {
        Integer widthDp = parsePositiveIntOrNullSafe(viewportInputView);
        Integer fontScalePercent = parseFontScalePercentOrNullSafe(fontInputView);
        String viewportMode = widthDp == null ? ViewportApplyMode.OFF : resolveViewportMode(viewportModeToggle);
        String fontMode = fontScalePercent == null ? FontApplyMode.OFF : resolveFontMode(fontModeToggle);
        String dialogStatusText = AppStatusFormatter.formatCompact(
                activity.getResources(), inScope, widthDp, viewportMode,
                fontScalePercent, fontMode, dpisEnabled);
        boolean warnViewport = AppStatusFormatter.shouldWarnViewportEmulation(
                widthDp, viewportMode, systemHooksEnabled, dpisEnabled);
        boolean warnFont = AppStatusFormatter.shouldWarnFontEmulation(
                fontScalePercent, fontMode, systemHooksEnabled, dpisEnabled);
        if (warnViewport || warnFont) {
            int warnColor = MaterialColors.getColor(statusView, androidx.appcompat.R.attr.colorError);
            statusView.setText(AppStatusFormatter.applyConfigSegmentsWarnStyle(
                    dialogStatusText, warnColor, warnViewport, warnFont));
            return;
        }
        statusView.setText(dialogStatusText);
    }

    private void bindHyperOsNativeWarning(MaterialTextView warningView, AppListItem item) {
        if (!item.hyperOsNativeProxyCandidate) {
            warningView.setVisibility(View.GONE);
            return;
        }
        HyperOsNativeProxyStatus proxyStatus = HyperOsNativeProxyStatus.inspect(activity, item.packageName);
        int colorAttr = proxyStatus.isPresent()
                ? androidx.appcompat.R.attr.colorPrimary
                : androidx.appcompat.R.attr.colorError;
        int statusColor = MaterialColors.getColor(warningView, colorAttr);
        warningView.setTextColor(statusColor);
        warningView.setText(resolveHyperOsNativeWarningText(proxyStatus));
        warningView.setVisibility(View.VISIBLE);
    }

    private String resolveHyperOsNativeWarningText(HyperOsNativeProxyStatus proxyStatus) {
        return switch (proxyStatus.state) {
            case PRESENT -> activity.getString(R.string.dialog_hyperos_native_proxy_present);
            case MISSING -> activity.getString(R.string.dialog_hyperos_native_proxy_missing);
            case UNKNOWN -> activity.getString(R.string.dialog_hyperos_native_proxy_unknown);
        };
    }

    private static Integer parsePositiveIntOrNullSafe(TextInputEditText inputView) {
        try {
            return parsePositiveIntOrNull(inputView);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Integer parseFontScalePercentOrNullSafe(TextInputEditText inputView) {
        try {
            return parseFontScalePercentOrNull(inputView);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Integer parsePositiveIntOrNull(TextInputEditText inputView)
            throws NumberFormatException {
        String raw = inputView.getText() != null ? inputView.getText().toString().trim() : "";
        if (raw.isEmpty()) {
            return null;
        }
        int value = Integer.parseInt(raw);
        if (value <= 0) {
            throw new NumberFormatException("must be positive");
        }
        return value;
    }

    private static Integer parseFontScalePercentOrNull(TextInputEditText inputView)
            throws NumberFormatException {
        String raw = inputView.getText() != null ? inputView.getText().toString().trim() : "";
        if (raw.isEmpty()) {
            return null;
        }
        int value = Integer.parseInt(raw);
        if (value < 50 || value > 300) {
            throw new NumberFormatException("font scale out of range");
        }
        return value;
    }

    private static String resolveFontMode(ModeToggle fontModeToggle) {
        Object modeTag = fontModeToggle.container.getTag();
        if (FontApplyMode.SYSTEM_EMULATION.equals(modeTag)) {
            return FontApplyMode.SYSTEM_EMULATION;
        }
        return FontApplyMode.FIELD_REWRITE;
    }

    private static String resolveViewportMode(ModeToggle viewportModeToggle) {
        Object modeTag = viewportModeToggle.container.getTag();
        if (ViewportApplyMode.SYSTEM_EMULATION.equals(modeTag)) {
            return ViewportApplyMode.SYSTEM_EMULATION;
        }
        return ViewportApplyMode.FIELD_REWRITE;
    }

    private static void bindFontModeToggle(ModeToggle fontModeToggle,
            String fontMode,
            boolean animate) {
        String resolved = FontApplyMode.SYSTEM_EMULATION.equals(fontMode)
                ? FontApplyMode.SYSTEM_EMULATION
                : FontApplyMode.FIELD_REWRITE;
        fontModeToggle.container.setTag(resolved);
        updateModeToggleVisual(fontModeToggle, FontApplyMode.SYSTEM_EMULATION.equals(resolved), animate);
    }

    private static void toggleFontMode(ModeToggle fontModeToggle) {
        String nextMode = FontApplyMode.FIELD_REWRITE.equals(resolveFontMode(fontModeToggle))
                ? FontApplyMode.SYSTEM_EMULATION
                : FontApplyMode.FIELD_REWRITE;
        bindFontModeToggle(fontModeToggle, nextMode, true);
    }

    private static void bindViewportModeToggle(ModeToggle viewportModeToggle,
            String viewportMode,
            boolean animate) {
        String resolved = ViewportApplyMode.SYSTEM_EMULATION.equals(viewportMode)
                ? ViewportApplyMode.SYSTEM_EMULATION
                : ViewportApplyMode.FIELD_REWRITE;
        viewportModeToggle.container.setTag(resolved);
        updateModeToggleVisual(viewportModeToggle,
                ViewportApplyMode.SYSTEM_EMULATION.equals(resolved), animate);
    }

    private static void toggleViewportMode(ModeToggle viewportModeToggle) {
        String nextMode = ViewportApplyMode.FIELD_REWRITE.equals(
                resolveViewportMode(viewportModeToggle))
                        ? ViewportApplyMode.SYSTEM_EMULATION
                        : ViewportApplyMode.FIELD_REWRITE;
        bindViewportModeToggle(viewportModeToggle, nextMode, true);
    }

    private static void updateModeToggleVisual(ModeToggle toggle,
            boolean emulationActive,
            boolean animate) {
        int activeTextColor = MaterialColors.getColor(
                toggle.container, com.google.android.material.R.attr.colorOnSecondaryContainer);
        int inactiveTextColor = MaterialColors.getColor(
                toggle.container, com.google.android.material.R.attr.colorOnSurface);
        toggle.emulationLabel.setTextColor(emulationActive ? activeTextColor : inactiveTextColor);
        toggle.replaceLabel.setTextColor(emulationActive ? inactiveTextColor : activeTextColor);
        toggle.emulationLabel.setAlpha(emulationActive ? 1f : 0.66f);
        toggle.replaceLabel.setAlpha(emulationActive ? 0.66f : 1f);
        toggle.emulationLabel.setTypeface(Typeface.DEFAULT,
                emulationActive ? Typeface.BOLD : Typeface.NORMAL);
        toggle.replaceLabel.setTypeface(Typeface.DEFAULT,
                emulationActive ? Typeface.NORMAL : Typeface.BOLD);
        toggle.emulationLabel.setScaleX(emulationActive ? 1.04f : 1f);
        toggle.emulationLabel.setScaleY(emulationActive ? 1.04f : 1f);
        toggle.replaceLabel.setScaleX(emulationActive ? 1f : 1.04f);
        toggle.replaceLabel.setScaleY(emulationActive ? 1f : 1.04f);
        toggle.container.post(() -> {
            int available = toggle.container.getWidth()
                    - toggle.container.getPaddingLeft()
                    - toggle.container.getPaddingRight();
            if (available <= 0) {
                return;
            }
            int half = available / 2;
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) toggle.thumb.getLayoutParams();
            if (params.width != half) {
                params.width = half;
                toggle.thumb.setLayoutParams(params);
            }
            float target = emulationActive ? 0f : half;
            if (animate) {
                toggle.thumb.animate().cancel();
                toggle.thumb.animate()
                        .translationX(target)
                        .setDuration(MODE_TOGGLE_ANIM_DURATION_MS)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            } else {
                toggle.thumb.setTranslationX(target);
            }
        });
    }

    private void bindScopeButton(MaterialButton scopeButton,
            boolean inScope,
            ColorStateList defaultBgTint,
            int defaultStrokeWidth,
            int defaultTextColor) {
        int activeBgColor = MaterialColors.getColor(
                scopeButton, com.google.android.material.R.attr.colorSecondaryContainer);
        int activeFgColor = MaterialColors.getColor(
                scopeButton, com.google.android.material.R.attr.colorOnSecondaryContainer);
        scopeButton.setIcon(null);
        int scopeTextRes = inScope ? R.string.scope_remove_button : R.string.scope_add_button;
        scopeButton.setText(scopeTextRes);
        scopeButton.setBackgroundTintList(inScope
                ? ColorStateList.valueOf(activeBgColor)
                : defaultBgTint);
        scopeButton.setTextColor(inScope ? activeFgColor : defaultTextColor);
        scopeButton.setStrokeWidth(inScope ? 0 : defaultStrokeWidth);
        scopeButton.setContentDescription(activity.getString(scopeTextRes));
    }

    private void bindDpisToggleButton(MaterialButton dpisToggleButton,
            boolean dpisEnabled,
            ColorStateList defaultBgTint,
            int defaultStrokeWidth,
            int defaultTextColor) {
        String buttonText = activity.getString(
                dpisEnabled ? R.string.dialog_dpis_disable_button : R.string.dialog_dpis_enable_button);
        dpisToggleButton.setText(buttonText);
        dpisToggleButton.setIcon(null);
        int activeBgColor = MaterialColors.getColor(
                dpisToggleButton, com.google.android.material.R.attr.colorSecondaryContainer);
        int activeFgColor = MaterialColors.getColor(
                dpisToggleButton, com.google.android.material.R.attr.colorOnSecondaryContainer);
        boolean enabledActive = dpisEnabled;
        dpisToggleButton.setBackgroundTintList(
                enabledActive ? ColorStateList.valueOf(activeBgColor) : defaultBgTint);
        dpisToggleButton.setTextColor(enabledActive ? activeFgColor : defaultTextColor);
        dpisToggleButton.setStrokeWidth(enabledActive ? 0 : defaultStrokeWidth);
        dpisToggleButton.setContentDescription(buttonText);
    }

    private static final class ModeToggle {
        final View container;
        final View thumb;
        final MaterialTextView emulationLabel;
        final MaterialTextView replaceLabel;

        ModeToggle(View container, View thumb, MaterialTextView emulationLabel,
                MaterialTextView replaceLabel) {
            this.container = container;
            this.thumb = thumb;
            this.emulationLabel = emulationLabel;
            this.replaceLabel = replaceLabel;
        }
    }

    private static final class AppConfigDialogViews {
        final android.widget.ImageView iconView;
        final MaterialTextView titleView;
        final MaterialTextView packageView;
        final MaterialTextView statusView;
        final MaterialTextView hyperOsNativeWarningView;
        final TextInputLayout viewportInputLayout;
        final TextInputEditText viewportInputView;
        final TextInputLayout fontInputLayout;
        final TextInputEditText fontInputView;
        final ModeToggle viewportModeToggle;
        final ModeToggle fontModeToggle;
        final MaterialButton scopeButton;
        final MaterialButton startButton;
        final MaterialButton restartButton;
        final MaterialButton stopButton;
        final MaterialButton dpisToggleButton;
        final MaterialButton disableButton;
        final MaterialButton saveButton;

        AppConfigDialogViews(android.widget.ImageView iconView,
                MaterialTextView titleView,
                MaterialTextView packageView,
                MaterialTextView statusView,
                MaterialTextView hyperOsNativeWarningView,
                TextInputLayout viewportInputLayout,
                TextInputEditText viewportInputView,
                TextInputLayout fontInputLayout,
                TextInputEditText fontInputView,
                ModeToggle viewportModeToggle,
                ModeToggle fontModeToggle,
                MaterialButton scopeButton,
                MaterialButton startButton,
                MaterialButton restartButton,
                MaterialButton stopButton,
                MaterialButton dpisToggleButton,
                MaterialButton disableButton,
                MaterialButton saveButton) {
            this.iconView = iconView;
            this.titleView = titleView;
            this.packageView = packageView;
            this.statusView = statusView;
            this.hyperOsNativeWarningView = hyperOsNativeWarningView;
            this.viewportInputLayout = viewportInputLayout;
            this.viewportInputView = viewportInputView;
            this.fontInputLayout = fontInputLayout;
            this.fontInputView = fontInputView;
            this.viewportModeToggle = viewportModeToggle;
            this.fontModeToggle = fontModeToggle;
            this.scopeButton = scopeButton;
            this.startButton = startButton;
            this.restartButton = restartButton;
            this.stopButton = stopButton;
            this.dpisToggleButton = dpisToggleButton;
            this.disableButton = disableButton;
            this.saveButton = saveButton;
        }
    }

    private static final class AppConfigDialogState {
        boolean scopeSelected;
        boolean dpisEnabled;

        AppConfigDialogState(boolean scopeSelected, boolean dpisEnabled) {
            this.scopeSelected = scopeSelected;
            this.dpisEnabled = dpisEnabled;
        }
    }

    private static final class AppConfigDialogActionStyle {
        final ColorStateList defaultActionBgTint;
        final int defaultActionStrokeWidth;
        final int defaultActionTextColor;

        AppConfigDialogActionStyle(ColorStateList defaultActionBgTint,
                int defaultActionStrokeWidth,
                int defaultActionTextColor) {
            this.defaultActionBgTint = defaultActionBgTint;
            this.defaultActionStrokeWidth = defaultActionStrokeWidth;
            this.defaultActionTextColor = defaultActionTextColor;
        }
    }
}
