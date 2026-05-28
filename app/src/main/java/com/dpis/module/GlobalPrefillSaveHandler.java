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

        ViewportTargetSpec viewportTargetSpec = AppConfigInputValidation.parseViewportTargetSpec(
                request.viewportInput, request.viewportTargetType);
        Integer fontScalePercent = AppConfigInputValidation.parseFontScalePercentOrNull(
                request.fontScaleInput);
        String viewportApplyMode = normalizeViewportApplyMode(
                request.viewportApplyMode, viewportTargetSpec);
        String fontApplyMode = normalizeFontApplyMode(request.fontApplyMode, fontScalePercent);
        TemplateConfigValue value = new TemplateConfigValue(
                viewportTargetSpec,
                viewportApplyMode,
                fontScalePercent,
                fontApplyMode,
                request.selectedTypefaceId,
                request.fontHookDomainsRaw);
        return store.write(value)
                ? Result.success(R.string.global_prefill_save_success)
                : Result.failure(R.string.global_prefill_save_failed);
    }

    private static String normalizeViewportApplyMode(
            String requestedMode, ViewportTargetSpec viewportTargetSpec) {
        if (viewportTargetSpec == null || !viewportTargetSpec.isEnabled()) {
            return ViewportApplyMode.OFF;
        }
        String normalized = ViewportApplyMode.normalize(requestedMode);
        return ViewportApplyMode.isEnabled(normalized)
                ? normalized
                : ViewportApplyMode.AUTO;
    }

    private static String normalizeFontApplyMode(String requestedMode, Integer fontScalePercent) {
        if (fontScalePercent == null) {
            return FontApplyMode.OFF;
        }
        String normalized = FontApplyMode.normalize(requestedMode);
        return FontApplyMode.isEnabled(normalized)
                ? normalized
                : FontApplyMode.SYSTEM_EMULATION;
    }

    static final class Request {
        final String viewportInput;
        final String viewportTargetType;
        final String viewportApplyMode;
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
            this.viewportInput = viewportInput != null ? viewportInput.trim() : "";
            this.viewportTargetType = ViewportTargetType.normalize(viewportTargetType);
            this.viewportApplyMode = viewportApplyMode;
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
