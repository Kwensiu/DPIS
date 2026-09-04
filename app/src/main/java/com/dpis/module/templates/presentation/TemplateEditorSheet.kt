package com.dpis.module.templates.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dpis.module.ConfigEditorDestination
import com.dpis.module.R
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.res.stringResource
import com.dpis.module.ui.compose.AppConfigSheetUiTokens
import com.dpis.module.ui.compose.ComposeMotionTokens
import com.dpis.module.ui.compose.EditorSheetScaffoldFrame
import com.dpis.module.ui.compose.LocalTextInputFocusBoundary
import com.dpis.module.ui.compose.TextInputFocusBoundary
import com.dpis.module.ui.compose.clearTextInputFocusOutside
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Template-owned portrait editor sheet.
 *
 * This intentionally duplicates the app editor's interaction contract instead of sharing its
 * implementation: template navigation and draft ownership may evolve independently, while the
 * visible sheet semantics remain directly comparable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TemplateEditorSheet(
    destination: ConfigEditorDestination,
    onDismissRequest: () -> Unit,
    onReturnToMain: () -> Unit,
    topChrome: @Composable () -> Unit,
    content: @Composable ColumnScope.((Dp) -> Unit, Boolean, () -> Unit) -> Unit,
    overlayContent: @Composable BoxScope.() -> Unit = {},
) {
    @Suppress("DEPRECATION")
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        skipHiddenState = false,
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)
    val scrimInteractionSource = remember { MutableInteractionSource() }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val inputFocusBoundary = LocalTextInputFocusBoundary.current ?: remember { TextInputFocusBoundary() }
    var contentBottom by remember { mutableStateOf<Dp?>(null) }
    var mainCollapsedContentBottom by remember { mutableStateOf<Dp?>(null) }
    var hasOpened by remember { mutableStateOf(false) }
    var hasExpandedOnce by remember { mutableStateOf(false) }
    var dismissalInProgress by remember { mutableStateOf(false) }
    var previousDestination by remember { mutableStateOf(destination) }
    var childPageTransitionActive by remember { mutableStateOf(false) }
    var returnToMainPending by remember { mutableStateOf(false) }
    val scrimAlpha by animateFloatAsState(
        targetValue = if (bottomSheetState.targetValue == SheetValue.Hidden) 0f else 0.32f,
        animationSpec = tween(durationMillis = ComposeMotionTokens.SHEET_SETTLE_DURATION_MILLIS),
        label = "template-editor-sheet-scrim",
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
        mainCollapsedContentBottom?.let { contentBottom = it }
        onReturnToMain()
    }

    LaunchedEffect(bottomSheetState) {
        snapshotFlow { bottomSheetState.currentValue }.collectLatest { value ->
            if (value == SheetValue.Expanded) hasExpandedOnce = true
            if (value != SheetValue.Hidden) {
                hasOpened = true
            } else if (hasOpened && !dismissalInProgress) {
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
        if (isChild) returnToMainPending = false
        if (wasChild && !isChild) hasExpandedOnce = false
        previousDestination = destination
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val statusBarTopInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val minPeekHeight = maxHeight * 0.3f
        val maxPeekHeight = maxHeight * 0.75f
        val sheetViewportHeight = (
            maxHeight - statusBarTopInset - AppConfigSheetUiTokens.ExpandedTopClearance
        ).coerceAtLeast(maxHeight * 0.75f)
        val targetPeekHeight = contentBottom?.let {
            (it + AppConfigSheetUiTokens.TopChromeHeight +
                AppConfigSheetUiTokens.CollapsedBottomClearance).coerceIn(minPeekHeight, maxPeekHeight)
        } ?: 0.dp
        val measuredPeekHeight by animateDpAsState(
            targetValue = targetPeekHeight,
            animationSpec = when {
                !hasOpened -> snap()
                returnToMainPending -> tween(durationMillis = ComposeMotionTokens.CONTENT_TRANSITION_DURATION_MILLIS)
                childPageTransitionActive -> snap()
                else -> tween(durationMillis = ComposeMotionTokens.CONTENT_TRANSITION_DURATION_MILLIS)
            },
            label = "template-editor-sheet-peek-height",
        )
        val sheetMotionInProgress = measuredPeekHeight != targetPeekHeight || returnToMainPending
        // A template's main form has one deliberately fixed partial anchor. Letting Material
        // handle drag gestures here exposes its unused expanded anchor as a small, misleading
        // upward movement. Child editors are the only template destinations that can scroll the
        // sheet between partial and expanded states.
        val sheetSwipeEnabled = destination.isChildPage() && !sheetMotionInProgress

        CompositionLocalProvider(LocalTextInputFocusBoundary provides inputFocusBoundary) {
            TemplateEditorSheetScaffold(
            scaffoldState = scaffoldState,
            bottomSheetState = bottomSheetState,
            scrimInteractionSource = scrimInteractionSource,
            scrimAlpha = scrimAlpha,
            measuredPeekHeight = measuredPeekHeight,
            sheetViewportHeight = sheetViewportHeight,
            sheetSwipeEnabled = sheetSwipeEnabled,
            returnToMainPending = returnToMainPending,
            topChrome = topChrome,
            content = content,
            overlayContent = overlayContent,
            onDismiss = ::dismissWithAnimation,
            onReturnToMain = ::returnToMainCollapsed,
            onContentBottomMeasured = { measuredAnchor ->
                if (measuredAnchor > 0.dp &&
                    ((!returnToMainPending && destination.isChildPage()) ||
                        (!destination.isChildPage() &&
                            (!returnToMainPending && !hasExpandedOnce || mainCollapsedContentBottom == null)))
                ) {
                    contentBottom = measuredAnchor
                    if (!destination.isChildPage()) mainCollapsedContentBottom = measuredAnchor
                }
            },
                focusManager = focusManager,
                inputFocusBoundary = inputFocusBoundary,
            )
        }

        LaunchedEffect(
            returnToMainPending,
            measuredPeekHeight,
            targetPeekHeight,
            bottomSheetState.currentValue,
            bottomSheetState.targetValue,
        ) {
            if (returnToMainPending && mainCollapsedContentBottom != null &&
                measuredPeekHeight == targetPeekHeight &&
                bottomSheetState.currentValue == SheetValue.Expanded &&
                bottomSheetState.targetValue != SheetValue.PartiallyExpanded
            ) {
                bottomSheetState.partialExpand()
            } else if (returnToMainPending && mainCollapsedContentBottom != null &&
                measuredPeekHeight == targetPeekHeight &&
                bottomSheetState.currentValue == SheetValue.PartiallyExpanded
            ) {
                returnToMainPending = false
            }
        }
    }
    LaunchedEffect(contentBottom) {
        if (contentBottom != null && bottomSheetState.currentValue == SheetValue.Hidden &&
            bottomSheetState.targetValue == SheetValue.Hidden && !dismissalInProgress
        ) {
            bottomSheetState.partialExpand()
        }
    }
}

/** Template-owned chrome; it intentionally does not depend on the app editor's chrome composable. */
@Composable
internal fun TemplateEditorSheetChrome(showUnsaved: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth().height(AppConfigSheetUiTokens.TopChromeHeight),
        contentAlignment = Alignment.Center,
    ) {
        if (showUnsaved) {
            Surface(
                shape = AppConfigSheetUiTokens.UnsavedBadgeShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Text(
                    text = stringResource(R.string.sheet_unsaved_badge),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        } else {
            Box(
                Modifier
                    .size(
                        width = AppConfigSheetUiTokens.TopChromeIndicatorWidth,
                        height = AppConfigSheetUiTokens.TopChromeIndicatorHeight,
                    )
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateEditorSheetScaffold(
    scaffoldState: androidx.compose.material3.BottomSheetScaffoldState,
    bottomSheetState: androidx.compose.material3.SheetState,
    scrimInteractionSource: MutableInteractionSource,
    scrimAlpha: Float,
    measuredPeekHeight: Dp,
    sheetViewportHeight: Dp,
    sheetSwipeEnabled: Boolean,
    returnToMainPending: Boolean,
    focusManager: FocusManager,
    inputFocusBoundary: TextInputFocusBoundary,
    topChrome: @Composable () -> Unit,
    content: @Composable ColumnScope.((Dp) -> Unit, Boolean, () -> Unit) -> Unit,
    overlayContent: @Composable BoxScope.() -> Unit,
    onDismiss: () -> Unit,
    onReturnToMain: () -> Unit,
    onContentBottomMeasured: (Dp) -> Unit,
) {
    EditorSheetScaffoldFrame(
        scaffoldState = scaffoldState,
        bottomSheetState = bottomSheetState,
        scrimInteractionSource = scrimInteractionSource,
        scrimAlpha = scrimAlpha,
        sheetPeekHeight = measuredPeekHeight,
        sheetViewportHeight = sheetViewportHeight,
        sheetSwipeEnabled = sheetSwipeEnabled && !returnToMainPending,
        focusManager = focusManager,
        inputFocusBoundary = inputFocusBoundary,
        topChrome = topChrome,
        content = { measureAnchor, expanded, returnFromChild ->
            content(measureAnchor, expanded && !returnToMainPending, returnFromChild)
        },
        overlayContent = overlayContent,
        onDismiss = onDismiss,
        onContentBottomMeasured = onContentBottomMeasured,
        onReturnToMain = onReturnToMain,
    )
}
