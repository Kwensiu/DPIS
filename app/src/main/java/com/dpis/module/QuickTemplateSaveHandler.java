package com.dpis.module;

import java.util.LinkedHashSet;
import java.util.Set;

final class QuickTemplateSaveHandler {
    Result save(QuickTemplateStore store, Request request) {
        if (store == null) {
            return Result.failure(R.string.status_save_requires_init, null);
        }
        if (request == null) {
            return Result.failure(R.string.status_save_invalid, null);
        }
        String name = request.name.trim();
        if (name.isEmpty()) {
            return Result.failure(R.string.quick_template_name_required, request.templateId);
        }
        if (store.hasDuplicateName(name, request.templateId)) {
            return Result.failure(R.string.quick_template_name_duplicate, request.templateId);
        }
        if (!AppConfigInputValidation.isViewportInputValid(
                request.viewportInput, request.viewportTargetType)
                || !AppConfigInputValidation.isFontScaleInputValid(request.fontScaleInput)) {
            return Result.failure(R.string.status_save_invalid, request.templateId);
        }

        String templateId = request.templateId;
        if (templateId == null || templateId.trim().isEmpty()) {
            templateId = store.newTemplateId();
        }
        QuickTemplateStore.QuickTemplate currentTemplate = store.read(templateId);
        Set<String> selectedPackages = currentTemplate != null
                ? currentTemplate.selectedPackages
                : new LinkedHashSet<>();
        TemplateConfigValue configValue = buildConfigValue(request);
        QuickTemplateStore.QuickTemplate template = new QuickTemplateStore.QuickTemplate(
                templateId,
                name,
                System.currentTimeMillis(),
                selectedPackages,
                configValue);
        return store.save(template)
                ? Result.success(R.string.quick_template_save_success, templateId)
                : Result.failure(R.string.quick_template_save_failed, templateId);
    }

    private static TemplateConfigValue buildConfigValue(Request request) {
        ViewportTargetSpec viewportTargetSpec = AppConfigInputValidation.parseViewportTargetSpec(
                request.viewportInput, request.viewportTargetType);
        Integer fontScalePercent = AppConfigInputValidation.parseFontScalePercentOrNull(
                request.fontScaleInput);
        String viewportApplyMode = normalizeViewportApplyMode(
                request.viewportApplyMode, viewportTargetSpec);
        String fontApplyMode = normalizeFontApplyMode(request.fontApplyMode, fontScalePercent);
        return new TemplateConfigValue(
                viewportTargetSpec,
                ConfigDraftSaveSemantics.viewportTargetTypeForSave(request.viewportTargetType),
                viewportApplyMode,
                fontScalePercent,
                fontApplyMode,
                request.selectedTypefaceId,
                request.fontHookDomainsRaw);
    }

    private static String normalizeViewportApplyMode(
            String requestedMode, ViewportTargetSpec viewportTargetSpec) {
        if (viewportTargetSpec == null || !viewportTargetSpec.isEnabled()) {
            return ViewportApplyMode.OFF;
        }
        return ConfigDraftSaveSemantics.viewportApplyModeForSave(requestedMode, viewportTargetSpec);
    }

    private static String normalizeFontApplyMode(String requestedMode, Integer fontScalePercent) {
        return ConfigDraftSaveSemantics.fontApplyModeForSave(requestedMode);
    }

    static final class Request {
        final String templateId;
        final String name;
        final String viewportInput;
        final String viewportTargetType;
        final String viewportApplyMode;
        final String fontScaleInput;
        final String fontApplyMode;
        final String selectedTypefaceId;
        final String fontHookDomainsRaw;

        Request(String templateId,
                String name,
                String viewportInput,
                String viewportTargetType,
                String viewportApplyMode,
                String fontScaleInput,
                String fontApplyMode,
                String selectedTypefaceId,
                String fontHookDomainsRaw) {
            this.templateId = templateId != null ? templateId.trim() : null;
            this.name = name != null ? name.trim() : "";
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
        final String templateId;

        private Result(boolean success, int messageResId, String templateId) {
            this.success = success;
            this.messageResId = messageResId;
            this.templateId = templateId;
        }

        static Result success(int messageResId, String templateId) {
            return new Result(true, messageResId, templateId);
        }

        static Result failure(int messageResId, String templateId) {
            return new Result(false, messageResId, templateId);
        }
    }
}
