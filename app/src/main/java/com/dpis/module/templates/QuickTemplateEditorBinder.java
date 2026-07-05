package com.dpis.module.templates;

import com.dpis.module.appconfig.AppConfigDialogBinder;
import com.dpis.module.appconfig.AppConfigInputValidation;
import com.dpis.module.appconfig.AppConfigSaveHandler;

import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry;

import com.dpis.module.fonts.hookdomain.FontHookDomainPresentation;

import com.dpis.module.fonts.hookdomain.FontHookDomainDialog;

import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetType;

import com.dpis.module.applist.AppListItem;
import com.dpis.module.hooks.HookDomainOverride;
import com.dpis.module.hooks.HookDomainOverrideStore;

import com.dpis.module.*;




import com.dpis.module.ui.TouchFeedbackBinder;

import com.dpis.module.ui.DialogWindowSizer;

import com.dpis.module.appconfig.UnsavedBadgeBinder;

import com.dpis.module.appconfig.ConfigValueInputErrorBinder;

import com.dpis.module.ui.FormInputFocusBinder;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageButton;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import java.util.Set;

public final class QuickTemplateEditorBinder {
    public static QuickTemplateEditorBinder bind(
            Activity activity,
            View root,
            String templateId,
            Runnable onUpdated,
            Runnable onClose,
            boolean showSheetBadge,
            Draft initialDraft
    ) {
        QuickTemplateEditorBinder binder = new QuickTemplateEditorBinder(
                activity,
                root,
                templateId,
                onUpdated,
                onClose,
                showSheetBadge,
                initialDraft
        );
        return binder.ready ? binder : null;
    }

    public static QuickTemplateEditorBinder bind(
            Activity activity,
            View root,
            String templateId,
            Runnable onUpdated,
            Runnable onClose,
            boolean showSheetBadge
    ) {
        return bind(activity, root, templateId, onUpdated, onClose, showSheetBadge, null);
    }

    private static final String PREVIEW_PACKAGE_NAME = "__quick_template__";
    private final Activity activity;
    private final QuickTemplateSaveHandler saveHandler = new QuickTemplateSaveHandler();
    private final Runnable onUpdated;
    private final Runnable onClose;
    private final boolean showSheetBadge;
    private boolean ready;

    private QuickTemplateStore quickTemplateStore;
    private AppConfigDialogBinder typefaceBinder;
    private AppConfigDialogBinder.AppConfigDialogState state;
    private View rootView;

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
    private UnsavedBadgeBinder unsavedBadgeBinder;
    private String initialDraftSignature = "";

    private QuickTemplateEditorBinder(
            Activity activity,
            View root,
            String requestedTemplateId,
            Runnable onUpdated,
            Runnable onClose,
            boolean showSheetBadge,
            Draft initialDraft
    ) {
        this.activity = activity;
        this.onUpdated = onUpdated;
        this.onClose = onClose;
        this.showSheetBadge = showSheetBadge;
        SharedPreferences preferences = activity.getSharedPreferences(
                DpisConfigStore.GROUP, Activity.MODE_PRIVATE);
        quickTemplateStore = new QuickTemplateStore(preferences);
        typefaceBinder = new AppConfigDialogBinder(activity, createTypefaceHost());
        ready = resolveTemplate(requestedTemplateId);
        if (!ready) {
            showToast(R.string.quick_template_missing);
            return;
        }
        rootView = root;
        if (rootView == null) {
            ready = false;
            return;
        }
        bindViews(rootView);
        bindForm();
        if (initialDraft != null) {
            applyDraft(initialDraft);
        }
        bindActions();
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
        unsavedBadgeBinder = UnsavedBadgeBinder.bind(
                root, this::hasUnsavedChanges, showSheetBadge);
        titleView.setText(isNewTemplate
                ? R.string.quick_template_edit_page_title_new
                : R.string.quick_template_edit_page_title_edit);
        viewportApplyModeButton.setVisibility(View.GONE);
        deleteButton.setVisibility(isNewTemplate ? View.GONE : View.VISIBLE);
        footerResetButton.setVisibility(View.GONE);
        footerDeleteButton.setVisibility(View.GONE);
    }

    private void bindForm() {
        TemplateConfigValue value = originalTemplate != null
                ? originalTemplate.configValue
                : TemplateConfigValue.EMPTY;
        String storedName = originalTemplate != null ? originalTemplate.name : "";
        String initialName = storedName;
        String initialViewportType = value.initialViewportTargetType();
        String initialViewportInput = value.initialViewportInput();
        String initialViewportScaleInput = value.initialViewportScaleInput();
        String initialViewportAbsoluteInput = value.initialViewportAbsoluteInput();
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

    public Draft snapshotDraft() {
        return new Draft(
                textOf(nameInputView),
                textOf(viewportInputView),
                AppConfigDialogBinder.resolveViewportMode(viewportModeToggle),
                state != null ? state.viewportApplyMode : null,
                textOf(fontInputView),
                AppConfigDialogBinder.resolveFontMode(fontModeToggle),
                state != null ? state.selectedTypefaceId : null,
                state != null ? state.draftFontHookDomainsRaw : null
        );
    }

    public String currentTemplateId() {
        return templateId;
    }

    private void applyDraft(Draft draft) {
        nameInputView.setText(draft.nameInput);
        viewportInputView.setText(draft.viewportInput);
        fontInputView.setText(draft.fontInput);
        state.viewportApplyMode = ViewportApplyMode.normalize(draft.viewportApplyMode);
        state.selectedTypefaceId = draft.selectedTypefaceId;
        state.draftFontHookDomainsRaw = draft.draftFontHookDomainsRaw;
        state.clearViewportInputs();
        AppConfigDialogBinder.bindViewportModeToggle(
                viewportModeToggle,
                ViewportTargetType.normalize(draft.viewportMode),
                false
        );
        typefaceBinder.bindViewportInputHint(
                viewportInputLayout,
                ViewportTargetType.normalize(draft.viewportMode)
        );
        AppConfigDialogBinder.bindFontModeToggle(
                fontModeToggle,
                AppConfigInputValidation.initialFontMode(draft.fontMode),
                false
        );
        typefaceBinder.bindTypefaceSelector(typefaceSelectorButton, state.selectedTypefaceId);
        refreshHookDomainsButton();
        refreshValidationUi();
    }

    private void bindActions() {
        TouchFeedbackBinder.bindPressHaptic(typefaceSelectorButton);
        TouchFeedbackBinder.bindPressHaptic(hookDomainsButton);
        TouchFeedbackBinder.bindPressHaptic(resetButton);
        TouchFeedbackBinder.bindPressHaptic(deleteButton);
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
            resetTemplateConfig();
        });
        deleteButton.setOnClickListener(v -> {
            clearInputFocus();
            confirmDelete();
        });
        saveButton.setOnClickListener(v -> {
            clearInputFocus();
            saveTemplate();
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
        nameInputView.addTextChangedListener(watcher);
        viewportInputView.addTextChangedListener(watcher);
        fontInputView.addTextChangedListener(watcher);
    }

    private void bindInputFocusBehavior() {
        View scroll = rootView != null
                ? rootView.findViewById(R.id.quick_template_edit_scroll)
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
        nameInputView.setOnEditorActionListener(doneListener);
        viewportInputView.setOnEditorActionListener(doneListener);
        fontInputView.setOnEditorActionListener(doneListener);
        FormInputFocusBinder.bindDismissOnOutsideTouch(
                scroll,
                rootView,
                nameInputView,
                viewportInputView,
                fontInputView
        );
    }

    private void clearInputFocus() {
        FormInputFocusBinder.clearFocusAndHideIme(
                rootView,
                nameInputView,
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
                PREVIEW_PACKAGE_NAME,
                FontHookDomainRegistry.recommendedTemplateKnownDomains(),
                HookDomainOverrideStore.fromRaw(state.draftFontHookDomainsRaw),
                state.viewportApplyMode,
                FontApplyMode.FIELD_REWRITE.equals(
                        AppConfigDialogBinder.resolveFontMode(fontModeToggle)),
                this::refreshHookDomainsButton);
    }

    private void resetTemplateConfig() {
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
            close();
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
        state.updateViewportInput(
                AppConfigDialogBinder.resolveViewportMode(viewportModeToggle),
                textOf(viewportInputView));
        QuickTemplateSaveHandler.Result result = saveHandler.save(quickTemplateStore,
                new QuickTemplateSaveHandler.Request(
                        templateId,
                        name,
                        textOf(viewportInputView),
                        AppConfigDialogBinder.resolveViewportMode(viewportModeToggle),
                        state.viewportApplyMode,
                        state.viewportScaleInput,
                        state.viewportAbsoluteInput,
                        textOf(fontInputView),
                        AppConfigDialogBinder.resolveFontMode(fontModeToggle),
                        state.selectedTypefaceId,
                        normalizeTemplateHookDomainsRaw(state.draftFontHookDomainsRaw)));
        if (!result.success && result.messageResId == R.string.quick_template_name_duplicate) {
            bindNameErrorState(false, R.string.quick_template_name_duplicate);
        }
        showToast(result.messageResId);
        if (result.success) {
            if (onUpdated != null) {
                onUpdated.run();
            }
            close();
        }
    }

    private void close() {
        if (onClose != null) {
            onClose.run();
        }
    }

    private boolean refreshValidationUi() {
        boolean nameValid = !textOf(nameInputView).trim().isEmpty();
        boolean viewportValid = AppConfigInputValidation.isViewportInputValid(
                textOf(viewportInputView),
                AppConfigDialogBinder.resolveViewportMode(viewportModeToggle));
        boolean fontValid = AppConfigInputValidation.isFontScaleInputValid(textOf(fontInputView));
        bindNameErrorState(nameValid, R.string.quick_template_name_required);
        ConfigValueInputErrorBinder.bindFullMessage(viewportInputLayout, viewportValid);
        ConfigValueInputErrorBinder.bindFullMessage(fontInputLayout, fontValid);
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
                        state != null ? state.draftFontHookDomainsRaw : null)));
    }

    private void refreshHookDomainsButton() {
        hookDomainsButton.setText(FontHookDomainPresentation
                .forRecommendedTemplateRaw(state.draftFontHookDomainsRaw)
                .buttonText(activity));
    }

    private static String normalizeTemplateHookDomainsRaw(String raw) {
        return FontHookDomainPresentation.forRecommendedTemplateRaw(raw).normalizedRawOrNull();
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
            public AppConfigSaveHandler.Result saveAppConfig(View dialogView,
                    AppListItem item,
                    boolean dpisEnabled,
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
                return AppConfigSaveHandler.Result.failure(0);
            }

            @Override
            public void showToast(int messageResId) {
                QuickTemplateEditorBinder.this.showToast(messageResId);
            }

            @Override
            public DpisConfigStore getConfigStore() {
                return new DpisConfigStore(activity.getSharedPreferences(
                        DpisConfigStore.GROUP, Activity.MODE_PRIVATE));
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

    public static final class Draft {
        public final String nameInput;
        public final String viewportInput;
        public final String viewportMode;
        public final String viewportApplyMode;
        public final String fontInput;
        public final String fontMode;
        public final String selectedTypefaceId;
        public final String draftFontHookDomainsRaw;

        public Draft(
                String nameInput,
                String viewportInput,
                String viewportMode,
                String viewportApplyMode,
                String fontInput,
                String fontMode,
                String selectedTypefaceId,
                String draftFontHookDomainsRaw
        ) {
            this.nameInput = nameInput != null ? nameInput : "";
            this.viewportInput = viewportInput != null ? viewportInput : "";
            this.viewportMode = ViewportTargetType.normalize(viewportMode);
            this.viewportApplyMode = ViewportApplyMode.normalize(viewportApplyMode);
            this.fontInput = fontInput != null ? fontInput : "";
            this.fontMode = FontApplyMode.normalize(fontMode);
            this.selectedTypefaceId = selectedTypefaceId;
            this.draftFontHookDomainsRaw = draftFontHookDomainsRaw;
        }
    }
}
