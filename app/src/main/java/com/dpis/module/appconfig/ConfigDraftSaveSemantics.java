package com.dpis.module.appconfig;

import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;
import com.dpis.module.viewport.ViewportTargetType;

final class ConfigDraftSaveSemantics {
    private ConfigDraftSaveSemantics() {
    }

    static String viewportTargetTypeForSave(String viewportTargetType) {
        return ViewportTargetType.normalize(viewportTargetType);
    }

    static String viewportApplyModeForSave(String viewportApplyMode, ViewportTargetSpec spec) {
        if (spec == null || !spec.isEnabled()) {
            return ViewportApplyMode.OFF;
        }
        String normalized = ViewportApplyMode.normalize(viewportApplyMode);
        return ViewportApplyMode.isEnabled(normalized)
                ? normalized
                : ViewportApplyMode.AUTO;
    }

    static String fontApplyModeForSave(String fontMode) {
        return AppConfigInputValidation.initialFontMode(fontMode);
    }
}
