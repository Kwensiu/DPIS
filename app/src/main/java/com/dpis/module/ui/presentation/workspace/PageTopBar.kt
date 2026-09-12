package com.dpis.module.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dpis.module.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CollapsingPageTopBar(
    onBack: (() -> Unit)?,
    includeHorizontalSafeInsets: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior,
    title: @Composable () -> Unit,
    subtitle: @Composable (() -> Unit)? = null,
) {
    LargeFlexibleTopAppBar(
        title = { PageTitleSlot(title, startInset = !includeHorizontalSafeInsets) },
        subtitle = subtitle?.let { content ->
            {
                CompositionLocalProvider(LocalTextStyle provides pageExpandedSubtitleStyle()) {
                    PageTitleSlot(content, startInset = !includeHorizontalSafeInsets)
                }
            }
        },
        navigationIcon = { PageNavigationIcon(onBack?.let { rememberClickAction(it) }) },
        actions = actions,
        scrollBehavior = scrollBehavior,
        windowInsets = pageTopBarWindowInsets(includeHorizontalSafeInsets),
        colors = pageTopBarColors(),
    )
}

/** Compact workspace app bar for pages whose top area contains tools or search. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PinnedPageTopBar(
    onBack: (() -> Unit)? = null,
    includeHorizontalSafeInsets: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    title: @Composable () -> Unit
) {
    val ruleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    TopAppBar(
        modifier = modifier.drawWithContent {
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
        title = { ProvideCompactTitle(title, startInset = !includeHorizontalSafeInsets) },
        navigationIcon = { PageNavigationIcon(onBack?.let { rememberClickAction(it) }) },
        actions = actions,
        windowInsets = pageTopBarWindowInsets(includeHorizontalSafeInsets),
        colors = pageTopBarColors(),
    )
}

/**
 * Landscape tool slot used only by the template app picker.
 * Height matches the left-pane search slot; the rule sits below the slot, not inside it.
 */
@Composable
internal fun SplitPaneHeader(
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    showDivider: Boolean = true,
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit = {},
) {
    val ruleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(PageChromeTokens.CompactSlotHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PageNavigationIcon(onBack?.let { rememberClickAction(it) })
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                ProvideCompactTitle(title)
            }
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
        if (showDivider) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ruleColor),
            )
        }
    }
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
        title = { ProvideCompactTitle(title, startInset = !includeHorizontalSafeInsets) },
        navigationIcon = { PageNavigationIcon(onBack?.let { rememberClickAction(it) }) },
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

@Composable
private fun ProvideCompactTitle(
    title: @Composable () -> Unit,
    startInset: Boolean = true,
) {
    CompositionLocalProvider(LocalTextStyle provides pageCompactTitleStyle()) {
        PageTitleSlot(title, startInset)
    }
}

@Composable
private fun PageTitleSlot(
    content: @Composable () -> Unit,
    startInset: Boolean = true,
) {
    Box(if (startInset) Modifier.pageTitleStart() else Modifier) {
        content()
    }
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
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    ),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back_24),
                            contentDescription = stringResource(R.string.system_settings_back),
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.size(PageChromeTokens.ContentInset))
        }
    }
}
