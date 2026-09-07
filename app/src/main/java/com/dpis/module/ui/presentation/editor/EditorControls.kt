package com.dpis.module.ui.compose

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import android.content.res.Configuration
import androidx.compose.ui.zIndex
import com.dpis.module.R
import com.dpis.module.templates.presentation.TemplateUiTokens

/**
 * Compact outlined input used by DPIS editor rows.
 *
 * The normal state keeps the 48dp visual baseline, while larger system font scales are allowed
 * to grow the field instead of clipping the label or entered value. Validation text belongs to
 * the owning row, outside this control, so an error never changes the field's visible container.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CompactEditorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false,
    onFocused: (() -> Unit)? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    trailingIcon: (@Composable (() -> Unit))? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    // Keep selection/cursor state inside the text field. The editor draft is a mutable domain
    // object and can trigger recomposition after every keystroke; rebuilding TextFieldValue from
    // the plain String would otherwise place the cursor back at index zero.
    var textFieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    LaunchedEffect(value) {
        if (textFieldValue.text != value) {
            textFieldValue = TextFieldValue(value, selection = TextRange(value.length))
        }
    }
    val controlHeight = rememberEditorControlHeight()
    Box(
        modifier = modifier.height(controlHeight)
    ) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = { changedValue ->
                textFieldValue = changedValue
                onValueChange(changedValue.text)
            },
            modifier = Modifier
                .fillMaxSize()
                .onFocusChanged {
                    onFocusChanged?.invoke(it.isFocused)
                }
                .inputFocusFeedback(onFocused),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            singleLine = true,
            keyboardOptions = keyboardOptions,
            interactionSource = interactionSource,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                // DecorationBox owns labelProgress, so the motion scheme must wrap the complete
                // decorator rather than only the label content.
                MaterialTheme(motionScheme = EditorTextFieldMotionScheme) {
                    OutlinedTextFieldDefaults.DecorationBox(
                    // Keep label placement tied to the same text rendered by BasicTextField. The
                    // local value can lead the parent draft by one frame while IME composition
                    // settles; using the parent string here makes the label overlap that text.
                    value = textFieldValue.text,
                    innerTextField = innerTextField,
                    enabled = true,
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    isError = isError,
                    // The Material label is deliberately retained, including its border cutout.
                    // Only this field's motion scheme changes its transition from the Material
                    // spring to a bounded tween, so the label settles without a visible rebound.
                    label = {
                        Text(
                            text = label,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    trailingIcon = trailingIcon,
                    contentPadding = PaddingValues(horizontal = LocalSpacing.current.lg, vertical = 6.dp),
                    container = {
                        OutlinedTextFieldDefaults.Container(
                            enabled = true,
                            isError = isError,
                            interactionSource = interactionSource,
                            shape = AppConfigSheetUiTokens.FieldAndActionShape
                        )
                    }
                    )
                }
            }
        )
    }
}

@Composable
internal fun EditorClearButton(
    visible: Boolean = true,
    onClear: () -> Unit,
) {
    IconButton(
        onClick = rememberClickAction(onClear),
        enabled = visible,
        modifier = Modifier.alpha(if (visible) 1f else 0f),
    ) {
        Icon(painterResource(R.drawable.ic_close_24), stringResource(R.string.search_clear))
    }
}

/** Keeps adjacent editor controls visually aligned while allowing large system text to breathe. */
@Composable
internal fun rememberEditorControlHeight(): androidx.compose.ui.unit.Dp {
    // A device can report an extreme accessibility font scale. Let the row breathe, but do not
    // turn a compact editor control into a full-screen panel; the text field still ellipsizes
    // its single-line value when the available width is the limiting dimension.
    val fontScale = LocalDensity.current.fontScale.coerceIn(1f, 1.5f)
    return (AppConfigSheetUiTokens.ActionHeight * fontScale)
        .coerceAtLeast(AppConfigSheetUiTokens.ActionHeight)
}

/**
 * Lets a page's non-input content dismiss the current IME without becoming a new semantic
 * control or consuming its click/scroll gesture. Keep this modifier off the search field itself.
 */
internal fun Modifier.clearTextInputFocusOnPointerDown(
    focusManager: FocusManager,
): Modifier = pointerInput(focusManager) {
    awaitEachGesture {
        awaitFirstDown(
            requireUnconsumed = false,
            pass = PointerEventPass.Initial,
        )
        focusManager.clearFocus(force = true)
        waitForUpOrCancellation()
    }
}

/** Tracks editor input bounds in root coordinates so outside-tap dismissal excludes the fields. */
internal class TextInputFocusBoundary {
    private var rootCoordinates: LayoutCoordinates? = null
    private val inputBounds = mutableMapOf<Any, Rect>()
    private var focusedInputKey by mutableStateOf<Any?>(null)
    private var focusedBoundsForPan by mutableStateOf<Rect?>(null)
    private var panOffsetY by mutableFloatStateOf(0f)
    private var animatedImeBottom by mutableStateOf<Int?>(null)
    private var imeOpeningState by mutableStateOf(false)
    private var stableImeBottom = 0

    fun updateRoot(coordinates: LayoutCoordinates) {
        rootCoordinates = coordinates
    }

    fun updateInput(key: Any, coordinates: LayoutCoordinates) {
        val measuredBounds = coordinates.boundsInRoot()
        // Input coordinates are reported after the root pan. Store them in the stable,
        // pre-pan coordinate space so switching fields never inherits the previous offset.
        val bounds = Rect(
            left = measuredBounds.left,
            top = measuredBounds.top - panOffsetY,
            right = measuredBounds.right,
            bottom = measuredBounds.bottom - panOffsetY,
        )
        inputBounds[key] = bounds
        if (focusedInputKey == key && focusedBoundsForPan == null) {
            focusedBoundsForPan = bounds
        }
    }

    fun removeInput(key: Any) {
        inputBounds.remove(key)
        if (focusedInputKey == key) {
            focusedInputKey = null
            focusedBoundsForPan = null
        }
    }

    fun updateInputFocus(key: Any, focused: Boolean) {
        if (focused) {
            focusedInputKey = key
            focusedBoundsForPan = inputBounds[key]
        } else if (focusedInputKey == key) {
            focusedInputKey = null
            focusedBoundsForPan = null
        }
    }

    val hasFocusedInput: Boolean
        get() = focusedInputKey != null

    val focusedBounds: Rect?
        get() = focusedBoundsForPan

    fun updatePanOffset(offsetY: Float) {
        panOffsetY = offsetY
    }

    fun updateAnimatedImeBottom(bottom: Int?) {
        animatedImeBottom = bottom
    }

    fun beginImeAnimation(targetBottom: Int) {
        imeOpeningState = stableImeBottom < targetBottom
    }

    fun finishImeAnimation() {
        stableImeBottom = animatedImeBottom ?: stableImeBottom
    }

    val imeBottomOverride: Int?
        get() = animatedImeBottom

    val isImeAnimationOpening: Boolean
        get() = imeOpeningState


    private fun isOutsideRootPosition(rootPosition: Offset): Boolean {
        return inputBounds.values.none { bounds -> bounds.contains(rootPosition) }
    }

    fun isOutsideInput(localPosition: Offset): Boolean {
        val root = rootCoordinates ?: return false
        val rootPosition = root.localToRoot(localPosition)
        return isOutsideRootPosition(rootPosition)
    }

}

/** Moves the complete editor/workspace layer just enough to keep the focused field above the IME. */
internal fun Modifier.imeWindowPan(boundary: TextInputFocusBoundary): Modifier = composed {
    val density = LocalDensity.current
    val imeBottom = boundary.imeBottomOverride
        ?: androidx.compose.foundation.layout.WindowInsets.ime.getBottom(density)
    val focusedBounds = boundary.focusedBounds
    val rootHeight = remember { mutableIntStateOf(0) }
    val margin = with(density) { 16.dp.toPx() }
    val imeTop = rootHeight.intValue - imeBottom
    val targetOffset = if (imeBottom > 0 && focusedBounds != null && imeTop > 0) {
        (imeTop - focusedBounds.bottom - margin).coerceAtMost(0f)
    } else {
        0f
    }
    val panOffset = remember { Animatable(0f) }
    val imeAnimationActive = boundary.imeBottomOverride != null
    LaunchedEffect(targetOffset, boundary.isImeAnimationOpening, imeAnimationActive) {
        if (imeAnimationActive && boundary.isImeAnimationOpening) {
            // Opening follows each platform frame immediately.
            panOffset.snapTo(targetOffset)
        }
    }
    LaunchedEffect(imeAnimationActive, boundary.isImeAnimationOpening) {
        if (imeAnimationActive && !boundary.isImeAnimationOpening) {
            // Start closing exactly once from the current visible offset. Do not key this effect
            // by the changing inset value, otherwise every frame cancels and restarts the tween.
            panOffset.animateTo(0f, animationSpec = tween(durationMillis = ComposeMotionTokens.FOCUS_PAN_DURATION_MILLIS))
        } else if (!imeAnimationActive) {
            panOffset.animateTo(targetOffset, animationSpec = tween(durationMillis = ComposeMotionTokens.FOCUS_PAN_DURATION_MILLIS))
        }
    }
    val animatedOffset = panOffset.value
    androidx.compose.runtime.SideEffect {
        boundary.updatePanOffset(animatedOffset)
    }
    this
        .onSizeChanged { rootHeight.intValue = it.height }
        .offset { IntOffset(0, animatedOffset.roundToInt()) }
}

@Composable
internal fun rememberTextInputFocusBoundary(): TextInputFocusBoundary = remember {
    TextInputFocusBoundary()
}

internal fun Modifier.reportTextInputFocusBounds(
    boundary: TextInputFocusBoundary,
    key: Any,
): Modifier = composed {
    DisposableEffect(boundary, key) {
        onDispose { boundary.removeInput(key) }
    }
    this
        .onGloballyPositioned { coordinates ->
            boundary.updateInput(key, coordinates)
        }
        .onFocusChanged { state ->
            boundary.updateInputFocus(key, state.isFocused)
        }
}

/** One focus boundary is shared by the editor content and its complete sheet chrome. */
internal val LocalTextInputFocusBoundary = staticCompositionLocalOf<TextInputFocusBoundary?> {
    null
}

/**
 * Gives an input-focused editor exclusive ownership of outside gestures while dismissing the IME.
 * With no focused input, the pointer remains untouched so buttons, scrolling, and sheet drags
 * retain their normal semantics.
 */
internal fun Modifier.clearTextInputFocusOutside(
    focusManager: FocusManager,
    boundary: TextInputFocusBoundary,
): Modifier = onGloballyPositioned { coordinates ->
    boundary.updateRoot(coordinates)
}.pointerInput(focusManager, boundary) {
    awaitEachGesture {
        val down = awaitFirstDown(
            requireUnconsumed = false,
            pass = PointerEventPass.Initial,
        )
        // A tap on another registered input is a focus-transfer gesture, not an outside
        // dismissal. Let the target field consume it so the window pan is recalculated for the
        // new focused editor instead of clearing the first field and racing the second focus.
        val dismissOutsideGesture = boundary.hasFocusedInput &&
            boundary.isOutsideInput(down.position)
        if (dismissOutsideGesture) {
            // While an editor input owns the IME, outside gestures belong to keyboard dismissal.
            // Consume the complete gesture so sheet dragging, scrolling, and button clicks do
            // not race the IME/inset transition and leave stale hit-test coordinates behind.
            down.consume()
            focusManager.clearFocus(force = true)
        }
        while (true) {
            val change = awaitPointerEvent(PointerEventPass.Initial)
                .changes
                .firstOrNull { it.id == down.id }
                ?: break
            if (dismissOutsideGesture) change.consume()
            if (!change.pressed) {
                break
            }
        }
    }
}

/**
 * Shows long app identity text as a slow ping-pong marquee only when it cannot fit on one line.
 * The parent remains clipped and stable, so the header never changes height or pushes controls.
 */
@Composable
internal fun AppIdentityMarqueeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = style.color,
    centerWhenStatic: Boolean = false,
    textHorizontalInset: androidx.compose.ui.unit.Dp = LocalSpacing.current.none,
    edgeFadeColor: Color? = null,
    edgeFadeEnabled: Boolean = true,
) {
    var containerWidth by remember { mutableIntStateOf(0) }
    var textWidth by remember { mutableIntStateOf(0) }
    val offset = remember { Animatable(0f) }
    val insetPx = with(LocalDensity.current) {
        textHorizontalInset.roundToPx().coerceAtLeast(0)
    }.coerceAtMost(containerWidth / 2)
    val scrollDistance = (textWidth - containerWidth + insetPx * 2).coerceAtLeast(0)
    val animatedStartFadeVisibility by animateFloatAsState(
        targetValue = if (edgeFadeColor != null && scrollDistance > 0 && offset.value < -0.5f) 1f else 0f,
        animationSpec = tween(HorizontalEdgeFadeTokens.VisibilityAnimationDurationMillis),
        label = "marquee-edge-fade-start",
    )
    val animatedEndFadeVisibility by animateFloatAsState(
        targetValue = if (edgeFadeColor != null && scrollDistance > 0 && offset.value > -scrollDistance + 0.5f) 1f else 0f,
        animationSpec = tween(HorizontalEdgeFadeTokens.VisibilityAnimationDurationMillis),
        label = "marquee-edge-fade-end",
    )

    LaunchedEffect(text, scrollDistance) {
        offset.stop()
        offset.snapTo(0f)
        if (scrollDistance == 0) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(900.milliseconds)
            offset.animateTo(-scrollDistance.toFloat(), tween(2200, easing = LinearEasing))
            kotlinx.coroutines.delay(700.milliseconds)
            offset.animateTo(0f, tween(2200, easing = LinearEasing))
        }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .horizontalEdgeFade(
                startVisibility = if (edgeFadeEnabled) animatedStartFadeVisibility else 0f,
                endVisibility = if (edgeFadeEnabled) animatedEndFadeVisibility else 0f,
                edgeColor = edgeFadeColor ?: Color.Transparent,
                edgeWidth = textHorizontalInset,
            )
            .onSizeChanged { containerWidth = it.width },
        contentAlignment = Alignment.TopStart
    ) {
        Text(
            text = text,
            modifier = Modifier
                .layout { measurable, constraints ->
                    // Measure the complete text independently of the viewport. The parent
                    // Box is the only clipping boundary; the text itself is never pre-clipped.
                    val placeable = measurable.measure(
                        constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity)
                    )
                    // Action labels retain their centered resting position. Once the label is
                    // wider than its viewport, it starts from the leading edge and follows the
                    // existing ping-pong path so no text is permanently hidden.
                    val insetPx = textHorizontalInset.roundToPx()
                        .coerceAtLeast(0)
                        .coerceAtMost(constraints.maxWidth / 2)
                    val staticStart = if (centerWhenStatic && placeable.width <= constraints.maxWidth - insetPx * 2) {
                        insetPx + (constraints.maxWidth - insetPx * 2 - placeable.width) / 2
                    } else {
                        insetPx
                    }
                    layout(constraints.maxWidth, placeable.height) {
                        placeable.placeRelative(staticStart + offset.value.roundToInt(), 0)
                    }
                },
            style = style,
            color = color,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            onTextLayout = { textWidth = it.size.width },
        )
    }
}

/** Shared two-action row used by the app and template editors. */
@Composable
internal fun EditorTypefaceHookRow(
    primary: @Composable (Modifier) -> Unit,
    secondary: @Composable (Modifier) -> Unit,
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val secondaryWidth = editorSecondaryControlWidth(
            maxWidth,
            AppConfigSheetUiTokens.TypefaceHookGap,
            isLandscape,
        )
        val primaryWidth = maxWidth - AppConfigSheetUiTokens.TypefaceHookGap - secondaryWidth
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                AppConfigSheetUiTokens.TypefaceHookGap,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            primary(Modifier.width(primaryWidth))
            secondary(Modifier.width(secondaryWidth))
        }
    }
}

/** Keeps the compact editor label's floating transition decisive without changing app-wide M3 motion. */
private object EditorTextFieldMotionScheme : MotionScheme {
    private fun <T> noBounceSpec(): FiniteAnimationSpec<T> = tween(
        durationMillis = 150,
        easing = FastOutSlowInEasing,
    )

    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = noBounceSpec()

    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = noBounceSpec()

    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = noBounceSpec()

    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = noBounceSpec()

    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = noBounceSpec()

    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = noBounceSpec()
}

/**
 * Value plus mode selector row. The mode selector consumes a proportional share at rest, then
 * yields its space while a sufficiently wide value field has focus. Only horizontal constraints
 * animate, keeping sheet height and its partial anchor stable.
 */
@Composable
internal fun EditorValueModeRow(
    input: @Composable (Modifier, (Boolean) -> Unit) -> Unit,
    first: String,
    second: String,
    firstSelected: Boolean,
    onFirst: () -> Unit,
    onSecond: () -> Unit,
    labelStyle: TextStyle,
) {
    var inputFocused by remember { mutableStateOf(false) }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppConfigSheetUiTokens.FieldTopInset + rememberEditorControlHeight()),
    ) {
        val restingModeWidth = editorSecondaryControlWidth(maxWidth, 8.dp, isLandscape)
        // The available width is already expressed in the user's scaled density. A fixed dp
        // threshold therefore rejects expansion at larger interface scales despite unchanged
        // physical width. The mode track itself is the only meaningful minimum constraint.
        val targetReservedWidth = if (inputFocused && maxWidth > 0.dp) {
            0.dp
        } else {
            restingModeWidth + 8.dp
        }
        val reservedWidth by animateDpAsState(
            targetValue = targetReservedWidth,
            animationSpec = tween(
                durationMillis = AppConfigSheetUiTokens.EditorRowWidthAnimationDurationMillis,
                easing = FastOutSlowInEasing,
            ),
            label = "editor-value-mode-width",
        )
        val inputWidth = (maxWidth - reservedWidth).coerceAtLeast(0.dp)
        val modeWidth = (reservedWidth - 8.dp).coerceAtLeast(0.dp)
        Row(verticalAlignment = Alignment.Bottom) {
            Box(
                modifier = Modifier
                    .width(inputWidth)
                    .padding(top = AppConfigSheetUiTokens.FieldTopInset),
            ) {
                input(Modifier.fillMaxWidth()) { inputFocused = it }
            }
            if (modeWidth > 0.dp) {
                Box(
                    modifier = Modifier
                        .width(8.dp + modeWidth)
                        .padding(start = 8.dp, top = AppConfigSheetUiTokens.FieldTopInset)
                        .clipToBounds(),
                ) {
                    ModeSelector(
                        selectedFirst = firstSelected,
                        firstLabel = first,
                        secondLabel = second,
                        onFirstSelected = onFirst,
                        onSecondSelected = onSecond,
                        labelStyle = labelStyle,
                        width = modeWidth,
                    )
                }
            }
        }
    }
}

internal fun editorSecondaryControlWidth(
    availableWidth: androidx.compose.ui.unit.Dp,
    gap: androidx.compose.ui.unit.Dp,
    isLandscape: Boolean,
) = ((availableWidth - gap).coerceAtLeast(0.dp) * if (isLandscape) {
    AppConfigSheetUiTokens.LandscapeSecondaryControlWidthFraction
} else {
    AppConfigSheetUiTokens.PortraitSecondaryControlWidthFraction
})
    .coerceAtMost(AppConfigSheetUiTokens.SecondaryControlMaxWidth)
    // Keep the input at least as wide as its mode selector, including the row gap.
    .coerceAtMost(((availableWidth - gap) / 2).coerceAtLeast(0.dp))

/**
 * Two-choice mode selector shared by template and app editors.
 *
 * The moving thumb is visual only; the semantic radio labels remain above it and the track
 * dispatches its own half-width taps. This prevents the animated layer from swallowing taps.
 */
@Composable
internal fun ModeSelector(
    selectedFirst: Boolean,
    firstLabel: String,
    secondLabel: String,
    onFirstSelected: () -> Unit,
    onSecondSelected: () -> Unit,
    labelStyle: TextStyle,
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp = AppConfigSheetUiTokens.SecondaryControlMaxWidth,
) {
    val modeInteractionSource = remember { MutableInteractionSource() }
    val selectFirst = rememberClickAction(onFirstSelected)
    val selectSecond = rememberClickAction(onSecondSelected)
    val controlHeight = rememberEditorControlHeight()
    // Animate a normalized position, not a Dp target derived from the animated row width.
    // This keeps the selected half stable while the entire selector slides out of view.
    val thumbPosition by animateFloatAsState(
        targetValue = if (selectedFirst) 0f else 1f,
        animationSpec = tween(ComposeMotionTokens.MODE_TRANSITION_DURATION_MILLIS),
        label = "dpis-mode-thumb"
    )
    val targetThumbPosition = if (selectedFirst) 0f else 1f
    val edgeFadeEnabled = abs(thumbPosition - targetThumbPosition) < 0.001f
    val thumbWidth = width / 2
    val thumbOffset = Modifier.offset {
        IntOffset(((width / 2) * thumbPosition).roundToPx(), 0)
    }
    Box(
        modifier = modifier
            .width(width)
            .height(controlHeight)
            .clip(AppConfigSheetUiTokens.FieldAndActionShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .selectableGroup()
    ) {
        Box(
            modifier = thumbOffset
                // The thumb occupies half of the track. Its right-mode origin is therefore the
                // track's half-width, not the full width (which would place it outside the clip).
                .width(thumbWidth)
                .height(controlHeight)
                .clip(AppConfigSheetUiTokens.FieldAndActionShape)
                .background(MaterialTheme.colorScheme.secondaryContainer, AppConfigSheetUiTokens.FieldAndActionShape)
                // The two halves own hit testing, but press feedback belongs to the animated
                // thumb. Sharing this source prevents a rectangular half-track ripple.
                .indication(modeInteractionSource, ripple(bounded = true))
        )
        Row(Modifier.fillMaxSize().zIndex(1f)) {
            ModeLabel(
                firstLabel, selectedFirst, labelStyle, selectFirst,
                modeInteractionSource, Modifier.weight(1f), edgeFadeEnabled
            )
            ModeLabel(
                secondLabel, !selectedFirst, labelStyle, selectSecond,
                modeInteractionSource, Modifier.weight(1f), edgeFadeEnabled
            )
        }
        // Keep the selected thumb outline above the label fade. The label layer must stay above
        // the thumb background for text and hit testing, but it must never paint over this edge.
        Box(
            modifier = thumbOffset
                .width(thumbWidth)
                .height(controlHeight)
                .clip(AppConfigSheetUiTokens.FieldAndActionShape)
                .border(
                    AppConfigSheetUiTokens.ModeThumbBorderWidth,
                    MaterialTheme.colorScheme.outline,
                    AppConfigSheetUiTokens.FieldAndActionShape,
                )
                .zIndex(2f),
        )
    }
}

@Composable
private fun ModeLabel(
    label: String,
    selected: Boolean,
    labelStyle: TextStyle,
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource,
    modifier: Modifier,
    edgeFadeEnabled: Boolean,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                interactionSource = interactionSource,
                indication = null,
                // Each half requests its own target. Re-selecting the active half is a no-op;
                // it must not invert the mode or restart the thumb animation.
                onClick = { if (!selected) onClick() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        AppIdentityMarqueeText(
            text = label,
            modifier = Modifier
                .fillMaxWidth(),
            style = labelStyle.copy(
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold
                else androidx.compose.ui.text.font.FontWeight.Normal
            ),
            centerWhenStatic = true,
            textHorizontalInset = LocalSpacing.current.xs,
            edgeFadeEnabled = edgeFadeEnabled,
            edgeFadeColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        )
    }
}
