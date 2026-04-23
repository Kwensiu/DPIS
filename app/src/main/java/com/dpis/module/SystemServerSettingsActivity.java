package com.dpis.module;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textview.MaterialTextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.github.libxposed.service.XposedService;

public final class SystemServerSettingsActivity extends Activity {
    private static final long STATS_REFRESH_INTERVAL_MS = 500L;
    private static final String SYSTEM_SCOPE_MODERN = "system";

    private DpiConfigStore store;
    private MaterialSwitch hooksEnabledSwitch;
    private MaterialSwitch safeModeSwitch;
    private MaterialSwitch globalLogSwitch;
    private MaterialSwitch hideLauncherIconSwitch;
    private View fontDebugEntryRow;
    private SharedPreferences statsPreferences;
    private int selectedMode = FontDebugStatsStore.MODE_CHAIN;
    private int selectedWindow = FontDebugStatsStore.WINDOW_ALL;

    private BottomSheetDialog fontDebugDialog;
    private MaterialButton dialogOverlayActionButton;
    private MaterialButton dialogStatsModeButton;
    private MaterialButton dialogStatsWindowButton;
    private MaterialTextView dialogStatsLastUpdatedView;
    private MaterialTextView dialogStatsContentView;
    private SystemHooksToggleController hooksToggleController;

    private final Handler statsHandler = new Handler(Looper.getMainLooper());
    private final Runnable statsRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            refreshStatsPanel();
            statsHandler.postDelayed(this, STATS_REFRESH_INTERVAL_MS);
        }
    };

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
        fontDebugEntryRow = bindEntryRow(
                R.id.row_font_debug_overlay,
                R.drawable.ic_info_outline_24,
                R.string.font_debug_overlay_label,
                R.string.font_debug_entry_hint,
                this::showFontDebugDialog);
        bindEntryRow(
                R.id.row_about,
                R.drawable.ic_info_outline_24,
                R.string.settings_about_label,
                R.string.settings_about_hint,
                v -> startActivity(new Intent(this, AboutActivity.class)));
        hideLauncherIconSwitch = bindSwitchRow(
                R.id.row_hide_launcher_icon,
                R.drawable.ic_block_24,
                R.string.settings_hide_launcher_icon_label,
                R.string.settings_hide_launcher_icon_hint);

        statsPreferences = FontDebugStatsStore.getPreferences(this);
        if (store != null) {
            selectedMode = store.getFontDebugSelectedMode();
            selectedWindow = store.getFontDebugSelectedWindow();
        }

        if (store == null) {
            hooksEnabledSwitch.setEnabled(false);
            safeModeSwitch.setEnabled(false);
            globalLogSwitch.setEnabled(false);
            hideLauncherIconSwitch.setEnabled(false);
            setRowEnabled(fontDebugEntryRow, false);
            showToast(R.string.status_save_requires_init);
            return;
        }

        safeModeSwitch.setChecked(store.isSystemServerSafeModeEnabled());
        globalLogSwitch.setChecked(store.isGlobalLogEnabled());
        boolean launcherIconHidden = resolveLauncherIconHiddenState(store.isLauncherIconHidden());
        setCheckedSilently(hideLauncherIconSwitch, launcherIconHidden, this::onHideLauncherIconChanged);
        if (launcherIconHidden != store.isLauncherIconHidden()) {
            store.setLauncherIconHidden(launcherIconHidden);
        }
        hooksToggleController = new SystemHooksToggleController(
                store,
                new ActivitySystemScopeGateway(),
                new ActivitySystemHooksToggleView());
        hooksToggleController.syncFromStore();

        hooksEnabledSwitch.setOnCheckedChangeListener(this::onHooksEnabledChanged);
        safeModeSwitch.setOnCheckedChangeListener(this::onSafeModeChanged);
        globalLogSwitch.setOnCheckedChangeListener(this::onGlobalLogChanged);
        hideLauncherIconSwitch.setOnCheckedChangeListener(this::onHideLauncherIconChanged);
        syncHooksSwitchWithScope();
    }

    @Override
    protected void onStart() {
        super.onStart();
        statsHandler.post(statsRefreshRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncHooksSwitchWithScope();
        syncLauncherIconSwitch();
        if (store != null && store.isFontDebugOverlayEnabled() && canDrawOverlays()) {
            startFontDebugOverlayService();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        statsHandler.removeCallbacks(statsRefreshRunnable);
        dismissFontDebugDialog();
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

    private MaterialSwitch bindSwitchRow(int rowId, int iconRes, int titleRes, int subtitleRes) {
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

    private View bindEntryRow(int rowId,
                              int iconRes,
                              int titleRes,
                              int subtitleRes,
                              View.OnClickListener clickListener) {
        View row = findViewById(rowId);
        ImageView iconView = row.findViewById(R.id.setting_icon);
        MaterialTextView titleView = row.findViewById(R.id.setting_title);
        MaterialTextView subtitleView = row.findViewById(R.id.setting_subtitle);
        iconView.setImageResource(iconRes);
        titleView.setText(titleRes);
        subtitleView.setText(subtitleRes);
        row.setOnClickListener(clickListener);
        return row;
    }

    private void showFontDebugDialog(View anchor) {
        if (store == null) {
            return;
        }
        dismissFontDebugDialog();
        ViewGroup root = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(this).inflate(
                R.layout.dialog_font_debug_stats, root, false);
        MaterialButton overlayActionButton = dialogView.findViewById(R.id.dialog_overlay_action);
        MaterialButton modeButton = dialogView.findViewById(R.id.dialog_stats_mode_button);
        MaterialButton windowButton = dialogView.findViewById(R.id.dialog_stats_window_button);
        MaterialButton clearButton = dialogView.findViewById(R.id.dialog_stats_clear);
        MaterialTextView lastUpdatedView = dialogView.findViewById(R.id.dialog_stats_last_updated);
        MaterialTextView contentView = dialogView.findViewById(R.id.dialog_stats_content);
        View closeButton = dialogView.findViewById(R.id.dialog_stats_close);

        overlayActionButton.setOnClickListener(v -> {
            boolean currentEnabled = store.isFontDebugOverlayEnabled();
            boolean requestedEnabled = !currentEnabled;
            if (requestedEnabled && !canDrawOverlays()) {
                requestOverlayPermission();
                showToast(R.string.font_debug_overlay_permission_needed);
                updateDialogButtons();
                return;
            }
            if (!store.setFontDebugOverlayEnabled(requestedEnabled)) {
                showToast(R.string.system_settings_save_failed);
                updateDialogButtons();
                return;
            }
            if (requestedEnabled) {
                startFontDebugOverlayService();
            } else {
                stopService(new Intent(this, FontDebugOverlayService.class));
            }
            updateDialogButtons();
        });

        modeButton.setOnClickListener(v -> {
            selectedMode = selectedMode == FontDebugStatsStore.MODE_CHAIN
                    ? FontDebugStatsStore.MODE_CHAIN_VIEW
                    : FontDebugStatsStore.MODE_CHAIN;
            store.setFontDebugSelectedMode(selectedMode);
            updateDialogButtons();
            refreshStatsPanel();
        });

        windowButton.setOnClickListener(v -> {
            if (selectedWindow == FontDebugStatsStore.WINDOW_5S) {
                selectedWindow = FontDebugStatsStore.WINDOW_30S;
            } else if (selectedWindow == FontDebugStatsStore.WINDOW_30S) {
                selectedWindow = FontDebugStatsStore.WINDOW_ALL;
            } else {
                selectedWindow = FontDebugStatsStore.WINDOW_5S;
            }
            store.setFontDebugSelectedWindow(selectedWindow);
            updateDialogButtons();
            refreshStatsPanel();
        });

        closeButton.setOnClickListener(v -> dismissFontDebugDialog());
        clearButton.setOnClickListener(v -> {
            clearDebugStatsData();
            refreshStatsPanel();
            showToast(R.string.font_debug_clear_done);
        });

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(dialogView);
        dialog.setOnDismissListener(d -> {
            dialogOverlayActionButton = null;
            dialogStatsModeButton = null;
            dialogStatsWindowButton = null;
            dialogStatsLastUpdatedView = null;
            dialogStatsContentView = null;
            fontDebugDialog = null;
        });
        fontDebugDialog = dialog;
        dialogOverlayActionButton = overlayActionButton;
        dialogStatsModeButton = modeButton;
        dialogStatsWindowButton = windowButton;
        dialogStatsLastUpdatedView = lastUpdatedView;
        dialogStatsContentView = contentView;
        updateDialogButtons();
        refreshStatsPanel();
        dialog.show();
    }

    private void dismissFontDebugDialog() {
        if (fontDebugDialog != null) {
            fontDebugDialog.dismiss();
        }
    }

    private void refreshStatsPanel() {
        if (statsPreferences == null
                || dialogStatsLastUpdatedView == null
                || dialogStatsContentView == null) {
            return;
        }
        String key = resolveStatsKey(selectedMode, selectedWindow);
        String statsText = statsPreferences.getString(key, null);
        long updatedAt = statsPreferences.getLong(FontDebugStatsStore.KEY_UPDATED_AT, 0L);
        int eventTotal = statsPreferences.getInt(FontDebugStatsStore.KEY_EVENT_TOTAL, 0);

        if (statsText == null || statsText.trim().isEmpty()) {
            FontDebugDataDiagnostics.NoDataReason reason =
                    FontDebugDataDiagnostics.resolveNoDataReason(store, statsPreferences);
            if (reason == FontDebugDataDiagnostics.NoDataReason.NONE) {
                dialogStatsContentView.setText(getString(R.string.font_debug_not_updated));
            } else {
                dialogStatsContentView.setText(getString(
                        R.string.font_debug_no_data_with_reason,
                        reasonTitleText(reason),
                        reasonHintText(reason)));
            }
        } else {
            dialogStatsContentView.setText(statsText);
        }

        if (updatedAt <= 0L) {
            dialogStatsLastUpdatedView.setText(getString(R.string.font_debug_not_updated));
            return;
        }
        DateFormat format = DateFormat.getTimeInstance(DateFormat.MEDIUM, Locale.getDefault());
        String timeText = format.format(new Date(updatedAt));
        dialogStatsLastUpdatedView.setText(getString(R.string.font_debug_last_updated, timeText, eventTotal));
    }

    private String reasonTitleText(FontDebugDataDiagnostics.NoDataReason reason) {
        return switch (reason) {
            case SCOPE_MISSING -> getString(R.string.font_debug_reason_scope_missing);
            case NOT_INJECTED -> getString(R.string.font_debug_reason_not_injected);
            case NO_EVENTS -> getString(R.string.font_debug_reason_no_events);
            default -> getString(R.string.font_debug_not_updated);
        };
    }

    private String reasonHintText(FontDebugDataDiagnostics.NoDataReason reason) {
        return switch (reason) {
            case SCOPE_MISSING -> getString(R.string.font_debug_reason_scope_missing_hint);
            case NOT_INJECTED -> getString(R.string.font_debug_reason_not_injected_hint);
            case NO_EVENTS -> getString(R.string.font_debug_reason_no_events_hint);
            default -> getString(R.string.font_debug_not_updated);
        };
    }

    private void clearDebugStatsData() {
        if (statsPreferences == null) {
            return;
        }
        statsPreferences.edit()
                .remove(FontDebugStatsStore.KEY_CHAIN_5S)
                .remove(FontDebugStatsStore.KEY_CHAIN_30S)
                .remove(FontDebugStatsStore.KEY_CHAIN_ALL)
                .remove(FontDebugStatsStore.KEY_CHAIN_VIEW_5S)
                .remove(FontDebugStatsStore.KEY_CHAIN_VIEW_30S)
                .remove(FontDebugStatsStore.KEY_CHAIN_VIEW_ALL)
                .remove(FontDebugStatsStore.KEY_EVENT_TOTAL)
                .remove(FontDebugStatsStore.KEY_UPDATED_AT)
                .remove(FontDebugStatsStore.KEY_UNIT_BREAKDOWN_5S)
                .remove(FontDebugStatsStore.KEY_VIEWPORT_DEBUG_SUMMARY)
                .apply();
    }

    private void updateDialogButtons() {
        if (store == null) {
            return;
        }
        if (dialogStatsModeButton != null) {
            dialogStatsModeButton.setText(selectedMode == FontDebugStatsStore.MODE_CHAIN
                    ? R.string.font_debug_mode_button_chain
                    : R.string.font_debug_mode_button_chain_view);
        }
        if (dialogStatsWindowButton != null) {
            int windowLabelRes = switch (selectedWindow) {
                case FontDebugStatsStore.WINDOW_5S -> R.string.font_debug_window_button_5s;
                case FontDebugStatsStore.WINDOW_30S -> R.string.font_debug_window_button_30s;
                default -> R.string.font_debug_window_button_all;
            };
            dialogStatsWindowButton.setText(windowLabelRes);
        }
        if (dialogOverlayActionButton != null) {
            boolean overlayEnabled = store.isFontDebugOverlayEnabled();
            dialogOverlayActionButton.setText(overlayEnabled
                    ? R.string.font_debug_overlay_disable_button
                    : R.string.font_debug_overlay_enable_button);
            int bgColor = MaterialColors.getColor(dialogOverlayActionButton,
                    overlayEnabled
                            ? com.google.android.material.R.attr.colorErrorContainer
                            : com.google.android.material.R.attr.colorPrimaryContainer);
            int fgColor = MaterialColors.getColor(dialogOverlayActionButton,
                    overlayEnabled
                            ? com.google.android.material.R.attr.colorOnErrorContainer
                            : com.google.android.material.R.attr.colorOnPrimaryContainer);
            dialogOverlayActionButton.setBackgroundTintList(ColorStateList.valueOf(bgColor));
            dialogOverlayActionButton.setTextColor(fgColor);
        }
    }

    private static String resolveStatsKey(int mode, int window) {
        if (mode == FontDebugStatsStore.MODE_CHAIN_VIEW) {
            if (window == FontDebugStatsStore.WINDOW_5S) {
                return FontDebugStatsStore.KEY_CHAIN_VIEW_5S;
            }
            if (window == FontDebugStatsStore.WINDOW_30S) {
                return FontDebugStatsStore.KEY_CHAIN_VIEW_30S;
            }
            return FontDebugStatsStore.KEY_CHAIN_VIEW_ALL;
        }
        if (window == FontDebugStatsStore.WINDOW_5S) {
            return FontDebugStatsStore.KEY_CHAIN_5S;
        }
        if (window == FontDebugStatsStore.WINDOW_30S) {
            return FontDebugStatsStore.KEY_CHAIN_30S;
        }
        return FontDebugStatsStore.KEY_CHAIN_ALL;
    }

    private void onHooksEnabledChanged(CompoundButton buttonView, boolean isChecked) {
        if (hooksToggleController == null) {
            return;
        }
        hooksToggleController.onUserToggle(isChecked);
    }

    private void onSafeModeChanged(CompoundButton buttonView, boolean isChecked) {
        if (store == null) {
            return;
        }
        if (!store.setSystemServerSafeModeEnabled(isChecked)) {
            setCheckedSilently(safeModeSwitch, !isChecked, this::onSafeModeChanged);
            showToast(R.string.system_settings_save_failed);
        }
    }

    private void onGlobalLogChanged(CompoundButton buttonView, boolean isChecked) {
        if (store == null) {
            return;
        }
        if (!store.setGlobalLogEnabled(isChecked)) {
            setCheckedSilently(globalLogSwitch, !isChecked, this::onGlobalLogChanged);
            showToast(R.string.system_settings_save_failed);
            return;
        }
        DpisLog.setLoggingEnabled(isChecked);
    }

    private void onHideLauncherIconChanged(CompoundButton buttonView, boolean isChecked) {
        if (store == null) {
            return;
        }
        if (isChecked) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.settings_hide_launcher_icon_confirm_title)
                    .setMessage(R.string.settings_hide_launcher_icon_confirm_message)
                    .setPositiveButton(R.string.dialog_process_action_confirm_positive,
                            (dialog, which) -> {
                                if (!persistLauncherIconState(true)) {
                                    setCheckedSilently(hideLauncherIconSwitch, false,
                                            this::onHideLauncherIconChanged);
                                }
                            })
                    .setNegativeButton(R.string.dialog_process_action_confirm_negative,
                            (dialog, which) -> setCheckedSilently(hideLauncherIconSwitch, false,
                                    this::onHideLauncherIconChanged))
                    .setOnCancelListener(dialog -> setCheckedSilently(hideLauncherIconSwitch, false,
                            this::onHideLauncherIconChanged))
                    .show();
            return;
        }
        if (!persistLauncherIconState(false)) {
            setCheckedSilently(hideLauncherIconSwitch, true, this::onHideLauncherIconChanged);
        }
    }

    private boolean canDrawOverlays() {
        return Settings.canDrawOverlays(this);
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void startFontDebugOverlayService() {
        Intent serviceIntent = new Intent(this, FontDebugOverlayService.class);
        startService(serviceIntent);
    }

    private void showToast(int messageResId) {
        showToast(getString(messageResId));
    }

    private void showToast(int messageResId, Object... formatArgs) {
        showToast(getString(messageResId, formatArgs));
    }

    private void showToast(CharSequence message) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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

    private void syncHooksSwitchWithScope() {
        if (hooksToggleController == null) {
            return;
        }
        hooksToggleController.syncFromStore();
    }

    private void syncLauncherIconSwitch() {
        if (store == null || hideLauncherIconSwitch == null) {
            return;
        }
        boolean hidden = resolveLauncherIconHiddenState(store.isLauncherIconHidden());
        if (hidden != store.isLauncherIconHidden()) {
            store.setLauncherIconHidden(hidden);
        }
        setCheckedSilently(hideLauncherIconSwitch, hidden, this::onHideLauncherIconChanged);
    }

    private boolean persistLauncherIconState(boolean hidden) {
        if (!setLauncherAliasHidden(hidden)) {
            showToast(R.string.settings_hide_launcher_icon_apply_failed);
            return false;
        }
        if (store.setLauncherIconHidden(hidden)) {
            return true;
        }
        setLauncherAliasHidden(!hidden);
        showToast(R.string.system_settings_save_failed);
        return false;
    }

    private boolean setLauncherAliasHidden(boolean hidden) {
        try {
            int state = hidden
                    ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
            getPackageManager().setComponentEnabledSetting(
                    getLauncherAliasComponentName(),
                    state,
                    PackageManager.DONT_KILL_APP);
            return true;
        } catch (RuntimeException error) {
            return false;
        }
    }

    private boolean resolveLauncherIconHiddenState(boolean fallback) {
        int state = getPackageManager().getComponentEnabledSetting(getLauncherAliasComponentName());
        if (state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                || state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED) {
            return true;
        }
        if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            return false;
        }
        return fallback;
    }

    private ComponentName getLauncherAliasComponentName() {
        return new ComponentName(this, getPackageName() + ".MainActivityLauncher");
    }

    private final class ActivitySystemHooksToggleView implements SystemHooksToggleController.View {
        @Override
        public void render(SystemHookState state) {
            if (hooksEnabledSwitch == null) {
                return;
            }
            setCheckedSilently(hooksEnabledSwitch, state.switchChecked,
                    SystemServerSettingsActivity.this::onHooksEnabledChanged);
            hooksEnabledSwitch.setEnabled(state.switchEnabled);
        }

        @Override
        public void showInitRequired() {
            showToast(R.string.status_save_requires_init);
        }

        @Override
        public void showSaveFailed() {
            showToast(R.string.system_settings_save_failed);
        }

        @Override
        public void showScopeRequired() {
            showToast(R.string.system_hooks_scope_required);
        }
    }

    private final class ActivitySystemScopeGateway implements SystemHooksToggleController.ScopeGateway {
        @Override
        public boolean isServiceAvailable() {
            return DpisApplication.getXposedService() != null;
        }

        @Override
        public boolean hasSystemScopeSelected() {
            XposedService service = DpisApplication.getXposedService();
            if (service == null) {
                return false;
            }
            try {
                List<String> scope = service.getScope();
                return scope != null && scope.contains(SYSTEM_SCOPE_MODERN);
            } catch (RuntimeException error) {
                return false;
            }
        }
    }
}
