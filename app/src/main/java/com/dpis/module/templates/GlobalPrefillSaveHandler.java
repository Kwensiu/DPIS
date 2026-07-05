package com.dpis.module.templates;

import com.dpis.module.R;

public final class GlobalPrefillSaveHandler {
    public Result save(GlobalPrefillStore store, Request request) {
        if (store == null) {
            return Result.failure(R.string.status_save_requires_init);
        }
        if (request == null) {
            return Result.failure(R.string.status_save_invalid);
        }
        if (!TemplateCustomSemantics.isViewportInputValid(
                request.viewportInput, request.viewportTargetType)
                || !TemplateCustomSemantics.isFontScaleInputValid(request.fontScaleInput)) {
            return Result.failure(R.string.status_save_invalid);
        }

        TemplateConfigValue value = TemplateCustomSemantics.fromEditorDraft(
                request.viewportInput,
                request.viewportTargetType,
                request.viewportApplyMode,
                request.viewportScaleInput,
                request.viewportAbsoluteInput,
                request.fontScaleInput,
                request.fontApplyMode,
                request.selectedTypefaceId,
                request.fontHookDomainsRaw);
        return store.write(value)
                ? Result.success(R.string.global_prefill_save_success)
                : Result.failure(R.string.global_prefill_save_failed);
    }

    public static final class Request {
        public final String viewportInput;
        public final String viewportTargetType;
        public final String viewportApplyMode;
        public final String viewportScaleInput;
        public final String viewportAbsoluteInput;
        public final String fontScaleInput;
        public final String fontApplyMode;
        public final String selectedTypefaceId;
        public final String fontHookDomainsRaw;

        public Request(String viewportInput,
                String viewportTargetType,
                String viewportApplyMode,
                String fontScaleInput,
                String fontApplyMode,
                String selectedTypefaceId,
                String fontHookDomainsRaw) {
            this(viewportInput, viewportTargetType, viewportApplyMode,
                    TemplateCustomSemantics.draftInputForTargetType(
                            viewportInput, viewportTargetType,
                            TemplateConfigValue.VIEWPORT_TARGET_RELATIVE_SCALE),
                    TemplateCustomSemantics.draftInputForTargetType(
                            viewportInput, viewportTargetType,
                            TemplateConfigValue.VIEWPORT_TARGET_ABSOLUTE_DP),
                    fontScaleInput, fontApplyMode, selectedTypefaceId, fontHookDomainsRaw);
        }

        public Request(String viewportInput,
                String viewportTargetType,
                String viewportApplyMode,
                String viewportScaleInput,
                String viewportAbsoluteInput,
                String fontScaleInput,
                String fontApplyMode,
                String selectedTypefaceId,
                String fontHookDomainsRaw) {
            this.viewportInput = viewportInput != null ? viewportInput.trim() : "";
            this.viewportTargetType = TemplateConfigValue.normalizeViewportTargetType(viewportTargetType);
            this.viewportApplyMode = viewportApplyMode;
            this.viewportScaleInput = viewportScaleInput != null ? viewportScaleInput.trim() : "";
            this.viewportAbsoluteInput = viewportAbsoluteInput != null
                    ? viewportAbsoluteInput.trim()
                    : "";
            this.fontScaleInput = fontScaleInput != null ? fontScaleInput.trim() : "";
            this.fontApplyMode = fontApplyMode;
            this.selectedTypefaceId = selectedTypefaceId;
            this.fontHookDomainsRaw = fontHookDomainsRaw;
        }
    }

    public static final class Result {
        public final boolean success;
        public final int messageResId;

        private Result(boolean success, int messageResId) {
            this.success = success;
            this.messageResId = messageResId;
        }

        public static Result success(int messageResId) {
            return new Result(true, messageResId);
        }

        public static Result failure(int messageResId) {
            return new Result(false, messageResId);
        }
    }
}
