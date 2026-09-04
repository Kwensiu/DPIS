package com.dpis.module.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Stable interaction frame shared by app and template editor sheets.
 *
 * The feature owners provide only their measured anchors and page content. This frame owns the
 * behavior that must stay identical: modal scrim dismissal, outside-gesture consumption while
 * an input is focused, and sheet swipe gating. IME movement is owned by the Compose root.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun EditorSheetScaffoldFrame(
    scaffoldState: BottomSheetScaffoldState,
    bottomSheetState: SheetState,
    scrimInteractionSource: MutableInteractionSource,
    scrimAlpha: Float,
    sheetPeekHeight: Dp,
    sheetViewportHeight: Dp,
    sheetSwipeEnabled: Boolean,
    focusManager: androidx.compose.ui.focus.FocusManager,
    inputFocusBoundary: TextInputFocusBoundary,
    topChrome: @Composable () -> Unit,
    content: @Composable ColumnScope.((Dp) -> Unit, Boolean, () -> Unit) -> Unit,
    overlayContent: @Composable BoxScope.() -> Unit,
    onDismiss: () -> Unit,
    onContentBottomMeasured: (Dp) -> Unit,
    onReturnToMain: () -> Unit,
) {
    val sheetExpanded = bottomSheetState.currentValue == SheetValue.Expanded &&
        bottomSheetState.targetValue == SheetValue.Expanded

    Box(
        Modifier
            .fillMaxSize()
            // The boundary must share the exact coordinate space receiving pointer events.
            // Keeping it on this root lets taps on every sheet layer transfer focus correctly.
            .clearTextInputFocusOutside(focusManager, inputFocusBoundary),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = scrimInteractionSource,
                    indication = null,
                    onClick = onDismiss,
                )
        )
        BottomSheetScaffold(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f),
            scaffoldState = scaffoldState,
            containerColor = Color.Transparent,
            sheetSwipeEnabled = sheetSwipeEnabled,
            sheetPeekHeight = sheetPeekHeight,
            sheetContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            sheetContentColor = MaterialTheme.colorScheme.onSurface,
            sheetTonalElevation = 0.dp,
            sheetDragHandle = null,
            sheetContent = {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = sheetViewportHeight)
                ) {
                    Column {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(AppConfigSheetUiTokens.TopChromeHeight)
                        ) {
                            topChrome()
                        }
                        content(
                            onContentBottomMeasured,
                            sheetExpanded,
                            onReturnToMain,
                        )
                    }
                    overlayContent()
                }
            },
        ) {
            // BottomSheetScaffold is a standard (non-modal) sheet. Its content slot owns the
            // area above the sheet so an empty-space tap can dismiss the editor consistently.
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = scrimInteractionSource,
                        indication = null,
                        onClick = onDismiss,
                    )
            )
        }
    }
}
