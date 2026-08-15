package com.dpis.module.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

/**
 * A horizontally scrollable row whose edge fades reflect the current scroll position.
 *
 * The fade is only present while content remains hidden on that side, so a fully visible row
 * and either terminal scroll position do not retain misleading decoration.
 */
@Composable
internal fun DpisHorizontalScrollWithEdgeFade(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    edgeColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    edgeWidth: Dp = 20.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(contentPadding),
            horizontalArrangement = horizontalArrangement,
            content = content,
        )
        if (scrollState.value > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(edgeWidth)
                    .fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(edgeColor, Color.Transparent))),
            )
        }
        if (scrollState.value < scrollState.maxValue) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(edgeWidth)
                    .fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, edgeColor))),
            )
        }
    }
}
