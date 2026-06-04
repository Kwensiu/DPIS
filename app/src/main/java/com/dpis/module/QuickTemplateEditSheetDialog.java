package com.dpis.module;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import java.util.Set;

final class QuickTemplateEditSheetDialog {
    static void show(Activity activity, String templateId) {
        show(activity, templateId, null);
    }

    static void show(Activity activity, String templateId, Runnable onUpdated) {
        new QuickTemplateEditSheetDialog(activity, templateId, onUpdated).show();
    }

    private static final String PREVIEW_PACKAGE_NAME = "__quick_template__";
    private final Activity activity;
    private final BottomSheetDialog dialog;
    private final QuickTemplateSaveHandler saveHandler = new QuickTemplateSaveHandler();
    private final Runnable onUpdated;
    private boolean ready;

    private QuickTemplateStore quickTemplateStore;
    private AppConfigDialogBinder typefaceBinder;
    private AppConfigDialogBinder.AppConfigDialogState state;

    private String templateId;
    private boolean isNewTemplate;
    private QuickTemplateStore.QuickTemplate originalTemplate;

    private MaterialTextView titleView;
    private TextInputLayout nameInputLayout;
    private TextInputEditText nameInputView;
    private TextInputLayout viewportInputLayout;
    private TextInputEditText viewportInputView;
    private TextInputLayout fontInputLayout;
    private TextInputEditText fontInputView;
    private AppConfigDialogBinder.ModeToggle viewportModeToggle;
    private AppConfigDialogBinder.ModeToggle fontModeToggle;
    private MaterialButton typefaceSelectorButton;
    private MaterialButton hookDomainsButton;
    private AppCompatImageButton resetButton;
    private AppCompatImageButton deleteButton;
    private MaterialButton saveButton;
    private SheetUnsavedBadgeBinder unsavedBadgeBinder;
    private String initialDraftSignature = "";

    private QuickTemplateEditSheetDialog(Activity activity, String requestedTemplateId, Runnable onUpdated) {
        this.activity = activity;
        this.onUpdated = onUpdated;
        this.dialog = new BottomSheetDialog(activity);
        SharedPreferences preferences = activity.getSharedPreferences(
                DpiConfigStore.GROUP, Activity.MODE_PRIVATE);
        quickTemplateStore = new QuickTemplateStore(preferences);
        typefaceBinder = new AppConfigDialogBinder(activity, createTypefaceHost());
        ready = resolveTemplate(requestedTemplateId);
        if (!ready) {
            showToast(R.string.quick_template_missing);
            return;
        }
        View dialogView = LayoutInflater.from(activity).inflate(
                R.layout.dialog_quick_template_edit_sheet, null, false);
        dialog.setContentView(dialogView);
        bindViews(dialogView);
        applyInsets(dialogView);
        bindForm();
        bindActions();
    }

    private void show() {
        if (!ready) {
            return;
        }
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

    private boolean resolveTemplate(String requestedTemplateId) {
        boolean editRequested = requestedTemplateId != null && !requestedTemplateId.trim().isEmpty();
        String resolvedId = editRequested ? requestedTemplateId.trim() : null;
        if (resolvedId != null) {
            originalTemplate = quickTemplateStore.read(resolvedId);
        }
        if (originalTemplate != null) {
            templateId = originalTemplate.id;
            isNewTemplate = false;
            return true;
        }
        if (editRequested) {
            return false;
        }
        templateId = quickTemplateStore.newTemplateId();
        isNewTemplate = true;
        return true;
    }

    private void bindViews(View root) {
        titleView = root.findViewById(R.id.quick_template_edit_title);
        nameInputLayout = root.findViewById(R.id.quick_template_edit_name_layout);
        nameInputView = root.findViewById(R.id.quick_template_edit_name_input);
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
        View viewportApplyModeButton = root.findViewById(R.id.template_config_viewport_apply_mode_button);
        typefaceSelectorButton = root.findViewById(R.id.template_config_typeface_selector_button);
        hookDomainsButton = root.findViewById(R.id.template_config_font_hook_domains_button);
        resetButton = root.findViewById(R.id.quick_template_edit_reset_button);
        deleteButton = root.findViewById(R.id.quick_template_edit_delete_button);
        MaterialButton footerResetButton = root.findViewById(R.id.template_config_reset_button);
        MaterialButton footerDeleteButton = root.findViewById(R.id.template_config_delete_button);
        saveButton = root.findViewById(R.id.template_config_save_button);
        unsavedBadgeBinder = SheetUnsavedBadgeBinder.bind(
                root, this::hasUnsavedChanges);
        titleView.setText(isNewTemplate
                ? R.string.quick_template_edit_page_title_new
                : R.string.quick_template_edit_page_title_edit);
        viewportApplyModeButton.setVisibility(View.GONE);
        deleteButton.setVisibility(isNewTemplate ? View.GONE : View.VISIBLE);
        footerResetButton.setVisibility(View.GONE);
        footerDeleteButton.setVisibility(View.GONE);
    }

    private void applyInsets(View root) {
        View content = root.findViewById(R.id.quick_template_edit_scroll);
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
        TemplateConfigValue value = originalTemplate != null
                ? originalTemplate.configValue
                : TemplateConfigValue.EMPTY;
        String storedName = originalTemplate != null ? originalTemplate.name : "";
        String initialName = storedName;
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
                initialHookDomainsRaw,
                initialViewportApplyMode,
                initialTypefaceId,
                initialViewportType,
                initialViewportInput,
                initialViewportScaleInput,
                initialViewportAbsoluteInput);
        nameInputView.setText(initialName);
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
        TouchFeedbackBinder.bindPressHaptic(deleteButton);
        TouchFeedbackBinder.bindPressHaptic(saveButton);

        viewportModeToggle.emulationLabel.setOnClickListener(v -> {
            AppConfigDialogBinder.switchViewportTargetType(
                    viewportModeToggle, viewportInputView, state,
                    ViewportTargetType.RELATIVE_SCALE, true);
            typefaceBinder.bindViewportInputHint(
                    viewportInputLayout, ViewportTargetType.RELATIVE_SCALE);
            refreshValidationUi();
        });
        viewportModeToggle.replaceLabel.setOnClickListener(v -> {
            AppConfigDialogBinder.switchViewportTargetType(
                    viewportModeToggle, viewportInputView, state,
                    ViewportTargetType.ABSOLUTE_DP, true);
            typefaceBinder.bindViewportInputHint(
                    viewportInputLayout, ViewportTargetType.ABSOLUTE_DP);
            refreshValidationUi();
        });
        fontModeToggle.emulationLabel.setOnClickListener(v -> {
            AppConfigDialogBinder.bindFontModeToggle(
                    fontModeToggle, FontApplyMode.SYSTEM_EMULATION, true);
            refreshValidationUi();
        });
        fontModeToggle.replaceLabel.setOnClickListener(v -> {
            AppConfigDialogBinder.bindFontModeToggle(
                    fontModeToggle, FontApplyMode.FIELD_REWRITE, true);
            refreshValidationUi();
        });
        typefaceSelectorButton.setOnClickListener(v -> typefaceBinder.showTypefaceSelector(
                typefaceSelectorButton,
                state,
                this::refreshValidationUi));
        hookDomainsButton.setOnClickListener(v -> showHookDomainsDialog());
        resetButton.setOnClickListener(v -> resetTemplateConfig());
        deleteButton.setOnClickListener(v -> confirmDelete());
        saveButton.setOnClickListener(v -> saveTemplate());

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
        nameInputView.addTextChangedListener(watcher);
        viewportInputView.addTextChangedListener(watcher);
        fontInputView.addTextChangedListener(watcher);
    }

    private void showHookDomainsDialog() {
        FontHookDomainDialog.show(activity,
                new FontHookDomainDialog.Host() {
                    @Override
                    public boolean saveCustom(String packageName,
                            Set<String> selectedKnownDomains,
                            Set<String> automaticKnownDomains,
                            Set<String> unknownDomains) {
                        state.previewFontHookDomainsRaw = HookDomainOverrideStore.rawValueForSelection(
                                selectedKnownDomains,
                                automaticKnownDomains,
                                unknownDomains);
                        refreshHookDomainsButton();
                        refreshValidationUi();
                        return true;
                    }

                    @Override
                    public boolean restoreRecommended(String packageName) {
                        state.previewFontHookDomainsRaw = null;
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
                PREVIEW_PACKAGE_NAME,
                FontHookDomainRegistry.recommendedTemplateKnownDomains(),
                HookDomainOverrideStore.fromRaw(state.previewFontHookDomainsRaw),
                state.viewportApplyMode,
                this::refreshHookDomainsButton);
    }

    private void resetTemplateConfig() {
        viewportInputView.setText("");
        fontInputView.setText("");
        state.viewportApplyMode = ViewportApplyMode.OFF;
        state.selectedTypefaceId = null;
        state.previewFontHookDomainsRaw = null;
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

    private void confirmDelete() {
        if (originalTemplate == null) {
            return;
        }
        String displayName = originalTemplate.name;
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.quick_template_delete_title)
                .setMessage(activity.getString(R.string.quick_template_delete_message, displayName))
                .setNegativeButton(R.string.dialog_process_action_confirm_negative, null)
                .setPositiveButton(R.string.font_library_delete_action,
                        (unusedDialog, which) -> deleteTemplate())
                .create();
        dialog.setOnShowListener(d -> {
            TouchFeedbackBinder.bindPressHaptic(
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE));
            TouchFeedbackBinder.bindPressHaptic(
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE));
        });
        dialog.show();
        DialogWindowSizer.applyStandardWidth(dialog, activity);
    }

    private void deleteTemplate() {
        if (originalTemplate == null) {
            return;
        }
        if (quickTemplateStore.delete(originalTemplate.id)) {
            if (onUpdated != null) {
                onUpdated.run();
            }
            showToast(R.string.quick_template_delete_success);
            dialog.dismiss();
            return;
        }
        showToast(R.string.quick_template_delete_failed);
    }

    private void saveTemplate() {
        String name = textOf(nameInputView).trim();
        if (TextUtils.isEmpty(name)) {
            bindNameErrorState(false, R.string.quick_template_name_required);
            showToast(R.string.quick_template_name_required);
            return;
        }
        if (!refreshValidationUi()) {
            showToast(R.string.status_save_invalid);
            return;
        }
        QuickTemplateSaveHandler.Result result = saveHandler.save(quickTemplateStore,
                new QuickTemplateSaveHandler.Request(
                        templateId,
                        name,
                        textOf(viewportInputView),
                        AppConfigDialogBinder.resolveViewportMode(viewportModeToggle),
                        state.viewportApplyMode,
                        textOf(fontInputView),
                        AppConfigDialogBinder.resolveFontMode(fontModeToggle),
                        state.selectedTypefaceId,
                        normalizeTemplateHookDomainsRaw(state.previewFontHookDomainsRaw)));
        if (!result.success && result.messageResId == R.string.quick_template_name_duplicate) {
            bindNameErrorState(false, R.string.quick_template_name_duplicate);
        }
        showToast(result.messageResId);
        if (result.success) {
            if (onUpdated != null) {
                onUpdated.run();
            }
            dialog.dismiss();
        }
    }

    private boolean refreshValidationUi() {
        boolean nameValid = !textOf(nameInputView).trim().isEmpty();
        boolean viewportValid = AppConfigInputValidation.isViewportInputValid(
                textOf(viewportInputView),
                AppConfigDialogBinder.resolveViewportMode(viewportModeToggle));
        boolean fontValid = AppConfigInputValidation.isFontScaleInputValid(textOf(fontInputView));
        bindNameErrorState(nameValid, R.string.quick_template_name_required);
        bindInputErrorState(viewportInputLayout, viewportValid);
        bindInputErrorState(fontInputLayout, fontValid);
        saveButton.setEnabled(nameValid && viewportValid && fontValid);
        refreshUnsavedBadge();
        return nameValid && viewportValid && fontValid;
    }

    private void bindNameErrorState(boolean valid, int messageResId) {
        if (valid) {
            nameInputLayout.setError(null);
            nameInputLayout.setErrorEnabled(false);
            return;
        }
        nameInputLayout.setErrorEnabled(true);
        nameInputLayout.setError(activity.getString(messageResId));
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
                normalizeText(textOf(nameInputView)),
                normalizeText(textOf(viewportInputView)),
                AppConfigDialogBinder.resolveViewportMode(viewportModeToggle),
                ViewportApplyMode.normalize(state != null ? state.viewportApplyMode : null),
                normalizeText(textOf(fontInputView)),
                AppConfigDialogBinder.resolveFontMode(fontModeToggle),
                normalizeText(state != null ? state.selectedTypefaceId : null),
                normalizeText(normalizeTemplateHookDomainsRaw(
                        state != null ? state.previewFontHookDomainsRaw : null)));
    }

    private void refreshHookDomainsButton() {
        HookDomainOverride override = HookDomainOverrideStore.fromRaw(state.previewFontHookDomainsRaw);
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
            public void clearDialogInputFocus(View fallbackFocusView,
                    TextInputEditText viewportInputView,
                    TextInputEditText fontInputView) {
            }

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
                    boolean previewFromGlobalPrefill,
                    String previewFontHookDomainsRaw) {
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
                    String fontMode,
                    String selectedTypefaceId,
                    String previewFontHookDomainsRaw,
                    String viewportScaleInput,
                    String viewportAbsoluteInput) {
                return new int[0];
            }

            @Override
            public void showToast(int messageResId) {
                QuickTemplateEditSheetDialog.this.showToast(messageResId);
            }

            @Override
            public DpiConfigStore getConfigStore() {
                return new DpiConfigStore(activity.getSharedPreferences(
                        DpiConfigStore.GROUP, Activity.MODE_PRIVATE));
            }

            @Override
            public void requestAppsLoad() {
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
