package com.dpis.module;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textview.MaterialTextView;

public final class ExperimentalSettingsActivity extends LocalizedActivity {
    private DpiConfigStore configStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_experimental_settings);
        configStore = ConfigStoreFactory.createForModuleApp(
                this, DpisApplication.getXposedService());
        bindTtcImportSwitch();
        applyInsets();
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
        View content = findViewById(R.id.experimental_settings_content);
        final int baseTopPadding = content.getPaddingTop();
        final int baseBottomPadding = content.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    view.getPaddingLeft(),
                    baseTopPadding + systemBars.top,
                    view.getPaddingRight(),
                    baseBottomPadding + systemBars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(content);
    }
}
