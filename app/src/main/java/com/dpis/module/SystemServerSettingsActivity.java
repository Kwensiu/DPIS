package com.dpis.module;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textview.MaterialTextView;

public final class SystemServerSettingsActivity extends Activity {
    private DpiConfigStore store;
    private MaterialSwitch hooksEnabledSwitch;
    private MaterialSwitch safeModeSwitch;
    private MaterialSwitch globalLogSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_server_settings);
        applyInsets();

        ImageButton backButton = findViewById(R.id.settings_back_button);
        backButton.setOnClickListener(v -> finish());

        store = DpisApplication.getConfigStore();
        hooksEnabledSwitch = bindSwitchRow(
                R.id.row_system_hooks,
                R.drawable.ic_settings_24,
                R.string.system_hooks_enabled_label,
                R.string.system_hooks_enabled_hint);
        safeModeSwitch = bindSwitchRow(
                R.id.row_safe_mode,
                android.R.drawable.ic_lock_lock,
                R.string.system_safe_mode_label,
                R.string.system_safe_mode_hint);
        globalLogSwitch = bindSwitchRow(
                R.id.row_global_log,
                R.drawable.ic_info_outline_24,
                R.string.global_log_enabled_label,
                R.string.global_log_enabled_hint);
        if (store == null) {
            hooksEnabledSwitch.setEnabled(false);
            safeModeSwitch.setEnabled(false);
            globalLogSwitch.setEnabled(false);
            Toast.makeText(this, getString(R.string.status_save_requires_init), Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        hooksEnabledSwitch.setChecked(store.isSystemServerHooksEnabled());
        safeModeSwitch.setChecked(store.isSystemServerSafeModeEnabled());
        globalLogSwitch.setChecked(store.isGlobalLogEnabled());

        hooksEnabledSwitch.setOnCheckedChangeListener(this::onHooksEnabledChanged);
        safeModeSwitch.setOnCheckedChangeListener(this::onSafeModeChanged);
        globalLogSwitch.setOnCheckedChangeListener(this::onGlobalLogChanged);
    }

    private void applyInsets() {
        View content = findViewById(R.id.settings_content);
        final int baseTopPadding = content.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            view.setPadding(view.getPaddingLeft(), baseTopPadding + statusBars.top,
                    view.getPaddingRight(), view.getPaddingBottom());
            return insets;
        });
        ViewCompat.requestApplyInsets(content);
    }

    private MaterialSwitch bindSwitchRow(int rowId,
                                         int iconRes,
                                         int titleRes,
                                         int subtitleRes) {
        View row = findViewById(rowId);
        ImageView iconView = row.findViewById(R.id.setting_icon);
        MaterialTextView titleView = row.findViewById(R.id.setting_title);
        MaterialTextView subtitleView = row.findViewById(R.id.setting_subtitle);
        MaterialSwitch switchView = row.findViewById(R.id.setting_switch);

        iconView.setImageResource(iconRes);
        titleView.setText(titleRes);
        subtitleView.setText(subtitleRes);
        row.setOnClickListener(v -> {
            if (switchView.isEnabled()) {
                switchView.toggle();
            }
        });
        return switchView;
    }

    private void onHooksEnabledChanged(CompoundButton buttonView, boolean isChecked) {
        if (store == null) {
            return;
        }
        if (!store.setSystemServerHooksEnabled(isChecked)) {
            setCheckedSilently(hooksEnabledSwitch, !isChecked, this::onHooksEnabledChanged);
            Toast.makeText(this, getString(R.string.system_settings_save_failed),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void onSafeModeChanged(CompoundButton buttonView, boolean isChecked) {
        if (store == null) {
            return;
        }
        if (!store.setSystemServerSafeModeEnabled(isChecked)) {
            setCheckedSilently(safeModeSwitch, !isChecked, this::onSafeModeChanged);
            Toast.makeText(this, getString(R.string.system_settings_save_failed),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void onGlobalLogChanged(CompoundButton buttonView, boolean isChecked) {
        if (store == null) {
            return;
        }
        if (!store.setGlobalLogEnabled(isChecked)) {
            setCheckedSilently(globalLogSwitch, !isChecked, this::onGlobalLogChanged);
            Toast.makeText(this, getString(R.string.system_settings_save_failed),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        DpisLog.setLoggingEnabled(isChecked);
    }

    private void setCheckedSilently(CompoundButton switchView,
                                    boolean checked,
                                    CompoundButton.OnCheckedChangeListener listener) {
        switchView.setOnCheckedChangeListener(null);
        switchView.setChecked(checked);
        switchView.setOnCheckedChangeListener(listener);
    }
}
