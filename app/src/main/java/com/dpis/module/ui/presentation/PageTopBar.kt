package com.dpis.module.ui.compose

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TwoRowsTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dpis.module.R

/** Large title that collapses into the app bar as the page scrolls. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CollapsingPageTopBar(
    onBack: (() -> Unit)?,
    includeHorizontalSafeInsets: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior,
    collapsedTitleScale: Float = 1f,
    title: @Composable () -> Unit,
    collapsedTitle: (@Composable () -> Unit)? = null
) {
    var titleLeftPx by remember { mutableIntStateOf(0) }
    var titleRightPx by remember { mutableIntStateOf(0) }
    var topBarLeftPx by remember { mutableIntStateOf(0) }
    val collapsedFraction = scrollBehavior.state.collapsedFraction
    val ruleColor = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current
    val collapsedBarBottomPx = with(density) {
        WindowInsets.statusBars.getTop(this) + TopAppBarDefaults.LargeAppBarCollapsedHeight.toPx()
    }
    TwoRowsTopAppBar(
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                topBarLeftPx = coordinates.positionInRoot().x.toInt()
            }
            .drawWithContent {
                drawContent()
                val fraction = collapsedFraction.coerceIn(0f, 1f)
                val titleStart = (titleLeftPx - topBarLeftPx).toFloat().coerceIn(0f, size.width)
                val titleEnd = (titleRightPx - topBarLeftPx).toFloat().coerceIn(titleStart, size.width)
                val lineStart = lerp(titleStart, 0f, fraction)
                val lineEnd = lerp(titleEnd, size.width, fraction)
                val alpha = fraction
                val lineTop = (collapsedBarBottomPx - 1.dp.toPx()).coerceIn(0f, size.height)
                if (lineEnd > lineStart && alpha > 0f) {
                    drawRect(
                        color = ruleColor.copy(alpha = alpha * 0.18f),
                        topLeft = Offset(lineStart, lineTop),
                        size = Size(lineEnd - lineStart, 1.dp.toPx())
                    )
                }
                drawEdgeOcclusionFade(
                    visibility = collapsedFraction,
                    direction = EdgeOcclusionFadeDirection.TOP_TO_BOTTOM,
                    edgePosition = lineTop,
                )
            },
        title = { expanded ->
            androidx.compose.foundation.layout.Box(
                Modifier
                    .padding(start = if (onBack == null) 12.dp else 0.dp)
                    .onGloballyPositioned { coordinates ->
                        val position = coordinates.positionInRoot()
                        titleLeftPx = position.x.toInt()
                        titleRightPx = (position.x + coordinates.size.width).toInt()
                    }
                    .graphicsLayer {
                        val scale = if (expanded) 1f else collapsedTitleScale.coerceIn(0.5f, 1f)
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
            ) {
                CompositionLocalProvider(
                    LocalTextStyle provides if (expanded) {
                    // Keep the expanded title larger than the compact app-bar title;
                    // the size lives in the typography token rather than this scaffold.
                    MaterialTheme.typography.dpisExpandedPageTitle
                    } else {
                        MaterialTheme.typography.titleLarge
                    }
                ) {
                    if (!expanded && collapsedTitle != null) collapsedTitle() else title()
                }
            }
        },
        navigationIcon = { PageNavigationIcon(onBack?.let { rememberConfirmAction(it) }) },
        actions = actions,
        windowInsets = pageTopBarWindowInsets(includeHorizontalSafeInsets),
        colors = pageTopBarColors(),
        scrollBehavior = scrollBehavior
    )
}

/** Compact workspace app bar for pages whose top area contains tools or search. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PinnedPageTopBar(
    onBack: (() -> Unit)? = null,
    includeHorizontalSafeInsets: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior,
    showDivider: Boolean = true,
    title: @Composable () -> Unit
) {
    val ruleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    TopAppBar(
        modifier = Modifier.drawWithContent {
            drawContent()
            if (showDivider) {
                drawLine(
                    color = ruleColor,
                    start = Offset(0f, size.height - 1.dp.toPx()),
                    end = Offset(size.width, size.height - 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
        },
        title = title,
        navigationIcon = { PageNavigationIcon(onBack?.let { rememberConfirmAction(it) }) },
        actions = actions,
        windowInsets = pageTopBarWindowInsets(includeHorizontalSafeInsets),
        colors = pageTopBarColors(),
        scrollBehavior = scrollBehavior
    )
}

/** In-flow header for embedded landscape selectors; it must not overlay list content. */
@Composable
internal fun InFlowPageHeader(
    onBack: (() -> Unit)?,
    includeHorizontalSafeInsets: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    showDivider: Boolean = true,
    title: @Composable () -> Unit
) {
    val ruleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    TopAppBar(
        modifier = Modifier.drawWithContent {
            drawContent()
            if (showDivider) {
                drawLine(
                    color = ruleColor,
                    start = Offset(0f, size.height - 1.dp.toPx()),
                    end = Offset(size.width, size.height - 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
        },
        title = title,
        navigationIcon = { PageNavigationIcon(onBack?.let { rememberConfirmAction(it) }) },
        actions = actions,
        windowInsets = pageTopBarWindowInsets(includeHorizontalSafeInsets),
        colors = pageTopBarColors()
    )
}

/** Compatibility name for callers that still describe this in-flow header as a page top bar. */
@Composable
internal fun SecondaryPageTopBar(
    onBack: (() -> Unit)?,
    includeHorizontalSafeInsets: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    title: @Composable () -> Unit
) = InFlowPageHeader(
    onBack = onBack,
    includeHorizontalSafeInsets = includeHorizontalSafeInsets,
    actions = actions,
    showDivider = true,
    title = title,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun pageTopBarWindowInsets(includeHorizontalSafeInsets: Boolean): WindowInsets = if (includeHorizontalSafeInsets) {
        // Secondary pages keep navigation and title content clear of the camera
        // cutout; primary workspace bars intentionally paint beneath it.
        WindowInsets.statusBars.only(WindowInsetsSides.Top)
            .union(WindowInsets.displayCutout)
            .union(WindowInsets(left = 12.dp))
    } else {
        WindowInsets.statusBars.only(WindowInsetsSides.Top)
    }

@Composable
private fun pageTopBarColors() = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
    )

@Composable
private fun PageNavigationIcon(onBack: (() -> Unit)?) {
    if (onBack != null) {
        Row {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back_24),
                        contentDescription = stringResource(R.string.system_settings_back),
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
        }
    }
}
