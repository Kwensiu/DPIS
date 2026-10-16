package com.dpis.module.runtime.resources

import com.dpis.module.viewport.ViewportOverride

object ResourcesImplResultSelector {
    enum class Source {
        WINDOW_LIKE_BORROW,
        APP_PROCESS_WINDOW_METRICS,
        PUBLISHED_RECORD,
        TRUSTED_DISPLAY,
        DERIVED,
    }

    data class Selection(
        val result: ViewportOverride.Result?,
        val source: Source,
    )

    fun select(
        windowLikeBorrow: ViewportOverride.Result?,
        appProcessWindowMetrics: ViewportOverride.Result?,
        publishedRecord: ViewportOverride.Result?,
        trustedDisplay: ViewportOverride.Result?,
        deriveFallback: () -> ViewportOverride.Result?,
    ): Selection = when {
        windowLikeBorrow != null -> Selection(windowLikeBorrow, Source.WINDOW_LIKE_BORROW)
        appProcessWindowMetrics != null ->
            Selection(appProcessWindowMetrics, Source.APP_PROCESS_WINDOW_METRICS)
        publishedRecord != null -> Selection(publishedRecord, Source.PUBLISHED_RECORD)
        trustedDisplay != null -> Selection(trustedDisplay, Source.TRUSTED_DISPLAY)
        else -> Selection(deriveFallback(), Source.DERIVED)
    }
}
