package com.dpis.module.ui.compose

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.dpis.module.R
import com.dpis.module.BuildConfig
import com.dpis.module.home.HomeUpdateUiState
import com.dpis.module.home.HomeWorkspaceBinder
import com.dpis.module.root.RootAccessProbe

/** Native Home workspace. Actions remain owned by MainActivity's existing coordinator. */
@Composable
fun HomeWorkspaceContent(state: HomeWorkspaceBinder.State, padding: PaddingValues) {
    val context = LocalContext.current
    PrimaryPageScaffold(
        modifier = Modifier.padding(padding),
        title = {
            Column {
                Text(
                    stringResource(R.string.home_workspace_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.home_workspace_subtitle),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { pagePadding ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            contentPadding = PaddingValues(
                start = pagePadding.calculateStartPadding(layoutDirection) + 16.dp,
                top = pagePadding.calculateTopPadding() + 4.dp,
                end = pagePadding.calculateEndPadding(layoutDirection) + 16.dp,
                bottom = pagePadding.calculateBottomPadding() + LocalDpisTokens.current.spaceLg,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item { HomePrimaryStatus(state) }
            if (state.updateState.showsUpdateActionCard()) {
                item { HomeUpdateActions(state) }
            }
            item {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeCountCard(Modifier.weight(1f).fillMaxHeight(), R.string.home_workspace_status_configured_apps, state.configuredAppCount, state.actions::openConfiguredAppsWorkspace)
                    HomeCountCard(Modifier.weight(1f).fillMaxHeight(), R.string.home_workspace_status_imported_fonts, state.importedFontCount, state.actions::openFontLibrary)
                    HomeCountCard(Modifier.weight(1f).fillMaxHeight(), R.string.home_workspace_status_templates, state.templateCount, state.actions::openTemplateWorkspace)
                }
            }
            item { HomeInfoCard(state) }
            item {
                HomeNavigationEntry(R.string.home_mode_help_entry_title, R.string.home_mode_help_entry_summary) {
                    state.actions.openModeHelp()
                }
            }
            item {
                HomeFeedbackEntry(context)
            }
            item {
                HomeNavigationEntry(R.string.home_donate_title, R.string.home_donate_summary) {
                    state.actions.openDonate()
                }
            }
        }
    }
}

@Composable
private fun HomePrimaryStatus(state: HomeWorkspaceBinder.State) {
    val context = LocalContext.current
    val confirmFeedback = rememberDpisConfirmFeedback()
    val disabled = !state.xposedModuleActivated
    val container = when {
        disabled -> MaterialTheme.colorScheme.errorContainer
        state.updateState.showsUpdateActionCard() -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val content = when {
        disabled -> MaterialTheme.colorScheme.onErrorContainer
        state.updateState.showsUpdateActionCard() -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Card(colors = CardDefaults.cardColors(containerColor = container), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(if (disabled) R.drawable.ic_error_outline_24 else R.drawable.ic_check_24), null, tint = content)
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(stringResource(if (disabled) R.string.home_workspace_status_enable_in_lsposed else R.string.home_workspace_status_enabled), style = MaterialTheme.typography.titleMedium, color = content, fontWeight = FontWeight.Bold)
                Text(
                    state.updateState.subtitle(context),
                    modifier = if (state.updateState.status == HomeUpdateUiState.Status.FAILED) {
                        Modifier.clickable {
                            confirmFeedback()
                            state.actions.retryUpdateCheck()
                        }
                    } else Modifier,
                    style = MaterialTheme.typography.bodyMedium,
                    color = content
                )
            }
        }
    }
}

@Composable
private fun HomeUpdateActions(state: HomeWorkspaceBinder.State) {
    val showReleaseNotes = rememberDpisConfirmAction(state.actions::showReleaseNotes)
    val applyUpdate = rememberDpisConfirmAction {
        if (state.updateState.status == HomeUpdateUiState.Status.INSTALL_READY) {
            state.actions.installDownloadedUpdate()
        } else {
            state.actions.startUpdateDownload()
        }
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.home_update_available, state.updateState.versionName), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (state.updateState.status == HomeUpdateUiState.Status.DOWNLOADING) {
                LinearProgressIndicator(progress = { state.updateState.downloadProgress / 100f }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
            }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = showReleaseNotes) { Text(stringResource(R.string.home_update_action_release_notes)) }
                Button(
                    onClick = applyUpdate,
                    enabled = state.updateState.status != HomeUpdateUiState.Status.DOWNLOADING
                ) {
                    Text(stringResource(when (state.updateState.status) {
                        HomeUpdateUiState.Status.INSTALL_READY -> R.string.home_update_action_install_ready
                        HomeUpdateUiState.Status.DOWNLOADING -> R.string.home_update_action_downloading
                        else -> R.string.home_update_action_install
                    }))
                }
            }
        }
    }
}

@Composable
private fun HomeCountCard(modifier: Modifier, titleRes: Int, count: Int, onClick: () -> Unit) {
    val hapticClick = rememberDpisConfirmAction(onClick)
    Card(
        onClick = hapticClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            Text(stringResource(titleRes), modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HomeInfoCard(state: HomeWorkspaceBinder.State) {
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
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        rows.forEachIndexed { index, (titleRes, value) ->
            val shape = when (index) {
                0 -> RoundedCornerShape(16.dp, 16.dp, 3.dp, 3.dp)
                rows.lastIndex -> RoundedCornerShape(3.dp, 3.dp, 16.dp, 16.dp)
                else -> RoundedCornerShape(3.dp)
            }
            HomeInfoRow(titleRes, value, shape)
        }
    }
}

@Composable
private fun HomeInfoRow(titleRes: Int, value: String, shape: RoundedCornerShape) {
    Card(
        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(titleRes), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(value, modifier = Modifier.padding(start = 16.dp).weight(1.2f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HomeNavigationEntry(titleRes: Int, summaryRes: Int, onClick: () -> Unit) {
    val hapticClick = rememberDpisConfirmAction(onClick)
    Card(
        onClick = hapticClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = CardDefaults.outlinedCardBorder()
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

@Composable
private fun HomeFeedbackEntry(context: android.content.Context) {
    Card(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_feedback_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.home_feedback_summary), modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HomeFeedbackAction(R.drawable.ic_github_24, R.string.home_feedback_github, context.getString(R.string.about_issues_url), context)
            HomeFeedbackAction(R.drawable.ic_qq_24, R.string.home_feedback_qq, context.getString(R.string.home_feedback_qq_url), context)
            HomeFeedbackAction(R.drawable.ic_telegram_24, R.string.home_feedback_telegram, context.getString(R.string.home_feedback_telegram_url), context)
        }
    }
}

@Composable
private fun HomeFeedbackAction(@androidx.annotation.DrawableRes iconRes: Int, @androidx.annotation.StringRes descriptionRes: Int, url: String, context: android.content.Context) {
    val openFeedbackTarget = rememberDpisConfirmAction {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button) {
            openFeedbackTarget()
        },
        contentAlignment = Alignment.Center
    ) {
        Icon(painterResource(iconRes), stringResource(descriptionRes), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
