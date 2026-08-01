package com.dpis.module.ui.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dpis.module.R
import kotlinx.coroutines.flow.distinctUntilChanged

class LogUiEntry(
    val key: String,
    val level: String,
    val tag: String,
    val message: String,
    val time: String,
    val expanded: Boolean
)

class LogUiState(
    val selectedPage: Int,
    val newestAtBottom: Boolean,
    val autoRefreshEnabled: Boolean,
    val entries: List<LogUiEntry>,
    val stateMessage: String?,
    val scrollToLatestRevision: Int
)

class LogPresentation {
    var state: LogUiState by mutableStateOf(
        LogUiState(0, true, true, emptyList(), null, 0)
    )
        private set

    @Volatile
    var atLatestEdge: Boolean = true
        private set

    fun show(state: LogUiState) {
        this.state = state
    }

    fun updateAtLatestEdge(atLatestEdge: Boolean) {
        this.atLatestEdge = atLatestEdge
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogContent(
    presentation: LogPresentation,
    onBack: () -> Unit,
    onSelectPage: (Int) -> Unit,
    onToggleSort: () -> Unit,
    onToggleAutoRefresh: () -> Unit,
    onSaveLogs: () -> Unit,
    onShareLogs: () -> Unit,
    onRefresh: () -> Unit,
    onToggleExpanded: (String) -> Unit,
    onCopyEntry: (String) -> Unit
) {
    val state = presentation.state
    var exportMenuExpanded by remember { mutableStateOf(false) }
    // Standalone pages must paint an opaque root. This also protects the page if its
    // Activity is ever launched from a translucent host or during an Activity transition.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.log_page_title),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = rememberDpisConfirmAction(onBack)) {
                            Icon(
                                painterResource(R.drawable.ic_arrow_back_24),
                                contentDescription = stringResource(R.string.system_settings_back)
                            )
                        }
                    },
                    actions = {
                        LogTopBarAction(
                            R.drawable.ic_swap_vert_24,
                            if (state.newestAtBottom) R.string.log_action_sort_newest_first
                            else R.string.log_action_sort_oldest_first,
                            onToggleSort
                        )
                        LogTopBarAction(
                            if (state.autoRefreshEnabled) R.drawable.ic_pause_24
                            else R.drawable.ic_play_arrow_24,
                            if (state.autoRefreshEnabled) R.string.log_action_pause_auto_refresh
                            else R.string.log_action_start_auto_refresh,
                            onToggleAutoRefresh
                        )
                        Box {
                            LogTopBarAction(
                                R.drawable.ic_upload_file_24,
                                R.string.log_action_export
                            ) { exportMenuExpanded = true }
                            DropdownMenu(
                                expanded = exportMenuExpanded,
                                onDismissRequest = { exportMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.log_action_save_logs)) },
                                    onClick = {
                                        exportMenuExpanded = false
                                        onSaveLogs()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.log_action_share_logs)) },
                                    onClick = {
                                        exportMenuExpanded = false
                                        onShareLogs()
                                    }
                                )
                            }
                        }
                        LogTopBarAction(
                            R.drawable.ic_refresh_24,
                            R.string.log_action_refresh,
                            onRefresh
                        )
                    }
                )
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                PrimaryTabRow(selectedTabIndex = state.selectedPage) {
                    listOf(R.string.log_page_dpis, R.string.log_page_lsposed_related)
                        .forEachIndexed { index, textRes ->
                            Tab(
                                selected = state.selectedPage == index,
                                onClick = rememberDpisConfirmAction { onSelectPage(index) },
                                text = {
                                    Text(
                                        stringResource(textRes),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                }
                if (state.stateMessage != null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            state.stateMessage,
                            modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LogEntryList(
                        state = state,
                        presentation = presentation,
                        onToggleExpanded = onToggleExpanded,
                        onCopyEntry = onCopyEntry
                    )
                }
            }
        }
    }
}

@Composable
private fun LogEntryList(
    state: LogUiState,
    presentation: LogPresentation,
    onToggleExpanded: (String) -> Unit,
    onCopyEntry: (String) -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState, state.newestAtBottom) {
        snapshotFlow {
            val info = listState.layoutInfo
            if (info.totalItemsCount == 0) true
            else if (state.newestAtBottom) {
                info.visibleItemsInfo.lastOrNull()?.index ?: 0 >= info.totalItemsCount - 2
            } else {
                listState.firstVisibleItemIndex <= 1
            }
        }.distinctUntilChanged().collect(presentation::updateAtLatestEdge)
    }
    LaunchedEffect(state.scrollToLatestRevision, state.newestAtBottom) {
        if (state.entries.isNotEmpty()) {
            listState.scrollToItem(if (state.newestAtBottom) state.entries.lastIndex else 0)
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(state.entries.size, key = { state.entries[it].key }) { index ->
            LogEntryRow(state.entries[index], onToggleExpanded, onCopyEntry)
            if (index < state.entries.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LogEntryRow(
    entry: LogUiEntry,
    onToggleExpanded: (String) -> Unit,
    onCopyEntry: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().combinedClickable(
            onClick = { onToggleExpanded(entry.key) },
            onLongClick = { onCopyEntry(entry.key) }
        ),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = 64.dp),
            verticalAlignment = Alignment.Top
        ) {
            LogLevelRail(entry.level)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.tag,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        entry.time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    entry.message,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (entry.expanded) 40 else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LogLevelRail(level: String) {
    val normalized = level.trim().uppercase().firstOrNull()?.toString() ?: "I"
    val (container, content) = when (normalized) {
        "E", "F" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        "W" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        "D", "V" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(color = container) {
        Box(
            Modifier.fillMaxHeight().width(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                normalized,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = content
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogTopBarAction(iconRes: Int, descriptionRes: Int, onClick: () -> Unit) {
    val description = stringResource(descriptionRes)
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Below
        ),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState()
    ) {
        IconButton(onClick = rememberDpisConfirmAction(onClick)) {
            Icon(
                painterResource(iconRes),
                contentDescription = description
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LogContentPreview() {
    val presentation = remember {
        LogPresentation().apply {
            show(
                LogUiState(
                    0,
                    true,
                    true,
                    listOf(LogUiEntry("1", "I", "DPIS", "Configuration updated", "12:30:05", false)),
                    null,
                    1
                )
            )
        }
    }
    DpisTheme(darkTheme = false) {
        LogContent(presentation, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}
