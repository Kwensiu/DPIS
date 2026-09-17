package com.dpis.module.appconfig.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dpis.module.R
import com.dpis.module.ui.presentation.design.dpisClickable
import com.dpis.module.ui.presentation.design.wizardHintSurface

/** Arrow plus bubble for the collapsed-editor advanced-section hint. */
@Composable
internal fun AppConfigWizardHint(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    val hintSurface = MaterialTheme.colorScheme.wizardHintSurface()
    Column(
        modifier = modifier
            .offset(y = AppConfigSheetUiTokens.WizardHintTopOffset)
            .padding(horizontal = 20.dp)
            .zIndex(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(
            modifier = Modifier.size(width = 14.dp, height = 7.dp),
        ) {
            val path = Path().apply {
                moveTo(size.width / 2f, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(path, color = hintSurface)
        }
        Surface(
            shape = AppConfigSheetUiTokens.WizardHintShape,
            color = hintSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        ) {
            Row(
                modifier = Modifier.padding(
                    start = 14.dp, top = 6.dp, end = 6.dp, bottom = 6.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.dialog_advanced_wizard_hint),
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.labelMedium,
                )
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(AppConfigSheetUiTokens.WizardHintCloseSize)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .dpisClickable(role = Role.Button, onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close_24),
                        contentDescription = stringResource(
                            R.string.dialog_advanced_wizard_close,
                        ),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}
