package com.dpis.module.home.presentation

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import com.dpis.module.BuildConfig
import com.dpis.module.R
import com.dpis.module.home.HomeWorkspaceLayout
import com.dpis.module.home.HomeWorkspaceState
import com.dpis.module.root.RootAccessProbe
import com.dpis.module.ui.compose.LocalSpacing
import com.dpis.module.ui.compose.PageBarBehavior
import com.dpis.module.ui.compose.PageScaffold
import com.dpis.module.ui.compose.SecondaryPageContentTokens
import com.dpis.module.ui.compose.dpisClickable
import com.dpis.module.ui.compose.rememberClickAction
import com.dpis.module.ui.compose.rememberRestorableLazyListState

private data class HomeCountItem(
    val item: HomeWorkspaceLayout.Item,
    val titleRes: Int,
    val count: Int,
    val onOpen: () -> Unit,
)

/** Native Home workspace. Actions remain owned by MainActivity's existing coordinator. */
@Composable
fun HomeWorkspaceContent(
    state: HomeWorkspaceState,
    padding: PaddingValues,
    scrollStore: com.dpis.module.ui.compose.PageScrollPositionStore,
) {
    val context = LocalContext.current
    var editing by rememberSaveable { mutableStateOf(false) }
    var draftLayout by remember(state.layout) { mutableStateOf(state.layout) }
    val toggleEditing = rememberClickAction {
        if (editing) {
            state.actions.saveHomeWorkspaceLayout(draftLayout)
            editing = false
        } else {
            draftLayout = state.layout
            editing = true
        }
    }
    val restoreAll = rememberClickAction { draftLayout = HomeWorkspaceLayout.defaults() }
    val listState = rememberRestorableLazyListState("home", scrollStore)
    val contentCanScroll by remember {
        derivedStateOf { listState.canScrollForward || listState.canScrollBackward }
    }
    val visibleCountItems = listOf(
        HomeCountItem(
            HomeWorkspaceLayout.Item.CONFIGURED_APPS,
            R.string.home_workspace_status_configured_apps,
            state.configuredAppCount,
            state.actions::openConfiguredAppsWorkspace,
        ),
        HomeCountItem(
            HomeWorkspaceLayout.Item.IMPORTED_FONTS,
            R.string.home_workspace_status_imported_fonts,
            state.importedFontCount,
            state.actions::openFontLibrary,
        ),
        HomeCountItem(
            HomeWorkspaceLayout.Item.TEMPLATES,
            R.string.home_workspace_status_templates,
            state.templateCount,
            state.actions::openTemplateWorkspace,
        ),
    ).filter { editing || draftLayout.isVisible(it.item) }
    PageScaffold(
        pageBar = PageBarBehavior.Collapsing,
        onBack = null,
        scrollStore = scrollStore,
        scrollKey = "home",
        contentCanScroll = contentCanScroll,
        actions = if (state.showEditButton) {
            {
            IconButton(
                onClick = toggleEditing,
            ) {
                Icon(
                    painterResource(if (editing) R.drawable.ic_save_24dp else R.drawable.ic_edit_24),
                    stringResource(
                        if (editing) R.string.home_workspace_action_save
                        else R.string.home_workspace_action_edit,
                    ),
                )
            }
            }
        } else ({}),
        title = {
            Column {
                Text(
                    stringResource(R.string.home_workspace_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.home_workspace_subtitle),
                    modifier = Modifier.padding(top = 0.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        collapsedTitle = {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    ) { pagePadding ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = pagePadding.calculateStartPadding(layoutDirection) + 16.dp,
                top = pagePadding.calculateTopPadding() + SecondaryPageContentTokens.TitleToContentGap,
                end = pagePadding.calculateEndPadding(layoutDirection) + 16.dp,
                bottom = pagePadding.calculateBottomPadding() + LocalSpacing.current.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            item { HomePrimaryStatus(state) }
            if (visibleCountItems.isNotEmpty()) item {
                Row(
                    Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    visibleCountItems.forEach { item ->
                        HomeCountCard(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            titleRes = item.titleRes,
                            count = item.count,
                            editing = editing,
                            pendingHidden = !draftLayout.isVisible(item.item),
                            onClick = {
                                if (editing) {
                                    draftLayout = draftLayout.withVisibility(
                                        item.item,
                                        !draftLayout.isVisible(item.item),
                                    )
                                } else {
                                    item.onOpen()
                                }
                            },
                        )
                    }
                }
            }
            if (editing || draftLayout.isVisible(HomeWorkspaceLayout.Item.BASIC_INFO)) item {
                HomeInfoCard(
                    state = state,
                    editing = editing,
                    pendingHidden = !draftLayout.isVisible(HomeWorkspaceLayout.Item.BASIC_INFO),
                    onClick = {
                        draftLayout = draftLayout.withVisibility(
                            HomeWorkspaceLayout.Item.BASIC_INFO,
                            !draftLayout.isVisible(HomeWorkspaceLayout.Item.BASIC_INFO),
                        )
                    },
                )
            }
            if (editing || draftLayout.isVisible(HomeWorkspaceLayout.Item.MODE_HELP)) item {
                HomeNavigationEntry(
                    titleRes = R.string.home_mode_help_entry_title,
                    summaryRes = R.string.home_mode_help_entry_summary,
                    editing = editing,
                    pendingHidden = !draftLayout.isVisible(HomeWorkspaceLayout.Item.MODE_HELP),
                    onClick = {
                        if (editing) {
                            draftLayout = draftLayout.withVisibility(
                                HomeWorkspaceLayout.Item.MODE_HELP,
                                !draftLayout.isVisible(HomeWorkspaceLayout.Item.MODE_HELP),
                            )
                        } else {
                            state.actions.openModeHelp()
                        }
                    },
                )
            }
            if (editing || draftLayout.isVisible(HomeWorkspaceLayout.Item.FEEDBACK)) item {
                HomeFeedbackEntry(
                    context = context,
                    editing = editing,
                    pendingHidden = !draftLayout.isVisible(HomeWorkspaceLayout.Item.FEEDBACK),
                    onClick = {
                        draftLayout = draftLayout.withVisibility(
                            HomeWorkspaceLayout.Item.FEEDBACK,
                            !draftLayout.isVisible(HomeWorkspaceLayout.Item.FEEDBACK),
                        )
                    },
                )
            }
            if (editing || draftLayout.isVisible(HomeWorkspaceLayout.Item.DONATE)) item {
                HomeNavigationEntry(
                    titleRes = R.string.home_donate_title,
                    summaryRes = R.string.home_donate_summary,
                    editing = editing,
                    pendingHidden = !draftLayout.isVisible(HomeWorkspaceLayout.Item.DONATE),
                    onClick = {
                        if (editing) {
                            draftLayout = draftLayout.withVisibility(
                                HomeWorkspaceLayout.Item.DONATE,
                                !draftLayout.isVisible(HomeWorkspaceLayout.Item.DONATE),
                            )
                        } else {
                            state.actions.openDonate()
                        }
                    },
                )
            }
            if (editing) item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    FilledTonalButton(
                        onClick = restoreAll,
                        modifier = Modifier.heightIn(min = 52.dp),
                        shape = RoundedCornerShape(26.dp),
                    ) {
                        Text(stringResource(R.string.home_workspace_action_restore_all))
                    }
                }
            }
        }
    }
}

@Composable
private fun HomePrimaryStatus(state: HomeWorkspaceState) {
    val context = LocalContext.current
    val disabled = !state.xposedModuleActivated
    val onClick = rememberClickAction {
        if (!disabled) state.actions.checkForUpdates()
    }
    val container = when {
        disabled -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val content = when {
        disabled -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = container), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(if (disabled) R.drawable.ic_error_outline_24 else R.drawable.ic_check_24), null, tint = content)
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(stringResource(if (disabled) R.string.home_workspace_status_enable_in_lsposed else R.string.home_workspace_status_enabled), style = MaterialTheme.typography.titleMedium, color = content, fontWeight = FontWeight.Bold)
                Text(
                    state.updateState.subtitle(context),
                    style = MaterialTheme.typography.bodyMedium,
                    color = content
                )
            }
        }
    }
}

@Composable
private fun HomeCountCard(
    modifier: Modifier,
    titleRes: Int,
    count: Int,
    editing: Boolean,
    pendingHidden: Boolean,
    onClick: () -> Unit,
) {
    val hapticClick = rememberClickAction(onClick)
    HomeEditableBlock(modifier, editing, pendingHidden) { border ->
        Card(
            onClick = hapticClick,
            modifier = Modifier.fillMaxSize(),
            colors = homeEditableColors(pendingHidden),
            border = border,
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(stringResource(titleRes), modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HomeInfoCard(
    state: HomeWorkspaceState,
    editing: Boolean,
    pendingHidden: Boolean,
    onClick: () -> Unit,
) {
    val rootText = when (state.rootAccess.status) {
        RootAccessProbe.Status.AVAILABLE -> stringResource(R.string.home_workspace_info_root_available, state.rootAccess.provider)
        RootAccessProbe.Status.UNAVAILABLE -> stringResource(R.string.home_workspace_info_root_unavailable)
        RootAccessProbe.Status.UNKNOWN -> stringResource(R.string.home_workspace_info_root_checking)
    }
    val rows = listOf(
        R.string.home_workspace_info_version to stringResource(R.string.home_workspace_info_version_value, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
        R.string.home_workspace_info_system to stringResource(R.string.home_workspace_info_system_value, Build.VERSION.RELEASE, Build.VERSION.SDK_INT),
        R.string.home_workspace_info_root to rootText,
        R.string.home_workspace_info_device to listOf(Build.MANUFACTURER, Build.MODEL).filter { it.isNotBlank() }.distinct().joinToString(" ")
    )
    HomeEditableBlock(Modifier.fillMaxWidth(), editing, pendingHidden) { border ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .homeEditOutline(border, RoundedCornerShape(16.dp))
                .then(if (editing) Modifier.dpisClickable(role = Role.Button, onClick = onClick) else Modifier),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            rows.forEachIndexed { index, (titleRes, value) ->
                val shape = when (index) {
                    0 -> RoundedCornerShape(16.dp, 16.dp, 3.dp, 3.dp)
                    rows.lastIndex -> RoundedCornerShape(3.dp, 3.dp, 16.dp, 16.dp)
                    else -> RoundedCornerShape(3.dp)
                }
                HomeInfoRow(titleRes, value, shape, pendingHidden)
            }
        }
    }
}

@Composable
private fun HomeInfoRow(
    titleRes: Int,
    value: String,
    shape: RoundedCornerShape,
    pendingHidden: Boolean = false,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = homeEditableColors(pendingHidden)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(titleRes), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(value, modifier = Modifier.padding(start = 16.dp).weight(1.2f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HomeNavigationEntry(
    titleRes: Int,
    summaryRes: Int,
    editing: Boolean,
    pendingHidden: Boolean,
    onClick: () -> Unit,
) {
    val hapticClick = rememberClickAction(onClick)
    HomeEditableBlock(Modifier.fillMaxWidth(), editing, pendingHidden) { border ->
        Card(
            onClick = hapticClick,
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
            shape = RoundedCornerShape(12.dp),
            colors = homeEditableColors(pendingHidden),
            border = border,
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(titleRes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(stringResource(summaryRes), modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(painterResource(R.drawable.ic_chevron_right_24), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HomeFeedbackEntry(
    context: android.content.Context,
    editing: Boolean,
    pendingHidden: Boolean,
    onClick: () -> Unit,
) {
    HomeEditableBlock(Modifier.fillMaxWidth(), editing, pendingHidden) { border ->
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
            shape = RoundedCornerShape(12.dp),
            colors = homeEditableColors(pendingHidden),
            border = border,
        ) {
            Row(
                Modifier
                    .then(if (editing) Modifier.dpisClickable(role = Role.Button, onClick = onClick) else Modifier)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.home_feedback_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.home_feedback_summary), modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!editing) {
                    HomeFeedbackAction(R.drawable.ic_github_24, R.string.home_feedback_github, context.getString(R.string.about_issues_url), context)
                    HomeFeedbackAction(R.drawable.ic_qq_24, R.string.home_feedback_qq, context.getString(R.string.home_feedback_qq_url), context)
                    HomeFeedbackAction(R.drawable.ic_telegram_24, R.string.home_feedback_telegram, context.getString(R.string.home_feedback_telegram_url), context)
                }
            }
        }
    }
}

private fun Modifier.homeEditable(editing: Boolean, pendingHidden: Boolean): Modifier =
    if (editing) {
        alpha(if (pendingHidden) 0.62f else 1f)
    } else {
        this
    }

/**
 * Owns the edit-only presentation and deliberately leaves navigation to the enclosing card.
 * This keeps an edit tap from accidentally opening a workspace while making visibility explicit.
 */
@Composable
private fun HomeEditableBlock(
    modifier: Modifier,
    editing: Boolean,
    pendingHidden: Boolean,
    content: @Composable (BorderStroke?) -> Unit,
) {
    Box(modifier = modifier) {
        val border = if (editing) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
        // Keep the dimmed card content in its own layer. The visibility control must stay
        // above that layer so a pending-hidden card cannot clip or fade its own control.
        Box(modifier = Modifier.homeEditable(editing, pendingHidden)) {
            content(border)
        }
        if (editing) {
            HomeVisibilityBadge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .alpha(if (pendingHidden) 0.62f else 1f)
                    .zIndex(1f),
                pendingHidden = pendingHidden,
            )
        }
    }
}

@Composable
private fun HomeVisibilityBadge(modifier: Modifier, pendingHidden: Boolean) {
    val visibilityDescription = stringResource(
        if (pendingHidden) R.string.home_workspace_visibility_hidden
        else R.string.home_workspace_visibility_visible,
    )
    Box(
        modifier = modifier
            .semantics {
                stateDescription = visibilityDescription
            }
            .size(22.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(
                if (pendingHidden) R.drawable.ic_visibility_off_24dp else R.drawable.ic_close_24,
            ),
            contentDescription = visibilityDescription,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

private fun Modifier.homeEditOutline(border: BorderStroke?, shape: RoundedCornerShape): Modifier =
    if (border == null) this else border(border.width, border.brush, shape)

@Composable
private fun homeEditableColors(pendingHidden: Boolean) = CardDefaults.cardColors(
    containerColor = if (pendingHidden) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surfaceBright
    },
)

@Composable
private fun HomeFeedbackAction(@androidx.annotation.DrawableRes iconRes: Int, @androidx.annotation.StringRes descriptionRes: Int, url: String, context: android.content.Context) {
    val openFeedbackTarget = {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .dpisClickable(role = Role.Button, onClick = openFeedbackTarget),
        contentAlignment = Alignment.Center
    ) {
        Icon(painterResource(iconRes), stringResource(descriptionRes), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
