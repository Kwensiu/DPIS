package com.dpis.module.ui.compose

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
    // The edge is an occlusion shadow, so it stays dark in both light and dark themes.
    edgeColor: Color = Color.Black.copy(alpha = 0.12f),
    edgeWidth: Dp = 20.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .drawWithContent {
                drawContent()
                val edgePx = edgeWidth.toPx()
                if (scrollState.value > 0) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(edgeColor, Color.Transparent),
                            startX = 0f,
                            endX = edgePx,
                        ),
                        topLeft = Offset.Zero,
                        size = Size(edgePx, size.height),
                    )
                }
                if (scrollState.value < scrollState.maxValue) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, edgeColor),
                            startX = size.width - edgePx,
                            endX = size.width,
                        ),
                        topLeft = Offset(size.width - edgePx, 0f),
                        size = Size(edgePx, size.height),
                    )
                }
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(contentPadding),
            horizontalArrangement = horizontalArrangement,
            content = content,
        )
    }
}
