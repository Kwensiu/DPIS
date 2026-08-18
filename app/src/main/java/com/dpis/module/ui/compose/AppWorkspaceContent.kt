package com.dpis.module.ui.compose

import android.content.res.Configuration
import android.widget.ImageView
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.tooling.preview.Preview
import com.dpis.module.AppWorkspacePresentation
import com.dpis.module.appconfig.EditorPresentation
import com.dpis.module.ConfigEditorDestination
import com.dpis.module.R
import com.dpis.module.applist.AppListItem
import com.dpis.module.applist.AppListPage
import com.dpis.module.applist.AppListFilterState
import com.dpis.module.applist.AppStatusFormatter
import kotlin.math.roundToInt
import kotlin.math.floor
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

private val AppListRowHeight = 72.dp
private val AppListScrollbarThumbHeight = 36.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppWorkspaceContent(
    state: AppWorkspacePresentation.State,
    padding: PaddingValues,
    editorState: EditorPresentation.State? = null,
) {
    val focusManager = LocalFocusManager.current
    val topSafePadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val configuration = LocalConfiguration.current
    val compactVerticalChrome = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val searchTopPadding = if (compactVerticalChrome) 0.dp else 6.dp
    val searchBottomPadding = if (compactVerticalChrome) 4.dp else 8.dp
    // MainActivity owns the session snapshot because the programmatic ComposeView is recreated
    // across orientation changes. Each catalogue page still keeps an independent position.
    val allAppsListState = rememberLazyListState(
        initialFirstVisibleItemIndex = state.allAppsScrollPosition.index,
        initialFirstVisibleItemScrollOffset = state.allAppsScrollPosition.scrollOffset
    )
    val configuredAppsListState = rememberLazyListState(
        initialFirstVisibleItemIndex = state.configuredAppsScrollPosition.index,
        initialFirstVisibleItemScrollOffset = state.configuredAppsScrollPosition.scrollOffset
    )
    PersistAppListScrollPosition(allAppsListState, AppListPage.ALL_APPS, state.actions)
    PersistAppListScrollPosition(
        configuredAppsListState,
        AppListPage.CONFIGURED_APPS,
        state.actions
    )
    var filterSheetVisible by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(
        initialPage = state.selectedPage.position(),
        pageCount = { AppListPage.entries.size }
    )
    val pagerScope = rememberCoroutineScope()
    val latestSelectedPage by rememberUpdatedState(state.selectedPage)
    val latestActions by rememberUpdatedState(state.actions)
    LaunchedEffect(state.selectedPage) {
        val selectedPage = state.selectedPage.position()
        if (pagerState.settledPage != selectedPage) {
            pagerState.animateScrollToPage(selectedPage)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .drop(1)
            .collect { pageIndex ->
                val page = AppListPage.fromPosition(pageIndex)
                if (page != latestSelectedPage) {
                    latestActions.changePage(page)
                }
            }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val twoPane = compactVerticalChrome && maxWidth >= WorkspaceTwoPaneMinWidth
        Row(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .then(if (twoPane) Modifier.weight(1f) else Modifier.fillMaxWidth())
                    .padding(top = topSafePadding)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                AppSearchCard(
                    state = state,
                    onFilterClick = {
                        focusManager.clearFocus()
                        filterSheetVisible = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = searchTopPadding, bottom = searchBottomPadding)
                        .height(52.dp)
                )
                PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                    AppListPage.entries.forEach { page ->
                        Tab(
                            selected = page.position() == pagerState.currentPage,
                            onClick = {
                                focusManager.clearFocus()
                                pagerScope.launch {
                                    pagerState.animateScrollToPage(page.position())
                                }
                            },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            text = { Text(stringResource(page.titleRes())) }
                        )
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { pageIndex ->
                    val page = AppListPage.fromPosition(pageIndex)
                    val listState = when (page) {
                        AppListPage.ALL_APPS -> allAppsListState
                        AppListPage.CONFIGURED_APPS -> configuredAppsListState
                    }
                    AppListPageContent(
                        page = page,
                        pageItems = state.itemsFor(page),
                        refreshing = state.isRefreshing(page),
                        listState = listState,
                        bottomPadding = padding.calculateBottomPadding(),
                        systemScopeSelected = state.systemScopeSelected,
                        actions = state.actions,
                        inputFocusManager = focusManager
                    )
                }
            }
            if (twoPane) {
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    if (editorState != null) {
                        Box(Modifier.fillMaxSize()) {
                            ConfigEditorAnimatedContent(
                                destination = editorState.destination,
                                mainContent = {
                                    AppConfigEditorContent(
                                        editorState,
                                        extraTopPadding = topSafePadding
                                    )
                                },
                                hookContent = {
                                    AppHookChainEditorPage(
                                        state = editorState,
                                        modifier = Modifier.padding(top = topSafePadding),
                                        bottomPadding = padding.calculateBottomPadding()
                                    )
                                },
                                typefaceContent = {
                                    AppTypefacePickerPage(
                                        selectedTypefaceId = editorState.draft.selectedTypefaceId,
                                        onTypefaceSelected = { typefaceId ->
                                            editorState.actions.updateTypeface(typefaceId)
                                            editorState.actions.navigate(ConfigEditorDestination.MAIN)
                                        },
                                        onBack = {
                                            editorState.actions.navigate(ConfigEditorDestination.MAIN)
                                        },
                                        modifier = Modifier.padding(top = topSafePadding)
                                    )
                                }
                            )
                        }
                    } else {
                        AppWorkspaceEmptyDetail(Modifier.padding(top = topSafePadding))
                    }
                }
            }
        }
    }
    if (filterSheetVisible) {
        AppFilterSheet(
            filterState = state.filterState,
            onFilterChanged = state.actions::changeFilters,
            onDismissRequest = { filterSheetVisible = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppListPageContent(
    page: AppListPage,
    pageItems: List<AppListItem>,
    refreshing: Boolean,
    listState: LazyListState,
    bottomPadding: androidx.compose.ui.unit.Dp,
    systemScopeSelected: Boolean,
    actions: AppWorkspacePresentation.Actions,
    inputFocusManager: androidx.compose.ui.focus.FocusManager
) {
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { actions.refresh(page) },
        modifier = Modifier
            .fillMaxSize()
            .clearTextInputFocusOnPointerDown(inputFocusManager)
    ) {
        if (pageItems.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().offset(y = (-36).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    stringResource(R.string.quick_template_targets_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        top = 12.dp,
                        end = 12.dp,
                        bottom = bottomPadding + 12.dp
                    )
                ) {
                    items(pageItems, key = { it.packageName }) { item ->
                        AppRow(
                            item = item,
                            systemScopeSelected = systemScopeSelected,
                            onClick = {
                                actions.openApp(item)
                            }
                        )
                    }
                }
                AppListScrollbar(
                    listState = listState,
                    itemCount = pageItems.size,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(bottom = bottomPadding)
                )
            }
        }
    }
}

@Composable
private fun PersistAppListScrollPosition(
    listState: LazyListState,
    page: AppListPage,
    actions: AppWorkspacePresentation.Actions
) {
    val latestActions by rememberUpdatedState(actions)
    LaunchedEffect(listState, page) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.distinctUntilChanged().collect { (index, offset) ->
            latestActions.updateScrollPosition(page, index, offset)
        }
    }
}

@Composable
private fun AppWorkspaceEmptyDetail(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_apps_24),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.land_detail_empty_title),
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.land_detail_empty_message),
            modifier = Modifier.padding(top = 6.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun AppListScrollbar(
    listState: LazyListState,
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    if (itemCount == 0) return
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val density = LocalDensity.current
    var pressed by remember(listState) { mutableStateOf(false) }
    var trackHeightPx by remember { mutableStateOf(0) }
    var requestedThumbTopPx by remember(listState) { mutableFloatStateOf(0f) }
    val thumbWidth by animateDpAsState(
        targetValue = if (pressed) 8.dp else 6.dp,
        animationSpec = tween(150),
        label = "app-list-scrollbar-width"
    )
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(24.dp)
            .padding(vertical = 8.dp)
            .onSizeChanged { trackHeightPx = it.height }
    ) {
        val visibleCount = listState.layoutInfo.visibleItemsInfo.size
        if (visibleCount >= itemCount || trackHeightPx == 0) return@Box
        val rowHeightPx = with(density) { AppListRowHeight.toPx() }
        val viewportHeightPx = listState.layoutInfo.viewportSize.height.toFloat()
        val totalContentHeightPx = itemCount * rowHeightPx +
            listState.layoutInfo.beforeContentPadding +
            listState.layoutInfo.afterContentPadding
        val maximumScrollOffsetPx = (totalContentHeightPx - viewportHeightPx).coerceAtLeast(1f)
        // Match the legacy fast scroller: the thumb is a fixed physical control.
        // List length changes its position mapping, never its visual length.
        val thumbHeightPx = with(density) { AppListScrollbarThumbHeight.toPx() }
            .coerceAtMost(trackHeightPx.toFloat())
        val scrollableThumbRangePx = (trackHeightPx - thumbHeightPx).coerceAtLeast(0f)
        if (scrollableThumbRangePx <= 0f) return@Box
        val currentScrollOffsetPx = (
            listState.firstVisibleItemIndex * rowHeightPx +
                listState.firstVisibleItemScrollOffset
            )
        val scrollFraction = currentScrollOffsetPx / maximumScrollOffsetPx
        val thumbTopPx = scrollableThumbRangePx * scrollFraction.coerceIn(0f, 1f)
        // pointerInput intentionally survives ordinary list scrolling. Keep its drag-start
        // position current without restarting the active gesture handler on every scroll frame.
        val currentThumbTopPx = rememberUpdatedState(thumbTopPx)
        val displayedThumbTopPx = if (pressed) requestedThumbTopPx else thumbTopPx
        val visualThumbTopPx = displayedThumbTopPx.coerceIn(0f, trackHeightPx - thumbHeightPx)
        LaunchedEffect(pressed, scrollableThumbRangePx, maximumScrollOffsetPx) {
            if (!pressed) return@LaunchedEffect
            while (pressed) {
                // Compose receives pointer moves more often than it can render a LazyColumn.
                // Coalesce them to the display frame, then jump to the requested row. Scrolling
                // by the full pixel delta walks every skipped lazy item and causes a long measure
                // pass; requestScrollToItem has the same direct-position semantics as AdClose's
                // RecyclerView scrollToPositionWithOffset implementation.
                withFrameNanos { }
                val targetScrollOffsetPx = requestedThumbTopPx / scrollableThumbRangePx *
                    maximumScrollOffsetPx
                val targetIndex = floor(targetScrollOffsetPx / rowHeightPx)
                    .toInt()
                    .coerceIn(0, itemCount - 1)
                val targetItemOffsetPx = (targetScrollOffsetPx - targetIndex * rowHeightPx)
                    .roundToInt()
                    .coerceAtLeast(0)
                listState.requestScrollToItem(targetIndex, targetItemOffsetPx)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(itemCount, trackHeightPx, thumbHeightPx) {
                detectVerticalDragGestures(
                    onDragStart = {
                        pressed = true
                        requestedThumbTopPx = currentThumbTopPx.value
                    },
                    onDragEnd = {
                        pressed = false
                    },
                    onDragCancel = {
                        pressed = false
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        requestedThumbTopPx = (requestedThumbTopPx + dragAmount)
                            .coerceIn(0f, scrollableThumbRangePx)
                    }
                )
            }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, visualThumbTopPx.roundToInt()) }
                    .width(thumbWidth)
                    .height(AppListScrollbarThumbHeight)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f))
            )
        }
    }
}

@Composable
private fun AppSearchCard(
    state: AppWorkspacePresentation.State,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .height(52.dp)
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(R.drawable.ic_search_24),
                contentDescription = null,
                modifier = Modifier.padding(start = 12.dp, end = 8.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BasicTextField(
                value = state.query,
                onValueChange = state.actions::changeQuery,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                        if (state.query.isEmpty()) {
                            Text(
                                stringResource(R.string.search_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        inner()
                    }
                }
            )
            if (state.query.isNotEmpty()) {
                IconButton(onClick = { state.actions.changeQuery("") }) {
                    Icon(
                        painterResource(R.drawable.ic_close_24),
                        stringResource(R.string.search_clear)
                    )
                }
            }
            IconButton(onClick = onFilterClick) {
                Icon(
                    painterResource(R.drawable.ic_tune_24),
                    stringResource(R.string.filter_button)
                )
            }
        }
    }
}

@Composable
private fun AppRow(
    item: AppListItem,
    systemScopeSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val resources = context.resources
    // This composable exists only for visible LazyColumn rows. Loading here keeps icon I/O out
    // of the catalogue state, so an icon result cannot rebuild or re-filter every app row.
    val icon = rememberInstalledAppIcon(item.packageName, item.icon)
    val statusInput = AppStatusFormatter.StatusInput(
        item.inScope,
        item.scopeKnown,
        item.installed,
        item.viewportTargetSpec,
        item.viewportMode,
        item.fontScalePercent,
        item.fontMode,
        item.typefaceId,
        item.dpisEnabled,
        item.hasAppSpecificConfig(),
        item.wechatDpi
    )
    val warn = item.scopeKnown && (
        AppStatusFormatter.shouldWarnViewportEmulation(
            item.viewportTargetSpec, item.viewportMode, systemScopeSelected, item.dpisEnabled
        ) || AppStatusFormatter.shouldWarnFontEmulation(
            item.fontScalePercent, item.fontMode, systemScopeSelected, item.dpisEnabled
        )
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppListRowHeight)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(12.dp))
                // The rounded surface is only an icon-loading placeholder. Keeping it
                // behind a resolved launcher icon makes the icon look double-masked.
                .then(
                    if (icon == null) {
                        Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                AndroidView(
                    factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
                    update = { it.setImageDrawable(icon) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f).padding(start = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                item.label,
                // Trim only the boundary facing the package name. The row keeps its normal
                // title line height and does not rely on a negative layout offset.
                style = MaterialTheme.typography.titleMedium.copy(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.LastLineBottom,
                        mode = LineHeightStyle.Mode.Fixed
                    )
                ),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                item.packageName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.FirstLineTop,
                        mode = LineHeightStyle.Mode.Fixed
                    )
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                AppStatusFormatter.formatCompact(resources, statusInput),
                style = MaterialTheme.typography.bodySmall,
                color = if (warn) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun AppWorkspacePreview() {
    val actions = object : AppWorkspacePresentation.Actions {
        override fun changeQuery(query: String) = Unit
        override fun changePage(page: AppListPage) = Unit
        override fun changeFilters(filterState: AppListFilterState) = Unit
        override fun refresh(page: AppListPage) = Unit
        override fun openApp(item: AppListItem) = Unit
        override fun updateScrollPosition(page: AppListPage, index: Int, scrollOffset: Int) = Unit
    }
    DpisTheme(darkTheme = false, dynamicColor = false) {
        AppWorkspaceContent(
            state = AppWorkspacePresentation.State(
                "", AppListPage.ALL_APPS, emptyList(), emptyList(), false, false,
                AppListFilterState.defaultState(), false,
                AppWorkspacePresentation.ScrollPosition(0, 0),
                AppWorkspacePresentation.ScrollPosition(0, 0),
                actions
            ),
            padding = PaddingValues()
        )
    }
}
