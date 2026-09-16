package com.dpis.module.ui.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.SelectableChipElevation
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import com.dpis.module.R

@Composable
internal fun successButtonColors(): ButtonColors {
    val semantic = LocalSemanticColors.current
    return ButtonDefaults.buttonColors(
        containerColor = semantic.successContainer,
        contentColor = semantic.onSuccessContainer,
    )
}

@Composable
internal fun warningButtonColors(): ButtonColors {
    val semantic = LocalSemanticColors.current
    return ButtonDefaults.outlinedButtonColors(
        containerColor = semantic.warningContainer,
        contentColor = semantic.onWarningContainer,
    )
}

/** Material button with the product's discrete confirmation feedback. */
@Composable
fun FeedbackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = rememberClickAction(onClick),
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        contentPadding = contentPadding,
        content = content,
    )
}

@Composable
fun FeedbackOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = rememberClickAction(onClick),
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        content = content,
    )
}

@Composable
fun FeedbackTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    TextButton(
        onClick = rememberClickAction(onClick),
        modifier = modifier,
        enabled = enabled,
        content = content,
    )
}

@Composable
fun FeedbackIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = rememberClickAction(onClick),
        modifier = modifier,
        enabled = enabled,
        content = content,
    )
}

@Composable
fun FeedbackSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SwitchColors? = null,
) {
    val feedbackCallback = onCheckedChange?.let { rememberClickValueAction<Boolean>(it) }
    DpisSwitch(
        checked = checked,
        onCheckedChange = feedbackCallback,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
    )
}

/** Thumb glyphs keep checked and unchecked readable at a glance. */
@Composable
fun DpisSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SwitchColors? = null,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = colors ?: dpisSwitchColors(),
        thumbContent = {
            Icon(
                painter = painterResource(if (checked) R.drawable.ic_check_24 else R.drawable.ic_close_24),
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        },
    )
}

@Composable
private fun dpisSwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedIconColor = MaterialTheme.colorScheme.primary,
    uncheckedIconColor = MaterialTheme.colorScheme.surfaceContainerHighest,
)

@Composable
fun FeedbackFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = FilterChipDefaults.shape,
    leadingIcon: (@Composable (() -> Unit))? = null,
    trailingIcon: (@Composable (() -> Unit))? = null,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors(),
    elevation: SelectableChipElevation? = FilterChipDefaults.filterChipElevation(),
) {
    FilterChip(
        selected = selected,
        onClick = rememberClickAction(onClick),
        label = label,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        colors = colors,
        elevation = elevation,
    )
}
