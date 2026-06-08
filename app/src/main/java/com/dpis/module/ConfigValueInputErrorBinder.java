package com.dpis.module;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.textfield.TextInputLayout;

final class ConfigValueInputErrorBinder {
    private ConfigValueInputErrorBinder() {
    }

    static void bindFullMessage(TextInputLayout inputLayout, boolean valid) {
        int defaultStrokeColor = MaterialColors.getColor(
                inputLayout, com.google.android.material.R.attr.colorOutline);
        int errorStrokeColor = MaterialColors.getColor(
                inputLayout, androidx.appcompat.R.attr.colorError);
        if (valid) {
            inputLayout.setError(null);
            inputLayout.setErrorEnabled(false);
            inputLayout.setBoxStrokeColor(defaultStrokeColor);
            return;
        }
        inputLayout.setError(
                inputLayout.getContext().getString(R.string.status_save_invalid)
        );
        inputLayout.setBoxStrokeColor(errorStrokeColor);
    }
}
