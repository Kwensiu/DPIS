package com.dpis.module;

final class GlobalPrefillSaveHandler {
    Result save(GlobalPrefillStore store, Request request) {
        if (store == null) {
            return Result.failure(R.string.status_save_requires_init);
        }
        if (request == null) {
            return Result.failure(R.string.status_save_invalid);
        }
        if (!AppConfigInputValidation.isViewportInputValid(
                request.viewportInput, request.viewportTargetType)
                || !AppConfigInputValidation.isFontScaleInputValid(request.fontScaleInput)) {
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

    static final class Request {
        final String viewportInput;
        final String viewportTargetType;
        final String viewportApplyMode;
        final String viewportScaleInput;
        final String viewportAbsoluteInput;
        final String fontScaleInput;
        final String fontApplyMode;
        final String selectedTypefaceId;
        final String fontHookDomainsRaw;

        Request(String viewportInput,
                String viewportTargetType,
                String viewportApplyMode,
                String fontScaleInput,
                String fontApplyMode,
                String selectedTypefaceId,
                String fontHookDomainsRaw) {
            this(viewportInput, viewportTargetType, viewportApplyMode,
                    TemplateCustomSemantics.draftInputForTargetType(
                            viewportInput, viewportTargetType, ViewportTargetType.RELATIVE_SCALE),
                    TemplateCustomSemantics.draftInputForTargetType(
                            viewportInput, viewportTargetType, ViewportTargetType.ABSOLUTE_DP),
                    fontScaleInput, fontApplyMode, selectedTypefaceId, fontHookDomainsRaw);
        }

        Request(String viewportInput,
                String viewportTargetType,
                String viewportApplyMode,
                String viewportScaleInput,
                String viewportAbsoluteInput,
                String fontScaleInput,
                String fontApplyMode,
                String selectedTypefaceId,
                String fontHookDomainsRaw) {
            this.viewportInput = viewportInput != null ? viewportInput.trim() : "";
            this.viewportTargetType = ViewportTargetType.normalize(viewportTargetType);
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

    static final class Result {
        final boolean success;
        final int messageResId;

        private Result(boolean success, int messageResId) {
            this.success = success;
            this.messageResId = messageResId;
        }

        static Result success(int messageResId) {
            return new Result(true, messageResId);
        }

        static Result failure(int messageResId) {
            return new Result(false, messageResId);
        }
    }
}
