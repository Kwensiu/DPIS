package com.dpis.module;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.libxposed.service.XposedService;

public final class QuickConfigActivity extends LocalizedActivity {
    private static final String EXTRA_PACKAGE_NAME = "com.dpis.module.extra.QUICK_CONFIG_PACKAGE";
    private static final int REQUEST_SAVE_FEEDBACK_DIAGNOSTIC = 20024;
    private static final String SHARED_FEEDBACK_DIAGNOSTIC_DIRECTORY_NAME
            = "shared-feedback-diagnostics";

    private final AppConfigSaveHandler appConfigSaveHandler = new AppConfigSaveHandler();
    private final ProcessActionHandler processActionHandler
            = new ProcessActionHandler(this, this::syncRuntimePropertiesForTargetLaunch);
    private final SystemScopeCoordinator systemScopeCoordinator
            = new SystemScopeCoordinator(createSystemScopeHost());
    private final FeedbackDiagnosticAppLauncher feedbackDiagnosticAppLauncher
            = new FeedbackDiagnosticAppLauncher(this);
    private final FeedbackDiagnosticExportBuilder feedbackDiagnosticExportBuilder
            = new FeedbackDiagnosticExportBuilder(this);
    private final ExecutorService feedbackDiagnosticExportExecutor
            = Executors.newSingleThreadExecutor();
    private final AppConfigDialogBinder.Host appConfigDialogHost = createHost();
    private final FeedbackDiagnosticCoordinator feedbackDiagnosticCoordinator
            = new FeedbackDiagnosticCoordinator(createFeedbackDiagnosticHost());
    private View activeEditorRoot;
    private boolean activityResumed;
    private FeedbackDiagnosticCoordinator.Result pendingFeedbackDiagnosticResult;
    private FeedbackDiagnosticExportBuilder.DiagnosticPackage pendingFeedbackDiagnosticPackage;
    private AlertDialog activeFeedbackDiagnosticPackagingDialog;

    static Intent createIntent(Context context, String packageName) {
        Intent intent = new Intent(context, QuickConfigActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (packageName != null && !packageName.isBlank()) {
            intent.putExtra(EXTRA_PACKAGE_NAME, packageName);
        }
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout root = new FrameLayout(this);
        root.setId(android.R.id.content);
        root.setOnClickListener(view -> finish());
        setContentView(root);

        String packageName = getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
        if (packageName == null || packageName.isBlank()) {
            packageName = ForegroundPackageResolver.resolve(this);
        }
        AppListItem item = packageName != null ? createItem(packageName) : null;
        if (item == null) {
            Toast.makeText(this, R.string.quick_config_target_unavailable, Toast.LENGTH_SHORT)
                    .show();
            finish();
            return;
        }
        showPanel(root, item);
    }

    private void showPanel(FrameLayout root, AppListItem item) {
        View panel = LayoutInflater.from(this).inflate(
                R.layout.dialog_app_config,
                root,
                false);
        panel.setBackgroundResource(R.drawable.bg_quick_config_panel);
        panel.setClickable(true);
        panel.setFocusable(true);

        AppConfigDialogBinder binder = new AppConfigDialogBinder(this, appConfigDialogHost);
        binder.bind(
                panel,
                AppConfigPrefillPreview.applyIfEligible(
                        item,
                        getHookConfigStore(),
                        new GlobalPrefillStore(
                                getSharedPreferences(DpiConfigStore.GROUP, Context.MODE_PRIVATE))
                                .read()),
                isSystemHookEnabled());
        activeEditorRoot = panel;

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        root.addView(panel, params);
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityResumed = true;
        feedbackDiagnosticCoordinator.onDpisResumed();
        maybeShowPendingFeedbackDiagnosticResult();
    }

    @Override
    protected void onStop() {
        activityResumed = false;
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        dismissFeedbackDiagnosticPackagingDialog();
        feedbackDiagnosticCoordinator.shutdown();
        feedbackDiagnosticExportExecutor.shutdownNow();
        super.onDestroy();
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SAVE_FEEDBACK_DIAGNOSTIC
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {
            saveFeedbackDiagnosticZip(data.getData());
        }
    }

    private AppListItem createItem(String packageName) {
        try {
            PackageManager packageManager = getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            String label = packageManager.getApplicationLabel(applicationInfo).toString();
            Drawable icon = applicationInfo.loadIcon(packageManager);
            boolean systemApp = (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                    && (applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0;
            return InstalledAppCatalogCoordinator.createAppListItem(
                    getHookConfigStore(),
                    loadScopePackages(),
                    DpisApplication.getXposedService() != null,
                    label,
                    packageName,
                    systemApp,
                    HyperOsNativeAppDetector.isNativeProxyCandidate(applicationInfo),
                    true,
                    icon);
        } catch (PackageManager.NameNotFoundException | RuntimeException exception) {
            return null;
        }
    }

    private Set<String> loadScopePackages() {
        XposedService service = DpisApplication.getXposedService();
        if (service == null) {
            return Collections.emptySet();
        }
        try {
            return new HashSet<>(service.getScope());
        } catch (RuntimeException exception) {
            return Collections.emptySet();
        }
    }

    private AppConfigDialogBinder.Host createHost() {
        return new AppConfigDialogBinder.Host() {
            @Override
            public void toggleScope(
                    AppListItem item,
                    boolean currentlyInScope,
                    Runnable onTurnedInScope,
                    Runnable onTurnedOutScope
            ) {
                systemScopeCoordinator.toggleScope(
                        item.packageName,
                        item.label,
                        currentlyInScope,
                        onTurnedInScope,
                        onTurnedOutScope
                );
            }

            @Override
            public boolean requestScope(
                    AppListItem item,
                    Runnable onTurnedInScope,
                    Runnable onRequestFinished
            ) {
                return systemScopeCoordinator.requestScope(
                        item.packageName,
                        item.label,
                        onTurnedInScope,
                        onRequestFinished,
                        false
                );
            }

            @Override
            public void executeProcessAction(
                    AppListItem item,
                    AppConfigDialogBinder.ProcessAction action
            ) {
                executeDialogProcessAction(item, action);
            }

            @Override
            public void applyHyperOsNativeProxy(AppListItem item, Runnable onFinished) {
                executeHyperOsNativeProxyMount(item, true, onFinished);
            }

            @Override
            public void unmountHyperOsNativeProxy(AppListItem item, Runnable onFinished) {
                executeHyperOsNativeProxyMount(item, false, onFinished);
            }

            @Override
            public boolean setDpisEnabled(String packageName, boolean enabled) {
                DpiConfigStore store = getHookConfigStore();
                if (store == null || !store.setTargetDpisEnabled(packageName, enabled)) {
                    showToast(R.string.system_settings_save_failed);
                    return false;
                }
                if (!enabled) {
                    FontRuntimePropertySyncer.clearTargetAsync(packageName);
                    FontHookDomainPropertySyncer.clearTargetAsync(packageName);
                    ViewportPropertySyncer.clearTargetAsync(packageName);
                }
                showToast(enabled
                        ? R.string.dialog_dpis_enabled_status
                        : R.string.dialog_dpis_disabled_status);
                WechatDpiSheetBinder.publishForDpisState(packageName, enabled);
                RuntimeConfigDelivery.publishLocalSnapshotAfterSave();
                return true;
            }

            @Override
            public void showFontHookDomains(
                    AppListItem item,
                    AppConfigDialogBinder.AppConfigDialogState state,
                    Runnable onStateChanged
            ) {
                QuickConfigActivity.this.showFontHookDomains(item, state, onStateChanged);
            }

            @Override
            public String getFontHookDomainsButtonText(
                    AppListItem item,
                    AppConfigDialogBinder.AppConfigDialogState state
            ) {
                return FontHookDomainPresentation.forOverride(
                        resolveFontHookDomainsForDraft(item, state),
                        recommendedTemplateFontHookDomains())
                        .buttonText(QuickConfigActivity.this);
            }

            @Override
            public void openTypefaceLibrary() {
                startActivity(new Intent(QuickConfigActivity.this, FontLibraryActivity.class));
            }

            @Override
            public void startFeedbackDiagnostic(
                    AppListItem item,
                    AppConfigDialogBinder.AppConfigDialogState state
            ) {
                QuickConfigActivity.this.startFeedbackDiagnostic(item, state);
            }

            @Override
            public AppConfigSaveHandler.Result saveAppConfig(
                    View dialogView,
                    AppListItem item,
                    boolean dpisEnabled,
                    TextInputEditText viewportInput,
                    TextInputEditText fontScaleInput,
                    String viewportMode,
                    String viewportApplyMode,
                    boolean viewportApplyModeResetRequested,
                    String fontMode,
                    String selectedTypefaceId,
                    String draftFontHookDomainsRaw,
                    boolean fontHookDomainsResetRequested,
                    String viewportScaleInput,
                    String viewportAbsoluteInput
            ) {
                AppConfigSaveHandler.Result result = appConfigSaveHandler.save(
                        item,
                        viewportInput,
                        fontScaleInput,
                        viewportMode,
                        viewportApplyMode,
                        viewportApplyModeResetRequested,
                        fontMode,
                        selectedTypefaceId,
                        draftFontHookDomainsRaw,
                        fontHookDomainsResetRequested,
                        viewportScaleInput,
                        viewportAbsoluteInput,
                        isSystemHookEnabled(),
                        getHookConfigStore(),
                        null);
                return finalizeSave(result, dialogView, item.packageName, dpisEnabled);
            }

            @Override
            public DpiConfigStore getConfigStore() {
                return getHookConfigStore();
            }

            @Override
            public void requestAppsLoad() {
            }

            @Override
            public void onRuntimeConfigSaved() {
                RuntimeConfigDelivery.publishLocalSnapshotAfterSave();
            }

            @Override
            public void onDraftStateChanged(AppConfigDialogBinder.AppConfigDialogState state) {
            }

            @Override
            public void showToast(int messageResId) {
                QuickConfigActivity.this.showToast(messageResId);
            }
        };
    }

    private FeedbackDiagnosticCoordinator.Host createFeedbackDiagnosticHost() {
        return new FeedbackDiagnosticCoordinator.Host() {
            @Override
            public boolean restartTargetAppForDiagnostic(String packageName) {
                syncRuntimePropertiesForTargetLaunch(packageName);
                return feedbackDiagnosticAppLauncher.restartForDiagnostic(packageName);
            }

            @Override
            public String dpisPackageName() {
                return getPackageName();
            }

            @Override
            public RootAccessProbe.Result rootAccess() {
                return RootAccessProbe.cachedResult();
            }

            @Override
            public boolean systemHooksEnabled() {
                return isSystemHookEnabled();
            }

            @Override
            public long currentTimeMillis() {
                return System.currentTimeMillis();
            }

            @Override
            public void onFeedbackDiagnosticStarted() {
                showToast(R.string.feedback_diagnostic_started);
            }

            @Override
            public void onFeedbackDiagnosticUnavailable() {
                showToast(R.string.feedback_diagnostic_unavailable);
            }

            @Override
            public void onFeedbackDiagnosticRootRequired() {
                showToast(R.string.feedback_diagnostic_root_required);
            }

            @Override
            public void onFeedbackDiagnosticFinished(FeedbackDiagnosticCoordinator.Result result) {
                pendingFeedbackDiagnosticResult = result;
                maybeShowPendingFeedbackDiagnosticResult();
            }
        };
    }

    private SystemScopeCoordinator.Host createSystemScopeHost() {
        return new SystemScopeCoordinator.Host() {
            @Override
            public void showToast(int messageResId, Object... formatArgs) {
                Toast.makeText(
                        QuickConfigActivity.this,
                        getString(messageResId, formatArgs),
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void requestAppsLoad() {
            }

            @Override
            public void runOnUiThread(Runnable runnable) {
                QuickConfigActivity.this.runOnUiThread(runnable);
            }
        };
    }

    private boolean isSystemHookEnabled() {
        DpiConfigStore store = getHookConfigStore();
        return store != null && store.isSystemServerHooksEnabled();
    }

    private DpiConfigStore getHookConfigStore() {
        return DpisApplication.getActiveHookConfigStore(this);
    }

    private void publishAfterSave(String packageName) {
        RuntimeConfigDelivery.publishLocalSnapshotAfterSave();
        DpiConfigStore store = getHookConfigStore();
        ViewportPropertySyncer.syncTarget(packageName, store);
        FontRuntimePropertySyncer.syncTarget(packageName, store);
    }

    private void syncRuntimePropertiesForTargetLaunch(String packageName) {
        publishAfterSave(packageName);
    }

    private AppConfigSaveHandler.Result finalizeSave(
            AppConfigSaveHandler.Result result,
            View dialogView,
            String packageName,
            boolean dpisEnabled
    ) {
        if (result == null) {
            return AppConfigSaveHandler.Result.failure(R.string.system_settings_save_failed);
        }
        if (!result.success) {
            return result;
        }
        DpiConfigStore store = getHookConfigStore();
        if (!WechatDpiSheetBinder.save(dialogView, packageName, dpisEnabled, store)) {
            return AppConfigSaveHandler.Result.failure(
                    WechatDpiSheetBinder.isInputValid(dialogView)
                            ? R.string.system_settings_save_failed
                            : R.string.status_save_invalid);
        }
        publishAfterSave(packageName);
        return result;
    }

    private void startFeedbackDiagnostic(
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state
    ) {
        if (item == null) {
            return;
        }
        if (!DiagnosticLogGate.ensureEnabled(
                this,
                () -> showFeedbackDiagnosticConfirmation(item, state),
                null
        )) {
            return;
        }
        showFeedbackDiagnosticConfirmation(item, state);
    }

    private void showFeedbackDiagnosticConfirmation(
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state
    ) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.feedback_diagnostic_action)
                .setMessage(getString(
                        R.string.feedback_diagnostic_confirm_message,
                        item.label
                ))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.feedback_diagnostic_save_and_start_button, (dialog, which) -> {
                    AppListItem diagnosticItem = saveCurrentConfigForDiagnostic(item);
                    if (diagnosticItem == null) {
                        return;
                    }
                    boolean started = feedbackDiagnosticCoordinator.start(
                            FeedbackDiagnosticCoordinator.Request.fromPersisted(
                                    diagnosticItem,
                                    state,
                                    resolvePackageVersionName(item.packageName),
                                    getHookConfigStore()
                            )
                    );
                    if (!started) {
                        showToast(R.string.feedback_diagnostic_unavailable);
                    }
                })
                .show();
    }

    private AppListItem saveCurrentConfigForDiagnostic(AppListItem item) {
        View root = activeEditorRoot;
        AppConfigDialogBinder.AppConfigDialogViews views
                = AppConfigDialogBinder.viewsFor(root);
        AppConfigDialogBinder.AppConfigDialogState state
                = AppConfigDialogBinder.stateFor(root);
        if (root == null || views == null || state == null) {
            return item;
        }
        if (!AppConfigDialogBinder.updateSaveButtonState(root, views)) {
            showToast(R.string.status_save_invalid);
            return null;
        }
        AppConfigSaveHandler.Result result = appConfigDialogHost.saveAppConfig(
                root,
                item,
                state.dpisEnabled,
                views.viewportInputView,
                views.fontInputView,
                AppConfigDialogBinder.resolveViewportMode(views.viewportModeToggle),
                state.viewportApplyMode,
                state.viewportApplyModeResetRequested,
                AppConfigDialogBinder.resolveFontMode(views.fontModeToggle),
                state.selectedTypefaceId,
                state.draftFontHookDomainsRaw,
                state.fontHookDomainsResetRequested,
                state.viewportScaleInput,
                state.viewportAbsoluteInput
        );
        if (result.messageResId != 0) {
            showToast(result.messageResId);
        }
        if (!result.success) {
            return null;
        }
        state.previewFromGlobalPrefill = false;
        state.draftFontHookDomainsRaw = null;
        state.fontHookDomainsResetRequested = false;
        state.viewportApplyModeResetRequested = false;
        state.captureSavedDraft(views, false);
        AppConfigDialogBinder.showSaveButtonFeedback(views.saveButton);
        AppConfigDialogBinder binder = new AppConfigDialogBinder(this, appConfigDialogHost);
        AppConfigDialogBinder.AppConfigDialogActionStyle style
                = AppConfigDialogBinder.captureDialogActionStyle(views.scopeButton);
        binder.refreshDialogState(views, state, style, isSystemHookEnabled(), item);
        binder.syncHyperOsNativeProxyAfterSave(item, views, state);
        binder.requestScopeAfterSuccessfulSave(root, item, views, state, style, isSystemHookEnabled());
        return item.withWechatDpi(readPersistedWechatDpiForDiagnostic(item.packageName));
    }

    private Integer readPersistedWechatDpiForDiagnostic(String packageName) {
        if (!WechatDpiConfig.appliesTo(packageName)) {
            return null;
        }
        DpiConfigStore store = getHookConfigStore();
        return store != null ? store.getWechatDpi(packageName) : null;
    }

    private void maybeShowPendingFeedbackDiagnosticResult() {
        FeedbackDiagnosticExportBuilder.DiagnosticPackage diagnosticPackage
                = pendingFeedbackDiagnosticPackage;
        if (diagnosticPackage != null && activityResumed) {
            pendingFeedbackDiagnosticPackage = null;
            dismissFeedbackDiagnosticPackagingDialog();
            showFeedbackDiagnosticResultSheet(diagnosticPackage);
            return;
        }
        FeedbackDiagnosticCoordinator.Result result = pendingFeedbackDiagnosticResult;
        if (result == null || !activityResumed) {
            return;
        }
        pendingFeedbackDiagnosticResult = null;
        showFeedbackDiagnosticPackagingDialog();
        feedbackDiagnosticExportExecutor.execute(() -> {
            FeedbackDiagnosticExportBuilder.DiagnosticPackage built = null;
            try {
                built = feedbackDiagnosticExportBuilder.buildPackage(result);
            } catch (IOException | RuntimeException ignored) {
                built = null;
            }
            FeedbackDiagnosticExportBuilder.DiagnosticPackage finalBuilt = built;
            runOnUiThread(() -> {
                dismissFeedbackDiagnosticPackagingDialog();
                if (finalBuilt == null) {
                    showToast(R.string.feedback_diagnostic_save_failed);
                    return;
                }
                if (!activityResumed) {
                    pendingFeedbackDiagnosticPackage = finalBuilt;
                    return;
                }
                showFeedbackDiagnosticResultSheet(finalBuilt);
            });
        });
    }

    private void showFeedbackDiagnosticResultSheet(
            FeedbackDiagnosticExportBuilder.DiagnosticPackage diagnosticPackage
    ) {
        if (diagnosticPackage == null) {
            return;
        }
        new FeedbackDiagnosticResultSheet(this, new FeedbackDiagnosticResultSheet.Host() {
            @Override
            public void shareFeedbackDiagnostic(
                    FeedbackDiagnosticExportBuilder.DiagnosticPackage diagnosticPackage
            ) {
                QuickConfigActivity.this.shareFeedbackDiagnostic(diagnosticPackage);
            }

            @Override
            public void saveFeedbackDiagnostic(
                    FeedbackDiagnosticExportBuilder.DiagnosticPackage diagnosticPackage
            ) {
                QuickConfigActivity.this.launchSaveFeedbackDiagnosticPicker(diagnosticPackage);
            }
        }).show(diagnosticPackage);
    }

    private void showFeedbackDiagnosticPackagingDialog() {
        dismissFeedbackDiagnosticPackagingDialog();
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_feedback_diagnostic_packaging, null, false);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();
        dialog.show();
        DialogWindowSizer.applyStandardWidth(dialog, this);
        activeFeedbackDiagnosticPackagingDialog = dialog;
    }

    private void dismissFeedbackDiagnosticPackagingDialog() {
        if (activeFeedbackDiagnosticPackagingDialog != null) {
            activeFeedbackDiagnosticPackagingDialog.dismiss();
            activeFeedbackDiagnosticPackagingDialog = null;
        }
    }

    @SuppressWarnings("deprecation")
    private void launchSaveFeedbackDiagnosticPicker(
            FeedbackDiagnosticExportBuilder.DiagnosticPackage diagnosticPackage
    ) {
        if (diagnosticPackage == null) {
            return;
        }
        pendingFeedbackDiagnosticPackage = diagnosticPackage;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(FeedbackDiagnosticExportBuilder.MIME_TYPE)
                .putExtra(Intent.EXTRA_TITLE, diagnosticPackage.fileName);
        try {
            startActivityForResult(intent, REQUEST_SAVE_FEEDBACK_DIAGNOSTIC);
        } catch (ActivityNotFoundException error) {
            pendingFeedbackDiagnosticPackage = null;
            showToast(R.string.feedback_diagnostic_save_failed);
        }
    }

    private void saveFeedbackDiagnosticZip(Uri uri) {
        FeedbackDiagnosticExportBuilder.DiagnosticPackage diagnosticPackage
                = pendingFeedbackDiagnosticPackage;
        pendingFeedbackDiagnosticPackage = null;
        if (uri == null || diagnosticPackage == null) {
            showToast(R.string.feedback_diagnostic_save_failed);
            return;
        }
        feedbackDiagnosticExportExecutor.execute(() -> {
            boolean success;
            try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                if (outputStream == null) {
                    throw new IOException("Unable to open diagnostic output");
                }
                outputStream.write(diagnosticPackage.zipBytes);
                success = true;
            } catch (IOException | RuntimeException error) {
                success = false;
            }
            boolean finalSuccess = success;
            runOnUiThread(() -> showToast(finalSuccess
                    ? R.string.feedback_diagnostic_save_success
                    : R.string.feedback_diagnostic_save_failed));
        });
    }

    private void shareFeedbackDiagnostic(
            FeedbackDiagnosticExportBuilder.DiagnosticPackage diagnosticPackage
    ) {
        if (diagnosticPackage == null) {
            return;
        }
        feedbackDiagnosticExportExecutor.execute(() -> {
            Uri uri = null;
            boolean success = false;
            try {
                File file = writeSharedFeedbackDiagnosticZip(diagnosticPackage);
                uri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".fileprovider",
                        file
                );
                success = true;
            } catch (IOException | RuntimeException error) {
                success = false;
            }
            Uri finalUri = uri;
            boolean finalSuccess = success;
            runOnUiThread(() -> {
                if (!finalSuccess || finalUri == null) {
                    showToast(R.string.feedback_diagnostic_share_failed);
                    return;
                }
                launchFeedbackDiagnosticShareSheet(finalUri);
            });
        });
    }

    private File writeSharedFeedbackDiagnosticZip(
            FeedbackDiagnosticExportBuilder.DiagnosticPackage diagnosticPackage
    ) throws IOException {
        File directory = new File(getCacheDir(), SHARED_FEEDBACK_DIAGNOSTIC_DIRECTORY_NAME);
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Unable to create diagnostic share directory");
        }
        File file = new File(directory, diagnosticPackage.fileName);
        try (OutputStream outputStream = new FileOutputStream(file, false)) {
            outputStream.write(diagnosticPackage.zipBytes);
        }
        return file;
    }

    private void launchFeedbackDiagnosticShareSheet(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_SEND)
                .setType(FeedbackDiagnosticExportBuilder.MIME_TYPE)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(
                    intent,
                    getString(R.string.feedback_diagnostic_share_action)
            ));
        } catch (ActivityNotFoundException error) {
            showToast(R.string.feedback_diagnostic_share_failed);
        }
    }

    private String resolvePackageVersionName(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return "";
        }
        try {
            return getPackageManager().getPackageInfo(packageName, 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
            return "";
        }
    }

    private void showFontHookDomains(
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state,
            Runnable onStateChanged
    ) {
        if (item == null || item.packageName == null || item.packageName.isBlank()) {
            return;
        }
        DpiConfigStore store = getHookConfigStore();
        Set<String> automaticKnownDomains = recommendedTemplateFontHookDomains();
        HookDomainOverride currentOverride = resolveFontHookDomainsForDraft(item, state);
        FontHookDomainDialog.show(
                this,
                new FontHookDomainDialog.Host() {
                    @Override
                    public boolean saveCustom(
                            String packageName,
                            Set<String> selectedKnownDomains,
                            Set<String> automaticKnownDomains,
                            Set<String> unknownDomains
                    ) {
                        if (state != null) {
                            state.draftFontHookDomainsRaw
                                    = HookDomainOverrideStore.rawValueForSelection(
                                            selectedKnownDomains,
                                            automaticKnownDomains,
                                            unknownDomains
                                    );
                            state.fontHookDomainsResetRequested
                                    = state.draftFontHookDomainsRaw == null;
                        }
                        if (onStateChanged != null) {
                            onStateChanged.run();
                        }
                        return true;
                    }

                    @Override
                    public boolean restoreRecommended(String packageName) {
                        if (state != null) {
                            state.draftFontHookDomainsRaw = null;
                            state.fontHookDomainsResetRequested = true;
                        }
                        if (onStateChanged != null) {
                            onStateChanged.run();
                        }
                        return true;
                    }

                    @Override
                    public boolean saveViewportApplyMode(String packageName, String mode) {
                        if (state != null) {
                            state.viewportApplyMode = ViewportApplyMode.normalize(mode);
                            state.viewportApplyModeResetRequested
                                    = ViewportApplyMode.OFF.equals(state.viewportApplyMode);
                        }
                        if (onStateChanged != null) {
                            onStateChanged.run();
                        }
                        return true;
                    }
                },
                item.packageName,
                automaticKnownDomains,
                currentOverride,
                state != null
                        ? state.viewportApplyMode
                        : store.getTargetViewportApplyMode(item.packageName),
                isFontHookDomainEditingEnabled(),
                onStateChanged
        );
    }

    private boolean isFontHookDomainEditingEnabled() {
        if (activeEditorRoot == null) {
            return false;
        }
        AppConfigDialogBinder.AppConfigDialogViews views
                = AppConfigDialogBinder.viewsFor(activeEditorRoot);
        return views != null && FontApplyMode.FIELD_REWRITE.equals(
                AppConfigDialogBinder.resolveFontMode(views.fontModeToggle));
    }

    private HookDomainOverride resolveFontHookDomainsForDraft(
            AppListItem item,
            AppConfigDialogBinder.AppConfigDialogState state
    ) {
        if (state != null && state.fontHookDomainsResetRequested) {
            return HookDomainOverride.automatic();
        }
        if (state != null
                && (state.previewFromGlobalPrefill
                        || state.draftFontHookDomainsRaw != null)) {
            return normalizedFontHookDomainsOverride(
                    HookDomainOverrideStore.fromRaw(state.draftFontHookDomainsRaw),
                    recommendedTemplateFontHookDomains());
        }
        return normalizedFontHookDomainsOverride(
                new HookDomainOverrideStore(getHookConfigStore()).read(
                        item != null ? item.packageName : null),
                recommendedTemplateFontHookDomains());
    }

    private HookDomainOverride normalizedFontHookDomainsOverride(
            HookDomainOverride override,
            Set<String> automaticKnownDomains
    ) {
        return HookDomainOverrideStore.automaticIfSelectionMatchesAutomatic(
                override,
                automaticKnownDomains);
    }

    private Set<String> recommendedTemplateFontHookDomains() {
        // Custom hook-chain edits are global-route semantics, not target guesses.
        return FontHookDomainRegistry.recommendedTemplateKnownDomains();
    }

    private void executeHyperOsNativeProxyMount(
            AppListItem item,
            boolean apply,
            Runnable onFinished
    ) {
        executeHyperOsNativeProxyMount(item, apply, ignored -> {
            if (onFinished != null) {
                onFinished.run();
            }
        });
    }

    private void executeHyperOsNativeProxyMount(
            AppListItem item,
            boolean apply,
            HyperOsNativeProxyMountCallback onFinished
    ) {
        new Thread(() -> {
            HyperOsNativeProxyBindMounter.MountPlan plan
                    = HyperOsNativeProxyBindMounter.createPlan(this, item.packageName);
            HyperOsNativeProxyBindMounter.MountResult result = apply
                    ? HyperOsNativeProxyBindMounter.apply(plan)
                    : HyperOsNativeProxyBindMounter.unmount(plan);
            DpisLog.i("Quick HyperOS Native Proxy "
                    + (apply ? "apply" : "rollback")
                    + " package="
                    + item.packageName
                    + " success="
                    + result.success
                    + " output="
                    + result.output);
            int messageResId = apply
                    ? R.string.dialog_hyperos_native_proxy_apply_failed
                    : R.string.dialog_hyperos_native_proxy_unmount_failed;
            runOnUiThread(() -> {
                if (!result.success) {
                    showToast(messageResId);
                }
                if (onFinished != null) {
                    onFinished.onFinished(result.success);
                }
            });
        }, "DPIS-Quick-HyperOsNativeProxyMount").start();
    }

    private void executeDialogProcessAction(
            AppListItem item,
            AppConfigDialogBinder.ProcessAction action
    ) {
        if (action == AppConfigDialogBinder.ProcessAction.RESTART
                && shouldPrepareHyperOsNativeProxyForRestart(item)) {
            // Re-prepare before restart because APK updates can stale the bind mount.
            executeHyperOsNativeProxyMount(item, true, success -> {
                if (success) {
                    executeDialogProcessActionAfterHyperOsProxyReady(item, action);
                }
            });
            return;
        }
        executeDialogProcessActionAfterHyperOsProxyReady(item, action);
    }

    private boolean shouldPrepareHyperOsNativeProxyForRestart(AppListItem item) {
        if (item == null || !item.hyperOsNativeProxyCandidate) {
            return false;
        }
        DpiConfigStore store = getHookConfigStore();
        return store != null
                && store.isTargetDpisEnabled(item.packageName)
                && hasActiveStoredConfig(store, item.packageName);
    }

    private static boolean hasActiveStoredConfig(DpiConfigStore store, String packageName) {
        ViewportTargetSpec viewportTargetSpec = store.getTargetViewportSpec(packageName);
        Integer fontScalePercent = store.getTargetFontScalePercent(packageName);
        return viewportTargetSpec.isEnabled()
                || fontScalePercent != null
                || store.hasTargetAppSpecificConfig(packageName);
    }

    private void executeDialogProcessActionAfterHyperOsProxyReady(
            AppListItem item,
            AppConfigDialogBinder.ProcessAction action
    ) {
        ProcessActionHandler.Action mappedAction = switch (action) {
            case START -> ProcessActionHandler.Action.START;
            case RESTART -> ProcessActionHandler.Action.RESTART;
            case STOP -> ProcessActionHandler.Action.STOP;
        };
        processActionHandler.execute(item, mappedAction);
    }

    private interface HyperOsNativeProxyMountCallback {
        void onFinished(boolean success);
    }

    private void showToast(int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }
}
