package com.dpis.module;

import com.google.android.material.textfield.TextInputEditText;

final class AppConfigSaveHandler {
    int[] save(AppListItem item,
            TextInputEditText viewportInput,
            TextInputEditText fontScaleInput,
            String viewportTargetType,
            String fontMode,
            String selectedTypefaceId,
            String viewportScaleInput,
            String viewportAbsoluteInput,
            boolean systemHooksEnabled,
            DpiConfigStore store,
            Runnable onChanged) {
        try {
            ViewportTargetSpec viewportTargetSpec = parseViewportTargetSpecOrNull(
                    viewportInput, viewportTargetType);
            Integer fontScalePercent = parseFontScalePercentOrNull(fontScaleInput);
            if (store == null) {
                return new int[] { 1, R.string.status_save_requires_init };
            }
            String viewportApplyMode = resolveViewportApplyModeForSave(
                    store, item.packageName, item.viewportMode, viewportTargetSpec);
            boolean viewportEmulationIneffective = viewportTargetSpec != null
                    && viewportTargetSpec.isEnabled()
                    && ViewportApplyMode.SYSTEM.equals(
                            ViewportApplyMode.normalize(viewportApplyMode))
                    && !ViewportApplyMode.SYSTEM.equals(
                            EffectiveModeResolver.resolveViewportMode(
                                    viewportApplyMode,
                                    systemHooksEnabled));
            boolean fontEmulationIneffective = fontScalePercent != null
                    && FontApplyMode.SYSTEM_EMULATION.equals(FontApplyMode.normalize(fontMode))
                    && !FontApplyMode.SYSTEM_EMULATION.equals(
                            EffectiveModeResolver.resolveFontMode(fontMode, systemHooksEnabled));
            boolean emulationRequestedWithoutSystemScope =
                    viewportEmulationIneffective || fontEmulationIneffective;
            boolean saved = true;
            int hint = 0;
            if (viewportTargetSpec == null || !viewportTargetSpec.isEnabled()) {
                saved = store.clearTargetViewportWidthDp(item.packageName) && saved;
                saved = store.setTargetViewportApplyMode(item.packageName, ViewportApplyMode.OFF)
                        && saved;
                ViewportPropertySyncer.clearTargetAsync(item.packageName);
            } else {
                saved = store.setTargetViewportSpec(item.packageName, viewportTargetSpec) && saved;
                saved = saveInactiveViewportDraft(
                        store,
                        item.packageName,
                        viewportTargetSpec,
                        viewportScaleInput,
                        viewportAbsoluteInput) && saved;
                saved = store.setTargetViewportApplyMode(item.packageName, viewportApplyMode)
                        && saved;
                ViewportPropertySyncer.publishTargetAsync(
                        item.packageName, viewportTargetSpec, viewportApplyMode);
            }
            if (fontScalePercent == null) {
                saved = store.clearTargetFontScalePercent(item.packageName) && saved;
                saved = store.setTargetFontApplyMode(item.packageName, FontApplyMode.OFF) && saved;
                FontRuntimePropertySyncer.clearFontScaleTargetAsync(item.packageName);
                FontHookDomainPropertySyncer.clearTargetAsync(item.packageName);
            } else {
                saved = store.setTargetFontScalePercent(item.packageName, fontScalePercent) && saved;
                saved = store.setTargetFontApplyMode(item.packageName, fontMode) && saved;
                FontRuntimePropertySyncer.publishTargetAsync(
                        item.packageName,
                        fontScalePercent,
                        fontMode,
                        FontHookDomainDecision.isHyperOsNativeFlutterEnabled(
                                store, item.packageName));
                FontHookDomainPropertySyncer.publishFromStoreAsync(item.packageName, store);
            }
            if (selectedTypefaceId == null || selectedTypefaceId.isBlank()) {
                saved = store.clearTargetTypefaceId(item.packageName) && saved;
                FontRuntimePropertySyncer.publishTypefaceTargetAsync(item.packageName, null);
            } else {
                saved = store.setTargetTypefaceId(item.packageName, selectedTypefaceId) && saved;
                FontRuntimePropertySyncer.publishTypefaceTargetAsync(item.packageName, selectedTypefaceId);
            }
            if (saved && onChanged != null) {
                onChanged.run();
            }
            if (saved && emulationRequestedWithoutSystemScope) {
                hint = R.string.system_mode_requires_system_scope_hint;
            }
            return new int[] { 1, hint };
        } catch (NumberFormatException exception) {
            return new int[] { 0, R.string.status_save_invalid };
        }
    }

    static String resolveViewportApplyModeForSave(DpiConfigStore store,
            String packageName,
            String itemViewportMode,
            ViewportTargetSpec viewportTargetSpec) {
        if (viewportTargetSpec == null || !viewportTargetSpec.isEnabled()) {
            return ViewportApplyMode.OFF;
        }
        String persistedMode = store != null
                ? store.getTargetViewportApplyMode(packageName)
                : ViewportApplyMode.OFF;
        if (ViewportApplyMode.isEnabled(persistedMode)) {
            return persistedMode;
        }
        return ViewportApplyMode.isEnabled(itemViewportMode)
                ? ViewportApplyMode.normalize(itemViewportMode)
                : ViewportApplyMode.AUTO;
    }

    private static boolean saveInactiveViewportDraft(DpiConfigStore store,
            String packageName,
            ViewportTargetSpec activeSpec,
            String viewportScaleInput,
            String viewportAbsoluteInput) {
        if (store == null || packageName == null || packageName.isBlank()
                || activeSpec == null || !activeSpec.isEnabled()) {
            return true;
        }
        if (activeSpec.isRelativeScale()) {
            ViewportDraftValue draft = parseViewportWidthDraft(viewportAbsoluteInput);
            if (!draft.valid) {
                return true;
            }
            return store.setTargetViewportWidthDraft(
                    packageName, draft.value);
        }
        if (activeSpec.isAbsoluteDp()) {
            ViewportDraftValue draft = parseViewportScalePermilleDraft(viewportScaleInput);
            if (!draft.valid) {
                return true;
            }
            return store.setTargetViewportScalePermilleDraft(
                    packageName, draft.value);
        }
        return true;
    }

    private static ViewportDraftValue parseViewportWidthDraft(String rawInput) {
        String raw = rawInput != null ? rawInput.trim() : "";
        if (raw.isEmpty()) {
            return ViewportDraftValue.valid(null);
        }
        Integer value = AppConfigInputValidation.parsePositiveIntOrNull(raw);
        return value != null ? ViewportDraftValue.valid(value) : ViewportDraftValue.invalid();
    }

    private static ViewportDraftValue parseViewportScalePermilleDraft(String rawInput) {
        String raw = rawInput != null ? rawInput.trim() : "";
        if (raw.isEmpty()) {
            return ViewportDraftValue.valid(null);
        }
        Integer value = AppConfigInputValidation.parsePositiveIntOrNull(raw);
        if (value == null
                || value < ViewportTargetSpec.MIN_SCALE_PERCENT
                || value > ViewportTargetSpec.MAX_SCALE_PERCENT) {
            return ViewportDraftValue.invalid();
        }
        return ViewportDraftValue.valid(value * 10);
    }

    private static final class ViewportDraftValue {
        final boolean valid;
        final Integer value;

        private ViewportDraftValue(boolean valid, Integer value) {
            this.valid = valid;
            this.value = value;
        }

        static ViewportDraftValue valid(Integer value) {
            return new ViewportDraftValue(true, value);
        }

        static ViewportDraftValue invalid() {
            return new ViewportDraftValue(false, null);
        }
    }

    private static ViewportTargetSpec parseViewportTargetSpecOrNull(TextInputEditText inputView,
                                                                    String viewportTargetType)
            throws NumberFormatException {
        String raw = inputView.getText() != null ? inputView.getText().toString().trim() : "";
        if (!AppConfigInputValidation.isViewportInputValid(raw, viewportTargetType)) {
            throw new NumberFormatException("invalid viewport target");
        }
        ViewportTargetSpec spec =
                AppConfigInputValidation.parseViewportTargetSpec(raw, viewportTargetType);
        return spec;
    }

    private static Integer parseFontScalePercentOrNull(TextInputEditText inputView)
            throws NumberFormatException {
        String raw = inputView.getText() != null ? inputView.getText().toString().trim() : "";
        if (raw.isEmpty()) {
            return null;
        }
        Integer value = AppConfigInputValidation.parseFontScalePercentOrNull(raw);
        if (value == null) {
            throw new NumberFormatException("invalid font scale");
        }
        return value;
    }
}
