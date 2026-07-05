package com.dpis.module;

import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;
import com.dpis.module.viewport.ViewportTargetType;

final class ConfigDraftSaveSemantics {
    private ConfigDraftSaveSemantics() {
    }

    // TODO: If app config, landscape detail, quick template, and global prefill
    // keep growing together, introduce a shared editor-draft request/result type
    // here so handlers reuse one parser while keeping their own side effects.
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
