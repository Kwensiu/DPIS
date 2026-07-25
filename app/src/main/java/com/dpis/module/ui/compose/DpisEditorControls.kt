package com.dpis.module.ui.compose

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.runtime.CompositionLocalProvider
import com.dpis.module.R

/**
 * Fixed-height outlined input used by DPIS editor rows.
 *
 * Its 48dp visual outline aligns with the mode track. Validation text belongs to the owning
 * row, outside this control, so an error never changes or clips the field's visible container.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DpisCompactEditorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false,
    onFocused: (() -> Unit)? = null,
    trailingIcon: (@Composable (() -> Unit))? = null,
    alwaysFloatLabel: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(modifier = modifier.height(TemplateUiTokens.SheetInputHeight)) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxSize()
                .onFocusChanged { if (it.isFocused) onFocused?.invoke() },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            singleLine = true,
            keyboardOptions = keyboardOptions,
            interactionSource = interactionSource,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = value,
                    innerTextField = innerTextField,
                    enabled = true,
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    isError = isError,
                    label = if (alwaysFloatLabel) null else label,
                    trailingIcon = trailingIcon,
                    contentPadding = TemplateUiTokens.SheetInputContentPadding,
                    container = {
                        OutlinedTextFieldDefaults.Container(
                            enabled = true,
                            isError = isError,
                            interactionSource = interactionSource,
                            shape = TemplateUiTokens.SheetInputShape
                        )
                    }
                )
            }
        )
        if (alwaysFloatLabel) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(y = (-8).dp)
                    .padding(start = 12.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 4.dp)
            ) {
                CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodySmall) {
                    label()
                }
            }
        }
    }
}

@Composable
internal fun DpisEditorClearButton(onClear: () -> Unit) {
    IconButton(onClick = onClear) {
        Icon(painterResource(R.drawable.ic_close_24), stringResource(R.string.search_clear))
    }
}

/**
 * Two-choice mode selector shared by template and app editors.
 *
 * The moving thumb is visual only; the semantic radio labels remain above it and the track
 * dispatches its own half-width taps. This prevents the animated layer from swallowing taps.
 */
@Composable
internal fun DpisModeSelector(
    selectedFirst: Boolean,
    firstLabel: String,
    secondLabel: String,
    onFirstSelected: () -> Unit,
    onSecondSelected: () -> Unit,
    labelStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    val modeInteractionSource = remember { MutableInteractionSource() }
    val thumbOffset by animateDpAsState(
        targetValue = if (selectedFirst) 0.dp else TemplateUiTokens.SheetSelectorWidth / 2,
        animationSpec = tween(TemplateUiTokens.ModeAnimationDurationMillis),
        label = "dpis-mode-thumb"
    )
    val thumbWidth = TemplateUiTokens.SheetSelectorWidth / 2
    Box(
        modifier = modifier
            .width(TemplateUiTokens.SheetSelectorWidth)
            .height(TemplateUiTokens.SheetSelectorHeight)
            .clip(TemplateUiTokens.ModeTrackShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .selectableGroup()
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(width = thumbWidth, height = TemplateUiTokens.SheetSelectorHeight)
                .clip(TemplateUiTokens.ModeThumbShape)
                .background(MaterialTheme.colorScheme.secondaryContainer, TemplateUiTokens.ModeThumbShape)
                .border(
                    TemplateUiTokens.ModeThumbBorderWidth,
                    MaterialTheme.colorScheme.outline,
                    TemplateUiTokens.ModeThumbShape
                )
                // The two halves own hit testing, but press feedback belongs to the animated
                // thumb. Sharing this source prevents a rectangular half-track ripple.
                .indication(modeInteractionSource, ripple(bounded = true))
        )
        Row(Modifier.fillMaxSize().zIndex(1f)) {
            DpisModeLabel(
                firstLabel, selectedFirst, labelStyle, onFirstSelected,
                modeInteractionSource, Modifier.weight(1f)
            )
            DpisModeLabel(
                secondLabel, !selectedFirst, labelStyle, onSecondSelected,
                modeInteractionSource, Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DpisModeLabel(
    label: String,
    selected: Boolean,
    labelStyle: TextStyle,
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource,
    modifier: Modifier
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
                onClick = { if (!selected) onClick() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            modifier = Modifier.scale(if (selected) 1.04f else 1f),
            style = labelStyle.copy(
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold
                else androidx.compose.ui.text.font.FontWeight.Normal
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
