package com.dpis.module.ui.presentation.workspace

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dpis.module.ui.presentation.design.rememberClickAction

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ToolbarIconButton(
    @DrawableRes iconRes: Int,
    @StringRes descriptionRes: Int,
    onClick: () -> Unit,
) {
    val description = stringResource(descriptionRes)
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Below,
        ),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = rememberClickAction(onClick)) {
            Icon(painterResource(iconRes), contentDescription = description)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun ToolbarOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    groupCount: Int = 1,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismiss,
        popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
            MenuAnchorPosition.Below,
        ),
    ) {
        if (groupCount == 1) {
            ToolbarOverflowMenuGroup(content = content)
        } else {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun ToolbarOverflowMenuGroup(
    index: Int = 0,
    count: Int = 1,
    spacingAfter: Boolean = false,
    containerColor: Color = if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    },
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = if (spacingAfter) Modifier.padding(bottom = 4.dp) else Modifier,
    ) {
        DropdownMenuGroup(
            shapes = MenuDefaults.groupShape(index, count),
            containerColor = containerColor,
            content = content,
        )
    }
}

@Composable
internal fun ToolbarOverflowMenuItem(
    @StringRes textRes: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(stringResource(textRes)) },
        onClick = rememberClickAction(onClick),
        enabled = enabled,
    )
}
