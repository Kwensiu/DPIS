package com.dpis.module.ui.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.snap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dpis.module.ConfigEditorDestination
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal const val AppConfigSheetScrimTestTag = "app-config-sheet-scrim"

/** Root-level app editor sheet whose collapsed edge stops at the advanced-section anchor. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppConfigEditorOverlay(
    onDismissRequest: () -> Unit,
    destination: ConfigEditorDestination,
    onReturnToMain: () -> Unit,
    topChrome: @Composable () -> Unit,
    content: @Composable ColumnScope.((Dp) -> Unit, Boolean, () -> Unit) -> Unit,
    overlayContent: @Composable BoxScope.() -> Unit = {}
) {
    @Suppress("DEPRECATION")
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        skipHiddenState = false
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)
    val scrimInteractionSource = remember { MutableInteractionSource() }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var advancedAnchor by remember { mutableStateOf<Dp?>(null) }
    var hasOpened by remember { mutableStateOf(false) }
    var hasExpandedOnce by remember { mutableStateOf(false) }
    var dismissalInProgress by remember { mutableStateOf(false) }
    var previousDestination by remember { mutableStateOf(destination) }
    var childContentOwnsHeightTransition by remember { mutableStateOf(false) }
    var mainCollapsedAnchor by remember { mutableStateOf<Dp?>(null) }
    var returnToMainPending by remember { mutableStateOf(false) }
    // BottomSheetScaffold has no modal scrim. Its target changes at the same instant as the
    // standard show/hide animation begins, so use it as the shared visibility timeline.
    val scrimAlpha by animateFloatAsState(
        targetValue = if (bottomSheetState.targetValue == SheetValue.Hidden) 0f else 0.32f,
        animationSpec = tween(durationMillis = 220),
        label = "app-config-sheet-scrim"
    )

    fun dismissWithAnimation() {
        if (dismissalInProgress) return
        dismissalInProgress = true
        coroutineScope.launch {
            try {
                bottomSheetState.hide()
            } finally {
                onDismissRequest()
            }
        }
    }

    fun returnToMainCollapsed() {
        if (!destination.isChildPage() || returnToMainPending) return
        returnToMainPending = true
        hasExpandedOnce = false
        mainCollapsedAnchor?.let { retainedMainAnchor ->
            // Returning is its own height transition. Publish the retained main anchor and the
            // main destination together so resizing and horizontal navigation run together.
            advancedAnchor = retainedMainAnchor
        }
        // Landscape can enter a child page without ever measuring the portrait main content. In that
        // case MAIN must render once before the portrait collapsed anchor can be established.
        onReturnToMain()
    }

    // A downward gesture has the same meaning as dismissing the legacy BottomSheetDialog:
    // terminate the editing session instead of leaving an invisible draft in Compose state.
    LaunchedEffect(bottomSheetState) {
        snapshotFlow { bottomSheetState.currentValue }.collectLatest { value ->
            if (value == SheetValue.Expanded) {
                hasExpandedOnce = true
            }
            if (value != SheetValue.Hidden) {
                hasOpened = true
            } else if (value == SheetValue.Hidden && hasOpened && !dismissalInProgress) {
                dismissalInProgress = true
                onDismissRequest()
            }
        }
    }
    BackHandler(onBack = ::dismissWithAnimation)
    LaunchedEffect(destination) {
        val wasChild = previousDestination.isChildPage()
        val isChild = destination.isChildPage()
        childContentOwnsHeightTransition = wasChild && isChild
        if (wasChild && !isChild) {
            // The return action has already moved the sheet to the retained main anchor.
            hasExpandedOnce = false
        }
        previousDestination = destination
        if (destination == ConfigEditorDestination.TYPEFACE &&
            bottomSheetState.currentValue != SheetValue.Hidden) {
            // Typeface selection owns a persistent bottom management action, so it always uses
            // the sheet's maximum anchor instead of inheriting MAIN's partial anchor.
            bottomSheetState.expand()
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val minPeekHeight = maxHeight * 0.3f
        val maxPeekHeight = maxHeight * 0.75f
        // The content report points at the advanced divider. Keep the collapsed edge before the
        // divider so the advanced section does not leak into the initial half-expanded sheet.
        val targetPeekHeight = advancedAnchor?.let {
            (it - AppConfigSheetUiTokens.SaveToAdvancedDividerGap
                + AppConfigSheetUiTokens.TopChromeHeight
                + AppConfigSheetUiTokens.CollapsedBottomClearance)
                .coerceIn(minPeekHeight, maxPeekHeight)
        } ?: 0.dp
        val measuredPeekHeight by animateDpAsState(
            targetValue = targetPeekHeight,
            // The first measured anchor must be ready before opening; later changes animate.
            animationSpec = when {
                !hasOpened -> snap()
                returnToMainPending -> tween(durationMillis = 180)
                childContentOwnsHeightTransition -> snap()
                else -> tween(durationMillis = 180)
            },
            label = "app-config-sheet-peek-height"
        )

        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .testTag(AppConfigSheetScrimTestTag)
                    // A modal scrim is a dismissal surface, not a button; preserve its click
                    // semantics without showing a ripple over the dimmed background.
                    .clickable(
                        interactionSource = scrimInteractionSource,
                        indication = null,
                        onClick = ::dismissWithAnimation
                    )
            )
            BottomSheetScaffold(
                // The wizard hint intentionally overflows above the sheet surface. Elevate the
                // complete scaffold so its chrome and overflow are composited above workspace
                // content, matching the legacy dialog's overlay elevation.
                modifier = Modifier.fillMaxSize().zIndex(1f),
                scaffoldState = scaffoldState,
                containerColor = Color.Transparent,
                // Typeface is an expanded-or-dismissed child page. A zero peek removes its
                // visible partial anchor while keeping sheet gestures available outside the
                // font lists; MAIN restores its measured advanced-section anchor.
                sheetPeekHeight = if (destination == ConfigEditorDestination.TYPEFACE) {
                    0.dp
                } else {
                    measuredPeekHeight
                },
                sheetContainerColor = MaterialTheme.colorScheme.surface,
                sheetContentColor = MaterialTheme.colorScheme.onSurface,
                sheetTonalElevation = 0.dp,
                // Material assigns the "drag handle" accessibility role to this slot. The
                // app's short line is visual-only, so render it in regular sheet content.
                sheetDragHandle = null,
                sheetContent = {
                    Box(Modifier.fillMaxWidth()) {
                        Column {
                            topChrome()
                            content({ measuredAnchor ->
                                // Before the user opens advanced actions, layout changes (for example,
                                // validation text or a window resize) keep the collapsed edge aligned.
                                // An expanded sheet remains under the user's control until dismissal.
                                if (measuredAnchor > 0.dp &&
                                    (destination.isChildPage() || !hasExpandedOnce)) {
                                    advancedAnchor = measuredAnchor
                                    if (!destination.isChildPage()) {
                                        mainCollapsedAnchor = measuredAnchor
                                    }
                                }
                            }, bottomSheetState.currentValue == SheetValue.Expanded &&
                                bottomSheetState.targetValue == SheetValue.Expanded &&
                                !returnToMainPending, ::returnToMainCollapsed)
                        }
                        // Draw after the editor content while keeping the overlay out of the
                        // measured column height, so it remains anchored to the sheet chrome.
                        overlayContent()
                    }
                }
            ) { }
        }

        LaunchedEffect(
            returnToMainPending,
            measuredPeekHeight,
            targetPeekHeight,
            bottomSheetState.currentValue
        ) {
            if (returnToMainPending &&
                mainCollapsedAnchor != null &&
                measuredPeekHeight == targetPeekHeight &&
                bottomSheetState.currentValue == SheetValue.PartiallyExpanded) {
                // Keep MAIN in its return transition until both the retained anchor and the
                // sheet state agree. Clearing this after height alone lets an Expanded sheet
                // briefly reclaim the main content and reveal advanced actions.
                returnToMainPending = false
            }
        }
        LaunchedEffect(returnToMainPending, mainCollapsedAnchor) {
            if (returnToMainPending && mainCollapsedAnchor != null) {
                bottomSheetState.partialExpand()
            }
        }
    }
    // Do not animate from a provisional 50% peek height. The editor measures its advanced
    // divider while hidden, then enters once the legacy collapsed edge is known and stable.
    LaunchedEffect(advancedAnchor) {
        if (advancedAnchor != null
                && bottomSheetState.currentValue == SheetValue.Hidden
                && bottomSheetState.targetValue == SheetValue.Hidden
                && !dismissalInProgress) {
            if (destination == ConfigEditorDestination.TYPEFACE) {
                bottomSheetState.expand()
            } else {
                bottomSheetState.partialExpand()
            }
        }
    }
}
