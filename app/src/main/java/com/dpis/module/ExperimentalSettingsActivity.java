package com.dpis.module;

import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textview.MaterialTextView;

public final class ExperimentalSettingsActivity extends LocalizedActivity
        implements DpisApplication.ServiceStateListener {
    private DpiConfigStore store;
    private MaterialSwitch flutterFontHookSwitch;
    private MaterialSwitch flutterSettingsFontHookSwitch;
    private MaterialSwitch hyperOsFlutterFontHookSwitch;
    private View flutterFontHookRow;
    private View flutterSettingsFontHookRow;
    private View hyperOsFlutterFontHookRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_experimental_settings);
        applyInsets();

        ImageButton backButton = findViewById(R.id.experimental_settings_back_button);
        backButton.setOnClickListener(v -> finish());

        flutterFontHookRow = findViewById(R.id.row_flutter_font_hook);
        flutterSettingsFontHookRow = findViewById(R.id.row_flutter_settings_font_hook);
        hyperOsFlutterFontHookRow = findViewById(R.id.row_hyperos_flutter_font_hook);
        flutterFontHookSwitch = bindSwitchRow(
                flutterFontHookRow,
                R.string.settings_flutter_font_hook_label,
                R.string.settings_flutter_font_hook_hint);
        flutterSettingsFontHookSwitch = bindSwitchRow(
                flutterSettingsFontHookRow,
                R.string.settings_flutter_settings_font_hook_label,
                R.string.settings_flutter_settings_font_hook_hint);
        hyperOsFlutterFontHookSwitch = bindSwitchRow(
                hyperOsFlutterFontHookRow,
                R.string.settings_hyperos_flutter_font_hook_label,
                R.string.settings_hyperos_flutter_font_hook_hint);

        flutterFontHookSwitch.setOnCheckedChangeListener(this::onFlutterFontHookChanged);
        flutterSettingsFontHookSwitch.setOnCheckedChangeListener(
                this::onFlutterSettingsFontHookChanged);
        hyperOsFlutterFontHookSwitch.setOnCheckedChangeListener(this::onHyperOsFlutterFontHookChanged);
        refreshStoreState(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        DpisApplication.addServiceStateListener(this, true);
    }

    @Override
    protected void onStop() {
        DpisApplication.removeServiceStateListener(this);
        super.onStop();
    }

    @Override
    public void onServiceStateChanged() {
        runOnUiThread(() -> refreshStoreState(false));
    }

    private void applyInsets() {
        View content = findViewById(R.id.experimental_settings_content);
        final int baseTopPadding = content.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            view.setPadding(view.getPaddingLeft(), baseTopPadding + statusBars.top,
                    view.getPaddingRight(), view.getPaddingBottom());
            return insets;
        });
        ViewCompat.requestApplyInsets(content);
    }

    private MaterialSwitch bindSwitchRow(View row, int titleRes, int subtitleRes) {
        MaterialTextView titleView = row.findViewById(R.id.setting_title);
        MaterialTextView subtitleView = row.findViewById(R.id.setting_subtitle);
        MaterialSwitch switchView = row.findViewById(R.id.setting_switch);

        titleView.setText(titleRes);
        if (subtitleView != null) {
            subtitleView.setText(subtitleRes);
        }
        row.setOnClickListener(v -> {
            if (switchView.isEnabled()) {
                switchView.toggle();
            }
        });
        return switchView;
    }

    private void refreshStoreState(boolean showInitToast) {
        store = DpisApplication.getConfigStore();
        if (store == null) {
            setRowEnabled(flutterFontHookRow, false);
            setRowEnabled(flutterSettingsFontHookRow, false);
            setRowEnabled(hyperOsFlutterFontHookRow, false);
            flutterFontHookSwitch.setEnabled(false);
            flutterSettingsFontHookSwitch.setEnabled(false);
            hyperOsFlutterFontHookSwitch.setEnabled(false);
            if (showInitToast) {
                showToast(R.string.status_save_requires_init);
            }
            return;
        }
        setRowEnabled(flutterFontHookRow, true);
        flutterFontHookSwitch.setEnabled(true);
        applyRestoredStoreState();
    }

    private void applyRestoredStoreState() {
        if (store == null) {
            return;
        }
        setCheckedSilently(flutterFontHookSwitch,
                store.isFlutterFontHookEnabled(),
                this::onFlutterFontHookChanged);
        setCheckedSilently(flutterSettingsFontHookSwitch,
                store.isFlutterSettingsFontHookEnabled(),
                this::onFlutterSettingsFontHookChanged);
        setCheckedSilently(hyperOsFlutterFontHookSwitch,
                store.isHyperOsFlutterFontHookEnabled(),
                this::onHyperOsFlutterFontHookChanged);
        applyFlutterSupplementDependencyState();
    }

    private void applyFlutterSupplementDependencyState() {
        boolean flutterEnabled = store != null && store.isFlutterFontHookEnabled();
        setRowEnabled(flutterSettingsFontHookRow, flutterEnabled);
        flutterSettingsFontHookSwitch.setEnabled(flutterEnabled);
        setRowEnabled(hyperOsFlutterFontHookRow, flutterEnabled);
        hyperOsFlutterFontHookSwitch.setEnabled(flutterEnabled);
    }

    private void onFlutterFontHookChanged(CompoundButton buttonView, boolean isChecked) {
        if (store == null) {
            return;
        }
        if (!store.setFlutterFontHookEnabled(isChecked)) {
            setCheckedSilently(flutterFontHookSwitch, !isChecked,
                    this::onFlutterFontHookChanged);
            showToast(R.string.system_settings_save_failed);
            return;
        }
        applyFlutterSupplementDependencyState();
        if (!isChecked) {
            HyperOsNativeFontPropertySyncer.clearConfiguredFontTargetsAsync(store);
        }
    }

    private void onFlutterSettingsFontHookChanged(CompoundButton buttonView, boolean isChecked) {
        if (store == null) {
            return;
        }
        if (!store.isFlutterFontHookEnabled()) {
            setCheckedSilently(flutterSettingsFontHookSwitch,
                    store.isFlutterSettingsFontHookEnabled(),
                    this::onFlutterSettingsFontHookChanged);
            return;
        }
        if (!store.setFlutterSettingsFontHookEnabled(isChecked)) {
            setCheckedSilently(flutterSettingsFontHookSwitch, !isChecked,
                    this::onFlutterSettingsFontHookChanged);
            showToast(R.string.system_settings_save_failed);
        }
    }

    private void onHyperOsFlutterFontHookChanged(CompoundButton buttonView, boolean isChecked) {
        if (store == null) {
            return;
        }
        if (!store.isFlutterFontHookEnabled()) {
            setCheckedSilently(hyperOsFlutterFontHookSwitch,
                    store.isHyperOsFlutterFontHookEnabled(),
                    this::onHyperOsFlutterFontHookChanged);
            return;
        }
        if (!store.setHyperOsFlutterFontHookEnabled(isChecked)) {
            setCheckedSilently(hyperOsFlutterFontHookSwitch, !isChecked,
                    this::onHyperOsFlutterFontHookChanged);
            showToast(R.string.system_settings_save_failed);
            return;
        }
        if (!isChecked) {
            HyperOsNativeFontPropertySyncer.clearConfiguredFontTargetsAsync(store);
        }
    }

    private void showToast(int messageResId) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

    private static void setRowEnabled(View row, boolean enabled) {
        if (row == null) {
            return;
        }
        row.setEnabled(enabled);
        row.setAlpha(enabled ? 1f : 0.5f);
    }

    private void setCheckedSilently(CompoundButton switchView,
            boolean checked,
            CompoundButton.OnCheckedChangeListener listener) {
        if (switchView == null) {
            return;
        }
        switchView.setOnCheckedChangeListener(null);
        switchView.setChecked(checked);
        switchView.setOnCheckedChangeListener(listener);
    }
}
