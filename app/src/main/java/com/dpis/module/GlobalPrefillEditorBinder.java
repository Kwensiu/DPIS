package com.dpis.module;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageButton;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Set;

final class GlobalPrefillEditorBinder {
    private static final String PREFILL_PACKAGE_NAME = "__global_prefill__";

    static GlobalPrefillEditorBinder bind(
            Activity activity,
            View root,
            Runnable onUpdated,
            Runnable onClose,
            boolean showSheetBadge,
            Draft initialDraft
    ) {
        GlobalPrefillEditorBinder binder = new GlobalPrefillEditorBinder(
                activity,
                root,
                onUpdated,
                onClose,
                showSheetBadge,
                initialDraft
        );
        return binder.ready ? binder : null;
    }

    static GlobalPrefillEditorBinder bind(
            Activity activity,
            View root,
            Runnable onUpdated,
            Runnable onClose,
            boolean showSheetBadge
    ) {
        return bind(activity, root, onUpdated, onClose, showSheetBadge, null);
    }

    private final Activity activity;
    private final GlobalPrefillSaveHandler saveHandler = new GlobalPrefillSaveHandler();
    private final Runnable onUpdated;
    private final Runnable onClose;
    private final boolean showSheetBadge;
    private boolean ready;

    private GlobalPrefillStore globalPrefillStore;
    private AppConfigDialogBinder typefaceBinder;
    private AppConfigDialogBinder.AppConfigDialogState state;
    private View rootView;

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
    private UnsavedBadgeBinder unsavedBadgeBinder;
    private String initialDraftSignature = "";

    private GlobalPrefillEditorBinder(
            Activity activity,
            View root,
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
                DpiConfigStore.GROUP, Activity.MODE_PRIVATE);
        globalPrefillStore = new GlobalPrefillStore(preferences);
        typefaceBinder = new AppConfigDialogBinder(activity, createTypefaceHost());
        rootView = root;
        if (rootView == null) {
            return;
        }
        bindViews(rootView);
        bindForm();
        if (initialDraft != null) {
            applyDraft(initialDraft);
        }
        bindActions();
        ready = true;
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
        unsavedBadgeBinder = UnsavedBadgeBinder.bind(
                root, this::hasUnsavedChanges, showSheetBadge);
        View footerResetButton = root.findViewById(R.id.template_config_reset_button);
        if (footerResetButton != null) {
            footerResetButton.setVisibility(View.GONE);
        }
    }

    private void bindForm() {
        TemplateConfigValue value = globalPrefillStore.read();
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

    Draft snapshotDraft() {
        return new Draft(
                textOf(viewportInputView),
                AppConfigDialogBinder.resolveViewportMode(viewportModeToggle),
                state != null ? state.viewportApplyMode : null,
                textOf(fontInputView),
                AppConfigDialogBinder.resolveFontMode(fontModeToggle),
                state != null ? state.selectedTypefaceId : null,
                state != null ? state.draftFontHookDomainsRaw : null
        );
    }

    private void applyDraft(Draft draft) {
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
        View scroll = rootView != null
                ? rootView.findViewById(R.id.global_prefill_scroll)
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
                rootView,
                viewportInputView,
                fontInputView
        );
    }

    private void clearInputFocus() {
        FormInputFocusBinder.clearFocusAndHideIme(
                rootView,
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
                FontApplyMode.FIELD_REWRITE.equals(
                        AppConfigDialogBinder.resolveFontMode(fontModeToggle)),
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
        state.updateViewportInput(
                AppConfigDialogBinder.resolveViewportMode(viewportModeToggle),
                textOf(viewportInputView));
        GlobalPrefillSaveHandler.Result result = saveHandler.save(globalPrefillStore,
                new GlobalPrefillSaveHandler.Request(
                        textOf(viewportInputView),
                        AppConfigDialogBinder.resolveViewportMode(viewportModeToggle),
                        state.viewportApplyMode,
                        state.viewportScaleInput,
                        state.viewportAbsoluteInput,
                        textOf(fontInputView),
                        AppConfigDialogBinder.resolveFontMode(fontModeToggle),
                        state.selectedTypefaceId,
                        normalizeTemplateHookDomainsRaw(state.draftFontHookDomainsRaw)));
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
        boolean viewportValid = AppConfigInputValidation.isViewportInputValid(
                textOf(viewportInputView),
                AppConfigDialogBinder.resolveViewportMode(viewportModeToggle));
        boolean fontValid = AppConfigInputValidation.isFontScaleInputValid(textOf(fontInputView));
        ConfigValueInputErrorBinder.bindFullMessage(viewportInputLayout, viewportValid);
        ConfigValueInputErrorBinder.bindFullMessage(fontInputLayout, fontValid);
        saveButton.setEnabled(viewportValid && fontValid);
        refreshUnsavedBadge();
        return viewportValid && fontValid;
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
                GlobalPrefillEditorBinder.this.showToast(messageResId);
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

    static final class Draft {
        final String viewportInput;
        final String viewportMode;
        final String viewportApplyMode;
        final String fontInput;
        final String fontMode;
        final String selectedTypefaceId;
        final String draftFontHookDomainsRaw;

        Draft(
                String viewportInput,
                String viewportMode,
                String viewportApplyMode,
                String fontInput,
                String fontMode,
                String selectedTypefaceId,
                String draftFontHookDomainsRaw
        ) {
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
