package com.dpis.module;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import java.util.LinkedHashSet;
import java.util.Set;

public final class QuickTemplateEditActivity extends LocalizedActivity {
    static final String EXTRA_TEMPLATE_ID = "quick_template_edit.template_id";

    private static final String PREVIEW_PACKAGE_NAME = "__quick_template__";
    private static final String STATE_TEMPLATE_ID = "quick_template_edit.template_id";
    private static final String STATE_NAME_INPUT = "quick_template_edit.name_input";
    private static final String STATE_VIEWPORT_TARGET_TYPE = "quick_template_edit.viewport_target_type";
    private static final String STATE_VIEWPORT_INPUT = "quick_template_edit.viewport_input";
    private static final String STATE_VIEWPORT_SCALE_INPUT = "quick_template_edit.viewport_scale_input";
    private static final String STATE_VIEWPORT_ABSOLUTE_INPUT = "quick_template_edit.viewport_absolute_input";
    private static final String STATE_VIEWPORT_APPLY_MODE = "quick_template_edit.viewport_apply_mode";
    private static final String STATE_FONT_INPUT = "quick_template_edit.font_input";
    private static final String STATE_FONT_MODE = "quick_template_edit.font_mode";
    private static final String STATE_TYPEFACE_ID = "quick_template_edit.typeface_id";
    private static final String STATE_FONT_HOOK_DOMAINS = "quick_template_edit.font_hook_domains";

    private final QuickTemplateSaveHandler saveHandler = new QuickTemplateSaveHandler();

    private QuickTemplateStore quickTemplateStore;
    private AppConfigDialogBinder typefaceBinder;
    private AppConfigDialogBinder.AppConfigDialogState state;

    private String templateId;
    private boolean isNewTemplate;
    private QuickTemplateStore.QuickTemplate originalTemplate;

    private View toolbar;
    private MaterialTextView titleView;
    private TextInputLayout nameInputLayout;
    private TextInputEditText nameInputView;
    private TextInputLayout viewportInputLayout;
    private TextInputEditText viewportInputView;
    private TextInputLayout fontInputLayout;
    private TextInputEditText fontInputView;
    private AppConfigDialogBinder.ModeToggle viewportModeToggle;
    private AppConfigDialogBinder.ModeToggle fontModeToggle;
    private MaterialButton viewportApplyModeButton;
    private MaterialButton typefaceSelectorButton;
    private MaterialButton hookDomainsButton;
    private MaterialButton deleteButton;
    private MaterialButton saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_template_edit);
        SharedPreferences preferences = getSharedPreferences(
                DpiConfigStore.GROUP, MODE_PRIVATE);
        quickTemplateStore = new QuickTemplateStore(preferences);
        typefaceBinder = new AppConfigDialogBinder(this, createTypefaceHost());
        if (!resolveTemplate(savedInstanceState)) {
            showToast(R.string.quick_template_missing);
            finish();
            return;
        }
        bindViews();
        bindToolbar();
        applyInsets();
        bindForm(savedInstanceState);
        bindActions();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        String viewportTargetType = AppConfigDialogBinder.resolveViewportMode(viewportModeToggle);
        if (state != null) {
            state.updateViewportInput(viewportTargetType, viewportInputView.getText());
            outState.putString(STATE_VIEWPORT_SCALE_INPUT, state.viewportScaleInput);
            outState.putString(STATE_VIEWPORT_ABSOLUTE_INPUT, state.viewportAbsoluteInput);
            outState.putString(STATE_VIEWPORT_APPLY_MODE, state.viewportApplyMode);
            outState.putString(STATE_TYPEFACE_ID, state.selectedTypefaceId);
            outState.putString(STATE_FONT_HOOK_DOMAINS, state.previewFontHookDomainsRaw);
        }
        outState.putString(STATE_TEMPLATE_ID, templateId);
        outState.putString(STATE_NAME_INPUT, textOf(nameInputView));
        outState.putString(STATE_VIEWPORT_TARGET_TYPE, viewportTargetType);
        outState.putString(STATE_VIEWPORT_INPUT, textOf(viewportInputView));
        outState.putString(STATE_FONT_INPUT, textOf(fontInputView));
        outState.putString(STATE_FONT_MODE, AppConfigDialogBinder.resolveFontMode(fontModeToggle));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (state != null) {
            typefaceBinder.bindTypefaceSelector(typefaceSelectorButton, state.selectedTypefaceId);
            refreshHookDomainsButton();
        }
    }

    private boolean resolveTemplate(Bundle savedInstanceState) {
        String restoredId = savedInstanceState != null
                ? savedInstanceState.getString(STATE_TEMPLATE_ID)
                : null;
        String extraId = getIntent() != null
                ? getIntent().getStringExtra(EXTRA_TEMPLATE_ID)
                : null;
        boolean editRequested = extraId != null && !extraId.trim().isEmpty();
        String requestedId = editRequested ? extraId : restoredId;
        if (requestedId != null && !requestedId.trim().isEmpty()) {
            originalTemplate = quickTemplateStore.read(requestedId);
        }
        if (originalTemplate != null) {
            templateId = originalTemplate.id;
            isNewTemplate = false;
            return true;
        }
        if (editRequested) {
            return false;
        }
        templateId = requestedId != null && !requestedId.trim().isEmpty()
                ? requestedId.trim()
                : quickTemplateStore.newTemplateId();
        isNewTemplate = true;
        return true;
    }

    private void bindViews() {
        toolbar = findViewById(R.id.quick_template_edit_toolbar);
        titleView = findViewById(R.id.quick_template_edit_title);
        nameInputLayout = findViewById(R.id.quick_template_edit_name_layout);
        nameInputView = findViewById(R.id.quick_template_edit_name_input);
        viewportInputLayout = findViewById(R.id.quick_template_edit_viewport_input_layout);
        viewportInputView = findViewById(R.id.quick_template_edit_viewport_input);
        fontInputLayout = findViewById(R.id.quick_template_edit_font_scale_input_layout);
        fontInputView = findViewById(R.id.quick_template_edit_font_scale_input);
        viewportModeToggle = new AppConfigDialogBinder.ModeToggle(
                findViewById(R.id.quick_template_edit_viewport_mode_toggle_button),
                findViewById(R.id.quick_template_edit_viewport_mode_toggle_thumb),
                findViewById(R.id.quick_template_edit_viewport_mode_system_label),
                findViewById(R.id.quick_template_edit_viewport_mode_compat_label));
        fontModeToggle = new AppConfigDialogBinder.ModeToggle(
                findViewById(R.id.quick_template_edit_font_mode_toggle_button),
                findViewById(R.id.quick_template_edit_font_mode_toggle_thumb),
                findViewById(R.id.quick_template_edit_font_mode_system_label),
                findViewById(R.id.quick_template_edit_font_mode_compat_label));
        viewportApplyModeButton = findViewById(R.id.quick_template_edit_viewport_apply_mode_button);
        typefaceSelectorButton = findViewById(R.id.quick_template_edit_typeface_selector_button);
        hookDomainsButton = findViewById(R.id.quick_template_edit_font_hook_domains_button);
        deleteButton = findViewById(R.id.quick_template_edit_delete_button);
        saveButton = findViewById(R.id.quick_template_edit_save_button);
        titleView.setText(isNewTemplate
                ? R.string.quick_template_edit_page_title_new
                : R.string.quick_template_edit_page_title_edit);
        deleteButton.setVisibility(isNewTemplate ? View.GONE : View.VISIBLE);
    }

    private void bindToolbar() {
        AppCompatImageButton backButton = findViewById(R.id.quick_template_edit_back_button);
        TouchFeedbackBinder.bindPressHaptic(backButton);
        backButton.setOnClickListener(v -> finish());
    }

    private void applyInsets() {
        final int baseTopPadding = toolbar.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (view, insets) -> {
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            view.setPadding(view.getPaddingLeft(), baseTopPadding + statusBars.top,
                    view.getPaddingRight(), view.getPaddingBottom());
            return insets;
        });
        View content = findViewById(R.id.quick_template_edit_scroll);
        final int baseBottomPadding = content.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
            Insets navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
                    view.getPaddingRight(), baseBottomPadding + navigationBars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(toolbar);
        ViewCompat.requestApplyInsets(content);
    }

    private void bindForm(Bundle savedInstanceState) {
        TemplateConfigValue value = originalTemplate != null
                ? originalTemplate.configValue
                : TemplateConfigValue.EMPTY;
        String storedName = originalTemplate != null ? originalTemplate.name : "";
        String initialName = savedInstanceState != null
                ? savedInstanceState.getString(STATE_NAME_INPUT, storedName)
                : storedName;
        String storedViewportInput = AppConfigInputValidation.formatViewportInput(
                value.viewportTargetSpec);
        String initialViewportType = savedInstanceState != null
                ? ViewportTargetType.normalize(savedInstanceState.getString(
                        STATE_VIEWPORT_TARGET_TYPE,
                        AppConfigInputValidation.initialViewportTargetType(value.viewportTargetSpec)))
                : AppConfigInputValidation.initialViewportTargetType(value.viewportTargetSpec);
        String initialViewportInput = savedInstanceState != null
                ? savedInstanceState.getString(STATE_VIEWPORT_INPUT, storedViewportInput)
                : storedViewportInput;
        String initialViewportScaleInput = savedInstanceState != null
                ? savedInstanceState.getString(STATE_VIEWPORT_SCALE_INPUT, "")
                : (value.viewportTargetSpec.isRelativeScale() ? storedViewportInput : "");
        String initialViewportAbsoluteInput = savedInstanceState != null
                ? savedInstanceState.getString(STATE_VIEWPORT_ABSOLUTE_INPUT, "")
                : (value.viewportTargetSpec.isAbsoluteDp() ? storedViewportInput : "");
        String initialViewportApplyMode = savedInstanceState != null
                ? savedInstanceState.getString(STATE_VIEWPORT_APPLY_MODE, value.viewportApplyMode)
                : value.viewportApplyMode;
        String initialFontInput = savedInstanceState != null
                ? savedInstanceState.getString(STATE_FONT_INPUT, "")
                : (value.fontScalePercent != null ? String.valueOf(value.fontScalePercent) : "");
        String initialFontMode = savedInstanceState != null
                ? savedInstanceState.getString(STATE_FONT_MODE, value.fontApplyMode)
                : value.fontApplyMode;
        String initialTypefaceId = restoredNullableString(
                savedInstanceState, STATE_TYPEFACE_ID, value.typefaceId);
        String initialHookDomainsRaw = restoredNullableString(
                savedInstanceState, STATE_FONT_HOOK_DOMAINS, value.fontHookDomainsRaw);
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
        refreshViewportApplyModeButton();
        typefaceBinder.bindTypefaceSelector(typefaceSelectorButton, state.selectedTypefaceId);
        refreshHookDomainsButton();
        refreshValidationUi();
    }

    private void bindActions() {
        TouchFeedbackBinder.bindPressHaptic(typefaceSelectorButton);
        TouchFeedbackBinder.bindPressHaptic(viewportApplyModeButton);
        TouchFeedbackBinder.bindPressHaptic(hookDomainsButton);
        TouchFeedbackBinder.bindPressHaptic(deleteButton);
        TouchFeedbackBinder.bindPressHaptic(saveButton);

        viewportModeToggle.container.setOnClickListener(v -> {
            AppConfigDialogBinder.toggleViewportMode(
                    viewportModeToggle, viewportInputView, state);
            typefaceBinder.bindViewportInputHint(
                    viewportInputLayout,
                    AppConfigDialogBinder.resolveViewportMode(viewportModeToggle));
            refreshValidationUi();
        });
        fontModeToggle.container.setOnClickListener(v -> {
            AppConfigDialogBinder.toggleFontMode(fontModeToggle);
            refreshValidationUi();
        });
        typefaceSelectorButton.setOnClickListener(v -> typefaceBinder.showTypefaceSelector(
                typefaceSelectorButton,
                state,
                this::refreshValidationUi));
        viewportApplyModeButton.setOnClickListener(v -> showViewportApplyModeDialog());
        hookDomainsButton.setOnClickListener(v -> showHookDomainsDialog());
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
        FontHookDomainDialog.show(this,
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
                        return true;
                    }

                    @Override
                    public boolean restoreRecommended(String packageName) {
                        state.previewFontHookDomainsRaw = null;
                        refreshHookDomainsButton();
                        return true;
                    }

                    @Override
                    public boolean saveViewportApplyMode(String packageName, String mode) {
                        state.viewportApplyMode = ViewportApplyMode.normalize(mode);
                        refreshViewportApplyModeButton();
                        refreshValidationUi();
                        return true;
                    }
                },
                PREVIEW_PACKAGE_NAME,
                new LinkedHashSet<>(),
                HookDomainOverrideStore.fromRaw(state.previewFontHookDomainsRaw),
                state.viewportApplyMode,
                this::refreshHookDomainsButton);
    }

    private void showViewportApplyModeDialog() {
        String[] modes = {
                ViewportApplyMode.AUTO,
                ViewportApplyMode.SYSTEM,
                ViewportApplyMode.COMPAT
        };
        CharSequence[] labels = {
                getString(R.string.dialog_viewport_apply_auto),
                getString(R.string.dialog_viewport_apply_system),
                getString(R.string.dialog_viewport_apply_compat)
        };
        int checkedIndex = 0;
        String currentMode = normalizeViewportApplyModeForDisplay(state.viewportApplyMode);
        for (int index = 0; index < modes.length; index++) {
            if (modes[index].equals(currentMode)) {
                checkedIndex = index;
                break;
            }
        }
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_viewport_apply_strategy_title)
                .setSingleChoiceItems(labels, checkedIndex, (selectedDialog, which) -> {
                    state.viewportApplyMode = modes[which];
                    refreshViewportApplyModeButton();
                    refreshValidationUi();
                    selectedDialog.dismiss();
                })
                .create();
        dialog.show();
    }

    private void confirmDelete() {
        if (originalTemplate == null) {
            return;
        }
        String displayName = originalTemplate.name;
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.quick_template_delete_title)
                .setMessage(getString(R.string.quick_template_delete_message, displayName))
                .setNegativeButton(R.string.dialog_process_action_confirm_negative, null)
                .setPositiveButton(R.string.font_library_delete_action,
                        (unusedDialog, which) -> deleteTemplate())
                .create();
        dialog.setOnShowListener(d -> {
            TouchFeedbackBinder.bindPressHaptic(
                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE));
            TouchFeedbackBinder.bindPressHaptic(
                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE));
        });
        dialog.show();
    }

    private void deleteTemplate() {
        if (originalTemplate == null) {
            return;
        }
        if (quickTemplateStore.delete(originalTemplate.id)) {
            showToast(R.string.quick_template_delete_success);
            finish();
            return;
        }
        showToast(R.string.quick_template_delete_failed);
    }

    private void saveTemplate() {
        String name = textOf(nameInputView).trim();
        if (TextUtils.isEmpty(name)) {
            nameInputLayout.setError(getString(R.string.quick_template_name_required));
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
                        state.previewFontHookDomainsRaw));
        showToast(result.messageResId);
        if (result.success) {
            finish();
        }
    }

    private boolean refreshValidationUi() {
        boolean nameValid = !textOf(nameInputView).trim().isEmpty();
        boolean viewportValid = AppConfigInputValidation.isViewportInputValid(
                textOf(viewportInputView),
                AppConfigDialogBinder.resolveViewportMode(viewportModeToggle));
        boolean fontValid = AppConfigInputValidation.isFontScaleInputValid(textOf(fontInputView));
        nameInputLayout.setError(nameValid
                ? null
                : getString(R.string.quick_template_name_required));
        viewportInputLayout.setError(viewportValid ? null : getString(R.string.status_save_invalid));
        fontInputLayout.setError(fontValid ? null : getString(R.string.status_save_invalid));
        saveButton.setEnabled(nameValid && viewportValid && fontValid);
        return nameValid && viewportValid && fontValid;
    }

    private void refreshHookDomainsButton() {
        HookDomainOverride override = HookDomainOverrideStore.fromRaw(state.previewFontHookDomainsRaw);
        if (!override.customPathEnabled) {
            hookDomainsButton.setText(R.string.dialog_font_hook_domains_title);
            return;
        }
        int selectedCount = FontHookDomainRegistry.orderedCustomizableDisplaySubset(
                override.enabledKnownDomains).size();
        int totalCount = FontHookDomainRegistry.orderedCustomizableDisplayIdsList().size();
        hookDomainsButton.setText(getString(
                R.string.dialog_font_hook_domains_title_with_count,
                selectedCount,
                totalCount));
    }

    private void refreshViewportApplyModeButton() {
        if (viewportApplyModeButton == null || state == null) {
            return;
        }
        viewportApplyModeButton.setText(getString(
                R.string.quick_template_viewport_apply_mode_value,
                viewportApplyModeLabel(state.viewportApplyMode)));
    }

    private String viewportApplyModeLabel(String mode) {
        String normalized = normalizeViewportApplyModeForDisplay(mode);
        if (ViewportApplyMode.SYSTEM.equals(normalized)) {
            return getString(R.string.dialog_viewport_apply_system);
        }
        if (ViewportApplyMode.COMPAT.equals(normalized)) {
            return getString(R.string.dialog_viewport_apply_compat);
        }
        return getString(R.string.dialog_viewport_apply_auto);
    }

    private static String normalizeViewportApplyModeForDisplay(String mode) {
        String normalized = ViewportApplyMode.normalize(mode);
        return ViewportApplyMode.isEnabled(normalized)
                ? normalized
                : ViewportApplyMode.AUTO;
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
                return getString(R.string.dialog_font_hook_domains_title);
            }

            @Override
            public void openTypefaceLibrary() {
                startActivity(new Intent(QuickTemplateEditActivity.this, FontLibraryActivity.class));
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
                return new int[] { 0, R.string.status_save_invalid };
            }

            @Override
            public void showToast(int messageResId) {
                QuickTemplateEditActivity.this.showToast(messageResId);
            }
        };
    }

    private void showToast(int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

    private static String textOf(TextInputEditText view) {
        return view.getText() != null ? view.getText().toString() : "";
    }

    private static String restoredNullableString(Bundle savedInstanceState,
            String key,
            String fallback) {
        if (savedInstanceState == null || !savedInstanceState.containsKey(key)) {
            return fallback;
        }
        return savedInstanceState.getString(key);
    }
}
