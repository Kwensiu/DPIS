package com.dpis.module.quirks;

import com.dpis.module.DpisConfigStore;

import com.dpis.module.viewport.DpiConfig;

import com.dpis.module.DpisApplication;
import com.dpis.module.R;
import com.dpis.module.appconfig.WechatDpiConfig;

import com.dpis.module.ui.DialogWindowSizer;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.HapticFeedbackConstants;
import android.view.View;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public final class WechatDpiSheetBinder {
    private WechatDpiSheetBinder() {
    }

    public static void bind(View dialogView, String packageName, Runnable onValidationChanged) {
        View row = row(dialogView);
        TextInputLayout inputLayout = inputLayout(dialogView);
        TextInputEditText inputView = inputView(dialogView);
        if (row == null || inputLayout == null || inputView == null) {
            return;
        }
        if (!WechatDpiConfig.appliesTo(packageName)) {
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
        DpisConfigStore store = DpisApplication.getActiveHookConfigStore(
                dialogView.getContext());
        Integer initial = store != null ? store.getWechatDpi(packageName) : null;
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

    public static boolean isInputValid(View dialogView) {
        TextInputEditText inputView = inputView(dialogView);
        if (inputView == null || inputView.getText() == null) {
            return true;
        }
        return isInputValid(inputView.getText().toString());
    }

    /** Validates the retained editor value without requiring a legacy View. */
    public static boolean isInputValid(String rawValue) {
        return WechatDpiConfig.isInputValid(rawValue);
    }

    public static void bindDoneAction(android.widget.TextView.OnEditorActionListener listener,
            View dialogView) {
        TextInputEditText inputView = inputView(dialogView);
        if (inputView != null) {
            inputView.setOnEditorActionListener(listener);
        }
    }

    public static TextInputEditText inputViewForFocus(View dialogView) {
        return inputView(dialogView);
    }

    public static boolean save(View dialogView, String packageName, boolean dpisEnabled,
            DpisConfigStore store) {
        TextInputEditText inputView = inputView(dialogView);
        String rawValue = inputView != null && inputView.getText() != null
                ? inputView.getText().toString()
                : null;
        return save(rawValue, packageName, dpisEnabled, store);
    }

    /**
     * Saves the raw editor value shared by Compose and legacy editor surfaces.
     */
    public static boolean save(String rawValue, String packageName, boolean dpisEnabled,
            DpisConfigStore store) {
        if (!WechatDpiConfig.appliesTo(packageName)) {
            return true;
        }
        if (store == null || !isInputValid(rawValue)) {
            return false;
        }
        Integer dpi = WechatDpiConfig.parseOrNull(rawValue);
        boolean saved = store.setWechatDpi(packageName, dpi);
        if (saved) {
            WechatDpiPropertySyncer.publishDpiAsync(packageName, dpisEnabled ? dpi : null);
        }
        return saved;
    }

    public static void publishForDpisState(String packageName, boolean dpisEnabled) {
        if (!WechatDpiConfig.appliesTo(packageName)) {
            return;
        }
        DpisConfigStore store = DpisApplication.getActiveHookConfigStore(null);
        Integer dpi = store != null && dpisEnabled ? store.getWechatDpi(packageName) : null;
        WechatDpiPropertySyncer.publishDpiAsync(packageName, dpi);
    }

    public static void clearDraft(View dialogView) {
        TextInputEditText inputView = inputView(dialogView);
        if (inputView != null) {
            inputView.setText("");
        }
    }

    public static String captureDraft(View dialogView) {
        TextInputEditText inputView = inputView(dialogView);
        if (inputView == null || inputView.getText() == null) {
            return null;
        }
        return inputView.getText().toString();
    }

    public static void applyDraft(View dialogView, String rawValue) {
        if (rawValue == null) {
            return;
        }
        TextInputEditText inputView = inputView(dialogView);
        TextInputLayout inputLayout = inputLayout(dialogView);
        if (inputView == null) {
            return;
        }
        inputView.setText(rawValue);
        updateValidationState(inputLayout, inputView);
    }

    private static Integer readDpiOrNull(View dialogView) {
        TextInputEditText inputView = inputView(dialogView);
        if (inputView == null || inputView.getText() == null) {
            return null;
        }
        return WechatDpiConfig.parseOrNull(inputView.getText().toString());
    }

    private static void updateValidationState(TextInputLayout inputLayout,
            TextInputEditText inputView) {
        if (inputLayout == null || inputView == null) {
            return;
        }
        String raw = inputView.getText() != null ? inputView.getText().toString() : "";
        boolean valid = WechatDpiConfig.isInputValid(raw);
        int defaultStrokeColor = MaterialColors.getColor(
                inputLayout, com.google.android.material.R.attr.colorOutline);
        int errorStrokeColor = MaterialColors.getColor(
                inputLayout, androidx.appcompat.R.attr.colorError);
        inputLayout.setError(null);
        inputLayout.setErrorEnabled(false);
        inputLayout.setBoxStrokeColor(valid ? defaultStrokeColor : errorStrokeColor);
    }

    private static View row(View dialogView) {
        return dialogView.findViewById(R.id.dialog_wechat_dpi_row);
    }

    private static TextInputLayout inputLayout(View dialogView) {
        return dialogView.findViewById(R.id.dialog_wechat_dpi_input_layout);
    }

    private static TextInputEditText inputView(View dialogView) {
        return dialogView.findViewById(R.id.dialog_wechat_dpi_input);
    }

    private static View helpButton(View dialogView) {
        return dialogView.findViewById(R.id.dialog_wechat_dpi_help_button);
    }

    private static void showHelpDialog(View anchor) {
        if (anchor == null) {
            return;
        }
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(anchor.getContext())
                .setTitle(R.string.dialog_wechat_dpi_help_title)
                .setMessage(R.string.dialog_wechat_dpi_help_message)
                .setPositiveButton(R.string.dialog_close_button, null)
                .create();
        dialog.setOnShowListener(d -> DialogWindowSizer.applyStandardWidth(
                dialog, anchor.getContext()));
        dialog.show();
    }
}
