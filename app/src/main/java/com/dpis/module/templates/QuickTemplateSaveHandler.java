package com.dpis.module.templates;

import com.dpis.module.viewport.ViewportTargetType;

import com.dpis.module.R;

import java.util.LinkedHashSet;
import java.util.Set;

public final class QuickTemplateSaveHandler {
    public Result save(QuickTemplateStore store, Request request) {
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
        if (!TemplateCustomSemantics.isViewportInputValid(
                request.viewportInput, request.viewportTargetType)
                || !TemplateCustomSemantics.isFontScaleInputValid(request.fontScaleInput)) {
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
        return TemplateCustomSemantics.fromEditorDraft(
                request.viewportInput,
                request.viewportTargetType,
                request.viewportApplyMode,
                request.viewportScaleInput,
                request.viewportAbsoluteInput,
                request.fontScaleInput,
                request.fontApplyMode,
                request.selectedTypefaceId,
                request.fontHookDomainsRaw);
    }

    public static final class Request {
        public final String templateId;
        public final String name;
        public final String viewportInput;
        public final String viewportTargetType;
        public final String viewportApplyMode;
        public final String viewportScaleInput;
        public final String viewportAbsoluteInput;
        public final String fontScaleInput;
        public final String fontApplyMode;
        public final String selectedTypefaceId;
        public final String fontHookDomainsRaw;

        public Request(String templateId,
                String name,
                String viewportInput,
                String viewportTargetType,
                String viewportApplyMode,
                String fontScaleInput,
                String fontApplyMode,
                String selectedTypefaceId,
                String fontHookDomainsRaw) {
            this(templateId, name, viewportInput, viewportTargetType, viewportApplyMode,
                    TemplateCustomSemantics.draftInputForTargetType(
                            viewportInput, viewportTargetType,
                            TemplateConfigValue.VIEWPORT_TARGET_RELATIVE_SCALE),
                    TemplateCustomSemantics.draftInputForTargetType(
                            viewportInput, viewportTargetType,
                            TemplateConfigValue.VIEWPORT_TARGET_ABSOLUTE_DP),
                    fontScaleInput, fontApplyMode, selectedTypefaceId, fontHookDomainsRaw);
        }

        public Request(String templateId,
                String name,
                String viewportInput,
                String viewportTargetType,
                String viewportApplyMode,
                String viewportScaleInput,
                String viewportAbsoluteInput,
                String fontScaleInput,
                String fontApplyMode,
                String selectedTypefaceId,
                String fontHookDomainsRaw) {
            this.templateId = templateId != null ? templateId.trim() : null;
            this.name = name != null ? name.trim() : "";
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
        public final String templateId;

        private Result(boolean success, int messageResId, String templateId) {
            this.success = success;
            this.messageResId = messageResId;
            this.templateId = templateId;
        }

        public static Result success(int messageResId, String templateId) {
            return new Result(true, messageResId, templateId);
        }

        public static Result failure(int messageResId, String templateId) {
            return new Result(false, messageResId, templateId);
        }
    }
}
