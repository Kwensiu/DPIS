package com.dpis.module.templates;

import com.dpis.module.appconfig.AppConfigInputValidation;
import com.dpis.module.fonts.FontApplyMode;
import com.dpis.module.fonts.hookdomain.FontHookDomainPresentation;
import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetType;

/**
 * View-independent draft for the two template editors.
 *
 * It intentionally retains both viewport drafts through a target-type change.
 * The active input is presentation state; both saved drafts are data that must
 * survive a sheet/detail handoff and later save operation.
 */
public final class TemplateEditorForm {
    public final boolean quickTemplate;
    public String templateId;
    public boolean newTemplate;
    private String initialSignature;

    public String nameInput;
    public String viewportInput;
    public String viewportMode;
    public String viewportApplyMode;
    public String viewportScaleInput;
    public String viewportAbsoluteInput;
    public String fontInput;
    public String fontMode;
    public String selectedTypefaceId;
    public String fontHookDomainsRaw;

    private TemplateEditorForm(boolean quickTemplate, String templateId, boolean newTemplate,
            String nameInput, TemplateConfigValue value) {
        this.quickTemplate = quickTemplate;
        this.templateId = templateId;
        this.newTemplate = newTemplate;
        this.nameInput = text(nameInput);
        applyValue(value != null ? value : TemplateConfigValue.EMPTY);
        initialSignature = signature();
    }

    public static TemplateEditorForm global(TemplateConfigValue value) {
        return new TemplateEditorForm(false, null, false, "", value);
    }

    public static TemplateEditorForm quick(QuickTemplateStore.QuickTemplate template,
            String newTemplateId) {
        if (template == null) {
            return new TemplateEditorForm(true, newTemplateId, true, "", TemplateConfigValue.EMPTY);
        }
        return new TemplateEditorForm(true, template.id, false, template.name, template.configValue);
    }

    /** Restores an unsaved cross-surface draft without treating it as a freshly saved form. */
    public static TemplateEditorForm restore(boolean quickTemplate, String templateId,
            boolean newTemplate, String nameInput, String viewportInput, String viewportMode,
            String viewportApplyMode, String viewportScaleInput, String viewportAbsoluteInput,
            String fontInput, String fontMode, String selectedTypefaceId, String fontHookDomainsRaw,
            String initialSignature) {
        TemplateEditorForm form = new TemplateEditorForm(
                quickTemplate, templateId, newTemplate, nameInput, TemplateConfigValue.EMPTY);
        form.viewportInput = text(viewportInput);
        form.viewportMode = ViewportTargetType.normalize(viewportMode);
        form.viewportApplyMode = ViewportApplyMode.normalize(viewportApplyMode);
        form.viewportScaleInput = text(viewportScaleInput);
        form.viewportAbsoluteInput = text(viewportAbsoluteInput);
        form.fontInput = text(fontInput);
        form.fontMode = AppConfigInputValidation.initialFontMode(fontMode);
        form.selectedTypefaceId = selectedTypefaceId;
        form.fontHookDomainsRaw = fontHookDomainsRaw;
        form.initialSignature = initialSignature != null ? initialSignature : form.signature();
        return form;
    }

    public void applyValue(TemplateConfigValue value) {
        viewportMode = ViewportTargetType.normalize(value.initialViewportTargetType());
        viewportInput = text(value.initialViewportInput());
        viewportScaleInput = text(value.initialViewportScaleInput());
        viewportAbsoluteInput = text(value.initialViewportAbsoluteInput());
        viewportApplyMode = ViewportApplyMode.normalize(value.viewportApplyMode);
        fontInput = value.fontScalePercent != null ? String.valueOf(value.fontScalePercent) : "";
        // Persisted OFF means there is no font override. The legacy editor presents that default
        // state as System mode, which keeps an empty system-mode draft saveable and stable.
        fontMode = AppConfigInputValidation.initialFontMode(value.fontApplyMode);
        selectedTypefaceId = value.typefaceId;
        fontHookDomainsRaw = value.fontHookDomainsRaw;
    }

    public void applyDraft(TemplateEditorDraft draft) {
        if (draft == null || draft.quickTemplate != quickTemplate) return;
        nameInput = text(draft.nameInput);
        viewportInput = text(draft.viewportInput);
        viewportMode = ViewportTargetType.normalize(draft.viewportMode);
        viewportApplyMode = ViewportApplyMode.normalize(draft.viewportApplyMode);
        viewportScaleInput = text(draft.viewportScaleInput);
        viewportAbsoluteInput = text(draft.viewportAbsoluteInput);
        fontInput = text(draft.fontInput);
        fontMode = AppConfigInputValidation.initialFontMode(draft.fontMode);
        selectedTypefaceId = draft.selectedTypefaceId;
        fontHookDomainsRaw = draft.draftFontHookDomainsRaw;
    }

    public void switchViewportMode(String mode) {
        String next = ViewportTargetType.normalize(mode);
        updateActiveViewportDraft();
        viewportMode = next;
        viewportInput = ViewportTargetType.ABSOLUTE_DP.equals(next)
                ? viewportAbsoluteInput : viewportScaleInput;
    }

    public void updateActiveViewportDraft() {
        if (ViewportTargetType.ABSOLUTE_DP.equals(viewportMode)) {
            viewportAbsoluteInput = text(viewportInput);
        } else {
            viewportScaleInput = text(viewportInput);
        }
    }

    public void reset() {
        viewportInput = "";
        viewportMode = ViewportTargetType.RELATIVE_SCALE;
        viewportApplyMode = ViewportApplyMode.OFF;
        viewportScaleInput = "";
        viewportAbsoluteInput = "";
        fontInput = "";
        fontMode = FontApplyMode.SYSTEM_EMULATION;
        selectedTypefaceId = null;
        fontHookDomainsRaw = null;
    }

    /** Marks the current draft as persisted and adopts the generated id for a newly created item. */
    public void markSaved(String savedTemplateId) {
        if (savedTemplateId != null && !savedTemplateId.trim().isEmpty()) {
            templateId = savedTemplateId.trim();
            newTemplate = false;
        }
        initialSignature = signature();
    }

    public boolean isValid() {
        return isNameValid() && isViewportInputValid() && isFontInputValid();
    }

    public boolean isNameValid() {
        return !quickTemplate || !nameInput.trim().isEmpty();
    }

    public boolean isViewportInputValid() {
        return AppConfigInputValidation.isViewportInputValid(viewportInput, viewportMode);
    }

    public boolean isFontInputValid() {
        return AppConfigInputValidation.isFontScaleInputValid(fontInput);
    }

    public boolean isDirty() {
        return !initialSignature.equals(signature());
    }

    public String initialSignature() {
        return initialSignature;
    }

    public TemplateEditorDraft globalDraft() {
        return new TemplateEditorDraft(false, "", viewportInput, viewportMode,
                viewportApplyMode, viewportScaleInput, viewportAbsoluteInput, fontInput,
                fontMode, selectedTypefaceId, fontHookDomainsRaw);
    }

    public TemplateEditorDraft quickDraft() {
        return new TemplateEditorDraft(true, nameInput, viewportInput, viewportMode,
                viewportApplyMode, viewportScaleInput, viewportAbsoluteInput, fontInput,
                fontMode, selectedTypefaceId, fontHookDomainsRaw);
    }

    private String signature() {
        return String.join("|", text(nameInput).trim(), text(viewportInput).trim(),
                ViewportTargetType.normalize(viewportMode),
                ViewportApplyMode.normalize(viewportApplyMode), text(viewportScaleInput).trim(),
                text(viewportAbsoluteInput).trim(), text(fontInput).trim(),
                FontApplyMode.normalize(fontMode), text(selectedTypefaceId).trim(),
                text(FontHookDomainPresentation.forRecommendedTemplateRaw(fontHookDomainsRaw)
                        .normalizedRawOrNull()).trim());
    }

    private static String text(String value) {
        return value != null ? value : "";
    }
}
