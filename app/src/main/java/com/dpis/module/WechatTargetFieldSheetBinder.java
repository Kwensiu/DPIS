package com.dpis.module;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.HapticFeedbackConstants;
import android.view.View;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

final class WechatTargetFieldSheetBinder {
    private WechatTargetFieldSheetBinder() {
    }

    static void bind(View dialogView, AppListItem item, Runnable onValidationChanged) {
        View row = row(dialogView);
        TextInputLayout inputLayout = inputLayout(dialogView);
        TextInputEditText inputView = inputView(dialogView);
        if (row == null || inputLayout == null || inputView == null) {
            return;
        }
        if (!WechatTargetFieldConfig.appliesTo(item.packageName)) {
            row.setVisibility(View.GONE);
            return;
        }
        row.setVisibility(View.VISIBLE);
        View helpButton = helpButton(dialogView);
        if (helpButton != null) {
            helpButton.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                showHelpDialog(v);
            });
        }
        DpiConfigStore store = DpisApplication.getConfigStore();
        Integer initial = store != null ? store.getWechatTargetField(item.packageName) : null;
        inputView.setText(initial != null ? String.valueOf(initial) : "");
        inputView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateValidationState(inputLayout, inputView);
                if (onValidationChanged != null) {
                    onValidationChanged.run();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        updateValidationState(inputLayout, inputView);
    }

    static boolean isInputValid(View dialogView) {
        TextInputEditText inputView = inputView(dialogView);
        if (inputView == null || inputView.getText() == null) {
            return true;
        }
        String raw = inputView.getText().toString();
        WechatTargetFieldSupport.State support =
                WechatTargetFieldSupport.current(dialogView.getContext());
        if (!support.supported) {
            return raw.isBlank();
        }
        return WechatTargetFieldConfig.isInputValid(raw);
    }

    static boolean save(View dialogView, String packageName, boolean dpisEnabled,
            DpiConfigStore store) {
        if (!WechatTargetFieldConfig.appliesTo(packageName)) {
            return true;
        }
        if (store == null || !isInputValid(dialogView)) {
            return false;
        }
        Integer targetField = readTargetFieldOrNull(dialogView);
        return saveTargetField(packageName, targetField, dpisEnabled, store);
    }

    static void publishForDpisState(String packageName, boolean dpisEnabled) {
        if (!WechatTargetFieldConfig.appliesTo(packageName)) {
            return;
        }
        DpiConfigStore store = DpisApplication.getConfigStore();
        Integer targetField = store != null && dpisEnabled
                ? store.getWechatTargetField(packageName)
                : null;
        WechatTargetFieldPropertySyncer.publishTargetAsync(packageName, targetField);
    }

    private static boolean saveTargetField(String packageName, Integer targetField,
            boolean dpisEnabled, DpiConfigStore store) {
        if (store == null) {
            return false;
        }
        boolean saved = store.setWechatTargetField(packageName, targetField);
        saved = store.clearTargetViewportWidthDp(packageName) && saved;
        saved = store.setTargetViewportApplyMode(packageName, ViewportApplyMode.OFF) && saved;
        ViewportPropertySyncer.clearTargetAsync(packageName);
        WechatTargetFieldPropertySyncer.publishTargetAsync(
                packageName, dpisEnabled ? targetField : null);
        return saved;
    }

    static void clearDraft(View dialogView) {
        TextInputEditText inputView = inputView(dialogView);
        if (inputView != null) {
            inputView.setText("");
        }
    }

    private static Integer readTargetFieldOrNull(View dialogView) {
        TextInputEditText inputView = inputView(dialogView);
        if (inputView == null || inputView.getText() == null) {
            return null;
        }
        return WechatTargetFieldConfig.parseOrNull(inputView.getText().toString());
    }

    private static void updateValidationState(TextInputLayout inputLayout,
            TextInputEditText inputView) {
        if (inputLayout == null || inputView == null) {
            return;
        }
        String raw = inputView.getText() != null ? inputView.getText().toString() : "";
        WechatTargetFieldSupport.State support =
                WechatTargetFieldSupport.current(inputLayout.getContext());
        boolean supported = support.supported;
        boolean blank = raw.isBlank();
        boolean valid = supported
                ? WechatTargetFieldConfig.isInputValid(raw)
                : blank;
        int defaultStrokeColor = MaterialColors.getColor(
                inputLayout, com.google.android.material.R.attr.colorOutline);
        int errorStrokeColor = MaterialColors.getColor(
                inputLayout, androidx.appcompat.R.attr.colorError);
        if (!supported && !blank) {
            inputLayout.setHelperText(null);
            inputLayout.setError(inputLayout.getContext().getString(
                    R.string.dialog_wechat_target_field_unsupported));
            inputLayout.setErrorEnabled(true);
        } else {
            inputLayout.setError(null);
            inputLayout.setErrorEnabled(false);
            inputLayout.setHelperText(supported ? null : inputLayout.getContext().getString(
                    R.string.dialog_wechat_target_field_unsupported));
        }
        inputLayout.setBoxStrokeColor(valid ? defaultStrokeColor : errorStrokeColor);
    }

    private static View row(View dialogView) {
        return dialogView.findViewById(R.id.dialog_wechat_target_field_row);
    }

    private static TextInputLayout inputLayout(View dialogView) {
        return dialogView.findViewById(R.id.dialog_wechat_target_field_input_layout);
    }

    private static TextInputEditText inputView(View dialogView) {
        return dialogView.findViewById(R.id.dialog_wechat_target_field_input);
    }

    private static View helpButton(View dialogView) {
        return dialogView.findViewById(R.id.dialog_wechat_target_field_help_button);
    }

    private static void showHelpDialog(View anchor) {
        if (anchor == null) {
            return;
        }
        new MaterialAlertDialogBuilder(anchor.getContext())
                .setTitle(R.string.dialog_wechat_target_field_help_title)
                .setMessage(R.string.dialog_wechat_target_field_help_message)
                .setPositiveButton(R.string.dialog_close_button, null)
                .show();
    }
}
