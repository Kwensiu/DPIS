package com.dpis.module.applist.presentation

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dpis.module.R
import com.dpis.module.appconfig.editor.EditorPresentation
import com.dpis.module.appconfig.presentation.AppConfigEditorContent
import com.dpis.module.appconfig.presentation.AppTypefacePickerPage
import com.dpis.module.applist.AppListFilterState
import com.dpis.module.applist.AppListItem
import com.dpis.module.applist.AppListPage
import com.dpis.module.applist.AppListScopeTarget
import com.dpis.module.applist.AppStatusFormatter
import com.dpis.module.applist.AppWorkspacePresentation
import com.dpis.module.fonts.presentation.AppHookChainEditorPage
import com.dpis.module.fonts.presentation.ConfigEditorAnimatedContent
import com.dpis.module.ui.ConfigEditorDestination
import com.dpis.module.ui.dialog.ConfirmDialogUiTokens
import com.dpis.module.ui.dialog.DialogColumn
import com.dpis.module.ui.dialog.DialogTitle
import com.dpis.module.ui.dialog.ModalDialog
import com.dpis.module.ui.presentation.design.ComposeDesignSystem
import com.dpis.module.ui.presentation.design.ComposeMotionTokens
import com.dpis.module.ui.presentation.design.LocalSpacing
import com.dpis.module.ui.presentation.design.dpisCombinedClickable
import com.dpis.module.ui.presentation.design.rememberClickAction
import com.dpis.module.ui.presentation.editor.EdgeOcclusionFadeDirection
import com.dpis.module.ui.presentation.editor.EdgeOcclusionFadeTokens
import com.dpis.module.ui.presentation.editor.clearTextInputFocusOnPointerDown
import com.dpis.module.ui.presentation.editor.edgeOcclusionFade
import com.dpis.module.ui.presentation.workspace.PageChromeTokens
import com.dpis.module.ui.presentation.workspace.ToolbarOverflowMenu
import com.dpis.module.ui.presentation.workspace.ToolbarOverflowMenuGroup
import com.dpis.module.ui.presentation.workspace.ToolbarOverflowMenuItem
import com.dpis.module.ui.presentation.workspace.WorkspaceSearchCard
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.roundToInt

private val AppListRowMinHeight = 72.dp
private val AppListScrollbarThumbHeight = 36.dp
private val AppListPageChromeHeight = 48.dp

private enum class AppListBatchConfirmation {
    REMOVE_SCOPE,
    RESET_CONFIGS,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppWorkspaceContent(
    state: AppWorkspacePresentation.State,
    padding: PaddingValues,
    editorState: EditorPresentation.State? = null,
) {
    val focusManager = LocalFocusManager.current
    val selectionActions = state.actions as? AppWorkspacePresentation.SelectionActions
    BackHandler(enabled = state.selection.active && !state.selection.batchOperationRunning) {
        selectionActions?.exitSelection()
    }
    LaunchedEffect(state.restoreScopePromptShouldConsume) {
        if (state.restoreScopePromptShouldConsume) {
            state.actions.dismissRestoreScopePrompt()
        }
    }
    val topSafePadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val configuration = LocalConfiguration.current
    val compactVerticalChrome = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
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
    var pendingBatchConfirmation by remember { mutableStateOf<AppListBatchConfirmation?>(null) }
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
    Box(Modifier.fillMaxSize()) {
        // Orientation is the stable split-pane contract; scaled app density can make maxWidth Dp
        // smaller without reducing the physical landscape space available to the detail pane.
        val twoPane = compactVerticalChrome
        // Both panes share the workspace surface, including the camera-side
        // safe-area padding. Child content may still reserve that inset without
        // exposing the window's darker fallback background.
        Row(
            Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .then(if (twoPane) Modifier.weight(1f) else Modifier.fillMaxWidth())
                    .background(Color.Transparent)
                    .padding(top = topSafePadding)
            ) {
                WorkspaceSearchCard(
                    query = state.query,
                    onQueryChanged = state.actions::changeQuery,
                    trailingAction = {
                        IconButton(onClick = rememberClickAction {
                            focusManager.clearFocus()
                            filterSheetVisible = true
                        }) {
                            Icon(
                                painterResource(R.drawable.ic_tune_24),
                                stringResource(R.string.filter_button)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(
                            top = PageChromeTokens.SearchVerticalPadding,
                            bottom = PageChromeTokens.SearchVerticalPadding,
                        )
                        .height(PageChromeTokens.SearchCardHeight)
                )
                val currentPage =
                    state.selection.page ?: AppListPage.fromPosition(pagerState.currentPage)
                val currentItems = state.itemsFor(currentPage)
                val selectedItems = state.selectionItemsFor(currentPage).filter {
                    it.packageName in state.selection.packageNames
                }
                val visibleSelectedCount = currentItems.count {
                    it.packageName in state.selection.packageNames
                }
                if (state.selection.active && selectionActions != null) {
                    AppListSelectionToolbar(
                        selectedCount = selectedItems.size,
                        allVisibleSelected = currentItems.isNotEmpty() &&
                                visibleSelectedCount == currentItems.size,
                        operationRunning = state.selection.batchOperationRunning,
                        scopeActionsAvailable = selectedItems.isNotEmpty() &&
                                selectedItems.all { it.scopeKnown },
                        canEnableConfig = selectedItems.any {
                            it.hasDpisPackageConfig() && !it.dpisEnabled
                        },
                        canDisableConfig = selectedItems.any {
                            it.dpisEnabled
                        },
                        canReset = selectedItems.any { it.hasDpisPackageConfig() },
                        onClose = selectionActions::exitSelection,
                        onSelectAll = { selectionActions.selectAll(currentPage, currentItems) },
                        onInvert = { selectionActions.invertSelection(currentPage, currentItems) },
                        onAddScope = {
                            selectionActions.changeSelectedScope(
                                AppListScopeTarget.IN_SCOPE,
                                selectedItems,
                            )
                        },
                        onRemoveScope = {
                            pendingBatchConfirmation = AppListBatchConfirmation.REMOVE_SCOPE
                        },
                        onEnableConfig = {
                            selectionActions.setSelectedConfigsEnabled(true, selectedItems)
                        },
                        onDisableConfig = {
                            selectionActions.setSelectedConfigsEnabled(false, selectedItems)
                        },
                        onReset = {
                            pendingBatchConfirmation = AppListBatchConfirmation.RESET_CONFIGS
                        },
                    )
                } else {
                    PrimaryTabRow(
                        modifier = Modifier.height(AppListPageChromeHeight),
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        AppListPage.entries.forEach { page ->
                            Tab(
                                selected = page.position() == pagerState.currentPage,
                                onClick = rememberClickAction {
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
                }
                val currentPageListState = when (pagerState.currentPage) {
                    AppListPage.ALL_APPS.position() -> allAppsListState
                    else -> configuredAppsListState
                }
                Box(Modifier
                    .weight(1f)
                    .fillMaxWidth()) {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = !state.selection.active,
                        modifier = Modifier.fillMaxSize()
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
                            restoreScopePromptVisible = state.restoreScopePromptVisible,
                            actions = state.actions,
                            query = state.query,
                            filterState = state.filterState,
                            inputFocusManager = focusManager,
                            selection = state.selection,
                            selectionActions = selectionActions,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(EdgeOcclusionFadeTokens.Height)
                            .graphicsLayer {
                                val scrolled = currentPageListState.firstVisibleItemIndex > 0 ||
                                        currentPageListState.firstVisibleItemScrollOffset > 0
                                alpha = if (scrolled) 1f else 0f
                            }
                            .edgeOcclusionFade(
                                visibility = 1f,
                                direction = EdgeOcclusionFadeDirection.TOP_TO_BOTTOM,
                            )
                    )
                }
                }
            if (twoPane) {
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    if (editorState != null) {
                        ConfigEditorAnimatedContent(
                            destination = editorState.destination,
                            mainContent = {
                                AppConfigEditorContent(
                                    editorState,
                                    extraTopPadding = topSafePadding,
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
                                    selectedTypefaceId = editorState.draft.selectedTypefaceId ?: "",
                                    onTypefaceSelected = { typefaceId ->
                                        editorState.actions.updateTypeface(typefaceId ?: "")
                                        editorState.actions.navigate(ConfigEditorDestination.MAIN)
                                    },
                                    onBack = {
                                        editorState.actions.navigate(ConfigEditorDestination.MAIN)
                                    },
                                    modifier = Modifier.padding(top = topSafePadding)
                                )
                            }
                        )
                    } else {
                        AppWorkspaceEmptyDetail()
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
    val selectedItems = state.selectionItemsFor(state.selection.page ?: state.selectedPage).filter {
        it.packageName in state.selection.packageNames
    }
    when (pendingBatchConfirmation) {
        AppListBatchConfirmation.REMOVE_SCOPE -> {
            AppListBatchConfirmationDialog(
                title = stringResource(R.string.app_list_batch_remove_scope_title),
                message = stringResource(
                    R.string.app_list_batch_remove_scope_message,
                    selectedItems.size
                ),
                confirm = stringResource(R.string.scope_remove_button),
                onConfirm = {
                    pendingBatchConfirmation = null
                    selectionActions?.changeSelectedScope(
                        AppListScopeTarget.OUT_OF_SCOPE,
                        selectedItems
                    )
                },
                onDismiss = { pendingBatchConfirmation = null },
            )
        }

        AppListBatchConfirmation.RESET_CONFIGS -> {
            AppListBatchConfirmationDialog(
                title = stringResource(R.string.app_list_batch_reset_title),
                message = stringResource(R.string.app_list_batch_reset_message, selectedItems.size),
                confirm = stringResource(R.string.dialog_disable_button),
                onConfirm = {
                    pendingBatchConfirmation = null
                    selectionActions?.resetSelectedConfigs(selectedItems)
                },
                onDismiss = { pendingBatchConfirmation = null },
            )
        }

        null -> Unit
    }
}

@Composable
private fun AppListSelectionToolbar(
    selectedCount: Int,
    allVisibleSelected: Boolean,
    operationRunning: Boolean,
    scopeActionsAvailable: Boolean,
    canEnableConfig: Boolean,
    canDisableConfig: Boolean,
    canReset: Boolean,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onInvert: () -> Unit,
    onAddScope: () -> Unit,
    onRemoveScope: () -> Unit,
    onEnableConfig: () -> Unit,
    onDisableConfig: () -> Unit,
    onReset: () -> Unit,
) {
    var operationsExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppListPageChromeHeight)
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = rememberClickAction(onClose), enabled = !operationRunning) {
                Icon(
                    painterResource(R.drawable.ic_close_24),
                    contentDescription = stringResource(R.string.app_list_selection_close),
                )
            }
            Text(
                text = stringResource(R.string.app_list_selection_count, selectedCount),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(
                onClick = rememberClickAction(onSelectAll),
                enabled = !operationRunning && !allVisibleSelected,
            ) {
                Icon(
                    painterResource(R.drawable.ic_select_all_24),
                    contentDescription = stringResource(R.string.app_list_select_all),
                )
            }
            IconButton(
                onClick = rememberClickAction(onInvert),
                enabled = !operationRunning,
            ) {
                Icon(
                    painterResource(R.drawable.ic_flip_24),
                    contentDescription = stringResource(R.string.app_list_invert_selection),
                )
            }
            Box {
                IconButton(
                    onClick = rememberClickAction { operationsExpanded = true },
                    enabled = !operationRunning,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_more_vert_24),
                        contentDescription = stringResource(R.string.app_list_batch_actions),
                    )
                }
                ToolbarOverflowMenu(
                    expanded = operationsExpanded,
                    onDismiss = { operationsExpanded = false },
                    groupCount = 3,
                ) {
                    ToolbarOverflowMenuGroup(index = 0, count = 3, spacingAfter = true) {
                        ToolbarOverflowMenuItem(
                            textRes = R.string.app_list_menu_scope,
                            enabled = scopeActionsAvailable,
                            onClick = {
                                operationsExpanded = false
                                onAddScope()
                            },
                        )
                        ToolbarOverflowMenuItem(
                            textRes = R.string.app_list_menu_remove,
                            enabled = scopeActionsAvailable,
                            onClick = {
                                operationsExpanded = false
                                onRemoveScope()
                            },
                        )
                    }
                    ToolbarOverflowMenuGroup(index = 1, count = 3, spacingAfter = true) {
                        ToolbarOverflowMenuItem(
                            textRes = R.string.app_list_menu_enable,
                            enabled = canEnableConfig,
                            onClick = {
                                operationsExpanded = false
                                onEnableConfig()
                            },
                        )
                        ToolbarOverflowMenuItem(
                            textRes = R.string.app_list_menu_disable,
                            enabled = canDisableConfig,
                            onClick = {
                                operationsExpanded = false
                                onDisableConfig()
                            },
                        )
                    }
                    ToolbarOverflowMenuGroup(index = 2, count = 3) {
                        ToolbarOverflowMenuItem(
                            textRes = R.string.app_list_menu_reset,
                            enabled = canReset,
                            onClick = {
                                operationsExpanded = false
                                onReset()
                            },
                        )
                    }
                }
            }
        }
        // Keep the selection chrome boundary identical to the landscape pane divider.
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun AppListBatchConfirmationDialog(
    title: String,
    message: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = LocalSpacing.current
    ModalDialog(onDismissRequest = onDismiss) {
        DialogColumn(
            title = { DialogTitle(title) },
            actions = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    OutlinedButton(
                        onClick = rememberClickAction(onDismiss),
                        modifier = Modifier
                            .weight(1f)
                            .height(ConfirmDialogUiTokens.ActionHeight),
                        shape = ConfirmDialogUiTokens.ActionShape,
                    ) {
                        Text(stringResource(R.string.dialog_cancel_button))
                    }
                    Button(
                        onClick = rememberClickAction(onConfirm),
                        modifier = Modifier
                            .weight(1f)
                            .height(ConfirmDialogUiTokens.ActionHeight),
                        shape = ConfirmDialogUiTokens.ActionShape,
                    ) {
                        Text(confirm)
                    }
                }
            },
        ) {
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
    restoreScopePromptVisible: Boolean,
    actions: AppWorkspacePresentation.Actions,
    query: String,
    filterState: AppListFilterState,
    inputFocusManager: androidx.compose.ui.focus.FocusManager,
    selection: com.dpis.module.applist.AppListSelectionController.State,
    selectionActions: AppWorkspacePresentation.SelectionActions?,
) {
    val showRestoreScopePrompt =
        page == AppListPage.CONFIGURED_APPS && restoreScopePromptVisible
    var restoreScopeDialogVisible by remember { mutableStateOf(false) }
    val listBottomPadding = bottomPadding + 12.dp
    Box(Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { actions.refresh(page) },
            modifier = Modifier
                .fillMaxSize()
                .clearTextInputFocusOnPointerDown(inputFocusManager)
        ) {
            if (pageItems.isEmpty() && refreshing) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = (-36).dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        stringResource(R.string.quick_template_targets_loading),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (pageItems.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = (-36).dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        stringResource(R.string.quick_template_targets_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (query.isNotBlank() || !filterState.isDefaultSelection) {
                        Button(
                            onClick = {
                                actions.changeQuery("")
                                actions.changeFilters(AppListFilterState.defaultState())
                            },
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(stringResource(R.string.reset_filters_button))
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxSize()) {
                    val iconSizePx = with(LocalDensity.current) { 50.dp.roundToPx() }
                    PrefetchVisibleAppIcons(listState, pageItems, iconSizePx)
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            top = 12.dp,
                            end = 12.dp,
                            bottom = listBottomPadding
                        )
                    ) {
                        items(
                            pageItems,
                            key = { it.packageName },
                            contentType = { "app_row" },
                        ) { item ->
                            AppRow(
                                item = item,
                                systemScopeSelected = systemScopeSelected,
                                iconSizePx = iconSizePx,
                                selected = item.packageName in selection.packageNames,
                                selectionMode = selection.active,
                                onClick = {
                                    if (selection.active) selectionActions?.toggleSelection(
                                        page,
                                        item
                                    )
                                    else actions.openApp(item)
                                },
                                onLongClick = {
                                    if (!selection.active) selectionActions?.beginSelection(
                                        page,
                                        item
                                    )
                                },
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
        AnimatedVisibility(
            visible = showRestoreScopePrompt,
            enter = fadeIn(tween(ComposeMotionTokens.CONTENT_TRANSITION_DURATION_MILLIS)) + scaleIn(
                initialScale = 0.8f,
                transformOrigin = TransformOrigin(1f, 1f),
            ),
            exit = fadeOut(tween(ComposeMotionTokens.CONTENT_EXIT_DURATION_MILLIS)) + scaleOut(
                targetScale = 0.8f,
                transformOrigin = TransformOrigin(1f, 1f),
            ),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = bottomPadding + 16.dp),
        ) {
            FloatingActionButton(
                onClick = rememberClickAction { restoreScopeDialogVisible = true },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_sparkles_24),
                    contentDescription = stringResource(R.string.restore_scope_prompt_fab),
                )
            }
        }
    }
    if (restoreScopeDialogVisible && showRestoreScopePrompt) {
        RestoreScopePromptDialog(
            onRequest = {
                restoreScopeDialogVisible = false
                actions.requestRestoreScope()
            },
            onDismissPrompt = {
                restoreScopeDialogVisible = false
                actions.dismissRestoreScopePrompt()
            },
            onDismissDialog = { restoreScopeDialogVisible = false },
        )
    }
}

@Composable
private fun RestoreScopePromptDialog(
    onRequest: () -> Unit,
    onDismissPrompt: () -> Unit,
    onDismissDialog: () -> Unit,
) {
    val spacing = LocalSpacing.current
    var requesting by remember { mutableStateOf(false) }
    ModalDialog(onDismissRequest = onDismissDialog) {
        DialogColumn(
            title = { DialogTitle(stringResource(R.string.restore_scope_prompt_title)) },
            actions = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    OutlinedButton(
                        onClick = rememberClickAction(onDismissPrompt),
                        enabled = !requesting,
                        modifier = Modifier
                            .weight(1f)
                            .height(ConfirmDialogUiTokens.ActionHeight),
                        shape = ConfirmDialogUiTokens.ActionShape,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    ) {
                        Text(stringResource(R.string.restore_scope_prompt_dismiss))
                    }
                    Button(
                        onClick = rememberClickAction {
                            requesting = true
                            onRequest()
                        },
                        enabled = !requesting,
                        modifier = Modifier
                            .weight(1f)
                            .height(ConfirmDialogUiTokens.ActionHeight),
                        shape = ConfirmDialogUiTokens.ActionShape,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    ) {
                        Text(stringResource(R.string.restore_scope_prompt_request))
                    }
                }
            },
        ) {
            Text(
                text = stringResource(R.string.restore_scope_prompt_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
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
    val density = LocalDensity.current
    var pressed by remember(listState) { mutableStateOf(false) }
    var trackHeightPx by remember { mutableIntStateOf(0) }
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
        // Partially clipped first/last rows still count as visible layout items. The thumb's
        // presence follows actual scrollability so it stays available at every scroll position.
        val hasScrollableContent = listState.canScrollBackward || listState.canScrollForward
        if (!hasScrollableContent || trackHeightPx == 0) {
            return@Box
        }
        // Rows grow with the user's font scale. Use the measured visible rows for the
        // scrollbar estimate instead of assuming the compact 72.dp height everywhere.
        val rowHeightPx = listState.layoutInfo.visibleItemsInfo
            .map { it.size }
            .average()
            .toFloat()
            .coerceAtLeast(with(density) { AppListRowMinHeight.toPx() })
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
private fun AppRow(
    item: AppListItem,
    systemScopeSelected: Boolean,
    iconSizePx: Int,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    val resources = context.resources
    // Visible rows load a display-sized bitmap. That keeps PackageManager I/O off the list
    // snapshot, and Compose Image avoids creating an ImageView on every bind.
    val icon = rememberInstalledAppIconBitmap(item.packageName, item.icon, iconSizePx)
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
            .heightIn(min = AppListRowMinHeight)
            // Keep the original full-width card while reserving 2.dp on each vertical edge;
            // adjacent selected cards therefore have a 4.dp visual gap without changing width.
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
            )
            .dpisCombinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .semantics { this.selected = selected }
            .padding(horizontal = 8.dp, vertical = 2.dp),
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
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.label,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.packageName,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                AppStatusFormatter.formatCompact(resources, statusInput),
                style = MaterialTheme.typography.bodySmall,
                color = if (warn) MaterialTheme.colorScheme.error
                else if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (selectionMode) {
            Icon(
                painter = painterResource(
                    if (selected) R.drawable.ic_check_circle_24 else R.drawable.ic_radio_button_unchecked_24,
                ),
                contentDescription = if (selected) {
                    stringResource(R.string.app_list_selection_selected)
                } else {
                    stringResource(R.string.app_list_selection_unselected)
                },
                tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
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
        override fun dismissRestoreScopePrompt() = Unit
        override fun requestRestoreScope() = Unit
    }
    ComposeDesignSystem(darkTheme = false, dynamicColor = false) {
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
