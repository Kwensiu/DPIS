package com.dpis.module;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textview.MaterialTextView;

public final class ExperimentalSettingsActivity extends LocalizedActivity {
    private DpiConfigStore configStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_experimental_settings);
        configStore = ConfigStoreFactory.createActiveModuleConfigStore(
                this, DpisApplication.getXposedService());
        bindToolbar();
        bindTtcImportSwitch();
        applyInsets();
    }

    private void bindToolbar() {
        View backButton = findViewById(R.id.experimental_settings_back_button);
        backButton.setOnClickListener(v -> finish());
    }

    private void bindTtcImportSwitch() {
        View row = findViewById(R.id.experimental_ttc_import_row);
        MaterialTextView title = row.findViewById(R.id.setting_title);
        MaterialTextView subtitle = row.findViewById(R.id.setting_subtitle);
        MaterialSwitch toggle = row.findViewById(R.id.setting_switch);
        View icon = row.findViewById(R.id.setting_icon);
        icon.setVisibility(View.GONE);
        View textColumn = (View) title.getParent();
        ViewGroup.MarginLayoutParams textColumnLayoutParams =
                (ViewGroup.MarginLayoutParams) textColumn.getLayoutParams();
        textColumnLayoutParams.setMarginStart(0);
        textColumn.setLayoutParams(textColumnLayoutParams);
        title.setText(R.string.settings_ttc_import_label);
        subtitle.setText(R.string.settings_ttc_import_hint);
        toggle.setChecked(configStore.isTtcFontImportEnabled());
        row.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));
        toggle.setOnCheckedChangeListener((button, checked) ->
                configStore.setTtcFontImportEnabled(checked));
    }

    private void applyInsets() {
        View toolbar = findViewById(R.id.experimental_settings_toolbar);
        WindowInsetsBinder.applySafeDrawingPadding(toolbar, false, true, false, false);
        View content = findViewById(R.id.experimental_settings_content);
        WindowInsetsBinder.applySafeDrawingPadding(content, false, false, false, true);
    }
}
