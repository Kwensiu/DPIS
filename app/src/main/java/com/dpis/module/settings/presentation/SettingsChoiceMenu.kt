package com.dpis.module.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dpis.module.ui.presentation.design.rememberClickValueAction

internal data class SettingsChoiceOption(val value: String, val label: String)

/** Anchored choice menu shared by settings rows that pick one discrete value. */
@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun SettingsChoiceMenu(
    expanded: Boolean,
    options: List<SettingsChoiceOption>,
    selected: String,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    val selectOption = rememberClickValueAction(onSelected)
    Box(Modifier.fillMaxWidth()) {
        content()
        // Anchor below the setting's trailing edge. Starting at the row's top edge lets the
        // opening tap land on the first item after the popup is composed.
        Box(Modifier.align(Alignment.BottomEnd)) {
            DropdownMenuPopup(
                expanded = expanded,
                onDismissRequest = onDismiss,
                popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
                    MenuAnchorPosition.Below,
                ),
                modifier = Modifier
                    // Let the widest option define the menu. A fixed minimum made short
                    // choices look detached from their setting on compact displays.
                    .widthIn(max = 360.dp)
                    .heightIn(max = 360.dp),
            ) {
                Surface(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .heightIn(max = 360.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = MenuDefaults.TonalElevation,
                    shadowElevation = MenuDefaults.ShadowElevation,
                ) {
                    // Keep the vertical inset inside the scroll container. The stock menu
                    // applies it outside the scroller, producing an extra dark band and a
                    // second clipping edge before items reach the rounded menu boundary.
                    Column(
                        modifier = Modifier
                            .width(IntrinsicSize.Max)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                    ) {
                        options.forEach { option ->
                            val isSelected = selected == option.value
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.label,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                    )
                                },
                                onClick = { selectOption(option.value) },
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clip(MaterialTheme.shapes.large)
                                    .background(
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            Color.Transparent
                                        },
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}
