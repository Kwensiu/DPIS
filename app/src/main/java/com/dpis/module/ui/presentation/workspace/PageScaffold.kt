package com.dpis.module.ui.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class PageBarBehavior { Collapsing, Pinned }

@Composable
internal fun SecondaryPageScaffold(
    @StringRes titleRes: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    extraBottomPadding: Dp = edgeToEdgeContentBottomPadding(24.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(PageChromeTokens.ItemSpacing),
    contentHorizontalPadding: Dp = PageChromeTokens.ContentInset,
    content: LazyListScope.() -> Unit,
) = PageScaffold(
    titleRes = titleRes,
    pageBar = PageBarBehavior.Collapsing,
    onBack = onBack,
    startCollapsed = true,
    modifier = modifier,
    actions = actions,
    bottomBar = bottomBar,
    floatingActionButton = floatingActionButton,
    extraBottomPadding = extraBottomPadding,
    verticalArrangement = verticalArrangement,
    contentHorizontalPadding = contentHorizontalPadding,
    content = content,
)

@Composable
internal fun SecondaryPageScaffold(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    extraBottomPadding: Dp = edgeToEdgeContentBottomPadding(24.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(PageChromeTokens.ItemSpacing),
    title: @Composable () -> Unit,
    content: LazyListScope.() -> Unit,
) = PageScaffold(
    pageBar = PageBarBehavior.Collapsing,
    onBack = onBack,
    startCollapsed = true,
    modifier = modifier,
    actions = actions,
    bottomBar = bottomBar,
    floatingActionButton = floatingActionButton,
    extraBottomPadding = extraBottomPadding,
    verticalArrangement = verticalArrangement,
    title = title,
    content = content,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PageScaffold(
    @StringRes titleRes: Int,
    pageBar: PageBarBehavior,
    onBack: (() -> Unit)? = null,
    startCollapsed: Boolean = false,
    showTopBarDivider: Boolean = true,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    scrollStore: PageScrollPositionStore? = null,
    scrollKey: String? = null,
    bodyInsets: PaddingValues = PaddingValues(),
    listState: LazyListState? = null,
    extraBottomPadding: Dp = 0.dp,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(PageChromeTokens.ItemSpacing),
    contentHorizontalPadding: Dp = PageChromeTokens.ContentInset,
    subtitle: @Composable (() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    PageScaffold(
        pageBar = pageBar,
        onBack = onBack,
        startCollapsed = startCollapsed,
        showTopBarDivider = showTopBarDivider,
        modifier = modifier,
        actions = actions,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        scrollStore = scrollStore,
        scrollKey = scrollKey,
        bodyInsets = bodyInsets,
        listState = listState,
        extraBottomPadding = extraBottomPadding,
        verticalArrangement = verticalArrangement,
        contentHorizontalPadding = contentHorizontalPadding,
        title = { Text(stringResource(titleRes)) },
        subtitle = subtitle,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PageScaffold(
    pageBar: PageBarBehavior,
    onBack: (() -> Unit)? = null,
    startCollapsed: Boolean = false,
    showTopBarDivider: Boolean = true,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    scrollStore: PageScrollPositionStore? = null,
    scrollKey: String? = null,
    bodyInsets: PaddingValues = PaddingValues(),
    listState: LazyListState? = null,
    extraBottomPadding: Dp = 0.dp,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(PageChromeTokens.ItemSpacing),
    contentHorizontalPadding: Dp = PageChromeTokens.ContentInset,
    title: @Composable () -> Unit,
    subtitle: @Composable (() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val resolvedListState = listState ?: rememberLazyListState()
    val collapsing = pageBar == PageBarBehavior.Collapsing
    val storedCollapsed = if (scrollStore != null && scrollKey != null) {
        scrollStore.storedTopBarCollapsed(scrollKey)
    } else {
        null
    }
    val collapsedInitially = collapsing && (storedCollapsed ?: startCollapsed)
    // Seed both limit and offset with the collapsed delta so the first frame is
    // already collapsed. A later LaunchedEffect would paint expanded, then jump.
    val collapsedOffsetPx = if (collapsedInitially) {
        val expandedHeight = if (subtitle != null) {
            TopAppBarDefaults.LargeFlexibleAppBarWithSubtitleExpandedHeight
        } else {
            TopAppBarDefaults.LargeFlexibleAppBarWithoutSubtitleExpandedHeight
        }
        with(LocalDensity.current) {
            (TopAppBarDefaults.LargeAppBarCollapsedHeight - expandedHeight).toPx()
        }
    } else {
        0f
    }
    val topAppBarState = rememberTopAppBarState(
        initialHeightOffsetLimit = if (collapsedInitially) collapsedOffsetPx else -Float.MAX_VALUE,
        initialHeightOffset = collapsedOffsetPx,
    )
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val collapsingBarOwnsScroll by remember(collapsing, topAppBarState, resolvedListState) {
        derivedStateOf {
            collapsing && collapsingBarOwnsNestedScroll(
                collapsedFraction = topAppBarState.collapsedFraction,
                firstVisibleItemIndex = resolvedListState.firstVisibleItemIndex,
                firstVisibleItemScrollOffset = resolvedListState.firstVisibleItemScrollOffset,
            )
        }
    }
    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafe = pageHorizontalSafePadding(onBack != null)
    Scaffold(
        modifier = if (collapsingBarOwnsScroll) {
            modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        } else {
            modifier
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = {
            when (pageBar) {
                PageBarBehavior.Collapsing -> CollapsingPageTopBar(
                    onBack = onBack,
                    includeHorizontalSafeInsets = onBack != null,
                    actions = actions,
                    scrollBehavior = scrollBehavior,
                    title = title,
                    subtitle = subtitle,
                )
                PageBarBehavior.Pinned -> PinnedPageTopBar(
                    onBack = onBack,
                    includeHorizontalSafeInsets = onBack != null,
                    actions = actions,
                    showDivider = showTopBarDivider,
                    title = title,
                )
            }
        },
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = FabPosition.End,
    ) { scaffoldPadding ->
        LazyColumn(
            state = resolvedListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = bodyInsets.calculateStartPadding(layoutDirection)
                        + horizontalSafe.calculateStartPadding(layoutDirection),
                    end = bodyInsets.calculateEndPadding(layoutDirection)
                        + horizontalSafe.calculateEndPadding(layoutDirection),
                    bottom = bodyInsets.calculateBottomPadding(),
                ),
            contentPadding = PaddingValues(
                start = contentHorizontalPadding,
                top = scaffoldPadding.calculateTopPadding(),
                end = contentHorizontalPadding,
                bottom = extraBottomPadding,
            ),
            verticalArrangement = verticalArrangement,
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PageScaffold(
    pageBar: PageBarBehavior,
    onBack: (() -> Unit)? = null,
    showTopBarDivider: Boolean = true,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    title: @Composable () -> Unit,
    body: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = {
            PinnedPageTopBar(
                onBack = onBack,
                includeHorizontalSafeInsets = onBack != null,
                actions = actions,
                showDivider = showTopBarDivider,
                title = title,
            )
        },
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = FabPosition.End,
    ) { scaffoldPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val horizontalSafe = pageHorizontalSafePadding(onBack != null)
        body(
            PaddingValues(
                start = scaffoldPadding.calculateStartPadding(layoutDirection)
                    + horizontalSafe.calculateStartPadding(layoutDirection),
                top = scaffoldPadding.calculateTopPadding(),
                end = scaffoldPadding.calculateEndPadding(layoutDirection)
                    + horizontalSafe.calculateEndPadding(layoutDirection),
                bottom = scaffoldPadding.calculateBottomPadding(),
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PageScaffold(
    @StringRes titleRes: Int,
    pageBar: PageBarBehavior,
    onBack: (() -> Unit)? = null,
    showTopBarDivider: Boolean = true,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    body: @Composable (PaddingValues) -> Unit,
) = PageScaffold(
    pageBar = pageBar,
    onBack = onBack,
    showTopBarDivider = showTopBarDivider,
    modifier = modifier,
    actions = actions,
    bottomBar = bottomBar,
    floatingActionButton = floatingActionButton,
    title = { Text(stringResource(titleRes)) },
    body = body,
)

/** M3 settleAppBar uses exact 1f; treat near-collapsed as collapsed so body flings skip it. */
private const val COLLAPSED_FRACTION_EPSILON = 0.001f

internal fun collapsingBarOwnsNestedScroll(
    collapsedFraction: Float,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
): Boolean =
    collapsedFraction < 1f - COLLAPSED_FRACTION_EPSILON ||
        (firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0)
