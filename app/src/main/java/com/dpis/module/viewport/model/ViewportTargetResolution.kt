package com.dpis.module.viewport

class ViewportTargetResolution private constructor(
    @JvmField val spec: ViewportTargetSpec,
    @JvmField val effectiveSmallestWidthDp: Int,
    @JvmField val source: ViewportSourceSnapshot?,
    @JvmField val record: ViewportRuntimeRecord?,
    @JvmField val reason: String?,
) {
    companion object {
        const val REASON_APP_PROCESS_BORROW_TARGET = "app-process-borrow-target"
        const val REASON_APP_PROCESS_RELATIVE_SCALE = "app-process-relative-scale"

        @JvmStatic
        fun resolved(
            spec: ViewportTargetSpec?,
            effectiveSmallestWidthDp: Int,
            source: ViewportSourceSnapshot?,
            reason: String?,
        ): ViewportTargetResolution = ViewportTargetResolution(
            spec ?: ViewportTargetSpec.off(),
            maxOf(1, effectiveSmallestWidthDp),
            source,
            null,
            reason,
        )

        @JvmStatic
        fun fromRecord(record: ViewportRuntimeRecord?, reason: String?): ViewportTargetResolution =
            if (record == null) none(reason) else ViewportTargetResolution(
                record.targetSpec,
                record.effectiveSmallestWidthDp,
                null,
                record,
                reason,
            )

        @JvmStatic
        fun fromAppProcessBorrowRecord(record: ViewportRuntimeRecord?): ViewportTargetResolution =
            fromRecord(record, REASON_APP_PROCESS_BORROW_TARGET)

        @JvmStatic
        fun none(reason: String?): ViewportTargetResolution =
            ViewportTargetResolution(ViewportTargetSpec.off(), 0, null, null, reason)
    }

    fun hasTarget(): Boolean = effectiveSmallestWidthDp > 0 && spec.isEnabled()

    val isAppProcessBorrowTarget: Boolean
        get() = spec.isRelativeScale() &&
                (REASON_APP_PROCESS_BORROW_TARGET == reason || REASON_APP_PROCESS_RELATIVE_SCALE == reason)

    val isAppProcessDisplayBorrowTarget: Boolean
        get() =
            spec.isRelativeScale() && REASON_APP_PROCESS_BORROW_TARGET == reason
}
