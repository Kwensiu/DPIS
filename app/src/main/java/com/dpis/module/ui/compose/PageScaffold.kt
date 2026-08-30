package com.dpis.module.ui.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource

internal enum class PageBarBehavior { Collapsing, Pinned }

@Composable
internal fun SecondaryPageScaffold(
    @StringRes titleRes: Int, onBack: () -> Unit, modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}, bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {}, content: @Composable (PaddingValues) -> Unit
) = PageScaffold(titleRes = titleRes, pageBar = PageBarBehavior.Collapsing, onBack = onBack, modifier = modifier, actions = actions, bottomBar = bottomBar, floatingActionButton = floatingActionButton, content = content)

@Composable
internal fun SecondaryPageScaffold(
    onBack: (() -> Unit)?, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {}, floatingActionButton: @Composable () -> Unit = {},
    title: @Composable () -> Unit, content: @Composable (PaddingValues) -> Unit
) = PageScaffold(pageBar = PageBarBehavior.Collapsing, onBack = onBack, modifier = modifier, actions = actions, bottomBar = bottomBar, floatingActionButton = floatingActionButton, title = title, content = content)

@Composable
internal fun PrimaryPageScaffold(
    @StringRes titleRes: Int, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {},
    showTopBarDivider: Boolean = true,
    bottomBar: @Composable () -> Unit = {}, floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) = PageScaffold(titleRes = titleRes, pageBar = PageBarBehavior.Pinned, onBack = null, showTopBarDivider = showTopBarDivider, modifier = modifier, actions = actions, bottomBar = bottomBar, floatingActionButton = floatingActionButton, content = content)

@Composable
internal fun PrimaryPageScaffold(
    modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {},
    showTopBarDivider: Boolean = true,
    bottomBar: @Composable () -> Unit = {}, floatingActionButton: @Composable () -> Unit = {},
    title: @Composable () -> Unit, content: @Composable (PaddingValues) -> Unit
) = PageScaffold(pageBar = PageBarBehavior.Pinned, onBack = null, showTopBarDivider = showTopBarDivider, modifier = modifier, actions = actions, bottomBar = bottomBar, floatingActionButton = floatingActionButton, title = title, content = content)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PageScaffold(
    @StringRes titleRes: Int,
    pageBar: PageBarBehavior,
    onBack: (() -> Unit)? = null,
    collapsedTitleScale: Float = 1f,
    showTopBarDivider: Boolean = true,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    scrollStore: PageScrollPositionStore? = null,
    scrollKey: String? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    PageScaffold(
        pageBar = pageBar,
        onBack = onBack,
        collapsedTitleScale = collapsedTitleScale,
        showTopBarDivider = showTopBarDivider,
        modifier = modifier,
        actions = actions,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        scrollStore = scrollStore,
        scrollKey = scrollKey,
        title = { Text(stringResource(titleRes)) },
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PageScaffold(
    pageBar: PageBarBehavior,
    onBack: (() -> Unit)? = null,
    collapsedTitleScale: Float = 1f,
    showTopBarDivider: Boolean = true,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    scrollStore: PageScrollPositionStore? = null,
    scrollKey: String? = null,
    title: @Composable () -> Unit,
    collapsedTitle: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val scrollBehavior = when (pageBar) {
        PageBarBehavior.Collapsing -> TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        PageBarBehavior.Pinned -> TopAppBarDefaults.pinnedScrollBehavior()
    }
    LaunchedEffect(scrollBehavior, pageBar, scrollStore, scrollKey) {
        if (pageBar != PageBarBehavior.Collapsing || scrollStore == null || scrollKey == null) return@LaunchedEffect
        snapshotFlow { scrollBehavior.state.heightOffsetLimit }
            .collect { limit ->
                if (limit < 0f) {
                    scrollBehavior.state.heightOffset = if (scrollStore.topBarCollapsedFor(scrollKey)) limit else 0f
                    return@collect
                }
            }
    }
    LaunchedEffect(scrollBehavior, scrollStore, scrollKey) {
        if (pageBar != PageBarBehavior.Collapsing || scrollStore == null || scrollKey == null) return@LaunchedEffect
        snapshotFlow { scrollBehavior.state.collapsedFraction }
            .collect { fraction -> scrollStore.updateTopBar(scrollKey, fraction >= 0.98f) }
    }
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        // Keep the page canvas at the same surface level as the top app bar.
        // Individual cards then provide the intentional brighter elevation contrast.
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        // Transparent cannot resolve a contrasting content color itself; keep descendants on the
        // DPIS surface foreground instead of inheriting LocalContentColor's black fallback.
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = {
            when (pageBar) {
                PageBarBehavior.Collapsing -> CollapsingPageTopBar(onBack = onBack, includeHorizontalSafeInsets = onBack != null, actions = actions, scrollBehavior = scrollBehavior, collapsedTitleScale = collapsedTitleScale, title = title, collapsedTitle = collapsedTitle)
                PageBarBehavior.Pinned -> PinnedPageTopBar(onBack = onBack, includeHorizontalSafeInsets = onBack != null, actions = actions, scrollBehavior = scrollBehavior, showDivider = showTopBarDivider, title = title)
            }
        },
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = FabPosition.End
    ) { scaffoldPadding ->
        content(
            if (pageBar == PageBarBehavior.Collapsing) {
                pageContentPadding(scaffoldPadding, includeHorizontalSafeInsets = onBack != null)
            } else {
                scaffoldPadding
            }
        )
    }
}

@Composable
private fun pageContentPadding(
    scaffoldPadding: PaddingValues,
    includeHorizontalSafeInsets: Boolean
): PaddingValues {
    if (!includeHorizontalSafeInsets) return scaffoldPadding

    val layoutDirection = LocalLayoutDirection.current
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()
    return PaddingValues(
        start = scaffoldPadding.calculateStartPadding(layoutDirection) + safePadding.calculateStartPadding(layoutDirection),
        top = scaffoldPadding.calculateTopPadding(),
        end = scaffoldPadding.calculateEndPadding(layoutDirection) + safePadding.calculateEndPadding(layoutDirection),
        bottom = scaffoldPadding.calculateBottomPadding()
    )
}
