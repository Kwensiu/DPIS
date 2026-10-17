package com.dpis.module.templates.presentation

/** Layout values owned by the global-prefill editor sheet. */
internal object TemplateSheetUiTokens {
    // The template main page needs only its primary controls visible at launch; the full sheet
    // remains draggable so the remaining controls are available without a tall initial peek.
    const val GlobalPrefillCollapsedPeekFraction = 0.48f
    const val TemplateCollapsedPeekFraction = 0.56f

    const val MainCollapsedPeekFraction = TemplateCollapsedPeekFraction
}
