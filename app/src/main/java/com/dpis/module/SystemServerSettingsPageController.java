package com.dpis.module;

import com.dpis.module.fonts.FontDebugDataDiagnostics;
import com.dpis.module.fonts.FontDebugOverlayService;
import com.dpis.module.fonts.FontDebugStatsStore;
import com.dpis.module.fonts.FontDebugStatsSchema;
import com.dpis.module.fonts.FontLibraryActivity;

import com.dpis.module.runtime.RuntimeDebugPropertySyncer;
import com.dpis.module.runtime.RuntimeConfigDelivery;

import com.dpis.module.ui.DialogWindowSizer;
import com.dpis.module.ui.compose.ComposeConfirmDialog;
import com.dpis.module.ui.compose.LanguageDialogOption;
import com.dpis.module.ui.compose.SettingsComposeDialogs;
import com.dpis.module.ui.compose.FontDebugComposeSheet;

import com.dpis.module.settings.AppLocaleManager;
import com.dpis.module.settings.AppUiScaleManager;
import com.dpis.module.settings.InterfaceScaleStore;
import com.dpis.module.settings.LauncherIconVisibilityStore;
import com.dpis.module.settings.SafeCacheCleaner;
import com.dpis.module.settings.SystemHookState;
import com.dpis.module.settings.SystemHooksToggleController;
import com.dpis.module.settings.ExperimentalSettingsActivity;
import com.dpis.module.about.AboutActivity;
import com.dpis.module.home.DonateActivity;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.dpis.module.backup.ConfigBackupCodec;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.textview.MaterialTextView;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.github.libxposed.service.XposedService;

final class SystemServerSettingsPageController implements DpisApplication.ServiceStateListener {
    private static final long STATS_REFRESH_INTERVAL_MS = 500L;
    private static final String SYSTEM_SCOPE_MODERN = "system";
    private static final int REQUEST_EXPORT_CONFIG_BACKUP = 1001;
    private static final int REQUEST_IMPORT_CONFIG_BACKUP = 1002;
    private static final long CLEAR_CACHE_MIN_DISABLED_MS = 300L;

    private final LocalizedActivity activity;
    private final View root;
    private final LauncherIconVisibilityStore launcherIconVisibilityStore;
    private final InterfaceScaleStore interfaceScaleStore;
    private final SettingsPresentationController presentationController;
    private DpisConfigStore store;
    private MaterialSwitch hooksEnabledSwitch;
    private MaterialSwitch safeModeSwitch;
    private MaterialSwitch globalLogSwitch;
    private MaterialSwitch hideLauncherIconSwitch;
    private View primarySwitchCard;
    private View languageEntryRow;
    private View clearCacheEntryRow;
    private View fontDebugEntryRow;
    private View experimentalSettingsEntryRow;
    private View fontLibraryEntryRow;
    private View backupConfigEntryRow;
    private View interfaceScaleRow;
    private Slider interfaceScaleSlider;
    private MaterialTextView interfaceScaleValueView;
    private int lastInterfaceScaleFeedbackPercent = AppUiScaleManager.DEFAULT_SCALE_PERCENT;
    private boolean suppressInterfaceScaleSliderChange;
    private volatile boolean clearCacheInProgress;
    private String lastCacheUsage = "0 B";
    private SharedPreferences statsPreferences;
    private int selectedMode = FontDebugStatsStore.MODE_CHAIN;
    private int selectedWindow = FontDebugStatsStore.WINDOW_ALL;

    private FontDebugComposeSheet.Handle fontDebugDialog;
    private SystemHooksToggleController hooksToggleController;

    private final Handler statsHandler = new Handler(Looper.getMainLooper());
    private final Runnable statsRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            refreshStatsPanel();
            statsHandler.postDelayed(this, STATS_REFRESH_INTERVAL_MS);
        }
    };

    SystemServerSettingsPageController(LocalizedActivity activity, View root) {
        this.activity = activity;
        this.root = root;
        this.launcherIconVisibilityStore = new LauncherIconVisibilityStore(activity);
        this.interfaceScaleStore = new InterfaceScaleStore(activity);
        this.presentationController = new SettingsPresentationController(
                new SettingsPresentationController.Port() {
                    @Override
                    public SettingsUiState snapshot() {
                        return presentationState();
                    }

                    @Override
                    public void setSafeModeEnabled(boolean enabled) {
                        if (enabled) {
                            onSafeModeChanged(null, true);
                        } else {
                            showDisableSafeModeConfirmationDialog();
                        }
                    }

                    @Override
                    public void setGlobalLogEnabled(boolean enabled) {
                        onGlobalLogChanged(null, enabled);
                    }

                    @Override
                    public void setLauncherIconHidden(boolean hidden) {
                        if (hidden) {
                            showHideLauncherIconConfirmationDialog();
                        } else {
                            onHideLauncherIconChanged(null, false);
                        }
                    }

                    @Override
                    public void refresh() {
                        if (root == null) {
                            refreshComposeStoreState();
                        } else {
                            refreshStoreState(false);
                        }
                        publishPresentationState();
                    }
                });
    }

    /** The sole state/action boundary for a future Compose Settings workspace. */
    SettingsPresentationController presentationController() { return presentationController; }

    SettingsUiState presentationState() {
        boolean available = store != null;
        return new SettingsUiState(available,
                available && store.isSystemServerHooksEnabled(),
                available && store.isSystemServerSafeModeEnabled(),
                available && store.isGlobalLogEnabled(),
                launcherIconVisibilityStore.isHidden(), interfaceScaleStore.getPercent(),
                clearCacheInProgress, lastCacheUsage,
                getString(AppLocaleManager.selectedLabelResId(activity)));
    }

    private void publishPresentationState() {
        presentationController.publishState();
    }

    void bind() {
        applyInsets();

        primarySwitchCard = findViewById(R.id.settings_primary_switch_card);
        primarySwitchCard.setVisibility(View.GONE);
        hooksEnabledSwitch = bindSwitchRow(
                R.id.row_system_hooks,
                R.drawable.ic_android_24,
                R.string.system_hooks_enabled_label,
                R.string.system_hooks_enabled_hint);
        applySystemHooksRowVisibility();
        safeModeSwitch = bindSwitchRow(
                R.id.row_safe_mode,
                R.drawable.ic_shield_24,
                R.string.system_safe_mode_label,
                R.string.system_safe_mode_hint);
        globalLogSwitch = bindSwitchRow(
                R.id.row_global_log,
                R.drawable.ic_view_kanban_24,
                R.string.global_log_enabled_label,
                R.string.global_log_enabled_hint);
        fontDebugEntryRow = bindEntryRow(
                R.id.row_font_debug_overlay,
                R.drawable.ic_bug_report_24,
                R.string.font_debug_overlay_label,
                R.string.font_debug_entry_hint,
                this::showFontDebugDialog);
        experimentalSettingsEntryRow = bindEntryRow(
                R.id.row_experimental_settings,
                R.drawable.ic_experiment_24,
                R.string.settings_experimental_title,
                R.string.settings_experimental_hint,
                v -> startActivity(new Intent(activity, ExperimentalSettingsActivity.class)));
        fontLibraryEntryRow = bindEntryRow(
                R.id.row_font_library,
                R.drawable.ic_upload_file_24,
                R.string.settings_font_library_label,
                R.string.settings_font_library_hint,
                v -> startActivity(new Intent(activity, FontLibraryActivity.class)));
        bindInterfaceScaleRow();
        backupConfigEntryRow = bindEntryRow(
                R.id.row_config_backup,
                R.drawable.ic_upload_file_24,
                R.string.settings_config_backup_label,
                R.string.settings_config_backup_hint,
                this::showConfigBackupDialog);
        bindLanguageRow();
        clearCacheEntryRow = bindEntryRow(
                R.id.row_clear_cache,
                R.drawable.ic_mop_24,
                R.string.settings_clear_cache_label,
                R.string.settings_clear_cache_size,
                this::clearCache);
        setCacheEntrySubtitle("0 B");
        updateCacheEntrySubtitle();
        bindEntryRow(
                R.id.row_about,
                R.drawable.ic_info_24,
                R.string.settings_about_label,
                R.string.settings_about_hint,
                v -> startActivity(new Intent(activity, AboutActivity.class)));
        bindEntryRow(
                R.id.row_donate,
                R.drawable.ic_volunteer_24,
                R.string.settings_donate_label,
                R.string.settings_donate_hint,
                v -> startActivity(DonateActivity.createIntent(activity)));
        hideLauncherIconSwitch = bindSwitchRow(
                R.id.row_hide_launcher_icon,
                R.drawable.ic_hide_image_24,
                R.string.settings_hide_launcher_icon_label,
                R.string.settings_hide_launcher_icon_hint);

        statsPreferences = FontDebugStatsStore.getPreferences(activity);
        hooksEnabledSwitch.setOnCheckedChangeListener(this::onHooksEnabledChanged);
        safeModeSwitch.setOnCheckedChangeListener(this::onSafeModeChanged);
        globalLogSwitch.setOnCheckedChangeListener(this::onGlobalLogChanged);
        hideLauncherIconSwitch.setOnCheckedChangeListener(this::onHideLauncherIconChanged);
        refreshStoreState(true);
        publishPresentationState();
    }

    /** Initializes the same Java-owned workflows when Settings is Compose-native. */
    void initializeComposePresentation() {
        refreshComposeStoreState();
        statsPreferences = FontDebugStatsStore.getPreferences(activity);
        updateCacheEntrySubtitle();
        publishPresentationState();
    }

    void addPresentationListener(SettingsPresentationController.Listener listener) {
        presentationController.addListener(listener);
    }

    void removePresentationListener(SettingsPresentationController.Listener listener) {
        presentationController.removeListener(listener);
    }

    void setHooksEnabledFromPresentation(boolean enabled) { onHooksEnabledChanged(null, enabled); }
    void showFontDebugFromPresentation() { showFontDebugDialog(null); }
    void showExperimentalSettingsFromPresentation() { startActivity(new Intent(activity, ExperimentalSettingsActivity.class)); }
    void showFontLibraryFromPresentation() { startActivity(new Intent(activity, FontLibraryActivity.class)); }
    void showLanguageFromPresentation() { showLanguageDialog(null); }
    void saveInterfaceScaleFromPresentation(int percent) { saveInterfaceScalePercent(percent); }
    void showInterfaceScaleFromPresentation() { showInterfaceScaleDialog(); }
    void showConfigBackupFromPresentation() { showConfigBackupDialog(null); }
    void clearCacheFromPresentation() { clearCache(null); }
    void showAboutFromPresentation() { startActivity(new Intent(activity, AboutActivity.class)); }
    void showDonateFromPresentation() { startActivity(DonateActivity.createIntent(activity)); }

    void onStart() {
        DpisApplication.addServiceStateListener(this, true);
        statsHandler.post(statsRefreshRunnable);
    }

    void onResume() {
        syncHooksSwitchWithScope();
        syncLauncherIconSwitch();
        if (store != null && store.isFontDebugOverlayEnabled() && canDrawOverlays()) {
            startFontDebugOverlayService();
        }
        publishPresentationState();
    }

    void onStop() {
        DpisApplication.removeServiceStateListener(this);
        statsHandler.removeCallbacks(statsRefreshRunnable);
        dismissFontDebugDialog();
    }

    @Override
    public void onServiceStateChanged() {
        runOnUiThread(() -> {
            if (root == null) {
                refreshComposeStoreState();
            } else {
                refreshStoreState(false);
            }
            publishPresentationState();
        });
    }

    @SuppressWarnings("deprecation")
    void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != android.app.Activity.RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        if (requestCode == REQUEST_EXPORT_CONFIG_BACKUP) {
            exportConfigBackup(uri);
            publishPresentationState();
            return;
        }
        if (requestCode == REQUEST_IMPORT_CONFIG_BACKUP) {
            showImportBackupConfirmDialog(uri);
            publishPresentationState();
            return;
        }
    }

    private void applyInsets() {
        View toolbar = findViewById(R.id.settings_toolbar);
        if (root == null || toolbar == null) {
            return;
        }
        final int baseRootPaddingLeft = root.getPaddingLeft();
        final int baseRootPaddingRight = root.getPaddingRight();
        final int baseTopPadding = toolbar.getPaddingTop();
        final int baseToolbarPaddingLeft = toolbar.getPaddingLeft();
        final int baseToolbarPaddingRight = toolbar.getPaddingRight();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets safeDrawing = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(baseRootPaddingLeft + safeDrawing.left, view.getPaddingTop(),
                    baseRootPaddingRight + safeDrawing.right, view.getPaddingBottom());
            toolbar.setPadding(baseToolbarPaddingLeft, baseTopPadding + safeDrawing.top,
                    baseToolbarPaddingRight, toolbar.getPaddingBottom());
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private <T extends View> T findViewById(int id) {
        if (id == android.R.id.content) {
            return activity.findViewById(id);
        }
        return root.findViewById(id);
    }

    private android.content.res.Resources getResources() {
        return activity.getResources();
    }

    private String getString(int resId) {
        return activity.getString(resId);
    }

    private String getString(int resId, Object... formatArgs) {
        return activity.getString(resId, formatArgs);
    }

    private <T> T getSystemService(Class<T> serviceClass) {
        return activity.getSystemService(serviceClass);
    }

    private android.content.Context getApplicationContext() {
        return activity.getApplicationContext();
    }

    private ContentResolver getContentResolver() {
        return activity.getContentResolver();
    }

    private String getPackageName() {
        return activity.getPackageName();
    }

    private PackageManager getPackageManager() {
        return activity.getPackageManager();
    }

    private void startActivity(Intent intent) {
        activity.startActivity(intent);
    }

    @SuppressWarnings("deprecation")
    private void startActivityForResult(Intent intent, int requestCode) {
        activity.startActivityForResult(intent, requestCode);
    }

    private void startService(Intent intent) {
        activity.startService(intent);
    }

    private void stopService(Intent intent) {
        activity.stopService(intent);
    }

    private void runOnUiThread(Runnable action) {
        activity.runOnUiThread(action);
    }

    private boolean isFinishing() {
        return activity.isFinishing();
    }

    private boolean isDestroyed() {
        return activity.isDestroyed();
    }

    private void recreate() {
        activity.recreate();
    }

    private void finishAffinity() {
        activity.finishAffinity();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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

    private void bindLanguageRow() {
        languageEntryRow = findViewById(R.id.row_language);
        ImageView iconView = languageEntryRow.findViewById(R.id.setting_icon);
        MaterialTextView titleView = languageEntryRow.findViewById(R.id.setting_title);
        iconView.setImageResource(R.drawable.ic_language_24);
        titleView.setText(R.string.settings_language_label);
        updateLanguageEntrySubtitle();
        languageEntryRow.setOnClickListener(this::showLanguageDialog);
    }

    private void bindInterfaceScaleRow() {
        interfaceScaleRow = findViewById(R.id.row_interface_scale);
        ImageView iconView = interfaceScaleRow.findViewById(R.id.setting_icon);
        MaterialTextView titleView = interfaceScaleRow.findViewById(R.id.setting_title);
        MaterialTextView subtitleView = interfaceScaleRow.findViewById(R.id.setting_subtitle);
        interfaceScaleValueView = interfaceScaleRow.findViewById(R.id.setting_value);
        interfaceScaleSlider = interfaceScaleRow.findViewById(R.id.setting_slider);

        iconView.setImageResource(R.drawable.ic_fit_width_24);
        titleView.setText(R.string.settings_interface_scale_label);
        subtitleView.setText(R.string.settings_interface_scale_hint);
        interfaceScaleSlider.setValueFrom(AppUiScaleManager.MIN_SCALE_PERCENT);
        interfaceScaleSlider.setValueTo(AppUiScaleManager.MAX_SCALE_PERCENT);
        interfaceScaleSlider.setStepSize(10f);
        interfaceScaleRow.setOnClickListener(v -> showInterfaceScaleDialog());
        interfaceScaleSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser && !suppressInterfaceScaleSliderChange) {
                int percent = normalizeInterfaceScaleSliderPercent(Math.round(value));
                updateInterfaceScaleValue(percent);
                performInterfaceScaleStepFeedback(percent);
            }
        });
        interfaceScaleSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(Slider slider) {
                int percent = AppUiScaleManager.normalizeScalePercent(Math.round(slider.getValue()));
                lastInterfaceScaleFeedbackPercent = percent;
                updateInterfaceScaleValue(percent);
            }

            @Override
            public void onStopTrackingTouch(Slider slider) {
                saveInterfaceScalePercent(Math.round(slider.getValue()));
            }
        });
        setInterfaceScalePercentSilently(AppUiScaleManager.getEffectiveScalePercent(activity));
    }

    private void setInterfaceScalePercentSilently(int percent) {
        if (interfaceScaleSlider == null) {
            return;
        }
        int normalized = AppUiScaleManager.normalizeScalePercent(percent);
        int sliderPercent = nearestInterfaceScaleSliderPercent(normalized);
        suppressInterfaceScaleSliderChange = true;
        interfaceScaleSlider.setValue(sliderPercent);
        suppressInterfaceScaleSliderChange = false;
        lastInterfaceScaleFeedbackPercent = sliderPercent;
        updateInterfaceScaleValue(normalized);
    }

    private void updateInterfaceScaleValue(int percent) {
        if (interfaceScaleValueView != null) {
            interfaceScaleValueView.setText(getString(
                    R.string.settings_interface_scale_value,
                    AppUiScaleManager.normalizeScalePercent(percent)));
        }
    }

    private void saveInterfaceScalePercent(int percent) {
        int normalized = AppUiScaleManager.normalizeScalePercent(percent);
        if (normalized == interfaceScaleStore.getPercent()
                && interfaceScaleStore.hasExplicitPercent()) {
            setInterfaceScalePercentSilently(normalized);
            return;
        }
        if (!interfaceScaleStore.setPercent(normalized)) {
            setInterfaceScalePercentSilently(interfaceScaleStore.getPercent());
            showToast(R.string.system_settings_save_failed);
            return;
        }
        setInterfaceScalePercentSilently(normalized);
        publishPresentationState();
        recreate();
    }

    private void showInterfaceScaleDialog() {
        SettingsComposeDialogs.showInterfaceScale(
                activity,
                AppUiScaleManager.getEffectiveScalePercent(activity),
                AppUiScaleManager.MIN_SCALE_PERCENT,
                AppUiScaleManager.MAX_SCALE_PERCENT,
                this::saveInterfaceScalePercent);
    }

    private void performInterfaceScaleStepFeedback(int percent) {
        if (percent == lastInterfaceScaleFeedbackPercent || interfaceScaleSlider == null) {
            return;
        }
        lastInterfaceScaleFeedbackPercent = percent;
        interfaceScaleSlider.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
    }

    private int normalizeInterfaceScaleSliderPercent(int percent) {
        return nearestInterfaceScaleSliderPercent(
                AppUiScaleManager.normalizeScalePercent(percent));
    }

    private int nearestInterfaceScaleSliderPercent(int percent) {
        int normalized = AppUiScaleManager.normalizeScalePercent(percent);
        int min = AppUiScaleManager.MIN_SCALE_PERCENT;
        int rounded = Math.round((normalized - min) / 10f) * 10 + min;
        return AppUiScaleManager.normalizeScalePercent(rounded);
    }

    private void showLanguageDialog(View anchor) {
        List<AppLocaleManager.LanguageOption> languageOptions = AppLocaleManager.supportedLanguages();
        List<LanguageDialogOption> options = new ArrayList<>(languageOptions.size());
        for (AppLocaleManager.LanguageOption option : languageOptions) {
            options.add(new LanguageDialogOption(option.tag, getString(option.labelResId)));
        }
        SettingsComposeDialogs.showLanguage(activity, options,
                AppLocaleManager.getLanguageTag(activity), selectedTag -> {
        String previousTag = AppLocaleManager.getLanguageTag(activity);
        if (!AppLocaleManager.setLanguageTag(activity, selectedTag)) {
            showToast(R.string.system_settings_save_failed);
            return;
        }
        updateLanguageEntrySubtitle();
        if (!selectedTag.equals(previousTag)) {
            recreate();
        }
        });
    }

    private void updateLanguageEntrySubtitle() {
        if (languageEntryRow == null) {
            return;
        }
        MaterialTextView subtitleView = languageEntryRow.findViewById(R.id.setting_subtitle);
        subtitleView.setText(AppLocaleManager.selectedLabelResId(activity));
    }

    private void updateCacheEntrySubtitle() {
        android.content.Context appContext = getApplicationContext();
        new Thread(() -> {
            String usage = SafeCacheCleaner.formatCacheUsage(appContext);
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed() && !clearCacheInProgress) {
                    setCacheEntrySubtitle(usage);
                    publishPresentationState();
                }
            });
        }, "dpis-cache-size").start();
    }

    private void clearCache(View anchor) {
        if (clearCacheInProgress) {
            return;
        }
        clearCacheInProgress = true;
        setRowEnabled(clearCacheEntryRow, false);
        setCacheEntrySubtitle(getString(R.string.settings_clear_cache_cleaning));
        publishPresentationState();
        android.content.Context appContext = getApplicationContext();
        new Thread(() -> {
            long startedAt = System.currentTimeMillis();
            boolean legacyCacheStillNeedsManualDelete = false;
            boolean failed = false;
            try {
                SafeCacheCleaner.clearAll(appContext);
                legacyCacheStillNeedsManualDelete = SafeCacheCleaner.hasLegacyPublicFontDebugCache();
            } catch (RuntimeException exception) {
                failed = true;
                DpisLog.e("clear cache failed", exception);
            } finally {
                sleepUntilMinDisabledElapsed(startedAt);
                boolean finalLegacyCacheStillNeedsManualDelete = legacyCacheStillNeedsManualDelete;
                boolean finalFailed = failed;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    clearCacheInProgress = false;
                    setRowEnabled(clearCacheEntryRow, true);
                    updateCacheEntrySubtitle();
                    publishPresentationState();
                    if (finalLegacyCacheStillNeedsManualDelete) {
                        showToast(R.string.settings_clear_cache_legacy_public_file_blocked);
                        return;
                    }
                    showToast(finalFailed
                            ? R.string.system_settings_save_failed
                            : R.string.settings_clear_cache_done);
                });
            }
        }, "dpis-clear-cache").start();
    }

    private static void sleepUntilMinDisabledElapsed(long startedAt) {
        long elapsed = System.currentTimeMillis() - startedAt;
        long remaining = CLEAR_CACHE_MIN_DISABLED_MS - elapsed;
        if (remaining <= 0L) {
            return;
        }
        try {
            Thread.sleep(remaining);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void setCacheEntrySubtitle(String usage) {
        lastCacheUsage = usage != null ? usage : "";
        if (clearCacheEntryRow == null) {
            return;
        }
        MaterialTextView subtitleView = clearCacheEntryRow.findViewById(R.id.setting_subtitle);
        subtitleView.setText(getString(R.string.settings_clear_cache_size, usage));
    }

    private void showConfigBackupDialog(View anchor) {
        if (store == null) {
            showToast(R.string.status_save_requires_init);
            return;
        }
        SettingsComposeDialogs.showBackupActions(
                activity, this::launchExportBackupPicker, this::launchImportBackupPicker);
    }

    @SuppressWarnings("deprecation")
    private void launchExportBackupPicker() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/json")
                .putExtra(Intent.EXTRA_TITLE, buildBackupFileName());
        try {
            startActivityForResult(intent, REQUEST_EXPORT_CONFIG_BACKUP);
        } catch (ActivityNotFoundException error) {
            showToast(R.string.config_backup_picker_failed);
        }
    }

    @SuppressWarnings("deprecation")
    private void launchImportBackupPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*")
                .putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                        "application/json",
                        "text/plain"
                });
        try {
            startActivityForResult(intent, REQUEST_IMPORT_CONFIG_BACKUP);
        } catch (ActivityNotFoundException error) {
            showToast(R.string.config_backup_picker_failed);
        }
    }

    private void showImportBackupConfirmDialog(Uri uri) {
        ComposeConfirmDialog.show(
                activity,
                getString(R.string.config_backup_import_confirm_title),
                getString(R.string.config_backup_import_confirm_message),
                () -> { importConfigBackup(uri); publishPresentationState(); },
                this::publishPresentationState);
    }

    private void exportConfigBackup(Uri uri) {
        DpisConfigStore localStore = store;
        if (localStore == null) {
            showToast(R.string.status_save_requires_init);
            return;
        }
        new Thread(() -> {
            Map<String, Object> entries = localStore.snapshotBackup();
            boolean success = false;
            try {
                String payload = ConfigBackupCodec.encode(entries);
                writeUtf8(uri, payload);
                success = true;
            } catch (IOException | JSONException | RuntimeException ignored) {
                success = false;
            }
            boolean finalSuccess = success;
            runOnUiThread(() -> {
                if (finalSuccess) {
                    showToast(R.string.config_backup_export_success);
                    publishPresentationState();
                    return;
                }
                showToast(R.string.config_backup_export_failed);
                publishPresentationState();
            });
        }, "dpis-config-backup-export").start();
    }

    private void importConfigBackup(Uri uri) {
        DpisConfigStore localStore = store;
        if (localStore == null) {
            showToast(R.string.status_save_requires_init);
            return;
        }
        new Thread(() -> {
            Map<String, Object> entries;
            try {
                String payload = readUtf8(uri);
                entries = ConfigBackupCodec.decode(payload);
            } catch (IOException | JSONException | IllegalArgumentException ignored) {
                runOnUiThread(() -> { showToast(R.string.config_backup_import_invalid); publishPresentationState(); });
                return;
            }
            if (!localStore.replaceBackup(entries)) {
                runOnUiThread(() -> { showToast(R.string.config_backup_import_failed); publishPresentationState(); });
                return;
            }
            runOnUiThread(() -> {
                showToast(R.string.config_backup_import_success);
                publishPresentationState();
                relaunchDpisTask();
            });
        }, "dpis-config-backup-import").start();
    }

    private void relaunchDpisTask() {
        RuntimeConfigDelivery.publishLocalSnapshotAfterSave();
        Intent intent = new Intent(activity, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finishAffinity();
    }

    private void applyRestoredStoreState() {
        if (store == null) {
            return;
        }
        selectedMode = store.getFontDebugSelectedMode();
        selectedWindow = store.getFontDebugSelectedWindow();

        setCheckedSilently(safeModeSwitch,
                store.isSystemServerSafeModeEnabled(),
                this::onSafeModeChanged);
        setCheckedSilently(globalLogSwitch,
                store.isGlobalLogEnabled(),
                this::onGlobalLogChanged);
        DpisLog.setLoggingEnabled(store.isGlobalLogEnabled());
        setInterfaceScalePercentSilently(AppUiScaleManager.getEffectiveScalePercent(activity));

        applyLauncherIconVisibilityFromStore();
        syncHooksSwitchWithScope();

        if (store.isFontDebugOverlayEnabled() && canDrawOverlays()) {
            startFontDebugOverlayService();
        } else if (!store.isFontDebugOverlayEnabled()) {
            stopService(new Intent(activity, FontDebugOverlayService.class));
        }
        updateDialogButtons();
        refreshStatsPanel();
    }

    private void refreshStoreState(boolean showInitToast) {
        store = DpisApplication.getConfigStore();
        if (store == null) {
            applyUnavailableStoreState(showInitToast);
            return;
        }
        applyAvailableStoreState();
    }

    private void refreshComposeStoreState() {
        store = DpisApplication.getConfigStore();
        if (store == null) {
            hooksToggleController = null;
            return;
        }
        selectedMode = store.getFontDebugSelectedMode();
        selectedWindow = store.getFontDebugSelectedWindow();
        hooksToggleController = new SystemHooksToggleController(
                store,
                new ActivitySystemScopeGateway(),
                new ActivitySystemHooksToggleView(),
                this::publishPresentationState);
    }

    private void applyAvailableStoreState() {
        hooksEnabledSwitch.setEnabled(true);
        safeModeSwitch.setEnabled(true);
        globalLogSwitch.setEnabled(true);
        hideLauncherIconSwitch.setEnabled(true);
        interfaceScaleSlider.setEnabled(true);
        setRowEnabled(fontDebugEntryRow, true);
        setRowEnabled(experimentalSettingsEntryRow, true);
        setRowEnabled(fontLibraryEntryRow, true);
        setRowEnabled(backupConfigEntryRow, true);
        setRowEnabled(interfaceScaleRow, true);
        hooksToggleController = new SystemHooksToggleController(
                store,
                new ActivitySystemScopeGateway(),
                new ActivitySystemHooksToggleView(),
                this::publishPresentationState);
        applyRestoredStoreState();
        setPrimarySwitchRowsVisible(true);
    }

    private void applyUnavailableStoreState(boolean showInitToast) {
        hooksToggleController = null;
        setPrimarySwitchRowsVisible(true);
        hooksEnabledSwitch.setEnabled(false);
        safeModeSwitch.setEnabled(false);
        globalLogSwitch.setEnabled(false);
        hideLauncherIconSwitch.setEnabled(false);
        interfaceScaleSlider.setEnabled(false);
        setRowEnabled(fontDebugEntryRow, false);
        setRowEnabled(experimentalSettingsEntryRow, false);
        setRowEnabled(fontLibraryEntryRow, false);
        setRowEnabled(backupConfigEntryRow, false);
        setRowEnabled(interfaceScaleRow, false);
        setRowEnabled(languageEntryRow, false);
        if (showInitToast) {
            showToast(R.string.status_save_requires_init);
        }
    }

    private void setPrimarySwitchRowsVisible(boolean visible) {
        if (primarySwitchCard == null) {
            return;
        }
        primarySwitchCard.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void applyLauncherIconVisibilityFromStore() {
        if (hideLauncherIconSwitch == null) {
            return;
        }
        boolean storedHidden = launcherIconVisibilityStore.isHidden();
        boolean actualHidden = resolveLauncherIconHiddenState(storedHidden);
        if (actualHidden != storedHidden) {
            launcherIconVisibilityStore.setHidden(actualHidden);
        }
        setCheckedSilently(hideLauncherIconSwitch, actualHidden, this::onHideLauncherIconChanged);
    }

    private String buildBackupFileName() {
        return String.format(
                Locale.US,
                "dpis-backup-%1$tY%1$tm%1$td-%1$tH%1$tM%1$tS.json",
                new Date());
    }

    private void writeUtf8(Uri uri, String content) throws IOException {
        ContentResolver resolver = getContentResolver();
        try (OutputStream output = resolver.openOutputStream(uri, "wt")) {
            if (output == null) {
                throw new IOException("Unable to open backup output stream");
            }
            try (OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8)) {
                writer.write(content);
            }
        }
    }

    private String readUtf8(Uri uri) throws IOException {
        ContentResolver resolver = getContentResolver();
        try (InputStream input = resolver.openInputStream(uri)) {
            if (input == null) {
                throw new IOException("Unable to open backup input stream");
            }
            StringBuilder builder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8))) {
                char[] buffer = new char[1024];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    builder.append(buffer, 0, read);
                }
            }
            return builder.toString();
        }
    }

    private void showFontDebugDialog(View anchor) {
        if (store == null) {
            return;
        }
        dismissFontDebugDialog();
        fontDebugDialog = FontDebugComposeSheet.show(activity,
                () -> {
            selectedMode = selectedMode == FontDebugStatsStore.MODE_CHAIN
                    ? FontDebugStatsStore.MODE_CHAIN_VIEW
                    : FontDebugStatsStore.MODE_CHAIN;
            store.setFontDebugSelectedMode(selectedMode);
            refreshStatsPanel();
            publishPresentationState();
        }, () -> {
            if (selectedWindow == FontDebugStatsStore.WINDOW_5S) {
                selectedWindow = FontDebugStatsStore.WINDOW_30S;
            } else if (selectedWindow == FontDebugStatsStore.WINDOW_30S) {
                selectedWindow = FontDebugStatsStore.WINDOW_ALL;
            } else {
                selectedWindow = FontDebugStatsStore.WINDOW_5S;
            }
            store.setFontDebugSelectedWindow(selectedWindow);
            refreshStatsPanel();
            publishPresentationState();
        }, () -> {
            boolean currentEnabled = store.isFontDebugOverlayEnabled();
            boolean requestedEnabled = !currentEnabled;
            if (requestedEnabled && !canDrawOverlays()) {
                requestOverlayPermission();
                showToast(R.string.font_debug_overlay_permission_needed);
                updateDialogButtons();
                publishPresentationState();
                return;
            }
            if (!store.setFontDebugOverlayEnabled(requestedEnabled)) {
                showToast(R.string.system_settings_save_failed);
                updateDialogButtons();
                publishPresentationState();
                return;
            }
            RuntimeDebugPropertySyncer.publishAsync(
                    store.isGlobalLogEnabled(),
                    requestedEnabled);
            if (requestedEnabled) {
                startFontDebugOverlayService();
            } else {
                stopService(new Intent(activity, FontDebugOverlayService.class));
            }
            updateDialogButtons();
            publishPresentationState();
        }, () -> {
            clearDebugStatsData();
            refreshStatsPanel();
            showToast(R.string.font_debug_clear_done);
            publishPresentationState();
        }, () -> {
            fontDebugDialog = null;
            publishPresentationState();
        });
        refreshStatsPanel();
    }

    private void dismissFontDebugDialog() {
        if (fontDebugDialog != null) {
            fontDebugDialog.dismiss();
        }
    }

    private void refreshStatsPanel() {
        FontDebugComposeSheet.Handle handle = fontDebugDialog;
        if (statsPreferences == null || handle == null) {
            return;
        }
        String key = FontDebugStatsSchema.statsKeyFor(selectedMode, selectedWindow);
        String statsText = statsPreferences.getString(key, null);
        long updatedAt = statsPreferences.getLong(FontDebugStatsStore.KEY_UPDATED_AT, 0L);
        int eventTotal = statsPreferences.getInt(FontDebugStatsStore.KEY_EVENT_TOTAL, 0);

        String contentText;
        if (statsText == null || statsText.trim().isEmpty()) {
            FontDebugDataDiagnostics.NoDataReason reason = FontDebugDataDiagnostics.resolveNoDataReason(store,
                    statsPreferences);
            if (reason == FontDebugDataDiagnostics.NoDataReason.NONE) {
                contentText = getString(R.string.font_debug_not_updated);
            } else {
                contentText = getString(
                        R.string.font_debug_no_data_with_reason,
                        reasonTitleText(reason),
                        reasonHintText(reason));
            }
        } else {
            contentText = statsText;
        }
        String updatedText = getString(R.string.font_debug_not_updated);
        if (updatedAt > 0L) {
            DateFormat format = DateFormat.getTimeInstance(DateFormat.MEDIUM, Locale.getDefault());
            updatedText = getString(R.string.font_debug_last_updated,
                    format.format(new Date(updatedAt)), eventTotal);
        }
        int windowLabelRes = switch (selectedWindow) {
            case FontDebugStatsStore.WINDOW_5S -> R.string.font_debug_window_button_5s;
            case FontDebugStatsStore.WINDOW_30S -> R.string.font_debug_window_button_30s;
            default -> R.string.font_debug_window_button_all;
        };
        boolean overlayEnabled = store.isFontDebugOverlayEnabled();
        handle.update(
                getString(selectedMode == FontDebugStatsStore.MODE_CHAIN
                        ? R.string.font_debug_mode_button_chain
                        : R.string.font_debug_mode_button_chain_view),
                getString(windowLabelRes), updatedText, contentText,
                getString(overlayEnabled
                        ? R.string.font_debug_overlay_disable_button
                        : R.string.font_debug_overlay_enable_button),
                overlayEnabled);
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
        FontDebugStatsStore.clearStats(statsPreferences);
    }

    private void updateDialogButtons() {
        refreshStatsPanel();
    }

    private void onHooksEnabledChanged(CompoundButton buttonView, boolean isChecked) {
        if (!BuildConfig.DEBUG) {
            return;
        }
        if (hooksToggleController == null) {
            return;
        }
        hooksToggleController.onUserToggle(isChecked);
        publishPresentationState();
    }

    private void applySystemHooksRowVisibility() {
        View row = findViewById(R.id.row_system_hooks);
        if (row == null) {
            return;
        }
        row.setVisibility(BuildConfig.DEBUG ? View.VISIBLE : View.GONE);
    }

    private void onSafeModeChanged(CompoundButton buttonView, boolean isChecked) {
        if (store == null) {
            return;
        }
        if (!isChecked) {
            showDisableSafeModeConfirmationDialog();
            return;
        }
        if (!store.setSystemServerSafeModeEnabled(true)) {
            setCheckedSilently(safeModeSwitch, false, this::onSafeModeChanged);
            showToast(R.string.system_settings_save_failed);
            return;
        }
        RuntimeConfigDelivery.publishLocalSnapshotAfterSave();
        publishPresentationState();
    }

    private void showDisableSafeModeConfirmationDialog() {
        ComposeConfirmDialog.show(
                activity,
                activity.getString(R.string.system_safe_mode_disable_confirm_title),
                activity.getString(R.string.system_safe_mode_disable_confirm_message),
                () -> {
                    if (!store.setSystemServerSafeModeEnabled(false)) {
                        setCheckedSilently(safeModeSwitch, true, this::onSafeModeChanged);
                        showToast(R.string.system_settings_save_failed);
                        publishPresentationState();
                        return;
                    }
                    RuntimeConfigDelivery.publishLocalSnapshotAfterSave();
                    publishPresentationState();
                },
                () -> {
                    setCheckedSilently(safeModeSwitch, true, this::onSafeModeChanged);
                    publishPresentationState();
                });
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
        RuntimeDebugPropertySyncer.publishAsync(
                isChecked,
                store.isFontDebugOverlayEnabled());
        RuntimeConfigDelivery.publishLocalSnapshotAfterSave();
        publishPresentationState();
    }

    private void onHideLauncherIconChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            showHideLauncherIconConfirmationDialog();
            return;
        }
        if (!persistLauncherIconState(false)) {
            setCheckedSilently(hideLauncherIconSwitch, true, this::onHideLauncherIconChanged);
        }
        publishPresentationState();
    }

    private void showHideLauncherIconConfirmationDialog() {
        ComposeConfirmDialog.show(
                activity,
                activity.getString(R.string.settings_hide_launcher_icon_confirm_title),
                activity.getString(R.string.settings_hide_launcher_icon_confirm_message),
                () -> {
                    if (!persistLauncherIconState(true)) {
                        setCheckedSilently(hideLauncherIconSwitch, false,
                                this::onHideLauncherIconChanged);
                    }
                    publishPresentationState();
                },
                () -> {
                    setCheckedSilently(hideLauncherIconSwitch, false,
                            this::onHideLauncherIconChanged);
                    publishPresentationState();
                });
    }

    private boolean canDrawOverlays() {
        return Settings.canDrawOverlays(activity);
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivity(intent);
        publishPresentationState();
    }

    private void startFontDebugOverlayService() {
        Intent serviceIntent = new Intent(activity, FontDebugOverlayService.class);
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
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
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
        if (!BuildConfig.DEBUG) {
            return;
        }
        if (hooksToggleController == null) {
            return;
        }
        hooksToggleController.syncFromStore();
    }

    private void syncLauncherIconSwitch() {
        if (hideLauncherIconSwitch == null) {
            return;
        }
        boolean storedHidden = launcherIconVisibilityStore.isHidden();
        boolean hidden = resolveLauncherIconHiddenState(storedHidden);
        if (hidden != storedHidden) {
            launcherIconVisibilityStore.setHidden(hidden);
        }
        setCheckedSilently(hideLauncherIconSwitch, hidden, this::onHideLauncherIconChanged);
    }

    private boolean persistLauncherIconState(boolean hidden) {
        if (!setLauncherAliasHidden(hidden)) {
            showToast(R.string.settings_hide_launcher_icon_apply_failed);
            return false;
        }
        if (launcherIconVisibilityStore.setHidden(hidden)) {
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
        int state;
        try {
            state = getPackageManager().getComponentEnabledSetting(getLauncherAliasComponentName());
        } catch (RuntimeException error) {
            return fallback;
        }
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
        return new ComponentName(activity, MainActivity.class.getName() + "Launcher");
    }

    private final class ActivitySystemHooksToggleView implements SystemHooksToggleController.View {
        @Override
        public void render(SystemHookState state) {
            if (hooksEnabledSwitch == null) {
                return;
            }
            setCheckedSilently(hooksEnabledSwitch, state.switchChecked,
                    SystemServerSettingsPageController.this::onHooksEnabledChanged);
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
