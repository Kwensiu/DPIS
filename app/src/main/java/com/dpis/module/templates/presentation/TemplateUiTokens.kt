package com.dpis.module.templates.presentation

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Template-workspace dimensions that intentionally mirror the established XML surface.
 *
 * These values stay feature-local because they describe the template page's visual contract,
 * rather than a general application spacing scale.
 */
internal object TemplateUiTokens {
    val WorkspaceHorizontalPadding = 16.dp

    // The shell is edge-to-edge; this reserve reproduces the old workspace's visible top gutter
    // without stacking the full status-bar inset on top of the shell boundary.
    val WorkspaceTopPadding = 14.dp
    val WorkspaceBottomReserve = 120.dp

    // Both workspace pages use the same 64dp pinned MD3 top-app-bar slot.
    val SearchTopPadding = 6.dp
    val SearchBottomPadding = 6.dp
    val SearchCardHeight = 52.dp
    val SectionTopGap = 10.dp

    // The LazyColumn already owns the page gutter; header actions must align to it directly.
    val SectionTitleInset = 12.dp
    val SectionActionInset = 12.dp
    val ListGap = 12.dp
    val EmptyStateTopGap = 8.dp
    val EmptyStatePadding = 20.dp
    const val EMPTY_STATE_VIEWPORT_FRACTION = 0.5f
    val EmptyStateBottomBias = 32.dp

    val GlobalCardShape = RoundedCornerShape(24.dp)
    val TemplateCardShape = RoundedCornerShape(14.dp)
    val CardBorderWidth = 1.dp
    val CardPadding = 18.dp
    val TextSpacingTop = 6.dp
    val EditorNameToFirstInputGap = 8.dp

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
    const val DISABLED_ACTION_ALPHA = 0.45f
    val ActionSpacing = 8.dp
    val CardActionsTopGap = 14.dp
    val HeaderActionSpacing = 8.dp
    val ApplyActionVisualSize = CardActionVisualSize

    val CircularActionShape = CircleShape

    val UnsavedBadgeMinHeight = 22.dp
    val UnsavedBadgeShape = RoundedCornerShape(14.dp)

}

/** Stable route keys shared by the template list and its editor surface. */
internal object TemplateEditorKinds {
    const val GLOBAL = "global"
    const val QUICK = "quick"
}
