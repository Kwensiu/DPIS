package com.dpis.module;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.LinkedHashSet;
import java.util.Set;

public final class GlobalPrefillActivity extends LocalizedActivity {
    private static final String PREFILL_PACKAGE_NAME = "__global_prefill__";
    private static final String STATE_VIEWPORT_TARGET_TYPE = "global_prefill.viewport_target_type";
    private static final String STATE_VIEWPORT_INPUT = "global_prefill.viewport_input";
    private static final String STATE_VIEWPORT_SCALE_INPUT = "global_prefill.viewport_scale_input";
    private static final String STATE_VIEWPORT_ABSOLUTE_INPUT = "global_prefill.viewport_absolute_input";
    private static final String STATE_VIEWPORT_APPLY_MODE = "global_prefill.viewport_apply_mode";
    private static final String STATE_FONT_INPUT = "global_prefill.font_input";
    private static final String STATE_FONT_MODE = "global_prefill.font_mode";
    private static final String STATE_TYPEFACE_ID = "global_prefill.typeface_id";
    private static final String STATE_FONT_HOOK_DOMAINS = "global_prefill.font_hook_domains";

    private final GlobalPrefillSaveHandler saveHandler = new GlobalPrefillSaveHandler();

    private GlobalPrefillStore globalPrefillStore;
    private AppConfigDialogBinder typefaceBinder;
    private AppConfigDialogBinder.AppConfigDialogState state;

    private View toolbar;
    private TextInputLayout viewportInputLayout;
    private TextInputEditText viewportInputView;
    private TextInputLayout fontInputLayout;
    private TextInputEditText fontInputView;
    private AppConfigDialogBinder.ModeToggle viewportModeToggle;
    private AppConfigDialogBinder.ModeToggle fontModeToggle;
    private MaterialButton typefaceSelectorButton;
    private MaterialButton hookDomainsButton;
    private MaterialButton resetButton;
    private MaterialButton saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_global_prefill);
        SharedPreferences preferences = getSharedPreferences(
                DpiConfigStore.GROUP, MODE_PRIVATE);
        globalPrefillStore = new GlobalPrefillStore(preferences);
        typefaceBinder = new AppConfigDialogBinder(this, createTypefaceHost());
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

    private void bindViews() {
        toolbar = findViewById(R.id.global_prefill_toolbar);
        viewportInputLayout = findViewById(R.id.global_prefill_viewport_input_layout);
        viewportInputView = findViewById(R.id.global_prefill_viewport_input);
        fontInputLayout = findViewById(R.id.global_prefill_font_scale_input_layout);
        fontInputView = findViewById(R.id.global_prefill_font_scale_input);
        viewportModeToggle = new AppConfigDialogBinder.ModeToggle(
                findViewById(R.id.global_prefill_viewport_mode_toggle_button),
                findViewById(R.id.global_prefill_viewport_mode_toggle_thumb),
                findViewById(R.id.global_prefill_viewport_mode_system_label),
                findViewById(R.id.global_prefill_viewport_mode_compat_label));
        fontModeToggle = new AppConfigDialogBinder.ModeToggle(
                findViewById(R.id.global_prefill_font_mode_toggle_button),
                findViewById(R.id.global_prefill_font_mode_toggle_thumb),
                findViewById(R.id.global_prefill_font_mode_system_label),
                findViewById(R.id.global_prefill_font_mode_compat_label));
        typefaceSelectorButton = findViewById(R.id.global_prefill_typeface_selector_button);
        hookDomainsButton = findViewById(R.id.global_prefill_font_hook_domains_button);
        resetButton = findViewById(R.id.global_prefill_reset_button);
        saveButton = findViewById(R.id.global_prefill_save_button);
    }

    private void bindToolbar() {
        AppCompatImageButton backButton = findViewById(R.id.global_prefill_back_button);
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
        View content = findViewById(R.id.global_prefill_scroll);
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
        TemplateConfigValue value = globalPrefillStore.read();
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
        String initialTypefaceId = savedInstanceState != null
                ? savedInstanceState.getString(STATE_TYPEFACE_ID, value.typefaceId)
                : value.typefaceId;
        String initialHookDomainsRaw = savedInstanceState != null
                ? savedInstanceState.getString(STATE_FONT_HOOK_DOMAINS, value.fontHookDomainsRaw)
                : value.fontHookDomainsRaw;
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
        viewportInputView.setText(initialViewportInput);
        fontInputView.setText(initialFontInput);
        AppConfigDialogBinder.bindViewportModeToggle(
                viewportModeToggle, initialViewportType, false);
        typefaceBinder.bindViewportInputHint(viewportInputLayout, initialViewportType);
        AppConfigDialogBinder.bindFontModeToggle(
                fontModeToggle, AppConfigInputValidation.initialFontMode(initialFontMode), false);
        typefaceBinder.bindTypefaceSelector(typefaceSelectorButton, state.selectedTypefaceId);
        refreshHookDomainsButton();
        refreshValidationUi();
    }

    private void bindActions() {
        TouchFeedbackBinder.bindPressHaptic(typefaceSelectorButton);
        TouchFeedbackBinder.bindPressHaptic(hookDomainsButton);
        TouchFeedbackBinder.bindPressHaptic(resetButton);
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
        hookDomainsButton.setOnClickListener(v -> showHookDomainsDialog());
        resetButton.setOnClickListener(v -> resetGlobalPrefill());
        saveButton.setOnClickListener(v -> saveGlobalPrefill());

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
                        refreshValidationUi();
                        return true;
                    }
                },
                PREFILL_PACKAGE_NAME,
                new LinkedHashSet<>(),
                HookDomainOverrideStore.fromRaw(state.previewFontHookDomainsRaw),
                state.viewportApplyMode,
                this::refreshHookDomainsButton);
    }

    private void resetGlobalPrefill() {
        if (globalPrefillStore.clear()) {
            showToast(R.string.global_prefill_reset_success);
            finish();
            return;
        }
        showToast(R.string.global_prefill_reset_failed);
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
                        state.previewFontHookDomainsRaw));
        showToast(result.messageResId);
        if (result.success) {
            finish();
        }
    }

    private boolean refreshValidationUi() {
        boolean viewportValid = AppConfigInputValidation.isViewportInputValid(
                textOf(viewportInputView),
                AppConfigDialogBinder.resolveViewportMode(viewportModeToggle));
        boolean fontValid = AppConfigInputValidation.isFontScaleInputValid(textOf(fontInputView));
        viewportInputLayout.setError(viewportValid ? null : getString(R.string.status_save_invalid));
        fontInputLayout.setError(fontValid ? null : getString(R.string.status_save_invalid));
        saveButton.setEnabled(viewportValid && fontValid);
        return viewportValid && fontValid;
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
                startActivity(new Intent(GlobalPrefillActivity.this, FontLibraryActivity.class));
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
                GlobalPrefillActivity.this.showToast(messageResId);
            }
        };
    }

    private void showToast(int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

    private static String textOf(TextInputEditText view) {
        return view.getText() != null ? view.getText().toString() : "";
    }
}
