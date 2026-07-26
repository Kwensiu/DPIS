package com.dpis.module.templates;

import com.dpis.module.appconfig.AppConfigInputValidation;
import com.dpis.module.fonts.FontApplyMode;
import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetType;

/** Cross-configuration draft seed shared by the Compose template editor surfaces. */
public final class TemplateEditorDraft {
    public final boolean quickTemplate;
    public final String nameInput;
    public final String viewportInput;
    public final String viewportMode;
    public final String viewportApplyMode;
    public final String viewportScaleInput;
    public final String viewportAbsoluteInput;
    public final String fontInput;
    public final String fontMode;
    public final String selectedTypefaceId;
    public final String draftFontHookDomainsRaw;

    public TemplateEditorDraft(
            boolean quickTemplate,
            String nameInput,
            String viewportInput,
            String viewportMode,
            String viewportApplyMode,
            String viewportScaleInput,
            String viewportAbsoluteInput,
            String fontInput,
            String fontMode,
            String selectedTypefaceId,
            String draftFontHookDomainsRaw
    ) {
        this.quickTemplate = quickTemplate;
        this.nameInput = text(nameInput);
        this.viewportInput = text(viewportInput);
        this.viewportMode = ViewportTargetType.normalize(viewportMode);
        this.viewportApplyMode = ViewportApplyMode.normalize(viewportApplyMode);
        this.viewportScaleInput = text(viewportScaleInput);
        this.viewportAbsoluteInput = text(viewportAbsoluteInput);
        this.fontInput = text(fontInput);
        this.fontMode = AppConfigInputValidation.initialFontMode(fontMode);
        this.selectedTypefaceId = selectedTypefaceId;
        this.draftFontHookDomainsRaw = draftFontHookDomainsRaw;
    }

    private static String text(String value) {
        return value != null ? value : "";
    }
}
