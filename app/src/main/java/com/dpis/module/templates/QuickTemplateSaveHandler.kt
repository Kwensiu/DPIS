package com.dpis.module.templates

import com.dpis.module.R
import java.util.LinkedHashSet

/** Validates and persists one quick-template editor submission. */
class QuickTemplateSaveHandler {
    fun save(store: QuickTemplateStore?, request: Request?): Result {
        if (store == null) return Result.failure(R.string.status_save_requires_init, null)
        if (request == null) return Result.failure(R.string.status_save_invalid, null)
        val name = request.name.trim()
        if (name.isEmpty()) return Result.failure(R.string.quick_template_name_required, request.templateId)
        if (store.hasDuplicateName(name, request.templateId)) {
            return Result.failure(R.string.quick_template_name_duplicate, request.templateId)
        }
        if (!TemplateCustomSemantics.isViewportInputValid(request.viewportInput, request.viewportTargetType) ||
            !TemplateCustomSemantics.isFontScaleInputValid(request.fontScaleInput)
        ) {
            return Result.failure(R.string.status_save_invalid, request.templateId)
        }

        val templateId = request.templateId?.takeUnless { it.isBlank() } ?: store.newTemplateId()
        val current = store.read(templateId)
        val selectedPackages = current?.selectedPackages ?: LinkedHashSet()
        val template = QuickTemplateStore.QuickTemplate(
            templateId,
            name,
            System.currentTimeMillis(),
            selectedPackages,
            TemplateCustomSemantics.fromEditorDraft(
                request.viewportInput,
                request.viewportTargetType,
                request.viewportApplyMode,
                request.viewportScaleInput,
                request.viewportAbsoluteInput,
                request.fontScaleInput,
                request.fontApplyMode,
                request.selectedTypefaceId,
                request.fontHookDomainsRaw
            )
        )
        return if (store.save(template)) {
            Result.success(R.string.quick_template_save_success, templateId)
        } else {
            Result.failure(R.string.quick_template_save_failed, templateId)
        }
    }

    class Request {
        @JvmField val templateId: String?
        @JvmField val name: String
        @JvmField val viewportInput: String
        @JvmField val viewportTargetType: String
        @JvmField val viewportApplyMode: String?
        @JvmField val viewportScaleInput: String
        @JvmField val viewportAbsoluteInput: String
        @JvmField val fontScaleInput: String
        @JvmField val fontApplyMode: String?
        @JvmField val selectedTypefaceId: String?
        @JvmField val fontHookDomainsRaw: String?

        constructor(
            templateId: String?,
            name: String?,
            viewportInput: String?,
            viewportTargetType: String?,
            viewportApplyMode: String?,
            viewportScaleInput: String?,
            viewportAbsoluteInput: String?,
            fontScaleInput: String?,
            fontApplyMode: String?,
            selectedTypefaceId: String?,
            fontHookDomainsRaw: String?
        ) {
            this.templateId = templateId?.trim()
            this.name = name?.trim().orEmpty()
            this.viewportInput = viewportInput?.trim().orEmpty()
            this.viewportTargetType = TemplateConfigValue.normalizeViewportTargetType(viewportTargetType)
            this.viewportApplyMode = viewportApplyMode
            this.viewportScaleInput = viewportScaleInput?.trim().orEmpty()
            this.viewportAbsoluteInput = viewportAbsoluteInput?.trim().orEmpty()
            this.fontScaleInput = fontScaleInput?.trim().orEmpty()
            this.fontApplyMode = fontApplyMode
            this.selectedTypefaceId = selectedTypefaceId
            this.fontHookDomainsRaw = fontHookDomainsRaw
        }

        constructor(
            templateId: String?,
            name: String?,
            viewportInput: String?,
            viewportTargetType: String?,
            viewportApplyMode: String?,
            fontScaleInput: String?,
            fontApplyMode: String?,
            selectedTypefaceId: String?,
            fontHookDomainsRaw: String?
        ) : this(
            templateId,
            name,
            viewportInput,
            viewportTargetType,
            viewportApplyMode,
            TemplateCustomSemantics.draftInputForTargetType(
                viewportInput, viewportTargetType, TemplateConfigValue.VIEWPORT_TARGET_RELATIVE_SCALE
            ),
            TemplateCustomSemantics.draftInputForTargetType(
                viewportInput, viewportTargetType, TemplateConfigValue.VIEWPORT_TARGET_ABSOLUTE_DP
            ),
            fontScaleInput,
            fontApplyMode,
            selectedTypefaceId,
            fontHookDomainsRaw
        )

    }

    class Result private constructor(
        @JvmField val success: Boolean,
        @JvmField val messageResId: Int,
        @JvmField val templateId: String?
    ) {
        companion object {
            @JvmStatic fun success(messageResId: Int, templateId: String?) = Result(true, messageResId, templateId)
            @JvmStatic fun failure(messageResId: Int, templateId: String?) = Result(false, messageResId, templateId)
        }
    }
}
