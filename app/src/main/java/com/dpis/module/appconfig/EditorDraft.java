package com.dpis.module.appconfig;

import com.dpis.module.appconfig.AppConfigInputValidation;
import com.dpis.module.applist.AppListItem;
import com.dpis.module.fonts.FontApplyMode;
import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetType;

import java.util.Objects;

/**
 * Cross-surface draft for the per-app editor.
 *
 * The draft is retained independently from portrait Sheet or landscape detail
 * presentation. Persisted package configuration remains owned by the existing
 * config store and save handler.
 */
public final class EditorDraft {

    public final String packageName;
    public final String viewportInput;
    public final String viewportScaleInput;
    public final String viewportAbsoluteInput;
    public final String viewportMode;
    public final String fontInput;
    public final String fontMode;
    public final String selectedTypefaceId;
    public final String draftFontHookDomainsRaw;
    public final String viewportApplyMode;
    public final boolean fontHookDomainsResetRequested;
    public final boolean viewportApplyModeResetRequested;
    public final String wechatDpiInput;
    public final boolean scopeSelected;
    public final boolean dpisEnabled;

    public EditorDraft(
            String packageName,
            String viewportInput,
            String viewportScaleInput,
            String viewportAbsoluteInput,
            String viewportMode,
            String fontInput,
            String fontMode,
            String selectedTypefaceId,
            String draftFontHookDomainsRaw,
            String viewportApplyMode,
            boolean fontHookDomainsResetRequested,
            boolean viewportApplyModeResetRequested,
            String wechatDpiInput,
            boolean scopeSelected,
            boolean dpisEnabled
    ) {
        this.packageName = packageName;
        this.viewportInput = valueOrEmpty(viewportInput);
        this.viewportScaleInput = valueOrEmpty(viewportScaleInput);
        this.viewportAbsoluteInput = valueOrEmpty(viewportAbsoluteInput);
        this.viewportMode = viewportMode;
        this.fontInput = fontInput;
        this.fontMode = fontMode;
        this.selectedTypefaceId = selectedTypefaceId;
        this.draftFontHookDomainsRaw = draftFontHookDomainsRaw;
        this.viewportApplyMode = ViewportApplyMode.normalize(viewportApplyMode);
        this.fontHookDomainsResetRequested = fontHookDomainsResetRequested;
        this.viewportApplyModeResetRequested = viewportApplyModeResetRequested;
        this.wechatDpiInput = wechatDpiInput;
        this.scopeSelected = scopeSelected;
        this.dpisEnabled = dpisEnabled;
    }

    public String viewportInputFor(String targetType) {
        return ViewportTargetType.ABSOLUTE_DP.equals(
                ViewportTargetType.normalize(targetType))
                        ? viewportAbsoluteInput
                        : viewportScaleInput;
    }

    public static EditorDraft fromItem(AppListItem item) {
        String targetType = AppConfigInputValidation.initialViewportTargetType(
                item.viewportTargetSpec);
        String viewportInput = AppConfigInputValidation.formatViewportInput(item.viewportTargetSpec);
        String scaleInput = item.viewportScaleMilliPercent != null
                ? AppConfigInputValidation.formatScaleMilliPercentInput(
                        item.viewportScaleMilliPercent)
                : (item.viewportTargetSpec.isRelativeScale() ? viewportInput : "");
        String absoluteInput = item.viewportWidthDp != null
                ? String.valueOf(item.viewportWidthDp)
                : (item.viewportTargetSpec.isAbsoluteDp() ? viewportInput : "");
        return new EditorDraft(
                item.packageName,
                viewportInput,
                scaleInput,
                absoluteInput,
                targetType,
                item.fontScalePercent != null ? String.valueOf(item.fontScalePercent) : "",
                AppConfigInputValidation.initialFontMode(item.fontMode),
                item.typefaceId,
                item.effectiveFontHookDomainsRaw(),
                item.viewportMode,
                false,
                false,
                item.wechatDpi != null ? String.valueOf(item.wechatDpi) : "",
                item.inScope,
                item.dpisEnabled
        );
    }

    public EditorDraft withViewportInput(String targetType, String value) {
        String normalized = ViewportTargetType.normalize(targetType);
        String scale = ViewportTargetType.ABSOLUTE_DP.equals(normalized)
                ? viewportScaleInput : valueOrEmpty(value);
        String absolute = ViewportTargetType.ABSOLUTE_DP.equals(normalized)
                ? valueOrEmpty(value) : viewportAbsoluteInput;
        return copy(scale, absolute, normalized, fontInput, fontMode, selectedTypefaceId,
                draftFontHookDomainsRaw, viewportApplyMode, fontHookDomainsResetRequested,
                viewportApplyModeResetRequested, wechatDpiInput, scopeSelected, dpisEnabled);
    }

    public EditorDraft withViewportMode(String targetType) {
        String normalized = ViewportTargetType.normalize(targetType);
        return copy(viewportScaleInput, viewportAbsoluteInput, normalized, fontInput, fontMode,
                selectedTypefaceId, draftFontHookDomainsRaw, viewportApplyMode,
                fontHookDomainsResetRequested, viewportApplyModeResetRequested, wechatDpiInput,
                scopeSelected, dpisEnabled);
    }

    public EditorDraft withFontInput(String value) {
        return copy(viewportScaleInput, viewportAbsoluteInput, viewportMode, value, fontMode,
                selectedTypefaceId, draftFontHookDomainsRaw, viewportApplyMode,
                fontHookDomainsResetRequested, viewportApplyModeResetRequested, wechatDpiInput,
                scopeSelected, dpisEnabled);
    }

    public EditorDraft withFontMode(String value) {
        return copy(viewportScaleInput, viewportAbsoluteInput, viewportMode, fontInput,
                AppConfigInputValidation.initialFontMode(value), selectedTypefaceId,
                draftFontHookDomainsRaw, viewportApplyMode, fontHookDomainsResetRequested,
                viewportApplyModeResetRequested, wechatDpiInput, scopeSelected, dpisEnabled);
    }

    public EditorDraft withWechatDpiInput(String value) {
        return copy(viewportScaleInput, viewportAbsoluteInput, viewportMode, fontInput, fontMode,
                selectedTypefaceId, draftFontHookDomainsRaw, viewportApplyMode,
                fontHookDomainsResetRequested, viewportApplyModeResetRequested, value,
                scopeSelected, dpisEnabled);
    }

    public EditorDraft withAdvancedConfig(
            String typefaceId,
            String hookDomainsRaw,
            String applyMode,
            boolean hookDomainsReset,
            boolean applyModeReset
    ) {
        return copy(viewportScaleInput, viewportAbsoluteInput, viewportMode, fontInput, fontMode,
                typefaceId, hookDomainsRaw, applyMode, hookDomainsReset, applyModeReset,
                wechatDpiInput, scopeSelected, dpisEnabled);
    }

    public EditorDraft cleared() {
        return copy("", "", ViewportTargetType.RELATIVE_SCALE, "",
                FontApplyMode.SYSTEM_EMULATION, null, null, ViewportApplyMode.OFF,
                true, true, "", scopeSelected, dpisEnabled);
    }

    public EditorDraft withScopeSelected(boolean value) {
        return copy(viewportScaleInput, viewportAbsoluteInput, viewportMode, fontInput, fontMode,
                selectedTypefaceId, draftFontHookDomainsRaw, viewportApplyMode,
                fontHookDomainsResetRequested, viewportApplyModeResetRequested, wechatDpiInput,
                value, dpisEnabled);
    }

    public EditorDraft withDpisEnabled(boolean value) {
        return copy(viewportScaleInput, viewportAbsoluteInput, viewportMode, fontInput, fontMode,
                selectedTypefaceId, draftFontHookDomainsRaw, viewportApplyMode,
                fontHookDomainsResetRequested, viewportApplyModeResetRequested, wechatDpiInput,
                scopeSelected, value);
    }

    /** Custom font Hook domains are meaningful only for the field-rewrite compatibility route. */
    public boolean fontHookDomainsEditable() {
        return FontApplyMode.FIELD_REWRITE.equals(FontApplyMode.normalize(fontMode));
    }

    /** Scope and DPIS toggles persist immediately; dirty tracks only values committed by Save. */
    public boolean hasSameSavedConfig(EditorDraft other) {
        return other != null
                && Objects.equals(packageName, other.packageName)
                && Objects.equals(viewportScaleInput, other.viewportScaleInput)
                && Objects.equals(viewportAbsoluteInput, other.viewportAbsoluteInput)
                && Objects.equals(viewportMode, other.viewportMode)
                && Objects.equals(fontInput, other.fontInput)
                && Objects.equals(fontMode, other.fontMode)
                && Objects.equals(selectedTypefaceId, other.selectedTypefaceId)
                && Objects.equals(draftFontHookDomainsRaw, other.draftFontHookDomainsRaw)
                && Objects.equals(viewportApplyMode, other.viewportApplyMode)
                && fontHookDomainsResetRequested == other.fontHookDomainsResetRequested
                && viewportApplyModeResetRequested == other.viewportApplyModeResetRequested
                && Objects.equals(wechatDpiInput, other.wechatDpiInput);
    }

    public EditorDraft afterSuccessfulSave() {
        return copy(viewportScaleInput, viewportAbsoluteInput, viewportMode, fontInput, fontMode,
                selectedTypefaceId, draftFontHookDomainsRaw, viewportApplyMode,
                fontHookDomainsResetRequested, false, wechatDpiInput,
                scopeSelected, dpisEnabled);
    }

    private EditorDraft copy(
            String scaleInput, String absoluteInput, String nextViewportMode, String nextFontInput,
            String nextFontMode, String nextTypefaceId, String nextHookDomains,
            String nextViewportApplyMode, boolean nextHookDomainsReset,
            boolean nextViewportModeReset, String nextWechatDpiInput,
            boolean nextScopeSelected, boolean nextDpisEnabled
    ) {
        return new EditorDraft(packageName, viewportInputFor(nextViewportMode), scaleInput,
                absoluteInput, nextViewportMode, nextFontInput, nextFontMode, nextTypefaceId,
                nextHookDomains, nextViewportApplyMode, nextHookDomainsReset,
                nextViewportModeReset, nextWechatDpiInput, nextScopeSelected, nextDpisEnabled);
    }

    private static String valueOrEmpty(String value) {
        return value != null ? value : "";
    }
}
