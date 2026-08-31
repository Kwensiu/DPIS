package com.dpis.module.templates

import com.dpis.module.R

/** Validates and persists an editor submission for the global prefill. */
class GlobalPrefillSaveHandler {
    fun save(store: GlobalPrefillStore?, request: Request?): Result {
        if (store == null) return Result.failure(R.string.status_save_requires_init)
        if (request == null) return Result.failure(R.string.status_save_invalid)
        if (!TemplateCustomSemantics.isViewportInputValid(
                request.viewportInput,
                request.viewportTargetType,
            ) || !TemplateCustomSemantics.isFontScaleInputValid(request.fontScaleInput)
        ) {
            return Result.failure(R.string.status_save_invalid)
        }

        val value = TemplateCustomSemantics.fromEditorDraft(
            request.viewportInput,
            request.viewportTargetType,
            request.viewportApplyMode,
            request.viewportScaleInput,
            request.viewportAbsoluteInput,
            request.fontScaleInput,
            request.fontApplyMode,
            request.selectedTypefaceId,
            request.fontHookDomainsRaw,
        )
        return if (store.write(value)) {
            Result.success(R.string.global_prefill_save_success)
        } else {
            Result.failure(R.string.global_prefill_save_failed)
        }
    }

    class Request private constructor(
        viewportInput: String?,
        viewportTargetType: String?,
        @JvmField val viewportApplyMode: String?,
        viewportScaleInput: String?,
        viewportAbsoluteInput: String?,
        fontScaleInput: String?,
        @JvmField val fontApplyMode: String?,
        @JvmField val selectedTypefaceId: String?,
        @JvmField val fontHookDomainsRaw: String?,
        @Suppress("UNUSED_PARAMETER") normalized: Boolean,
    ) {
        @JvmField val viewportInput = viewportInput.orEmpty().trim()
        @JvmField val viewportTargetType = TemplateConfigValue.normalizeViewportTargetType(viewportTargetType)
        @JvmField val viewportScaleInput = viewportScaleInput.orEmpty().trim()
        @JvmField val viewportAbsoluteInput = viewportAbsoluteInput.orEmpty().trim()
        @JvmField val fontScaleInput = fontScaleInput.orEmpty().trim()

        constructor(
            viewportInput: String?,
            viewportTargetType: String?,
            viewportApplyMode: String?,
            fontScaleInput: String?,
            fontApplyMode: String?,
            selectedTypefaceId: String?,
            fontHookDomainsRaw: String?,
        ) : this(
            viewportInput.orEmpty().trim(),
            TemplateConfigValue.normalizeViewportTargetType(viewportTargetType),
            viewportApplyMode,
            TemplateCustomSemantics.draftInputForTargetType(
                viewportInput,
                viewportTargetType,
                TemplateConfigValue.VIEWPORT_TARGET_RELATIVE_SCALE,
            ),
            TemplateCustomSemantics.draftInputForTargetType(
                viewportInput,
                viewportTargetType,
                TemplateConfigValue.VIEWPORT_TARGET_ABSOLUTE_DP,
            ),
            fontScaleInput.orEmpty().trim(),
            fontApplyMode,
            selectedTypefaceId,
            fontHookDomainsRaw,
            true,
        )

        constructor(
            viewportInput: String?,
            viewportTargetType: String?,
            viewportApplyMode: String?,
            viewportScaleInput: String?,
            viewportAbsoluteInput: String?,
            fontScaleInput: String?,
            fontApplyMode: String?,
            selectedTypefaceId: String?,
            fontHookDomainsRaw: String?,
        ) : this(
            viewportInput,
            viewportTargetType,
            viewportApplyMode,
            viewportScaleInput,
            viewportAbsoluteInput,
            fontScaleInput,
            fontApplyMode,
            selectedTypefaceId,
            fontHookDomainsRaw,
            true,
        )

    }

    class Result private constructor(
        @JvmField val success: Boolean,
        @JvmField val messageResId: Int,
    ) {
        companion object {
            @JvmStatic fun success(messageResId: Int) = Result(true, messageResId)

            @JvmStatic fun failure(messageResId: Int) = Result(false, messageResId)
        }
    }
}
