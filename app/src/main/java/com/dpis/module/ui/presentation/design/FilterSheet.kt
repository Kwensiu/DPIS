package com.dpis.module.ui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dpis.module.R

/** Shared visual contract for lightweight filter sheets; filter state stays feature-owned. */
internal object FilterSheetUiTokens {
    val ContentHorizontalPadding = 16.dp
    val ContentVerticalPadding = 20.dp
    val HeaderActionSize = 40.dp
    val HeaderTitleOffset = (-4).dp
    val HeaderActionSpacing = 8.dp
    val ResetActionSize = 32.dp
    val ResetIconSize = 18.dp
    val PillShape = RoundedCornerShape(50)
}

/** Standard filter-sheet frame shared by app and template catalogues. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterSheetScaffold(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    trailingContent: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    horizontal = FilterSheetUiTokens.ContentHorizontalPadding,
                    vertical = FilterSheetUiTokens.ContentVerticalPadding,
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                FeedbackIconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .size(FilterSheetUiTokens.HeaderActionSize)
                        .offset(x = (-8).dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close_24),
                        contentDescription = stringResource(R.string.dialog_close),
                    )
                }
                title()
                Spacer(Modifier.weight(1f))
                trailingContent()
            }
            content()
        }
    }
}

@Composable
internal fun FilterSheetResetButton(
    onClick: () -> Unit,
    contentDescription: String,
) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(FilterSheetUiTokens.ResetActionSize)
            .clip(FilterSheetUiTokens.PillShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                FilterSheetUiTokens.PillShape,
            )
            .dpisClickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_reset_settings_24),
            contentDescription = contentDescription,
            modifier = Modifier.size(FilterSheetUiTokens.ResetIconSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
