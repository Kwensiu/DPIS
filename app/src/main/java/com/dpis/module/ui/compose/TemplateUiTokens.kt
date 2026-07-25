package com.dpis.module.ui.compose

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Template-workspace dimensions that intentionally mirror the established XML surface.
 *
 * These values stay feature-local because they describe the template page's visual contract,
 * rather than a general application spacing scale.
 */
internal object TemplateUiTokens {
    val TwoPaneMinWidth = 600.dp
    val WorkspaceHorizontalPadding = 16.dp
    // The shell is edge-to-edge; this reserve reproduces the old workspace's visible top gutter
    // without stacking the full status-bar inset on top of the shell boundary.
    val WorkspaceTopPadding = 14.dp
    val WorkspaceBottomReserve = 120.dp
    val SearchTopPadding = 8.dp
    val SearchBottomPadding = 6.dp
    val SearchCardHeight = 52.dp
    val SearchCardShape = RoundedCornerShape(24.dp)
    val SectionTopGap = 10.dp
    // The LazyColumn already owns the page gutter; header actions must align to it directly.
    val SectionTitleInset = 12.dp
    val SectionActionInset = 12.dp
    val ListGap = 12.dp
    val EmptyStateTopGap = 8.dp
    val EmptyStatePadding = 20.dp
    const val EmptyStateViewportFraction = 0.5f
    val EmptyStateBottomBias = 32.dp

    val GlobalCardShape = RoundedCornerShape(24.dp)
    val TemplateCardShape = RoundedCornerShape(14.dp)
    val CardBorderWidth = 1.dp
    val CardPadding = 18.dp
    val TextSpacingTop = 6.dp

    val SummaryTopGap = 12.dp
    val SummaryHorizontalGap = 7.dp
    val SummaryVerticalGap = 7.dp
    val SummaryMinHeight = 28.dp
    val SummaryHorizontalPadding = 10.dp
    val SummaryVerticalPadding = 6.dp
    val SummaryShape = RoundedCornerShape(14.dp)
    val EmptySummaryMinHeight = 32.dp
    val EmptySummaryShape = RoundedCornerShape(16.dp)
    val EmptySummaryTopGap = 14.dp

    val HeaderActionVisualSize = 36.dp
    // Card actions are compact secondary controls; keep them visibly below the 36dp page actions.
    val CardActionVisualSize = 28.dp
    const val DisabledActionAlpha = 0.45f
    val ActionSpacing = 8.dp
    val CardActionsTopGap = 14.dp
    val HeaderActionSpacing = 8.dp
    val ApplyActionVisualSize = CardActionVisualSize

    val CircularActionShape = CircleShape

    val SheetHorizontalPadding = 20.dp
    val SheetTopPadding = 14.dp
    // The legacy sheet had 24dp dialog bottom inset plus the template field reserve.
    // ModalBottomSheet does not inherit that View-dialog inset, so keep both parts here.
    val SheetBottomPadding = 32.dp
    val SheetVisualIndicatorWidth = 52.dp
    val SheetVisualIndicatorHeight = 5.dp
    val SheetTopChromeHeight = 28.dp
    // This is a content-level visual indicator, not a Material drag-handle slot.
    val SheetVisualIndicatorOffset = 0.dp
    val SheetHeaderTopGap = 10.dp
    val SheetHeaderMinHeight = 52.dp
    val SheetInputGap = 8.dp
    val SheetSelectorGap = 10.dp
    val SheetSelectorTopGap = 12.dp
    // The mode track is the visual row anchor; the outlined input moves down to align its
    // 48dp container while leaving its floating label outside the measured outline.
    val SheetControlTopOffset = 6.dp
    val SheetSelectorWidth = 132.dp
    val SheetSelectorHeight = 48.dp
    val SheetModeRowMinHeight = 56.dp
    // The outline matches the adjacent 48dp mode track. DecorationBox owns the compact inset so
    // the editor content remains visible instead of being clipped by a forced height.
    val SheetInputHeight = 48.dp
    val SheetInputContentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
    val SheetInputShape = RoundedCornerShape(18.dp)
    val SheetSaveTopGap = 12.dp
    val SheetSaveButtonHeight = 48.dp
    val SheetButtonCornerRadius = 18.dp
    val UnsavedBadgeMinHeight = 22.dp
    val UnsavedBadgeShape = RoundedCornerShape(14.dp)

    val ModeTrackShape = RoundedCornerShape(18.dp)
    // Match the outer track radius so the parent clip never cuts the thumb outline corners.
    val ModeThumbShape = RoundedCornerShape(18.dp)
    val ModeThumbBorderWidth = 1.dp
    val ModeAnimationDurationMillis = 200
}
