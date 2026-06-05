package com.dpis.module;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Set;

final class GlobalPrefillSheetDialog {
    private static final String PREFILL_PACKAGE_NAME = "__global_prefill__";

    static void show(Activity activity) {
        show(activity, null);
    }

    static void show(Activity activity, Runnable onUpdated) {
        new GlobalPrefillSheetDialog(activity, onUpdated).show();
    }

    private final Activity activity;
    private final BottomSheetDialog dialog;
    private final GlobalPrefillSaveHandler saveHandler = new GlobalPrefillSaveHandler();
    private final Runnable onUpdated;

    private GlobalPrefillStore globalPrefillStore;
    private AppConfigDialogBinder typefaceBinder;
    private AppConfigDialogBinder.AppConfigDialogState state;
    private View dialogView;

    private TextInputLayout viewportInputLayout;
    private TextInputEditText viewportInputView;
    private TextInputLayout fontInputLayout;
    private TextInputEditText fontInputView;
    private AppConfigDialogBinder.ModeToggle viewportModeToggle;
    private AppConfigDialogBinder.ModeToggle fontModeToggle;
    private AppCompatImageButton resetButton;
    private MaterialButton typefaceSelectorButton;
    private MaterialButton hookDomainsButton;
    private MaterialButton saveButton;
    private SheetUnsavedBadgeBinder unsavedBadgeBinder;
    private String initialDraftSignature = "";

    private GlobalPrefillSheetDialog(Activity activity, Runnable onUpdated) {
        this.activity = activity;
        this.onUpdated = onUpdated;
        this.dialog = new BottomSheetDialog(activity);
        SharedPreferences preferences = activity.getSharedPreferences(
                DpiConfigStore.GROUP, Activity.MODE_PRIVATE);
        globalPrefillStore = new GlobalPrefillStore(preferences);
        typefaceBinder = new AppConfigDialogBinder(activity, createTypefaceHost());
        dialogView = LayoutInflater.from(activity).inflate(
                R.layout.dialog_global_prefill_sheet, null, false);
        dialog.setContentView(dialogView);
        bindViews(dialogView);
        applyInsets(dialogView);
        bindForm();
        bindActions();
    }

    private void show() {
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.getBehavior().setFitToContents(true);
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        dialog.setOnShowListener(unused -> {
            if (dialog.getWindow() != null) {
                int surfaceColor = MaterialColors.getColor(
                        dialog.getWindow().getDecorView(),
                        com.google.android.material.R.attr.colorSurface);
                dialog.getWindow().setNavigationBarColor(surfaceColor);
                dialog.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
            applyWrapContentSheetHeight();
        });
        dialog.show();
    }

    private void applyWrapContentSheetHeight() {
        FrameLayout bottomSheet = dialog.findViewById(
                com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet == null) {
            return;
        }
        ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
        if (params != null && params.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            bottomSheet.setLayoutParams(params);
        }
        BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void bindViews(View root) {
        viewportInputLayout = root.findViewById(R.id.template_config_viewport_input_layout);
        viewportInputView = root.findViewById(R.id.template_config_viewport_input);
        fontInputLayout = root.findViewById(R.id.template_config_font_scale_input_layout);
        fontInputView = root.findViewById(R.id.template_config_font_scale_input);
        viewportModeToggle = new AppConfigDialogBinder.ModeToggle(
                root.findViewById(R.id.template_config_viewport_mode_toggle_button),
                root.findViewById(R.id.template_config_viewport_mode_toggle_thumb),
                root.findViewById(R.id.template_config_viewport_mode_system_label),
                root.findViewById(R.id.template_config_viewport_mode_compat_label));
        fontModeToggle = new AppConfigDialogBinder.ModeToggle(
                root.findViewById(R.id.template_config_font_mode_toggle_button),
                root.findViewById(R.id.template_config_font_mode_toggle_thumb),
                root.findViewById(R.id.template_config_font_mode_system_label),
                root.findViewById(R.id.template_config_font_mode_compat_label));
        resetButton = root.findViewById(R.id.global_prefill_reset_button);
        typefaceSelectorButton = root.findViewById(R.id.template_config_typeface_selector_button);
        hookDomainsButton = root.findViewById(R.id.template_config_font_hook_domains_button);
        saveButton = root.findViewById(R.id.template_config_save_button);
        unsavedBadgeBinder = SheetUnsavedBadgeBinder.bind(
                root, this::hasUnsavedChanges);
        View footerResetButton = root.findViewById(R.id.template_config_reset_button);
        if (footerResetButton != null) {
            footerResetButton.setVisibility(View.GONE);
        }
    }

    private void applyInsets(View root) {
        View content = root.findViewById(R.id.global_prefill_scroll);
        final int baseBottomPadding = content.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
            Insets navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
                    view.getPaddingRight(), baseBottomPadding + navigationBars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(content);
    }

    private void bindForm() {
        TemplateConfigValue value = globalPrefillStore.read();
        String storedViewportInput = AppConfigInputValidation.formatViewportInput(
                value.viewportTargetSpec);
        String initialViewportType = AppConfigInputValidation.initialViewportTargetType(
                value.viewportTargetSpec);
        String initialViewportInput = storedViewportInput;
        String initialViewportScaleInput = value.viewportTargetSpec.isRelativeScale()
                ? storedViewportInput
                : "";
        String initialViewportAbsoluteInput = value.viewportTargetSpec.isAbsoluteDp()
                ? storedViewportInput
                : "";
        String initialViewportApplyMode = value.viewportApplyMode;
        String initialFontInput = value.fontScalePercent != null
                ? String.valueOf(value.fontScalePercent)
                : "";
        String initialFontMode = value.fontApplyMode;
        String initialTypefaceId = value.typefaceId;
        String initialHookDomainsRaw = value.fontHookDomainsRaw;
        state = new AppConfigDialogBinder.AppConfigDialogState(
                false,
                false,
                true,
                false,
                null,
                initialHookDomainsRaw,
                initialViewportApplyMode,
                initialTypefaceId,
                initialViewportType,
                initialViewportInput,
                initialViewportScaleInput,
                initialViewportAbsoluteInput);
        viewportInputView.setText(initialViewportInput);
        fontInputView.setText(initialFontInput);
        AppConfigDialogBinder.bindViewportModeToggle(
                viewportModeToggle, initialViewportType, false);
        typefaceBinder.bindViewportInputHint(viewportInputLayout, initialViewportType);
        AppConfigDialogBinder.bindFontModeToggle(
                fontModeToggle, AppConfigInputValidation.initialFontMode(initialFontMode), false);
        typefaceBinder.bindTypefaceSelector(typefaceSelectorButton, state.selectedTypefaceId);
        refreshHookDomainsButton();
        initialDraftSignature = currentDraftSignature();
        refreshValidationUi();
    }

    private void bindActions() {
        TouchFeedbackBinder.bindPressHaptic(typefaceSelectorButton);
        TouchFeedbackBinder.bindPressHaptic(hookDomainsButton);
        TouchFeedbackBinder.bindPressHaptic(resetButton);
        TouchFeedbackBinder.bindPressHaptic(saveButton);
        bindInputFocusBehavior();

        viewportModeToggle.emulationLabel.setOnClickListener(v -> {
            clearInputFocus();
            AppConfigDialogBinder.switchViewportTargetType(
                    viewportModeToggle, viewportInputView, state,
                    ViewportTargetType.RELATIVE_SCALE, true);
            typefaceBinder.bindViewportInputHint(
                    viewportInputLayout, ViewportTargetType.RELATIVE_SCALE);
            refreshValidationUi();
        });
        viewportModeToggle.replaceLabel.setOnClickListener(v -> {
            clearInputFocus();
            AppConfigDialogBinder.switchViewportTargetType(
                    viewportModeToggle, viewportInputView, state,
                    ViewportTargetType.ABSOLUTE_DP, true);
            typefaceBinder.bindViewportInputHint(
                    viewportInputLayout, ViewportTargetType.ABSOLUTE_DP);
            refreshValidationUi();
        });
        fontModeToggle.emulationLabel.setOnClickListener(v -> {
            clearInputFocus();
            AppConfigDialogBinder.bindFontModeToggle(
                    fontModeToggle, FontApplyMode.SYSTEM_EMULATION, true);
            refreshValidationUi();
        });
        fontModeToggle.replaceLabel.setOnClickListener(v -> {
            clearInputFocus();
            AppConfigDialogBinder.bindFontModeToggle(
                    fontModeToggle, FontApplyMode.FIELD_REWRITE, true);
            refreshValidationUi();
        });
        typefaceSelectorButton.setOnClickListener(v -> {
            clearInputFocus();
            typefaceBinder.showTypefaceSelector(
                    typefaceSelectorButton,
                    state,
                    this::refreshValidationUi);
        });
        hookDomainsButton.setOnClickListener(v -> {
            clearInputFocus();
            showHookDomainsDialog();
        });
        resetButton.setOnClickListener(v -> {
            clearInputFocus();
            resetGlobalPrefillDraft();
        });
        saveButton.setOnClickListener(v -> {
            clearInputFocus();
            saveGlobalPrefill();
        });

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshValidationUi();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        viewportInputView.addTextChangedListener(watcher);
        fontInputView.addTextChangedListener(watcher);
    }

    private void bindInputFocusBehavior() {
        View scroll = dialogView != null
                ? dialogView.findViewById(R.id.global_prefill_scroll)
                : null;
        android.widget.TextView.OnEditorActionListener doneListener =
                (view, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE
                    && !(event != null
                            && event.getAction() == KeyEvent.ACTION_DOWN
                            && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                return false;
            }
            clearInputFocus();
            return true;
        };
        viewportInputView.setOnEditorActionListener(doneListener);
        fontInputView.setOnEditorActionListener(doneListener);
        FormInputFocusBinder.bindDismissOnOutsideTouch(
                scroll,
                dialogView,
                viewportInputView,
                fontInputView
        );
    }

    private void clearInputFocus() {
        FormInputFocusBinder.clearFocusAndHideIme(
                dialogView,
                viewportInputView,
                fontInputView
        );
    }

    private void showHookDomainsDialog() {
        FontHookDomainDialog.show(activity,
                new FontHookDomainDialog.Host() {
                    @Override
                    public boolean saveCustom(String packageName,
                            Set<String> selectedKnownDomains,
                            Set<String> automaticKnownDomains,
                            Set<String> unknownDomains) {
                        state.draftFontHookDomainsRaw = HookDomainOverrideStore.rawValueForSelection(
                                selectedKnownDomains,
                                automaticKnownDomains,
                                unknownDomains);
                        refreshHookDomainsButton();
                        refreshValidationUi();
                        return true;
                    }

                    @Override
                    public boolean restoreRecommended(String packageName) {
                        state.draftFontHookDomainsRaw = null;
                        refreshHookDomainsButton();
                        refreshValidationUi();
                        return true;
                    }

                    @Override
                    public boolean saveViewportApplyMode(String packageName, String mode) {
                        state.viewportApplyMode = ViewportApplyMode.normalize(mode);
                        refreshValidationUi();
                        return true;
                    }
                },
                PREFILL_PACKAGE_NAME,
                FontHookDomainRegistry.recommendedTemplateKnownDomains(),
                HookDomainOverrideStore.fromRaw(state.draftFontHookDomainsRaw),
                state.viewportApplyMode,
                this::refreshHookDomainsButton);
    }

    private void resetGlobalPrefillDraft() {
        viewportInputView.setText("");
        fontInputView.setText("");
        state.viewportApplyMode = ViewportApplyMode.OFF;
        state.selectedTypefaceId = null;
        state.draftFontHookDomainsRaw = null;
        state.clearViewportInputs();
        AppConfigDialogBinder.bindViewportModeToggle(
                viewportModeToggle, ViewportTargetType.RELATIVE_SCALE, false);
        typefaceBinder.bindViewportInputHint(viewportInputLayout, ViewportTargetType.RELATIVE_SCALE);
        AppConfigDialogBinder.bindFontModeToggle(
                fontModeToggle, FontApplyMode.SYSTEM_EMULATION, false);
        typefaceBinder.bindTypefaceSelector(typefaceSelectorButton, state.selectedTypefaceId);
        refreshHookDomainsButton();
        refreshValidationUi();
    }

    private void saveGlobalPrefill() {
        if (!refreshValidationUi()) {
            showToast(R.string.status_save_invalid);
            return;
        }
        GlobalPrefillSaveHandler.Result result = saveHandler.save(globalPrefillStore,
                new GlobalPrefillSaveHandler.Request(
                        textOf(viewportInputView),
                        AppConfigDialogBinder.resolveViewportMode(viewportModeToggle),
                        state.viewportApplyMode,
                        textOf(fontInputView),
                        AppConfigDialogBinder.resolveFontMode(fontModeToggle),
                        state.selectedTypefaceId,
                        normalizeTemplateHookDomainsRaw(state.draftFontHookDomainsRaw)));
        showToast(result.messageResId);
        if (result.success) {
            if (onUpdated != null) {
                onUpdated.run();
            }
            dialog.dismiss();
        }
    }

    private boolean refreshValidationUi() {
        boolean viewportValid = AppConfigInputValidation.isViewportInputValid(
                textOf(viewportInputView),
                AppConfigDialogBinder.resolveViewportMode(viewportModeToggle));
        boolean fontValid = AppConfigInputValidation.isFontScaleInputValid(textOf(fontInputView));
        bindInputErrorState(viewportInputLayout, viewportValid);
        bindInputErrorState(fontInputLayout, fontValid);
        saveButton.setEnabled(viewportValid && fontValid);
        refreshUnsavedBadge();
        return viewportValid && fontValid;
    }

    private static void bindInputErrorState(TextInputLayout inputLayout, boolean valid) {
        int defaultStrokeColor = MaterialColors.getColor(
                inputLayout, com.google.android.material.R.attr.colorOutline);
        int errorStrokeColor = MaterialColors.getColor(
                inputLayout, androidx.appcompat.R.attr.colorError);
        inputLayout.setError(null);
        inputLayout.setErrorEnabled(false);
        inputLayout.setBoxStrokeColor(valid ? defaultStrokeColor : errorStrokeColor);
    }

    private void refreshUnsavedBadge() {
        if (unsavedBadgeBinder != null) {
            unsavedBadgeBinder.refresh();
        }
    }

    private boolean hasUnsavedChanges() {
        return !initialDraftSignature.equals(currentDraftSignature());
    }

    private String currentDraftSignature() {
        return String.join("|",
                normalizeText(textOf(viewportInputView)),
                AppConfigDialogBinder.resolveViewportMode(viewportModeToggle),
                ViewportApplyMode.normalize(state != null ? state.viewportApplyMode : null),
                normalizeText(textOf(fontInputView)),
                AppConfigDialogBinder.resolveFontMode(fontModeToggle),
                normalizeText(state != null ? state.selectedTypefaceId : null),
                normalizeText(normalizeTemplateHookDomainsRaw(
                        state != null ? state.draftFontHookDomainsRaw : null)));
    }

    private void refreshHookDomainsButton() {
        HookDomainOverride override = HookDomainOverrideStore.fromRaw(state.draftFontHookDomainsRaw);
        if (!override.customPathEnabled || isRecommendedTemplateHookDomains(override)) {
            hookDomainsButton.setText(R.string.dialog_font_hook_domains_title);
            return;
        }
        int selectedCount = FontHookDomainRegistry.orderedCustomizableDisplaySubset(
                override.enabledKnownDomains).size();
        int totalCount = FontHookDomainRegistry.orderedCustomizableDisplayIdsList().size();
        hookDomainsButton.setText(activity.getString(
                R.string.dialog_font_hook_domains_title_with_count,
                selectedCount,
                totalCount));
    }

    private static String normalizeTemplateHookDomainsRaw(String raw) {
        HookDomainOverride override = HookDomainOverrideStore.fromRaw(raw);
        return isRecommendedTemplateHookDomains(override) ? null : raw;
    }

    private static boolean isRecommendedTemplateHookDomains(HookDomainOverride override) {
        return override != null
                && override.customPathEnabled
                && override.unknownDomains.isEmpty()
                && FontHookDomainRegistry.orderedCustomizableDisplaySubset(
                        override.enabledKnownDomains).equals(
                                FontHookDomainRegistry.recommendedTemplateKnownDomains());
    }

    private AppConfigDialogBinder.Host createTypefaceHost() {
        return new AppConfigDialogBinder.Host() {
            @Override
            public void toggleScope(AppListItem item,
                    boolean currentlyInScope,
                    Runnable onTurnedInScope,
                    Runnable onTurnedOutScope) {
            }

            @Override
            public boolean requestScope(AppListItem item,
                    Runnable onTurnedInScope,
                    Runnable onRequestFinished) {
                return false;
            }

            @Override
            public void executeProcessAction(AppListItem item, AppConfigDialogBinder.ProcessAction action) {
            }

            @Override
            public void applyHyperOsNativeProxy(AppListItem item, Runnable onFinished) {
            }

            @Override
            public void unmountHyperOsNativeProxy(AppListItem item, Runnable onFinished) {
            }

            @Override
            public boolean setDpisEnabled(String packageName, boolean enabled) {
                return false;
            }

            @Override
            public void showFontHookDomains(AppListItem item,
                    AppConfigDialogBinder.AppConfigDialogState state,
                    Runnable onStateChanged) {
            }

            @Override
            public String getFontHookDomainsButtonText(AppListItem item,
                    AppConfigDialogBinder.AppConfigDialogState state) {
                return activity.getString(R.string.dialog_font_hook_domains_title);
            }

            @Override
            public void openTypefaceLibrary() {
                activity.startActivity(new Intent(activity, FontLibraryActivity.class));
            }

            @Override
            public int[] saveAppConfig(AppListItem item,
                    TextInputEditText viewportInput,
                    TextInputEditText fontScaleInput,
                    String viewportMode,
                    String viewportApplyMode,
                    boolean viewportApplyModeResetRequested,
                    String fontMode,
                    String selectedTypefaceId,
                    String previewFontHookDomainsRaw,
                    boolean fontHookDomainsResetRequested,
                    String viewportScaleInput,
                    String viewportAbsoluteInput) {
                return new int[0];
            }

            @Override
            public void showToast(int messageResId) {
                GlobalPrefillSheetDialog.this.showToast(messageResId);
            }

            @Override
            public DpiConfigStore getConfigStore() {
                return new DpiConfigStore(activity.getSharedPreferences(
                        DpiConfigStore.GROUP, Activity.MODE_PRIVATE));
            }

            @Override
            public void requestAppsLoad() {
            }

            @Override
            public void onDraftStateChanged(
                    AppConfigDialogBinder.AppConfigDialogState state) {
            }
        };
    }

    private void showToast(int messageResId) {
        Toast.makeText(activity, messageResId, Toast.LENGTH_SHORT).show();
    }

    private static String textOf(TextInputEditText view) {
        return view.getText() != null ? view.getText().toString() : "";
    }

    private static String normalizeText(String value) {
        return value != null ? value.trim() : "";
    }
}
