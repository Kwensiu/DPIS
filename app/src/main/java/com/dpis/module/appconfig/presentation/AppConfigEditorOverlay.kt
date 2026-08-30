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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.offset
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
    var childPageTransitionActive by remember { mutableStateOf(false) }
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
        childPageTransitionActive = wasChild && isChild
        if (isChild) {
            // Re-entering a child page cancels a pending return before its retained MAIN anchor
            // can drive another partial-expand request.
            returnToMainPending = false
        }
        if (wasChild && !isChild) {
            // The return action has already moved the sheet to the retained main anchor.
            hasExpandedOnce = false
        }
        previousDestination = destination
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val statusBarTopInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        // The expanded viewport already stops below the system chrome. Moving the handle again
        // based only on the sheet state would create a visible jump for a non-full-height sheet.
        val chromeSafeOffset = 0.dp
        val minPeekHeight = maxHeight * 0.3f
        val maxPeekHeight = maxHeight * 0.75f
        // The standard sheet's expanded anchor is derived from its measured content height.
        // Keep a small top clearance so expanding advanced settings remains a sheet interaction,
        // rather than turning the editor into a full-screen page under the status bar.
        val sheetViewportHeight = (
            maxHeight - statusBarTopInset - AppConfigSheetUiTokens.ExpandedTopClearance
        ).coerceAtLeast(maxHeight * 0.75f)
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
                childPageTransitionActive -> snap()
                else -> tween(durationMillis = 180)
            },
            label = "app-config-sheet-peek-height"
        )
        val sheetMotionInProgress = measuredPeekHeight != targetPeekHeight ||
            returnToMainPending

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
                sheetSwipeEnabled = !sheetMotionInProgress,
                // Keep one shared peek anchor for MAIN, Hook, and Typeface pages. Child pages
                // report their content height through the same callback as the Hook editor.
                sheetPeekHeight = measuredPeekHeight,
                sheetContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                sheetContentColor = MaterialTheme.colorScheme.onSurface,
                sheetTonalElevation = 0.dp,
                // Material assigns the "drag handle" accessibility role to this slot. The
                // app's short line is visual-only, so render it in regular sheet content.
                sheetDragHandle = null,
                sheetContent = {
                    // Cap the expanded sheet without forcing short editor content to occupy the
                    // whole window. The editor owns scrolling when its content exceeds this cap.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = sheetViewportHeight)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    // Keep the chrome slot's measured height stable. Only move its
                                    // visual content around the cutout; padding here would remeasure
                                    // the sheet and make the body jump during collapse.
                                    .offset(y = chromeSafeOffset)
                            ) {
                                topChrome()
                            }
                            content({ measuredAnchor ->
                                // Before the user opens advanced actions, layout changes (for
                                // example, validation text or a window resize) keep the collapsed
                                // edge aligned. An expanded sheet remains under the user's control
                                // until dismissal.
                                if (measuredAnchor > 0.dp &&
                                    (
                                        (!returnToMainPending && destination.isChildPage()) ||
                                        (!destination.isChildPage() &&
                                            (!returnToMainPending && !hasExpandedOnce ||
                                                mainCollapsedAnchor == null))
                                    )
                                ) {
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
                        if (sheetMotionInProgress) {
                            // BottomSheetScaffold keeps its drag layer above sheet content while
                            // an anchor or settle animation is running. Consume the gesture here
                            // as well, otherwise a tap can start a drag against a moving layout
                            // and leave the visible page detached from its hit-test coordinates.
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .zIndex(2f)
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                awaitPointerEvent(PointerEventPass.Initial)
                                                    .changes
                                                    .forEach { it.consume() }
                                            }
                                        }
                                    }
                            )
                        }
                    }
                }
            ) { }
        }

        LaunchedEffect(
            returnToMainPending,
            measuredPeekHeight,
            targetPeekHeight,
            bottomSheetState.currentValue,
            bottomSheetState.targetValue
        ) {
            if (returnToMainPending &&
                mainCollapsedAnchor != null &&
                measuredPeekHeight == targetPeekHeight &&
                bottomSheetState.currentValue == SheetValue.Expanded &&
                bottomSheetState.targetValue != SheetValue.PartiallyExpanded) {
                // A partially expanded sheet already has the correct semantic state. Only an
                // actually expanded child page needs an explicit collapse request; calling
                // partialExpand() for every return makes Material rebuild its anchors and briefly
                // expose the full sheet even when the user never expanded it.
                bottomSheetState.partialExpand()
            } else if (returnToMainPending &&
                mainCollapsedAnchor != null &&
                measuredPeekHeight == targetPeekHeight &&
                bottomSheetState.currentValue == SheetValue.PartiallyExpanded) {
                // Keep MAIN in its return transition until both the retained anchor and the
                // sheet state agree. Clearing this after height alone lets an Expanded sheet
                // briefly reclaim the main content and reveal advanced actions.
                returnToMainPending = false
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
            bottomSheetState.partialExpand()
        }
    }
}
